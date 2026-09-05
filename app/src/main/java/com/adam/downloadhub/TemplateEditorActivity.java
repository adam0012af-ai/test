package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateEditorActivity extends Activity {
    private static final int PICK_MEDIA=501;
    private static final int PICK_AUDIO=502;

    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> undoStack=new ArrayDeque<>();
    private final ArrayDeque<String> redoStack=new ArrayDeque<>();

    private CreatorProject project;
    private EditText name,hook,body,cta,hashtags;
    private FrameLayout previewFrame;
    private VideoView videoPreview;
    private ImageView imagePreview;
    private TextView emptyPreview,previewHook,previewBody,previewCta,mediaStatus,exportStatus,timeLabel,clipInfo,audioStatus,ratioChip;
    private Button exportButton,playButton,speedButton,ratioButton,textToggleButton,transitionButton;
    private SeekBar timeline,sourceVolume,audioVolume,textScale,textY;
    private LinearLayout clipStrip;
    private MediaPlayer activeVideoPlayer,externalAudioPlayer;
    private boolean videoPrepared=false,previewIsVideo=false,timelineTouching=false,restoring=false;
    private long mediaDurationMs=0;
    private int colorIndex=0;
    private final int[] textColors={Color.WHITE,0xFF8DEBFF,0xFFFFE38A,0xFFFFA7D7,0xFF9DFFB1};
    private final float[] speeds={0.5f,0.75f,1f,1.25f,1.5f,2f};
    private final String[] ratios={"9:16","1:1","16:9"};
    private final String[] transitions={"None","Fade","Soft"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        loadProject();
        setContentView(buildUi());
        restoreUiFromProject();
        refreshClipStrip();
        bindCurrentClip();
        handler.post(ticker);
        if(getIntent().getBooleanExtra("pick_media",false)&&project.clips.isEmpty())previewFrame.postDelayed(this::pickMedia,300);
    }

    @Override protected void onPause(){
        pausePlayback();
        super.onPause();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        releaseExternalAudio();
        try{if(videoPreview!=null)videoPreview.stopPlayback();}catch(Exception ignored){}
        executor.shutdownNow();
        super.onDestroy();
    }

    private void loadProject(){
        String draft=getIntent().getStringExtra("draft_id");
        if(draft!=null)project=DraftStore.get(this,draft);
        if(project==null){
            String id=getIntent().getStringExtra("template_id");
            ReelTemplate t=TemplateCatalog.byId(id);
            if(t==null)t=TemplateCatalog.byId("islamic-0");
            project=CreatorProject.fromTemplate(t);
            if("blank".equals(id)){
                project.categoryKey="creator";project.categoryName="مشروع جديد";project.name="مشروع جديد";
                project.hook="";project.body="";project.cta="";project.hashtags="";
            }
        }
        project.ensureClips();
    }

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,34));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.ghost(this,"‹");back.setTextSize(24);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.addView(Ui.text(this,"Video Studio",21,Ui.TEXT,true));titleBox.addView(Ui.text(this,"Timeline Editor • Preview • Export",10,Ui.CYAN,true));top.addView(titleBox,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button undo=Ui.ghost(this,"↶");undo.setOnClickListener(v->undo());top.addView(undo,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));
        Button redo=Ui.ghost(this,"↷");redo.setOnClickListener(v->redo());top.addView(redo,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));
        Button menu=Ui.ghost(this,"☰");menu.setOnClickListener(v->AppSideMenu.show(this));top.addView(menu,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));
        root.addView(top);

        previewFrame=new FrameLayout(this);previewFrame.setBackground(Ui.gradient(0xFF050B15,0xFF111A29,24,this));
        LinearLayout.LayoutParams pp=Ui.matchWrap();pp.height=Ui.dp(this,480);pp.setMargins(0,Ui.dp(this,10),0,0);root.addView(previewFrame,pp);
        videoPreview=new VideoView(this);videoPreview.setVisibility(View.GONE);previewFrame.addView(videoPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        imagePreview=new ImageView(this);imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);imagePreview.setVisibility(View.GONE);previewFrame.addView(imagePreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        emptyPreview=Ui.text(this,"+ أضف فيديو أو صورة لبدء المونتاج",16,Ui.MUTED,true);emptyPreview.setGravity(Gravity.CENTER);previewFrame.addView(emptyPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);overlay.setGravity(Gravity.CENTER);overlay.setPadding(Ui.dp(this,20),Ui.dp(this,28),Ui.dp(this,20),Ui.dp(this,28));previewFrame.addView(overlay,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        ratioChip=Ui.chip(this,project.aspectRatio,Ui.CYAN);overlay.addView(ratioChip);
        previewHook=Ui.text(this,project.hook,24,project.textColor,true);previewHook.setGravity(Gravity.CENTER);previewHook.setShadowLayer(7,0,2,Color.BLACK);LinearLayout.LayoutParams h=Ui.matchWrap();h.setMargins(0,Ui.dp(this,34),0,Ui.dp(this,10));overlay.addView(previewHook,h);
        previewBody=Ui.text(this,project.body,15,project.textColor,false);previewBody.setGravity(Gravity.CENTER);previewBody.setShadowLayer(6,0,2,Color.BLACK);previewBody.setMaxLines(8);overlay.addView(previewBody);
        previewCta=Ui.text(this,project.cta,13,project.textColor,true);previewCta.setGravity(Gravity.CENTER);previewCta.setShadowLayer(6,0,2,Color.BLACK);LinearLayout.LayoutParams ct=Ui.matchWrap();ct.setMargins(0,Ui.dp(this,20),0,0);overlay.addView(previewCta,ct);

        LinearLayout transport=new LinearLayout(this);transport.setOrientation(LinearLayout.HORIZONTAL);transport.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams tr=Ui.matchWrap();tr.setMargins(0,Ui.dp(this,8),0,0);
        Button minus=Ui.ghost(this,"-5s");minus.setOnClickListener(v->seekRelative(-5000));transport.addView(minus,new LinearLayout.LayoutParams(Ui.dp(this,66),Ui.dp(this,48)));
        playButton=Ui.accent(this,"▶");playButton.setTextSize(18);playButton.setOnClickListener(v->togglePlay());LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f);pl.setMargins(Ui.dp(this,6),0,Ui.dp(this,6),0);transport.addView(playButton,pl);
        Button plus=Ui.ghost(this,"+5s");plus.setOnClickListener(v->seekRelative(5000));transport.addView(plus,new LinearLayout.LayoutParams(Ui.dp(this,66),Ui.dp(this,48)));root.addView(transport,tr);

        timeLabel=Ui.text(this,"00:00.0 / 00:00.0",11,Ui.CYAN,true);timeLabel.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tl=Ui.matchWrap();tl.setMargins(0,Ui.dp(this,4),0,0);root.addView(timeLabel,tl);
        timeline=new SeekBar(this);timeline.setMax(1000);timeline.setProgress(0);timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){if(from)seekTo(p);}public void onStartTrackingTouch(SeekBar s){timelineTouching=true;}public void onStopTrackingTouch(SeekBar s){timelineTouching=false;}});root.addView(timeline,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,46)));

        LinearLayout timelineCard=Ui.card(this);LinearLayout.LayoutParams tcp=Ui.matchWrap();tcp.setMargins(0,Ui.dp(this,4),0,0);timelineCard.setLayoutParams(tcp);timelineCard.addView(Ui.text(this,"Timeline",15,Ui.TEXT,true));
        clipInfo=Ui.text(this,"",11,Ui.MUTED,false);LinearLayout.LayoutParams ci=Ui.matchWrap();ci.setMargins(0,Ui.dp(this,3),0,Ui.dp(this,8));timelineCard.addView(clipInfo,ci);
        HorizontalScrollView clipScroll=new HorizontalScrollView(this);clipScroll.setHorizontalScrollBarEnabled(false);clipStrip=new LinearLayout(this);clipStrip.setOrientation(LinearLayout.HORIZONTAL);clipScroll.addView(clipStrip,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));timelineCard.addView(clipScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,58)));
        root.addView(timelineCard);

        HorizontalScrollView toolsScroll=new HorizontalScrollView(this);toolsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);tools.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,8));toolsScroll.addView(tools,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        tools.addView(tool("＋ Media",v->pickMedia()));
        tools.addView(tool("✂ Split",v->splitClip()));
        tools.addView(tool("◁ Trim In",v->setTrimStart()));
        tools.addView(tool("Trim Out ▷",v->setTrimEnd()));
        tools.addView(tool("⌫ Delete",v->deleteClip()));
        tools.addView(tool("♫ Audio",v->pickAudio()));
        speedButton=tool("1.0× Speed",v->cycleSpeed());tools.addView(speedButton);
        ratioButton=tool("9:16 Ratio",v->cycleRatio());tools.addView(ratioButton);
        transitionButton=tool("Fade",v->cycleTransition());tools.addView(transitionButton);
        textToggleButton=tool("Text ON",v->toggleText());tools.addView(textToggleButton);
        root.addView(toolsScroll);

        LinearLayout clipPanel=Ui.card(this);LinearLayout.LayoutParams cpp=Ui.matchWrap();cpp.setMargins(0,Ui.dp(this,4),0,0);clipPanel.setLayoutParams(cpp);clipPanel.addView(Ui.text(this,"Clip Controls",16,Ui.TEXT,true));
        mediaStatus=Ui.text(this,"",11,Ui.MUTED,false);LinearLayout.LayoutParams ms=Ui.matchWrap();ms.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,8));clipPanel.addView(mediaStatus,ms);
        sourceVolume=sliderRow(clipPanel,"Original volume",0,100,100,v->{EditorClip c=currentClip();if(c!=null){c.volume=v;applyVideoVolume();}});
        LinearLayout imageDur=new LinearLayout(this);imageDur.setOrientation(LinearLayout.HORIZONTAL);imageDur.setGravity(Gravity.CENTER_VERTICAL);TextView dl=Ui.text(this,"Image duration",11,Ui.MUTED,true);imageDur.addView(dl,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button d3=Ui.ghost(this,"3s");d3.setOnClickListener(v->setImageDuration(3000));imageDur.addView(d3,new LinearLayout.LayoutParams(Ui.dp(this,62),Ui.dp(this,42)));Button d5=Ui.ghost(this,"5s");d5.setOnClickListener(v->setImageDuration(5000));imageDur.addView(d5,new LinearLayout.LayoutParams(Ui.dp(this,62),Ui.dp(this,42)));Button d10=Ui.ghost(this,"10s");d10.setOnClickListener(v->setImageDuration(10000));imageDur.addView(d10,new LinearLayout.LayoutParams(Ui.dp(this,68),Ui.dp(this,42)));clipPanel.addView(imageDur);
        root.addView(clipPanel);

        LinearLayout audioPanel=Ui.card(this);LinearLayout.LayoutParams ap=Ui.matchWrap();ap.setMargins(0,Ui.dp(this,10),0,0);audioPanel.setLayoutParams(ap);audioPanel.addView(Ui.text(this,"Audio Mix",16,Ui.TEXT,true));audioStatus=Ui.text(this,"No extra audio",11,Ui.MUTED,false);LinearLayout.LayoutParams as=Ui.matchWrap();as.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,8));audioPanel.addView(audioStatus,as);
        audioVolume=sliderRow(audioPanel,"Music volume",0,100,project.audioVolume,v->{project.audioVolume=v;applyAudioVolume();});
        LinearLayout offset=new LinearLayout(this);offset.setOrientation(LinearLayout.HORIZONTAL);offset.setGravity(Gravity.CENTER_VERTICAL);TextView ol=Ui.text(this,"Audio timing",11,Ui.MUTED,true);offset.addView(ol,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Button om=Ui.ghost(this,"-1s");om.setOnClickListener(v->changeAudioOffset(-1000));offset.addView(om,new LinearLayout.LayoutParams(Ui.dp(this,68),Ui.dp(this,42)));Button op=Ui.ghost(this,"+1s");op.setOnClickListener(v->changeAudioOffset(1000));offset.addView(op,new LinearLayout.LayoutParams(Ui.dp(this,68),Ui.dp(this,42)));Button rm=Ui.ghost(this,"Remove");rm.setOnClickListener(v->removeAudio());offset.addView(rm,new LinearLayout.LayoutParams(Ui.dp(this,82),Ui.dp(this,42)));audioPanel.addView(offset);root.addView(audioPanel);

        LinearLayout textPanel=Ui.card(this);LinearLayout.LayoutParams txp=Ui.matchWrap();txp.setMargins(0,Ui.dp(this,10),0,0);textPanel.setLayoutParams(txp);textPanel.addView(Ui.text(this,"Text & Captions",16,Ui.TEXT,true));
        name=field(textPanel,"Project name",project.name,false);hook=field(textPanel,"Headline / Hook",project.hook,true);body=field(textPanel,"Main text",project.body,true);cta=field(textPanel,"CTA / Ending",project.cta,true);hashtags=field(textPanel,"Hashtags",project.hashtags,true);
        textScale=sliderRow(textPanel,"Text size",60,180,project.textScale,v->{project.textScale=v;applyTextStyle();});
        textY=sliderRow(textPanel,"Text vertical position",10,90,project.textYPercent,v->{project.textYPercent=v;applyTextStyle();});
        LinearLayout textButtons=new LinearLayout(this);textButtons.setOrientation(LinearLayout.HORIZONTAL);Button color=Ui.secondary(this,"Text Color");color.setOnClickListener(v->cycleTextColor());textButtons.addView(color,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));Button style=Ui.secondary(this,"Caption Style");style.setOnClickListener(v->cycleCaptionStyle());LinearLayout.LayoutParams sty=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);sty.setMargins(Ui.dp(this,8),0,0,0);textButtons.addView(style,sty);LinearLayout.LayoutParams tbp=Ui.matchWrap();tbp.setMargins(0,Ui.dp(this,8),0,0);textPanel.addView(textButtons,tbp);root.addView(textPanel);

        LinearLayout effects=Ui.card(this);LinearLayout.LayoutParams efp=Ui.matchWrap();efp.setMargins(0,Ui.dp(this,10),0,0);effects.setLayoutParams(efp);effects.addView(Ui.text(this,"Effects & Export",16,Ui.TEXT,true));
        LinearLayout fades=new LinearLayout(this);fades.setOrientation(LinearLayout.HORIZONTAL);Button fi=Ui.secondary(this,"Fade In");fi.setOnClickListener(v->cycleFade(true));fades.addView(fi,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));Button fo=Ui.secondary(this,"Fade Out");fo.setOnClickListener(v->cycleFade(false));LinearLayout.LayoutParams fop=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);fop.setMargins(Ui.dp(this,8),0,0,0);fades.addView(fo,fop);effects.addView(fades);
        Button save=Ui.secondary(this,"Save Draft");save.setOnClickListener(v->saveDraft(true));LinearLayout.LayoutParams sv=Ui.matchWrap();sv.height=Ui.dp(this,50);sv.setMargins(0,Ui.dp(this,8),0,0);effects.addView(save,sv);
        exportButton=Ui.accent(this,"EXPORT MP4");exportButton.setTextSize(17);exportButton.setOnClickListener(v->export());LinearLayout.LayoutParams ex=Ui.matchWrap();ex.height=Ui.dp(this,62);ex.setMargins(0,Ui.dp(this,8),0,0);effects.addView(exportButton,ex);
        exportStatus=Ui.text(this,"Ready",12,Ui.GREEN,true);exportStatus.setGravity(Gravity.CENTER);LinearLayout.LayoutParams es=Ui.matchWrap();es.setMargins(0,Ui.dp(this,8),0,0);effects.addView(exportStatus,es);root.addView(effects);
        return scroll;
    }

    private Button tool(String text,View.OnClickListener l){Button b=Ui.secondary(this,text);b.setOnClickListener(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,116),Ui.dp(this,50));p.setMargins(0,0,Ui.dp(this,7),0);b.setLayoutParams(p);return b;}

    private SeekBar sliderRow(LinearLayout parent,String label,int min,int max,int value,SliderListener listener){
        TextView t=Ui.text(this,label+"  "+value,11,Ui.MUTED,true);LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,7),0,0);parent.addView(t,tp);
        SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,value-min)));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean from){int v=progress+min;t.setText(label+"  "+v);if(from)listener.onValue(v);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){saveDraft(false);}});parent.addView(s,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42)));return s;
    }
    private interface SliderListener{void onValue(int value);}

    private EditText field(LinearLayout parent,String label,String value,boolean multi){TextView l=Ui.text(this,label,11,Ui.MUTED,true);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,9),0,Ui.dp(this,4));parent.addView(l,lp);EditText e=Ui.input(this,"",multi);e.setText(value==null?"":value);LinearLayout.LayoutParams ep=Ui.matchWrap();ep.height=Ui.dp(this,multi?82:50);parent.addView(e,ep);e.addTextChangedListener(textWatcher);return e;}

    private final TextWatcher textWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(!restoring)updatePreviewText();}public void afterTextChanged(Editable e){}};

    private EditorClip currentClip(){project.ensureClips();if(project.clips.isEmpty())return null;if(project.selectedClipIndex<0||project.selectedClipIndex>=project.clips.size())project.selectedClipIndex=0;return project.clips.get(project.selectedClipIndex);}

    private void refreshClipStrip(){
        if(clipStrip==null)return;clipStrip.removeAllViews();project.ensureClips();
        for(int i=0;i<project.clips.size();i++){final int index=i;EditorClip c=project.clips.get(i);String label=(i+1)+"  "+clipLabel(c);Button b=index==project.selectedClipIndex?Ui.accent(this,label):Ui.secondary(this,label);b.setOnClickListener(v->{pausePlayback();project.selectedClipIndex=index;refreshClipStrip();bindCurrentClip();saveDraft(false);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,132),Ui.dp(this,50));p.setMargins(0,0,Ui.dp(this,7),0);clipStrip.addView(b,p);}
        Button add=Ui.ghost(this,"＋ Add");add.setOnClickListener(v->pickMedia());clipStrip.addView(add,new LinearLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,50)));
        updateClipInfo();
    }

    private String clipLabel(EditorClip c){String u=c==null||c.uri==null?"":c.uri.toLowerCase(Locale.ROOT);return isImageUri(u)?"IMAGE":"VIDEO";}

    private void bindCurrentClip(){
        pausePlayback();videoPrepared=false;activeVideoPlayer=null;mediaDurationMs=0;previewIsVideo=false;
        try{videoPreview.stopPlayback();}catch(Exception ignored){}
        videoPreview.setVisibility(View.GONE);imagePreview.setVisibility(View.GONE);
        EditorClip c=currentClip();
        if(c==null||c.uri==null||c.uri.trim().isEmpty()){
            emptyPreview.setVisibility(View.VISIBLE);timeline.setMax(1000);timeline.setProgress(0);playButton.setEnabled(false);mediaStatus.setText("No media selected");updateClipInfo();return;
        }
        emptyPreview.setVisibility(View.GONE);Uri uri=Uri.parse(c.uri);String type=mimeOrUri(uri,c.uri);previewIsVideo=type.startsWith("video/")||isVideoUri(type);
        sourceVolume.setProgress(Math.max(0,Math.min(100,c.volume)));
        speedButton.setText(String.format(Locale.US,"%.2g× Speed",c.speed));
        if(previewIsVideo){
            videoPreview.setVisibility(View.VISIBLE);videoPreview.setVideoURI(uri);videoPreview.setOnPreparedListener(mp->{activeVideoPlayer=mp;videoPrepared=true;mediaDurationMs=Math.max(1,videoPreview.getDuration());if(c.trimEndMs<=c.trimStartMs||c.trimEndMs>mediaDurationMs)c.trimEndMs=mediaDurationMs;applyVideoVolume();applyPlaybackSpeed();timeline.setMax((int)Math.min(Integer.MAX_VALUE,mediaDurationMs));videoPreview.seekTo((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));timeline.setProgress((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));playButton.setEnabled(true);updateTimeLabel(c.trimStartMs);updateClipInfo();});videoPreview.setOnErrorListener((mp,w,e)->{videoPrepared=false;toast("Preview failed for this format. Export engine may still support it.");return true;});
        }else{
            imagePreview.setVisibility(View.VISIBLE);imagePreview.setImageURI(uri);mediaDurationMs=Math.max(1000,c.stillDurationMs);timeline.setMax((int)mediaDurationMs);timeline.setProgress(0);playButton.setEnabled(false);updateTimeLabel(0);updateClipInfo();
        }
        mediaStatus.setText("Clip "+(project.selectedClipIndex+1)+" / "+project.clips.size()+" • "+clipLabel(c));
        applyTextStyle();
    }

    private String mimeOrUri(Uri uri,String fallback){try{String t=getContentResolver().getType(uri);if(t!=null)return t.toLowerCase(Locale.ROOT);}catch(Exception ignored){}return fallback==null?"":fallback.toLowerCase(Locale.ROOT);}
    private boolean isVideoUri(String s){return s.contains(".mp4")||s.contains(".webm")||s.contains(".mkv")||s.contains(".mov")||s.contains(".m4v");}
    private boolean isImageUri(String s){return s.contains("image/")||s.endsWith(".jpg")||s.endsWith(".jpeg")||s.endsWith(".png")||s.endsWith(".webp");}

    private void togglePlay(){if(!videoPrepared){toast("Select a video clip first");return;}if(videoPreview.isPlaying()){pausePlayback();}else{EditorClip c=currentClip();if(c==null)return;int pos=videoPreview.getCurrentPosition();if(pos<c.trimStartMs||pos>=effectiveEnd(c))videoPreview.seekTo((int)c.trimStartMs);applyPlaybackSpeed();applyVideoVolume();videoPreview.start();startExternalAudio();playButton.setText("❚❚");}}
    private void pausePlayback(){try{if(videoPreview!=null&&videoPreview.isPlaying())videoPreview.pause();}catch(Exception ignored){}try{if(externalAudioPlayer!=null&&externalAudioPlayer.isPlaying())externalAudioPlayer.pause();}catch(Exception ignored){}if(playButton!=null)playButton.setText("▶");}

    private void seekRelative(long delta){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition()+delta;p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c)-50,p));videoPreview.seekTo((int)p);timeline.setProgress((int)p);syncExternalAudioToProjectPosition();updateTimeLabel(p);}
    private void seekTo(int value){if(!videoPrepared)return;EditorClip c=currentClip();long p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c),value));videoPreview.seekTo((int)p);syncExternalAudioToProjectPosition();updateTimeLabel(p);}
    private long effectiveEnd(EditorClip c){if(c==null)return mediaDurationMs;long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;return Math.max(c.trimStartMs+100,Math.min(mediaDurationMs,end));}

    private void setTrimStart(){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p>=effectiveEnd(c)-300){toast("Move playhead earlier");return;}pushUndo();c.trimStartMs=p;saveDraft(false);updateClipInfo();toast("Trim start set");}
    private void setTrimEnd(){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p<=c.trimStartMs+300){toast("Move playhead later");return;}pushUndo();c.trimEndMs=p;saveDraft(false);updateClipInfo();toast("Trim end set");}

    private void splitClip(){
        if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();long end=effectiveEnd(c);if(p<=c.trimStartMs+300||p>=end-300){toast("Move playhead inside the clip before Split");return;}pushUndo();EditorClip right=c.copy();right.trimStartMs=p;right.trimEndMs=end;c.trimEndMs=p;project.clips.add(project.selectedClipIndex+1,right);project.selectedClipIndex++;saveDraft(false);refreshClipStrip();bindCurrentClip();toast("Clip split into two parts ✓");
    }

    private void deleteClip(){EditorClip c=currentClip();if(c==null)return;pushUndo();project.clips.remove(project.selectedClipIndex);if(project.selectedClipIndex>=project.clips.size())project.selectedClipIndex=Math.max(0,project.clips.size()-1);project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();}

    private void pickMedia(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK_MEDIA);}
    private void pickAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");startActivityForResult(i,PICK_AUDIO);}

    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        if(request==PICK_MEDIA){ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){ClipData clips=data.getClipData();for(int x=0;x<clips.getItemCount();x++)uris.add(clips.getItemAt(x).getUri());}else if(data.getData()!=null)uris.add(data.getData());if(uris.isEmpty())return;pushUndo();for(Uri uri:uris){persist(uri,data);EditorClip c=new EditorClip(uri.toString());project.clips.add(c);}project.selectedClipIndex=Math.max(0,project.clips.size()-uris.size());project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();}
        else if(request==PICK_AUDIO&&data.getData()!=null){pushUndo();Uri uri=data.getData();persist(uri,data);project.audioUri=uri.toString();releaseExternalAudio();saveDraft(false);updateAudioStatus();toast("Audio added ✓");}
    }
    private void persist(Uri uri,Intent data){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}}

    private void cycleSpeed(){EditorClip c=currentClip();if(c==null)return;pushUndo();int index=0;float best=999;for(int i=0;i<speeds.length;i++){float d=Math.abs(speeds[i]-c.speed);if(d<best){best=d;index=i;}}c.speed=speeds[(index+1)%speeds.length];speedButton.setText(String.format(Locale.US,"%.2g× Speed",c.speed));applyPlaybackSpeed();saveDraft(false);updateClipInfo();}
    private void applyPlaybackSpeed(){if(activeVideoPlayer==null)return;EditorClip c=currentClip();try{activeVideoPlayer.setPlaybackParams(activeVideoPlayer.getPlaybackParams().setSpeed(c==null?1f:c.speed));}catch(Exception ignored){}}
    private void applyVideoVolume(){EditorClip c=currentClip();if(activeVideoPlayer==null||c==null)return;float v=c.volume/100f;try{activeVideoPlayer.setVolume(v,v);}catch(Exception ignored){}}

    private void cycleRatio(){pushUndo();int i=0;for(int x=0;x<ratios.length;x++)if(ratios[x].equals(project.aspectRatio))i=x;project.aspectRatio=ratios[(i+1)%ratios.length];ratioButton.setText(project.aspectRatio+" Ratio");ratioChip.setText(project.aspectRatio);applyPreviewRatio();saveDraft(false);}
    private void applyPreviewRatio(){ViewGroup.LayoutParams raw=previewFrame.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)raw;if("1:1".equals(project.aspectRatio))p.height=Ui.dp(this,360);else if("16:9".equals(project.aspectRatio))p.height=Ui.dp(this,240);else p.height=Ui.dp(this,480);previewFrame.setLayoutParams(p);}}

    private void cycleTransition(){pushUndo();int i=0;for(int x=0;x<transitions.length;x++)if(transitions[x].equals(project.transitionStyle))i=x;project.transitionStyle=transitions[(i+1)%transitions.length];transitionButton.setText(project.transitionStyle);saveDraft(false);}
    private void cycleFade(boolean in){pushUndo();int old=in?project.fadeInMs:project.fadeOutMs;int next=old==0?250:old==250?500:old==500?1000:0;if(in)project.fadeInMs=next;else project.fadeOutMs=next;toast((in?"Fade In: ":"Fade Out: ")+next+"ms");saveDraft(false);}

    private void toggleText(){pushUndo();project.showText=!project.showText;textToggleButton.setText(project.showText?"Text ON":"Text OFF");applyTextStyle();saveDraft(false);}
    private void cycleTextColor(){pushUndo();colorIndex=(colorIndex+1)%textColors.length;project.textColor=textColors[colorIndex];applyTextStyle();saveDraft(false);}
    private void cycleCaptionStyle(){pushUndo();String s=project.captionStyle==null?"Bold Highlight":project.captionStyle;if("Bold Highlight".equals(s))project.captionStyle="Clean";else if("Clean".equals(s))project.captionStyle="Subtitle";else project.captionStyle="Bold Highlight";applyTextStyle();saveDraft(false);toast(project.captionStyle);}

    private void updatePreviewText(){if(hook==null)return;project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);project.name=text(name);if(previewHook!=null)previewHook.setText(project.hook);if(previewBody!=null)previewBody.setText(project.body);if(previewCta!=null)previewCta.setText(project.cta);}
    private void applyTextStyle(){if(previewHook==null)return;float k=project.textScale/100f;previewHook.setTextSize(24*k);previewBody.setTextSize(15*k);previewCta.setTextSize(13*k);previewHook.setTextColor(project.textColor);previewBody.setTextColor(project.textColor);previewCta.setTextColor(project.textColor);int visibility=project.showText?View.VISIBLE:View.GONE;previewHook.setVisibility(visibility);previewBody.setVisibility(visibility);previewCta.setVisibility(visibility);float y=(project.textYPercent-50)*5f;previewHook.setTranslationY(y);previewBody.setTranslationY(y);previewCta.setTranslationY(y);boolean bold="Bold Highlight".equals(project.captionStyle);previewHook.setTypeface(null,bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);}

    private void setImageDuration(long ms){EditorClip c=currentClip();if(c==null)return;pushUndo();c.stillDurationMs=ms;mediaDurationMs=ms;timeline.setMax((int)ms);saveDraft(false);updateClipInfo();}

    private void updateClipInfo(){if(clipInfo==null)return;EditorClip c=currentClip();if(c==null){clipInfo.setText("Timeline is empty");return;}long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;long raw=previewIsVideo?Math.max(0,end-c.trimStartMs):c.stillDurationMs;long out=(long)(raw/Math.max(.5f,c.speed));clipInfo.setText("Clip "+(project.selectedClipIndex+1)+" / "+project.clips.size()+" • trim "+fmt(c.trimStartMs)+" → "+(end>0?fmt(end):"end")+" • "+String.format(Locale.US,"%.2g×",c.speed)+" • approx "+fmt(out));}

    private void changeAudioOffset(long delta){pushUndo();project.audioOffsetMs=Math.max(-15000,Math.min(15000,project.audioOffsetMs+delta));saveDraft(false);updateAudioStatus();syncExternalAudioToProjectPosition();}
    private void removeAudio(){pushUndo();project.audioUri="";releaseExternalAudio();saveDraft(false);updateAudioStatus();}
    private void updateAudioStatus(){if(audioStatus==null)return;audioStatus.setText((project.audioUri==null||project.audioUri.trim().isEmpty()?"No extra audio":"Extra audio added ✓")+" • offset "+String.format(Locale.US,"%.1fs",project.audioOffsetMs/1000f));if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);}
    private void startExternalAudio(){if(project.audioUri==null||project.audioUri.trim().isEmpty())return;try{if(externalAudioPlayer==null){externalAudioPlayer=new MediaPlayer();externalAudioPlayer.setDataSource(this,Uri.parse(project.audioUri));externalAudioPlayer.setLooping(true);externalAudioPlayer.prepare();}applyAudioVolume();syncExternalAudioToProjectPosition();externalAudioPlayer.start();}catch(Exception e){releaseExternalAudio();toast("Could not preview added audio");}}
    private void syncExternalAudioToProjectPosition(){if(externalAudioPlayer==null)return;try{long p=projectPlayheadMs()-project.audioOffsetMs;if(p<0){externalAudioPlayer.pause();return;}int d=externalAudioPlayer.getDuration();if(d>0)p%=d;externalAudioPlayer.seekTo((int)p);}catch(Exception ignored){}}
    private void applyAudioVolume(){if(externalAudioPlayer==null)return;float v=project.audioVolume/100f;try{externalAudioPlayer.setVolume(v,v);}catch(Exception ignored){}}
    private void releaseExternalAudio(){try{if(externalAudioPlayer!=null){externalAudioPlayer.stop();externalAudioPlayer.release();}}catch(Exception ignored){}externalAudioPlayer=null;}

    private long projectPlayheadMs(){long total=0;for(int i=0;i<project.clips.size();i++){EditorClip c=project.clips.get(i);if(i<project.selectedClipIndex)total+=estimateClipDuration(c);else if(i==project.selectedClipIndex){long pos=videoPrepared?videoPreview.getCurrentPosition():0;total+=(long)(Math.max(0,pos-c.trimStartMs)/Math.max(.5f,c.speed));break;}}return total;}
    private long estimateClipDuration(EditorClip c){long raw;if(isImageUri(c.uri==null?"":c.uri.toLowerCase(Locale.ROOT)))raw=c.stillDurationMs;else{long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:c.trimStartMs+5000;raw=Math.max(500,end-c.trimStartMs);}return (long)(raw/Math.max(.5f,c.speed));}

    private final Runnable ticker=new Runnable(){@Override public void run(){try{if(videoPrepared&&!timelineTouching){EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();long end=effectiveEnd(c);if(videoPreview.isPlaying()&&p>=end-60){videoPreview.seekTo((int)c.trimStartMs);syncExternalAudioToProjectPosition();p=c.trimStartMs;}timeline.setProgress((int)Math.min(Integer.MAX_VALUE,p));updateTimeLabel(p);}}catch(Exception ignored){}handler.postDelayed(this,120);}};
    private void updateTimeLabel(long p){if(timeLabel==null)return;timeLabel.setText(fmt(p)+" / "+fmt(mediaDurationMs));}
    private String fmt(long ms){if(ms<0)ms=0;long min=ms/60000;double sec=(ms%60000)/1000.0;return String.format(Locale.US,"%02d:%04.1f",min,sec);}

    private void pullFields(){if(name!=null){project.name=text(name);project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);}if(project.name==null||project.name.trim().isEmpty())project.name="Video Project";project.syncPrimarySource();}
    private void saveDraft(boolean notify){pullFields();DraftStore.save(this,project);if(notify)toast("Draft saved ✓");}

    private void pushUndo(){try{String s=project.toJson().toString();if(undoStack.size()>=20)undoStack.removeFirst();undoStack.addLast(s);redoStack.clear();}catch(Exception ignored){}}
    private void undo(){if(undoStack.isEmpty()){toast("Nothing to undo");return;}try{redoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(undoStack.removeLast()));restoreUiFromProject();refreshClipStrip();bindCurrentClip();}catch(Exception e){toast("Undo failed");}}
    private void redo(){if(redoStack.isEmpty()){toast("Nothing to redo");return;}try{undoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(redoStack.removeLast()));restoreUiFromProject();refreshClipStrip();bindCurrentClip();}catch(Exception e){toast("Redo failed");}}

    private void restoreUiFromProject(){restoring=true;try{project.ensureClips();if(name!=null)name.setText(project.name==null?"":project.name);if(hook!=null)hook.setText(project.hook==null?"":project.hook);if(body!=null)body.setText(project.body==null?"":project.body);if(cta!=null)cta.setText(project.cta==null?"":project.cta);if(hashtags!=null)hashtags.setText(project.hashtags==null?"":project.hashtags);if(textScale!=null)textScale.setProgress(project.textScale-60);if(textY!=null)textY.setProgress(project.textYPercent-10);if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);if(ratioButton!=null)ratioButton.setText(project.aspectRatio+" Ratio");if(ratioChip!=null)ratioChip.setText(project.aspectRatio);if(transitionButton!=null)transitionButton.setText(project.transitionStyle);if(textToggleButton!=null)textToggleButton.setText(project.showText?"Text ON":"Text OFF");updatePreviewText();applyTextStyle();applyPreviewRatio();updateAudioStatus();}finally{restoring=false;}}

    private void export(){
        pullFields();project.ensureClips();if(project.clips.isEmpty()){toast("Add at least one video or image");return;}DraftStore.save(this,project);pausePlayback();exportButton.setEnabled(false);exportButton.setAlpha(.55f);exportStatus.setTextColor(Ui.YELLOW);exportStatus.setText("Preparing timeline…");
        executor.execute(()->ReelExporter.export(this,project,new ReelExporter.Callback(){public void onStage(String s){runOnUiThread(()->exportStatus.setText(s));}public void onSuccess(File file){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.GREEN);exportStatus.setText("Export complete ✓");Intent i=new Intent(TemplateEditorActivity.this,PublishingActivity.class);i.putExtra("project_id",project.id);i.putExtra("video_path",file.getAbsolutePath());startActivity(i);});}public void onError(String m){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.RED);exportStatus.setText(m);toast(m);});}}));
    }

    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
