package com.adam.downloadhub;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateEditorActivity extends Activity {
    private static final int PICK_MEDIA=501;
    private static final int PICK_AUDIO=502;
    private static final int PICK_OVERLAY=503;
    private static final int REQ_MIC=710;

    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> undoStack=new ArrayDeque<>();
    private final ArrayDeque<String> redoStack=new ArrayDeque<>();

    private CreatorProject project;
    private EditText name,hook,body,cta,hashtags;
    private FrameLayout previewFrame;
    private VideoView videoPreview;
    private ImageView imagePreview,overlayPreview;
    private TextView emptyPreview,previewHook,previewBody,previewCta,mediaStatus,exportStatus,timeLabel,clipInfo,audioStatus,voiceStatus,overlayStatus,ratioChip,transformStatus;
    private Button exportButton,playButton,speedButton,ratioButton,textToggleButton,transitionButton,filterButton,mirrorButton,rotateButton,voiceButton,fontButton;
    private SeekBar timeline,sourceVolume,audioVolume,voiceVolume,textScale,textY,zoomSlider,overlayScale,overlayX,overlayY,overlayOpacity;
    private LinearLayout clipStrip;

    private MediaPlayer activeVideoPlayer,musicPlayer,voicePlayer;
    private MediaRecorder voiceRecorder;
    private File voiceRecordingFile;
    private long voiceRecordOffsetMs=0;

    private boolean videoPrepared=false,previewIsVideo=false,timelineTouching=false,restoring=false;
    private boolean playingProject=false,autoStartAfterBind=false,voiceRecording=false;
    private long mediaDurationMs=0,imagePlayStartedAt=0;
    private int colorIndex=0;

    private final int[] textColors={Color.WHITE,0xFF8DEBFF,0xFFFFE38A,0xFFFFA7D7,0xFF9DFFB1};
    private final float[] speeds={0.5f,0.75f,1f,1.25f,1.5f,2f};
    private final String[] ratios={"9:16","1:1","16:9"};
    private final String[] transitions={"None","Fade","Soft"};
    private final String[] filters={"None","Vivid","Mono","Soft","Contrast"};
    private final String[] fonts={"Sans","Serif","Mono"};

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
        if(voiceRecording)stopVoiceoverRecording();
        stopProjectPlayback(false);
        super.onPause();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        releaseAuxPlayers();
        releaseRecorder();
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
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.addView(Ui.text(this,"Video Studio Pro",21,Ui.TEXT,true));titleBox.addView(Ui.text(this,"Multi-track • Voiceover • Overlay • Export",10,Ui.CYAN,true));top.addView(titleBox,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button undo=Ui.ghost(this,"↶");undo.setOnClickListener(v->undo());top.addView(undo,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        Button redo=Ui.ghost(this,"↷");redo.setOnClickListener(v->redo());top.addView(redo,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        Button menu=Ui.ghost(this,"☰");menu.setOnClickListener(v->AppSideMenu.show(this));top.addView(menu,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));
        root.addView(top);

        previewFrame=new FrameLayout(this);previewFrame.setBackground(Ui.gradient(0xFF050B15,0xFF111A29,24,this));
        LinearLayout.LayoutParams pp=Ui.matchWrap();pp.height=Ui.dp(this,480);pp.setMargins(0,Ui.dp(this,10),0,0);root.addView(previewFrame,pp);
        videoPreview=new VideoView(this);videoPreview.setVisibility(View.GONE);previewFrame.addView(videoPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        imagePreview=new ImageView(this);imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);imagePreview.setVisibility(View.GONE);previewFrame.addView(imagePreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        emptyPreview=Ui.text(this,"+ أضف فيديو أو صورة لبدء المونتاج",16,Ui.MUTED,true);emptyPreview.setGravity(Gravity.CENTER);previewFrame.addView(emptyPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        overlayPreview=new ImageView(this);overlayPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);overlayPreview.setVisibility(View.GONE);previewFrame.addView(overlayPreview,new FrameLayout.LayoutParams(Ui.dp(this,120),Ui.dp(this,120)));

        LinearLayout overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);overlay.setGravity(Gravity.CENTER);overlay.setPadding(Ui.dp(this,20),Ui.dp(this,28),Ui.dp(this,20),Ui.dp(this,28));previewFrame.addView(overlay,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        ratioChip=Ui.chip(this,project.aspectRatio,Ui.CYAN);overlay.addView(ratioChip);
        previewHook=Ui.text(this,project.hook,24,project.textColor,true);previewHook.setGravity(Gravity.CENTER);previewHook.setShadowLayer(7,0,2,Color.BLACK);LinearLayout.LayoutParams h=Ui.matchWrap();h.setMargins(0,Ui.dp(this,34),0,Ui.dp(this,10));overlay.addView(previewHook,h);
        previewBody=Ui.text(this,project.body,15,project.textColor,false);previewBody.setGravity(Gravity.CENTER);previewBody.setShadowLayer(6,0,2,Color.BLACK);previewBody.setMaxLines(8);overlay.addView(previewBody);
        previewCta=Ui.text(this,project.cta,13,project.textColor,true);previewCta.setGravity(Gravity.CENTER);previewCta.setShadowLayer(6,0,2,Color.BLACK);LinearLayout.LayoutParams ct=Ui.matchWrap();ct.setMargins(0,Ui.dp(this,20),0,0);overlay.addView(previewCta,ct);

        LinearLayout transport=new LinearLayout(this);transport.setOrientation(LinearLayout.HORIZONTAL);transport.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams tr=Ui.matchWrap();tr.setMargins(0,Ui.dp(this,8),0,0);
        Button prev=Ui.ghost(this,"‹ Clip");prev.setOnClickListener(v->selectRelativeClip(-1));transport.addView(prev,new LinearLayout.LayoutParams(Ui.dp(this,78),Ui.dp(this,48)));
        playButton=Ui.accent(this,"▶ تشغيل المشروع");playButton.setTextSize(14);playButton.setOnClickListener(v->toggleProjectPlay());LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f);pl.setMargins(Ui.dp(this,6),0,Ui.dp(this,6),0);transport.addView(playButton,pl);
        Button next=Ui.ghost(this,"Clip ›");next.setOnClickListener(v->selectRelativeClip(1));transport.addView(next,new LinearLayout.LayoutParams(Ui.dp(this,78),Ui.dp(this,48)));root.addView(transport,tr);

        LinearLayout seekButtons=new LinearLayout(this);seekButtons.setOrientation(LinearLayout.HORIZONTAL);seekButtons.setGravity(Gravity.CENTER_VERTICAL);
        Button minus=Ui.ghost(this,"-5s");minus.setOnClickListener(v->seekRelative(-5000));seekButtons.addView(minus,new LinearLayout.LayoutParams(0,Ui.dp(this,42),1f));
        Button plus=Ui.ghost(this,"+5s");plus.setOnClickListener(v->seekRelative(5000));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,Ui.dp(this,42),1f);sp.setMargins(Ui.dp(this,8),0,0,0);seekButtons.addView(plus,sp);root.addView(seekButtons);

        timeLabel=Ui.text(this,"00:00.0 / 00:00.0",11,Ui.CYAN,true);timeLabel.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tl=Ui.matchWrap();tl.setMargins(0,Ui.dp(this,4),0,0);root.addView(timeLabel,tl);
        timeline=new SeekBar(this);timeline.setMax(1000);timeline.setProgress(0);timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){if(from)seekTo(p);}public void onStartTrackingTouch(SeekBar s){timelineTouching=true;}public void onStopTrackingTouch(SeekBar s){timelineTouching=false;}});root.addView(timeline,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,46)));

        LinearLayout timelineCard=Ui.card(this);LinearLayout.LayoutParams tcp=Ui.matchWrap();tcp.setMargins(0,Ui.dp(this,4),0,0);timelineCard.setLayoutParams(tcp);timelineCard.addView(Ui.text(this,"Timeline",15,Ui.TEXT,true));
        clipInfo=Ui.text(this,"",11,Ui.MUTED,false);LinearLayout.LayoutParams ci=Ui.matchWrap();ci.setMargins(0,Ui.dp(this,3),0,Ui.dp(this,8));timelineCard.addView(clipInfo,ci);
        HorizontalScrollView clipScroll=new HorizontalScrollView(this);clipScroll.setHorizontalScrollBarEnabled(false);clipStrip=new LinearLayout(this);clipStrip.setOrientation(LinearLayout.HORIZONTAL);clipScroll.addView(clipStrip,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));timelineCard.addView(clipScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,62)));
        root.addView(timelineCard);

        HorizontalScrollView toolsScroll=new HorizontalScrollView(this);toolsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);tools.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,8));toolsScroll.addView(tools,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        tools.addView(tool("＋ Media",v->pickMedia()));
        tools.addView(tool("✂ Split",v->splitClip()));
        tools.addView(tool("◁ Trim In",v->setTrimStart()));
        tools.addView(tool("Trim Out ▷",v->setTrimEnd()));
        tools.addView(tool("⧉ Duplicate",v->duplicateClip()));
        tools.addView(tool("← Move",v->moveClip(-1)));
        tools.addView(tool("Move →",v->moveClip(1)));
        tools.addView(tool("⌫ Delete",v->deleteClip()));
        tools.addView(tool("♫ Music",v->pickAudio()));
        tools.addView(tool("◇ Overlay",v->pickOverlay()));
        voiceButton=tool("● Voiceover",v->toggleVoiceoverRecording());tools.addView(voiceButton);
        speedButton=tool("1.0× Speed",v->cycleSpeed());tools.addView(speedButton);
        ratioButton=tool("9:16 Ratio",v->cycleRatio());tools.addView(ratioButton);
        transitionButton=tool("Fade",v->cycleTransition());tools.addView(transitionButton);
        textToggleButton=tool("Text ON",v->toggleText());tools.addView(textToggleButton);
        root.addView(toolsScroll);

        LinearLayout clipPanel=Ui.card(this);LinearLayout.LayoutParams cpp=Ui.matchWrap();cpp.setMargins(0,Ui.dp(this,4),0,0);clipPanel.setLayoutParams(cpp);clipPanel.addView(Ui.text(this,"Clip Controls",16,Ui.TEXT,true));
        mediaStatus=Ui.text(this,"",11,Ui.MUTED,false);LinearLayout.LayoutParams ms=Ui.matchWrap();ms.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,4));clipPanel.addView(mediaStatus,ms);
        transformStatus=Ui.text(this,"",11,Ui.CYAN,true);LinearLayout.LayoutParams tfs=Ui.matchWrap();tfs.setMargins(0,0,0,Ui.dp(this,6));clipPanel.addView(transformStatus,tfs);
        sourceVolume=sliderRow(clipPanel,"Original volume",0,100,100,v->{EditorClip c=currentClip();if(c!=null){c.volume=v;applyVideoVolume();}});
        zoomSlider=sliderRow(clipPanel,"Zoom",100,180,100,v->{EditorClip c=currentClip();if(c!=null){c.zoom=v;applyClipPreviewTransform();updateTransformStatus();}});
        LinearLayout transformButtons=new LinearLayout(this);transformButtons.setOrientation(LinearLayout.HORIZONTAL);
        rotateButton=Ui.secondary(this,"↻ Rotate");rotateButton.setOnClickListener(v->rotateClip());transformButtons.addView(rotateButton,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        mirrorButton=Ui.secondary(this,"⇋ Mirror");mirrorButton.setOnClickListener(v->toggleMirror());LinearLayout.LayoutParams mbp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);mbp.setMargins(Ui.dp(this,7),0,0,0);transformButtons.addView(mirrorButton,mbp);
        filterButton=Ui.secondary(this,"Filter");filterButton.setOnClickListener(v->cycleFilter());LinearLayout.LayoutParams fbp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);fbp.setMargins(Ui.dp(this,7),0,0,0);transformButtons.addView(filterButton,fbp);clipPanel.addView(transformButtons);
        LinearLayout imageDur=new LinearLayout(this);imageDur.setOrientation(LinearLayout.HORIZONTAL);imageDur.setGravity(Gravity.CENTER_VERTICAL);TextView dl=Ui.text(this,"Image duration",11,Ui.MUTED,true);imageDur.addView(dl,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button d3=Ui.ghost(this,"3s");d3.setOnClickListener(v->setImageDuration(3000));imageDur.addView(d3,new LinearLayout.LayoutParams(Ui.dp(this,62),Ui.dp(this,42)));Button d5=Ui.ghost(this,"5s");d5.setOnClickListener(v->setImageDuration(5000));imageDur.addView(d5,new LinearLayout.LayoutParams(Ui.dp(this,62),Ui.dp(this,42)));Button d10=Ui.ghost(this,"10s");d10.setOnClickListener(v->setImageDuration(10000));imageDur.addView(d10,new LinearLayout.LayoutParams(Ui.dp(this,68),Ui.dp(this,42)));clipPanel.addView(imageDur);
        root.addView(clipPanel);

        LinearLayout audioPanel=Ui.card(this);LinearLayout.LayoutParams ap=Ui.matchWrap();ap.setMargins(0,Ui.dp(this,10),0,0);audioPanel.setLayoutParams(ap);audioPanel.addView(Ui.text(this,"Audio Mixer",16,Ui.TEXT,true));
        audioStatus=Ui.text(this,"No music",11,Ui.MUTED,false);audioPanel.addView(audioStatus);
        audioVolume=sliderRow(audioPanel,"Music volume",0,100,project.audioVolume,v->{project.audioVolume=v;applyAuxVolumes();});
        LinearLayout offset=new LinearLayout(this);offset.setOrientation(LinearLayout.HORIZONTAL);offset.setGravity(Gravity.CENTER_VERTICAL);TextView ol=Ui.text(this,"Music timing",11,Ui.MUTED,true);offset.addView(ol,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Button om=Ui.ghost(this,"-1s");om.setOnClickListener(v->changeAudioOffset(-1000));offset.addView(om,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,42)));Button op=Ui.ghost(this,"+1s");op.setOnClickListener(v->changeAudioOffset(1000));offset.addView(op,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,42)));Button rm=Ui.ghost(this,"Remove");rm.setOnClickListener(v->removeAudio());offset.addView(rm,new LinearLayout.LayoutParams(Ui.dp(this,78),Ui.dp(this,42)));audioPanel.addView(offset);
        LinearLayout musicFade=new LinearLayout(this);musicFade.setOrientation(LinearLayout.HORIZONTAL);Button mfi=Ui.secondary(this,"Music Fade In");mfi.setOnClickListener(v->cycleMusicFade(true));musicFade.addView(mfi,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button mfo=Ui.secondary(this,"Music Fade Out");mfo.setOnClickListener(v->cycleMusicFade(false));LinearLayout.LayoutParams mfop=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);mfop.setMargins(Ui.dp(this,7),0,0,0);musicFade.addView(mfo,mfop);audioPanel.addView(musicFade);
        voiceStatus=Ui.text(this,"No voiceover",11,Ui.MUTED,false);LinearLayout.LayoutParams vs=Ui.matchWrap();vs.setMargins(0,Ui.dp(this,9),0,0);audioPanel.addView(voiceStatus,vs);
        voiceVolume=sliderRow(audioPanel,"Voiceover volume",0,100,project.voiceoverVolume,v->{project.voiceoverVolume=v;applyAuxVolumes();});
        LinearLayout vr=new LinearLayout(this);vr.setOrientation(LinearLayout.HORIZONTAL);Button rec=Ui.secondary(this,"● Record / Stop");rec.setOnClickListener(v->toggleVoiceoverRecording());vr.addView(rec,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));Button rv=Ui.ghost(this,"Remove Voice");rv.setOnClickListener(v->removeVoiceover());LinearLayout.LayoutParams rvp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);rvp.setMargins(Ui.dp(this,7),0,0,0);vr.addView(rv,rvp);audioPanel.addView(vr);
        root.addView(audioPanel);

        LinearLayout overlayPanel=Ui.card(this);LinearLayout.LayoutParams ovp=Ui.matchWrap();ovp.setMargins(0,Ui.dp(this,10),0,0);overlayPanel.setLayoutParams(ovp);overlayPanel.addView(Ui.text(this,"Overlay / Logo / Sticker",16,Ui.TEXT,true));
        overlayStatus=Ui.text(this,"No overlay",11,Ui.MUTED,false);overlayPanel.addView(overlayStatus);
        overlayScale=sliderRow(overlayPanel,"Overlay size",10,100,project.overlayScale,v->{project.overlayScale=v;applyOverlayPreview();});
        overlayX=sliderRow(overlayPanel,"Overlay X",0,100,project.overlayXPercent,v->{project.overlayXPercent=v;applyOverlayPreview();});
        overlayY=sliderRow(overlayPanel,"Overlay Y",0,100,project.overlayYPercent,v->{project.overlayYPercent=v;applyOverlayPreview();});
        overlayOpacity=sliderRow(overlayPanel,"Overlay opacity",10,100,project.overlayOpacity,v->{project.overlayOpacity=v;applyOverlayPreview();});
        LinearLayout ob=new LinearLayout(this);ob.setOrientation(LinearLayout.HORIZONTAL);Button addOv=Ui.secondary(this,"Choose Image");addOv.setOnClickListener(v->pickOverlay());ob.addView(addOv,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button remOv=Ui.ghost(this,"Remove");remOv.setOnClickListener(v->removeOverlay());LinearLayout.LayoutParams rop=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);rop.setMargins(Ui.dp(this,7),0,0,0);ob.addView(remOv,rop);overlayPanel.addView(ob);
        LinearLayout timing=new LinearLayout(this);timing.setOrientation(LinearLayout.HORIZONTAL);Button os=Ui.secondary(this,"Start @ Playhead");os.setOnClickListener(v->setOverlayTiming(true));timing.addView(os,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button oe=Ui.secondary(this,"End @ Playhead");oe.setOnClickListener(v->setOverlayTiming(false));LinearLayout.LayoutParams oep=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);oep.setMargins(Ui.dp(this,7),0,0,0);timing.addView(oe,oep);overlayPanel.addView(timing);
        root.addView(overlayPanel);

        LinearLayout textPanel=Ui.card(this);LinearLayout.LayoutParams txp=Ui.matchWrap();txp.setMargins(0,Ui.dp(this,10),0,0);textPanel.setLayoutParams(txp);textPanel.addView(Ui.text(this,"Text & Captions",16,Ui.TEXT,true));
        name=field(textPanel,"Project name",project.name,false);hook=field(textPanel,"Headline / Hook",project.hook,true);body=field(textPanel,"Main text",project.body,true);cta=field(textPanel,"CTA / Ending",project.cta,true);hashtags=field(textPanel,"Hashtags",project.hashtags,true);
        textScale=sliderRow(textPanel,"Text size",60,180,project.textScale,v->{project.textScale=v;applyTextStyle();});
        textY=sliderRow(textPanel,"Text vertical position",10,90,project.textYPercent,v->{project.textYPercent=v;applyTextStyle();});
        LinearLayout textButtons=new LinearLayout(this);textButtons.setOrientation(LinearLayout.HORIZONTAL);Button color=Ui.secondary(this,"Text Color");color.setOnClickListener(v->cycleTextColor());textButtons.addView(color,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));Button style=Ui.secondary(this,"Caption Style");style.setOnClickListener(v->cycleCaptionStyle());LinearLayout.LayoutParams sty=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);sty.setMargins(Ui.dp(this,7),0,0,0);textButtons.addView(style,sty);fontButton=Ui.secondary(this,"Font");fontButton.setOnClickListener(v->cycleTextFont());LinearLayout.LayoutParams ftp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);ftp.setMargins(Ui.dp(this,7),0,0,0);textButtons.addView(fontButton,ftp);LinearLayout.LayoutParams tbp=Ui.matchWrap();tbp.setMargins(0,Ui.dp(this,8),0,0);textPanel.addView(textButtons,tbp);root.addView(textPanel);

        LinearLayout effects=Ui.card(this);LinearLayout.LayoutParams efp=Ui.matchWrap();efp.setMargins(0,Ui.dp(this,10),0,0);effects.setLayoutParams(efp);effects.addView(Ui.text(this,"Project Effects & Export",16,Ui.TEXT,true));
        LinearLayout fades=new LinearLayout(this);fades.setOrientation(LinearLayout.HORIZONTAL);Button fi=Ui.secondary(this,"Project Fade In");fi.setOnClickListener(v->cycleFade(true));fades.addView(fi,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));Button fo=Ui.secondary(this,"Project Fade Out");fo.setOnClickListener(v->cycleFade(false));LinearLayout.LayoutParams fop=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);fop.setMargins(Ui.dp(this,7),0,0,0);fades.addView(fo,fop);effects.addView(fades);
        Button save=Ui.secondary(this,"Save Draft");save.setOnClickListener(v->saveDraft(true));LinearLayout.LayoutParams sv=Ui.matchWrap();sv.height=Ui.dp(this,50);sv.setMargins(0,Ui.dp(this,8),0,0);effects.addView(save,sv);
        exportButton=Ui.accent(this,"EXPORT MP4");exportButton.setTextSize(17);exportButton.setOnClickListener(v->export());LinearLayout.LayoutParams ex=Ui.matchWrap();ex.height=Ui.dp(this,62);ex.setMargins(0,Ui.dp(this,8),0,0);effects.addView(exportButton,ex);
        exportStatus=Ui.text(this,"Ready",12,Ui.GREEN,true);exportStatus.setGravity(Gravity.CENTER);LinearLayout.LayoutParams es=Ui.matchWrap();es.setMargins(0,Ui.dp(this,8),0,0);effects.addView(exportStatus,es);root.addView(effects);
        return scroll;
    }

    private Button tool(String text,View.OnClickListener l){Button b=Ui.secondary(this,text);b.setOnClickListener(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,118),Ui.dp(this,50));p.setMargins(0,0,Ui.dp(this,7),0);b.setLayoutParams(p);return b;}

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
        for(int i=0;i<project.clips.size();i++){
            final int index=i;EditorClip c=project.clips.get(i);String label=(i+1)+" • "+clipLabel(c)+"\n"+shortClipInfo(c);Button b=index==project.selectedClipIndex?Ui.accent(this,label):Ui.secondary(this,label);b.setTextSize(10);b.setOnClickListener(v->{stopProjectPlayback(false);project.selectedClipIndex=index;refreshClipStrip();bindCurrentClip();saveDraft(false);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,138),Ui.dp(this,56));p.setMargins(0,0,Ui.dp(this,7),0);clipStrip.addView(b,p);
        }
        Button add=Ui.ghost(this,"＋ Add");add.setOnClickListener(v->pickMedia());clipStrip.addView(add,new LinearLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,56)));
        updateClipInfo();
    }

    private String shortClipInfo(EditorClip c){if(c==null)return "";return String.format(Locale.US,"%.2g× • %d%%",c.speed,c.zoom);}
    private String clipLabel(EditorClip c){String u=c==null||c.uri==null?"":c.uri.toLowerCase(Locale.ROOT);return isImageUri(u)?"IMAGE":"VIDEO";}

    private void bindCurrentClip(){
        pauseMediaOnly();videoPrepared=false;activeVideoPlayer=null;mediaDurationMs=0;previewIsVideo=false;imagePlayStartedAt=0;
        try{videoPreview.stopPlayback();}catch(Exception ignored){}
        videoPreview.setVisibility(View.GONE);imagePreview.setVisibility(View.GONE);
        EditorClip c=currentClip();
        if(c==null||c.uri==null||c.uri.trim().isEmpty()){
            emptyPreview.setVisibility(View.VISIBLE);timeline.setMax(1000);timeline.setProgress(0);mediaStatus.setText("No media selected");updateClipInfo();applyOverlayPreview();return;
        }
        emptyPreview.setVisibility(View.GONE);Uri uri=Uri.parse(c.uri);String type=mimeOrUri(uri,c.uri);previewIsVideo=type.startsWith("video/")||isVideoUri(type);
        sourceVolume.setProgress(Math.max(0,Math.min(100,c.volume)));zoomSlider.setProgress(Math.max(0,Math.min(80,c.zoom-100)));
        speedButton.setText(String.format(Locale.US,"%.2g× Speed",c.speed));filterButton.setText(c.filter==null?"Filter":c.filter);
        if(previewIsVideo){
            videoPreview.setVisibility(View.VISIBLE);videoPreview.setVideoURI(uri);videoPreview.setOnPreparedListener(mp->{activeVideoPlayer=mp;videoPrepared=true;mediaDurationMs=Math.max(1,videoPreview.getDuration());if(c.trimEndMs<=c.trimStartMs||c.trimEndMs>mediaDurationMs)c.trimEndMs=mediaDurationMs;applyVideoVolume();applyPlaybackSpeed();applyClipPreviewTransform();timeline.setMax((int)Math.min(Integer.MAX_VALUE,mediaDurationMs));videoPreview.seekTo((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));timeline.setProgress((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));updateTimeLabel(c.trimStartMs);updateClipInfo();if(autoStartAfterBind){autoStartAfterBind=false;startCurrentClipPlayback();}});videoPreview.setOnErrorListener((mp,w,e)->{videoPrepared=false;toast("Preview failed for this format. Export engine may still support it.");if(playingProject)advanceProjectClip();return true;});
        }else{
            imagePreview.setVisibility(View.VISIBLE);imagePreview.setImageURI(uri);mediaDurationMs=Math.max(1000,c.stillDurationMs);timeline.setMax((int)mediaDurationMs);timeline.setProgress(0);updateTimeLabel(0);applyClipPreviewTransform();updateClipInfo();if(autoStartAfterBind){autoStartAfterBind=false;handler.postDelayed(this::startCurrentClipPlayback,80);}
        }
        mediaStatus.setText("Clip "+(project.selectedClipIndex+1)+" / "+project.clips.size()+" • "+clipLabel(c));
        updateTransformStatus();applyTextStyle();applyOverlayPreview();
    }

    private String mimeOrUri(Uri uri,String fallback){try{String t=getContentResolver().getType(uri);if(t!=null)return t.toLowerCase(Locale.ROOT);}catch(Exception ignored){}return fallback==null?"":fallback.toLowerCase(Locale.ROOT);}
    private boolean isVideoUri(String s){return s.contains(".mp4")||s.contains(".webm")||s.contains(".mkv")||s.contains(".mov")||s.contains(".m4v");}
    private boolean isImageUri(String s){return s.contains("image/")||s.endsWith(".jpg")||s.endsWith(".jpeg")||s.endsWith(".png")||s.endsWith(".webp");}

    private void toggleProjectPlay(){if(playingProject){stopProjectPlayback(false);return;}if(project.clips.isEmpty()){toast("Add media first");return;}playingProject=true;playButton.setText("❚❚ إيقاف");startCurrentClipPlayback();}
    private void startCurrentClipPlayback(){
        EditorClip c=currentClip();if(c==null){stopProjectPlayback(false);return;}
        if(previewIsVideo){if(!videoPrepared){autoStartAfterBind=true;return;}int pos=videoPreview.getCurrentPosition();if(pos<c.trimStartMs||pos>=effectiveEnd(c)-50)videoPreview.seekTo((int)c.trimStartMs);applyPlaybackSpeed();applyVideoVolume();try{videoPreview.start();}catch(Exception ignored){}}
        else{imagePlayStartedAt=System.currentTimeMillis();timeline.setProgress(0);}
        syncAuxPlayers(true);playButton.setText("❚❚ إيقاف");
    }
    private void stopProjectPlayback(boolean finished){playingProject=false;autoStartAfterBind=false;imagePlayStartedAt=0;pauseMediaOnly();if(playButton!=null)playButton.setText("▶ تشغيل المشروع");if(finished)toast("Preview finished");}
    private void pauseMediaOnly(){try{if(videoPreview!=null&&videoPreview.isPlaying())videoPreview.pause();}catch(Exception ignored){}try{if(musicPlayer!=null&&musicPlayer.isPlaying())musicPlayer.pause();}catch(Exception ignored){}try{if(voicePlayer!=null&&voicePlayer.isPlaying())voicePlayer.pause();}catch(Exception ignored){}}

    private void advanceProjectClip(){
        if(!playingProject)return;int next=project.selectedClipIndex+1;if(next>=project.clips.size()){stopProjectPlayback(true);return;}project.selectedClipIndex=next;autoStartAfterBind=true;refreshClipStrip();bindCurrentClip();
    }
    private void selectRelativeClip(int delta){if(project.clips.isEmpty())return;stopProjectPlayback(false);int n=Math.max(0,Math.min(project.clips.size()-1,project.selectedClipIndex+delta));if(n==project.selectedClipIndex)return;project.selectedClipIndex=n;refreshClipStrip();bindCurrentClip();}

    private void seekRelative(long delta){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition()+delta;p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c)-50,p));videoPreview.seekTo((int)p);timeline.setProgress((int)p);syncAuxPlayers(false);updateTimeLabel(p);}
    private void seekTo(int value){EditorClip c=currentClip();if(c==null)return;if(previewIsVideo&&videoPrepared){long p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c),value));videoPreview.seekTo((int)p);syncAuxPlayers(false);updateTimeLabel(p);}else if(!previewIsVideo){timeline.setProgress(value);imagePlayStartedAt=0;syncAuxPlayers(false);updateTimeLabel(value);}}
    private long effectiveEnd(EditorClip c){if(c==null)return mediaDurationMs;long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;return Math.max(c.trimStartMs+100,Math.min(mediaDurationMs,end));}

    private void setTrimStart(){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p>=effectiveEnd(c)-300){toast("Move playhead earlier");return;}pushUndo();c.trimStartMs=p;saveDraft(false);updateClipInfo();toast("Trim start set");}
    private void setTrimEnd(){if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p<=c.trimStartMs+300){toast("Move playhead later");return;}pushUndo();c.trimEndMs=p;saveDraft(false);updateClipInfo();toast("Trim end set");}

    private void splitClip(){
        if(!videoPrepared)return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();long end=effectiveEnd(c);if(p<=c.trimStartMs+300||p>=end-300){toast("Move playhead inside the clip before Split");return;}pushUndo();EditorClip right=c.copy();right.trimStartMs=p;right.trimEndMs=end;c.trimEndMs=p;project.clips.add(project.selectedClipIndex+1,right);project.selectedClipIndex++;saveDraft(false);refreshClipStrip();bindCurrentClip();toast("Clip split ✓");
    }
    private void duplicateClip(){EditorClip c=currentClip();if(c==null)return;pushUndo();project.clips.add(project.selectedClipIndex+1,c.copy());project.selectedClipIndex++;project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();toast("Clip duplicated ✓");}
    private void moveClip(int delta){if(project.clips.size()<2)return;int from=project.selectedClipIndex,to=from+delta;if(to<0||to>=project.clips.size())return;pushUndo();Collections.swap(project.clips,from,to);project.selectedClipIndex=to;project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();}
    private void deleteClip(){EditorClip c=currentClip();if(c==null)return;pushUndo();project.clips.remove(project.selectedClipIndex);if(project.selectedClipIndex>=project.clips.size())project.selectedClipIndex=Math.max(0,project.clips.size()-1);project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();}

    private void pickMedia(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK_MEDIA);}
    private void pickAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");startActivityForResult(i,PICK_AUDIO);}
    private void pickOverlay(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_OVERLAY);}

    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        if(request==PICK_MEDIA){ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){ClipData clips=data.getClipData();for(int x=0;x<clips.getItemCount();x++)uris.add(clips.getItemAt(x).getUri());}else if(data.getData()!=null)uris.add(data.getData());if(uris.isEmpty())return;pushUndo();for(Uri uri:uris){persist(uri,data);project.clips.add(new EditorClip(uri.toString()));}project.selectedClipIndex=Math.max(0,project.clips.size()-uris.size());project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();}
        else if(request==PICK_AUDIO&&data.getData()!=null){pushUndo();Uri uri=data.getData();persist(uri,data);project.audioUri=uri.toString();releaseMusicPlayer();saveDraft(false);updateAudioStatus();toast("Music added ✓");}
        else if(request==PICK_OVERLAY&&data.getData()!=null){pushUndo();Uri uri=data.getData();persist(uri,data);project.overlayUri=uri.toString();project.overlayStartMs=0;project.overlayEndMs=0;saveDraft(false);applyOverlayPreview();updateOverlayStatus();toast("Overlay added ✓");}
    }
    private void persist(Uri uri,Intent data){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}}

    private void cycleSpeed(){EditorClip c=currentClip();if(c==null)return;pushUndo();int index=0;float best=999;for(int i=0;i<speeds.length;i++){float d=Math.abs(speeds[i]-c.speed);if(d<best){best=d;index=i;}}c.speed=speeds[(index+1)%speeds.length];speedButton.setText(String.format(Locale.US,"%.2g× Speed",c.speed));applyPlaybackSpeed();saveDraft(false);refreshClipStrip();updateClipInfo();}
    private void applyPlaybackSpeed(){if(activeVideoPlayer==null)return;EditorClip c=currentClip();try{activeVideoPlayer.setPlaybackParams(activeVideoPlayer.getPlaybackParams().setSpeed(c==null?1f:c.speed));}catch(Exception ignored){}}
    private void applyVideoVolume(){EditorClip c=currentClip();if(activeVideoPlayer==null||c==null)return;float v=c.volume/100f;try{activeVideoPlayer.setVolume(v,v);}catch(Exception ignored){}}

    private void rotateClip(){EditorClip c=currentClip();if(c==null)return;pushUndo();c.rotation=(c.rotation+90)%360;applyClipPreviewTransform();updateTransformStatus();saveDraft(false);}
    private void toggleMirror(){EditorClip c=currentClip();if(c==null)return;pushUndo();c.mirror=!c.mirror;applyClipPreviewTransform();updateTransformStatus();saveDraft(false);}
    private void cycleFilter(){EditorClip c=currentClip();if(c==null)return;pushUndo();int idx=0;for(int i=0;i<filters.length;i++)if(filters[i].equals(c.filter))idx=i;c.filter=filters[(idx+1)%filters.length];filterButton.setText(c.filter);updateTransformStatus();saveDraft(false);toast("Filter "+c.filter+" • applied in export");}
    private void applyClipPreviewTransform(){EditorClip c=currentClip();if(c==null)return;float z=Math.max(1f,Math.min(1.8f,c.zoom/100f));float sx=c.mirror?-z:z;if(videoPreview!=null){videoPreview.setRotation(c.rotation);videoPreview.setScaleX(sx);videoPreview.setScaleY(z);}if(imagePreview!=null){imagePreview.setRotation(c.rotation);imagePreview.setScaleX(sx);imagePreview.setScaleY(z);}}
    private void updateTransformStatus(){if(transformStatus==null)return;EditorClip c=currentClip();if(c==null){transformStatus.setText("");return;}transformStatus.setText("Rotate "+c.rotation+"° • Zoom "+c.zoom+"% • "+(c.mirror?"Mirror ON":"Mirror OFF")+" • Filter "+c.filter);if(mirrorButton!=null)mirrorButton.setText(c.mirror?"⇋ Mirrored":"⇋ Mirror");if(rotateButton!=null)rotateButton.setText("↻ "+c.rotation+"°");if(filterButton!=null)filterButton.setText(c.filter);}

    private void cycleRatio(){pushUndo();int i=0;for(int x=0;x<ratios.length;x++)if(ratios[x].equals(project.aspectRatio))i=x;project.aspectRatio=ratios[(i+1)%ratios.length];ratioButton.setText(project.aspectRatio+" Ratio");ratioChip.setText(project.aspectRatio);applyPreviewRatio();saveDraft(false);}
    private void applyPreviewRatio(){ViewGroup.LayoutParams raw=previewFrame.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)raw;if("1:1".equals(project.aspectRatio))p.height=Ui.dp(this,360);else if("16:9".equals(project.aspectRatio))p.height=Ui.dp(this,240);else p.height=Ui.dp(this,480);previewFrame.setLayoutParams(p);previewFrame.post(this::applyOverlayPreview);}}

    private void cycleTransition(){pushUndo();int i=0;for(int x=0;x<transitions.length;x++)if(transitions[x].equals(project.transitionStyle))i=x;project.transitionStyle=transitions[(i+1)%transitions.length];transitionButton.setText(project.transitionStyle);saveDraft(false);}
    private void cycleFade(boolean in){pushUndo();int old=in?project.fadeInMs:project.fadeOutMs;int next=old==0?250:old==250?500:old==500?1000:0;if(in)project.fadeInMs=next;else project.fadeOutMs=next;toast((in?"Fade In: ":"Fade Out: ")+next+"ms");saveDraft(false);}
    private void cycleMusicFade(boolean in){pushUndo();int old=in?project.musicFadeInMs:project.musicFadeOutMs;int next=old==0?500:old==500?1000:old==1000?2000:0;if(in)project.musicFadeInMs=next;else project.musicFadeOutMs=next;toast((in?"Music Fade In: ":"Music Fade Out: ")+next+"ms");saveDraft(false);}

    private void toggleText(){pushUndo();project.showText=!project.showText;textToggleButton.setText(project.showText?"Text ON":"Text OFF");applyTextStyle();saveDraft(false);}
    private void cycleTextColor(){pushUndo();colorIndex=(colorIndex+1)%textColors.length;project.textColor=textColors[colorIndex];applyTextStyle();saveDraft(false);}
    private void cycleCaptionStyle(){pushUndo();String s=project.captionStyle==null?"Bold Highlight":project.captionStyle;if("Bold Highlight".equals(s))project.captionStyle="Clean";else if("Clean".equals(s))project.captionStyle="Subtitle";else project.captionStyle="Bold Highlight";applyTextStyle();saveDraft(false);toast(project.captionStyle);}
    private void cycleTextFont(){pushUndo();int i=0;for(int x=0;x<fonts.length;x++)if(fonts[x].equals(project.textFont))i=x;project.textFont=fonts[(i+1)%fonts.length];fontButton.setText(project.textFont);applyTextStyle();saveDraft(false);}

    private void updatePreviewText(){if(hook==null)return;project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);project.name=text(name);if(previewHook!=null)previewHook.setText(project.hook);if(previewBody!=null)previewBody.setText(project.body);if(previewCta!=null)previewCta.setText(project.cta);}
    private void applyTextStyle(){if(previewHook==null)return;float k=project.textScale/100f;previewHook.setTextSize(24*k);previewBody.setTextSize(15*k);previewCta.setTextSize(13*k);previewHook.setTextColor(project.textColor);previewBody.setTextColor(project.textColor);previewCta.setTextColor(project.textColor);int visibility=project.showText?View.VISIBLE:View.GONE;previewHook.setVisibility(visibility);previewBody.setVisibility(visibility);previewCta.setVisibility(visibility);float y=(project.textYPercent-50)*5f;previewHook.setTranslationY(y);previewBody.setTranslationY(y);previewCta.setTranslationY(y);boolean bold="Bold Highlight".equals(project.captionStyle);int style=bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL;android.graphics.Typeface base="Serif".equals(project.textFont)?android.graphics.Typeface.SERIF:"Mono".equals(project.textFont)?android.graphics.Typeface.MONOSPACE:android.graphics.Typeface.SANS_SERIF;previewHook.setTypeface(base,style);previewBody.setTypeface(base,android.graphics.Typeface.NORMAL);previewCta.setTypeface(base,style);if(fontButton!=null)fontButton.setText(project.textFont);}

    private void setImageDuration(long ms){EditorClip c=currentClip();if(c==null)return;pushUndo();c.stillDurationMs=ms;mediaDurationMs=ms;timeline.setMax((int)ms);saveDraft(false);refreshClipStrip();updateClipInfo();}

    private void updateClipInfo(){if(clipInfo==null)return;EditorClip c=currentClip();if(c==null){clipInfo.setText("Timeline is empty");return;}long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;long raw=previewIsVideo?Math.max(0,end-c.trimStartMs):c.stillDurationMs;long out=(long)(raw/Math.max(.5f,c.speed));clipInfo.setText("Clip "+(project.selectedClipIndex+1)+" / "+project.clips.size()+" • trim "+fmt(c.trimStartMs)+" → "+(end>0?fmt(end):"end")+" • "+String.format(Locale.US,"%.2g×",c.speed)+" • output "+fmt(out)+" • project "+fmt(totalProjectDurationMs()));}

    private void changeAudioOffset(long delta){pushUndo();project.audioOffsetMs=Math.max(-30000,Math.min(30000,project.audioOffsetMs+delta));saveDraft(false);updateAudioStatus();syncAuxPlayers(false);}
    private void removeAudio(){pushUndo();project.audioUri="";releaseMusicPlayer();saveDraft(false);updateAudioStatus();}
    private void updateAudioStatus(){if(audioStatus==null)return;audioStatus.setText((empty(project.audioUri)?"No music":"Music added ✓")+" • offset "+String.format(Locale.US,"%.1fs",project.audioOffsetMs/1000f)+" • fade "+project.musicFadeInMs+"/"+project.musicFadeOutMs+"ms");if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);updateVoiceStatus();}

    private void toggleVoiceoverRecording(){if(voiceRecording){stopVoiceoverRecording();return;}if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}startVoiceoverRecording();}
    private void startVoiceoverRecording(){
        try{pushUndo();File dir=new File(getFilesDir(),"voiceovers");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Cannot create voiceover folder");voiceRecordingFile=new File(dir,"voice_"+System.currentTimeMillis()+".m4a");voiceRecordOffsetMs=projectPlayheadMs();voiceRecorder=new MediaRecorder();voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);voiceRecorder.setAudioSamplingRate(44100);voiceRecorder.setAudioEncodingBitRate(128000);voiceRecorder.setOutputFile(voiceRecordingFile.getAbsolutePath());voiceRecorder.prepare();voiceRecorder.start();voiceRecording=true;if(voiceButton!=null)voiceButton.setText("■ Stop Voice");voiceStatus.setText("Recording voiceover…");if(!playingProject)toggleProjectPlay();}
        catch(Exception e){releaseRecorder();voiceRecording=false;toast("Voiceover record failed");}
    }
    private void stopVoiceoverRecording(){
        if(!voiceRecording){releaseRecorder();return;}voiceRecording=false;boolean ok=true;try{voiceRecorder.stop();}catch(Exception e){ok=false;}releaseRecorder();if(ok&&voiceRecordingFile!=null&&voiceRecordingFile.exists()&&voiceRecordingFile.length()>512){project.voiceoverUri=Uri.fromFile(voiceRecordingFile).toString();project.voiceoverOffsetMs=voiceRecordOffsetMs;releaseVoicePlayer();saveDraft(false);toast("Voiceover saved ✓");}else{try{if(voiceRecordingFile!=null)voiceRecordingFile.delete();}catch(Exception ignored){}toast("Voiceover was too short");}if(voiceButton!=null)voiceButton.setText("● Voiceover");updateVoiceStatus();}
    private void releaseRecorder(){try{if(voiceRecorder!=null)voiceRecorder.release();}catch(Exception ignored){}voiceRecorder=null;}
    private void removeVoiceover(){if(voiceRecording)stopVoiceoverRecording();pushUndo();project.voiceoverUri="";project.voiceoverOffsetMs=0;releaseVoicePlayer();saveDraft(false);updateVoiceStatus();}
    private void updateVoiceStatus(){if(voiceStatus==null)return;voiceStatus.setText((empty(project.voiceoverUri)?"No voiceover":"Voiceover added ✓")+" • starts "+fmt(project.voiceoverOffsetMs));if(voiceVolume!=null)voiceVolume.setProgress(project.voiceoverVolume);}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_MIC){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startVoiceoverRecording();else toast("Microphone permission is required for voiceover");}}

    private void ensureAuxPlayers(){
        if(!empty(project.audioUri)&&musicPlayer==null){try{musicPlayer=new MediaPlayer();musicPlayer.setDataSource(this,Uri.parse(project.audioUri));musicPlayer.setLooping(true);musicPlayer.prepare();}catch(Exception e){releaseMusicPlayer();}}
        if(!empty(project.voiceoverUri)&&voicePlayer==null){try{voicePlayer=new MediaPlayer();voicePlayer.setDataSource(this,Uri.parse(project.voiceoverUri));voicePlayer.setLooping(false);voicePlayer.prepare();}catch(Exception e){releaseVoicePlayer();}}
        applyAuxVolumes();
    }
    private void syncAuxPlayers(boolean play){ensureAuxPlayers();syncOnePlayer(musicPlayer,project.audioOffsetMs,play,true);syncOnePlayer(voicePlayer,project.voiceoverOffsetMs,play,false);}
    private void syncOnePlayer(MediaPlayer p,long offset,boolean play,boolean loop){if(p==null)return;try{long target=projectPlayheadMs()-offset;if(target<0){if(p.isPlaying())p.pause();p.seekTo(0);return;}int d=p.getDuration();if(d<=0)return;if(loop)target%=d;else if(target>=d){if(p.isPlaying())p.pause();return;}int cur=p.getCurrentPosition();if(Math.abs(cur-target)>650)p.seekTo((int)Math.min(Integer.MAX_VALUE,target));if(play&&!p.isPlaying())p.start();else if(!play&&p.isPlaying())p.pause();}catch(Exception ignored){}}
    private void applyAuxVolumes(){float m=Math.max(0,Math.min(100,project.audioVolume))/100f,v=Math.max(0,Math.min(100,project.voiceoverVolume))/100f;try{if(musicPlayer!=null)musicPlayer.setVolume(m,m);}catch(Exception ignored){}try{if(voicePlayer!=null)voicePlayer.setVolume(v,v);}catch(Exception ignored){}}
    private void releaseMusicPlayer(){try{if(musicPlayer!=null){musicPlayer.stop();musicPlayer.release();}}catch(Exception ignored){}musicPlayer=null;}
    private void releaseVoicePlayer(){try{if(voicePlayer!=null){voicePlayer.stop();voicePlayer.release();}}catch(Exception ignored){}voicePlayer=null;}
    private void releaseAuxPlayers(){releaseMusicPlayer();releaseVoicePlayer();}

    private void applyOverlayPreview(){
        if(overlayPreview==null)return;if(empty(project.overlayUri)){overlayPreview.setVisibility(View.GONE);updateOverlayStatus();return;}try{overlayPreview.setImageURI(Uri.parse(project.overlayUri));overlayPreview.setAlpha(project.overlayOpacity/100f);int fw=previewFrame.getWidth(),fh=previewFrame.getHeight();if(fw<=0||fh<=0){previewFrame.postDelayed(this::applyOverlayPreview,100);return;}int size=Math.max(Ui.dp(this,48),(int)(fw*(project.overlayScale/100f)));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(size,size);lp.gravity=Gravity.TOP|Gravity.START;int x=(int)(fw*(project.overlayXPercent/100f)-size/2f),y=(int)(fh*(project.overlayYPercent/100f)-size/2f);lp.leftMargin=Math.max(0,Math.min(fw-size,x));lp.topMargin=Math.max(0,Math.min(fh-size,y));overlayPreview.setLayoutParams(lp);updateOverlayVisibility();updateOverlayStatus();}catch(Exception e){overlayPreview.setVisibility(View.GONE);}
    }
    private void updateOverlayVisibility(){if(overlayPreview==null||empty(project.overlayUri)){if(overlayPreview!=null)overlayPreview.setVisibility(View.GONE);return;}long p=projectPlayheadMs();long end=project.overlayEndMs>project.overlayStartMs?project.overlayEndMs:Long.MAX_VALUE;overlayPreview.setVisibility(p>=project.overlayStartMs&&p<=end?View.VISIBLE:View.GONE);}
    private void setOverlayTiming(boolean start){if(empty(project.overlayUri)){toast("Choose overlay first");return;}pushUndo();long p=projectPlayheadMs();if(start){project.overlayStartMs=p;if(project.overlayEndMs>0&&project.overlayEndMs<=p)project.overlayEndMs=0;}else{if(p<=project.overlayStartMs){toast("Move playhead after overlay start");return;}project.overlayEndMs=p;}saveDraft(false);updateOverlayStatus();updateOverlayVisibility();}
    private void removeOverlay(){pushUndo();project.overlayUri="";project.overlayStartMs=0;project.overlayEndMs=0;saveDraft(false);applyOverlayPreview();}
    private void updateOverlayStatus(){if(overlayStatus==null)return;String end=project.overlayEndMs>project.overlayStartMs?fmt(project.overlayEndMs):"end";overlayStatus.setText(empty(project.overlayUri)?"No overlay":"Overlay ✓ • "+project.overlayScale+"% • "+project.overlayOpacity+"% opacity • "+fmt(project.overlayStartMs)+" → "+end);}

    private long projectPlayheadMs(){long total=0;for(int i=0;i<project.clips.size();i++){EditorClip c=project.clips.get(i);if(i<project.selectedClipIndex)total+=estimateClipDuration(c);else if(i==project.selectedClipIndex){if(previewIsVideo&&videoPrepared){long pos=videoPreview.getCurrentPosition();total+=(long)(Math.max(0,pos-c.trimStartMs)/Math.max(.5f,c.speed));}else if(!previewIsVideo&&playingProject&&imagePlayStartedAt>0){total+=Math.max(0,System.currentTimeMillis()-imagePlayStartedAt);}else if(!previewIsVideo){total+=(long)(timeline.getProgress()/Math.max(.5f,c.speed));}break;}}return total;}
    private long totalProjectDurationMs(){long total=0;for(EditorClip c:project.clips)total+=estimateClipDuration(c);return total;}
    private long estimateClipDuration(EditorClip c){long raw;if(isImageUri(c.uri==null?"":c.uri.toLowerCase(Locale.ROOT)))raw=c.stillDurationMs;else{long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:c.trimStartMs+5000;raw=Math.max(500,end-c.trimStartMs);}return (long)(raw/Math.max(.5f,c.speed));}

    private final Runnable ticker=new Runnable(){@Override public void run(){try{EditorClip c=currentClip();if(c!=null){if(previewIsVideo&&videoPrepared&&!timelineTouching){long p=videoPreview.getCurrentPosition();timeline.setProgress((int)Math.min(Integer.MAX_VALUE,p));updateTimeLabel(p);if(playingProject&&videoPreview.isPlaying()&&p>=effectiveEnd(c)-80){advanceProjectClip();handler.postDelayed(this,100);return;}}else if(!previewIsVideo&&playingProject&&imagePlayStartedAt>0&&!timelineTouching){long elapsed=Math.max(0,System.currentTimeMillis()-imagePlayStartedAt);long raw=(long)(elapsed*Math.max(.5f,c.speed));timeline.setProgress((int)Math.min(mediaDurationMs,raw));updateTimeLabel(Math.min(mediaDurationMs,raw));if(raw>=mediaDurationMs-30){advanceProjectClip();handler.postDelayed(this,100);return;}}if(playingProject)syncAuxPlayers(true);updateOverlayVisibility();}}catch(Exception ignored){}handler.postDelayed(this,120);}};
    private void updateTimeLabel(long p){if(timeLabel==null)return;timeLabel.setText(fmt(p)+" / "+fmt(mediaDurationMs)+"   • project "+fmt(projectPlayheadMs())+" / "+fmt(totalProjectDurationMs()));}
    private String fmt(long ms){if(ms<0)ms=0;long min=ms/60000;double sec=(ms%60000)/1000.0;return String.format(Locale.US,"%02d:%04.1f",min,sec);}

    private void pullFields(){if(name!=null){project.name=text(name);project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);}if(project.name==null||project.name.trim().isEmpty())project.name="Video Project";project.syncPrimarySource();}
    private void saveDraft(boolean notify){pullFields();project.updatedAt=System.currentTimeMillis();DraftStore.save(this,project);if(notify)toast("Draft saved ✓");}

    private void pushUndo(){try{String s=project.toJson().toString();if(undoStack.size()>=30)undoStack.removeFirst();undoStack.addLast(s);redoStack.clear();}catch(Exception ignored){}}
    private void undo(){if(undoStack.isEmpty()){toast("Nothing to undo");return;}if(voiceRecording)stopVoiceoverRecording();stopProjectPlayback(false);try{redoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(undoStack.removeLast()));releaseAuxPlayers();restoreUiFromProject();refreshClipStrip();bindCurrentClip();}catch(Exception e){toast("Undo failed");}}
    private void redo(){if(redoStack.isEmpty()){toast("Nothing to redo");return;}if(voiceRecording)stopVoiceoverRecording();stopProjectPlayback(false);try{undoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(redoStack.removeLast()));releaseAuxPlayers();restoreUiFromProject();refreshClipStrip();bindCurrentClip();}catch(Exception e){toast("Redo failed");}}

    private void restoreUiFromProject(){restoring=true;try{project.ensureClips();if(name!=null)name.setText(project.name==null?"":project.name);if(hook!=null)hook.setText(project.hook==null?"":project.hook);if(body!=null)body.setText(project.body==null?"":project.body);if(cta!=null)cta.setText(project.cta==null?"":project.cta);if(hashtags!=null)hashtags.setText(project.hashtags==null?"":project.hashtags);if(textScale!=null)textScale.setProgress(project.textScale-60);if(textY!=null)textY.setProgress(project.textYPercent-10);if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);if(voiceVolume!=null)voiceVolume.setProgress(project.voiceoverVolume);if(overlayScale!=null)overlayScale.setProgress(project.overlayScale-10);if(overlayX!=null)overlayX.setProgress(project.overlayXPercent);if(overlayY!=null)overlayY.setProgress(project.overlayYPercent);if(overlayOpacity!=null)overlayOpacity.setProgress(project.overlayOpacity-10);if(ratioButton!=null)ratioButton.setText(project.aspectRatio+" Ratio");if(ratioChip!=null)ratioChip.setText(project.aspectRatio);if(transitionButton!=null)transitionButton.setText(project.transitionStyle);if(textToggleButton!=null)textToggleButton.setText(project.showText?"Text ON":"Text OFF");if(fontButton!=null)fontButton.setText(project.textFont);updatePreviewText();applyTextStyle();applyPreviewRatio();updateAudioStatus();updateVoiceStatus();updateOverlayStatus();applyOverlayPreview();}finally{restoring=false;}}

    private void export(){
        if(voiceRecording)stopVoiceoverRecording();pullFields();project.ensureClips();if(project.clips.isEmpty()){toast("Add at least one video or image");return;}DraftStore.save(this,project);stopProjectPlayback(false);exportButton.setEnabled(false);exportButton.setAlpha(.55f);exportStatus.setTextColor(Ui.YELLOW);exportStatus.setText("Preparing professional timeline…");
        executor.execute(()->ReelExporter.export(this,project,new ReelExporter.Callback(){public void onStage(String s){runOnUiThread(()->exportStatus.setText(s));}public void onSuccess(File file){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.GREEN);exportStatus.setText("Export complete ✓");Intent i=new Intent(TemplateEditorActivity.this,PublishingActivity.class);i.putExtra("project_id",project.id);i.putExtra("video_path",file.getAbsolutePath());startActivity(i);});}public void onError(String m){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.RED);exportStatus.setText(m);toast(m);});}}));
    }

    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
