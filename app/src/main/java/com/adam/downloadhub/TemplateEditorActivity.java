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
    private final ArrayList<Button> tabButtons=new ArrayList<>();

    private CreatorProject project;
    private EditText name,hook,body,cta,hashtags;
    private FrameLayout previewFrame;
    private VideoView videoPreview;
    private ImageView imagePreview,overlayPreview;
    private TextView emptyPreview,previewHook,previewBody,previewCta,mediaStatus,actionStatus,timeLabel,clipInfo,audioStatus,voiceStatus,overlayStatus,ratioChip,transformStatus;
    private Button exportButton,playButton,speedButton,ratioButton,transitionButton,filterButton,mirrorButton,rotateButton;
    private SeekBar timeline,sourceVolume,audioVolume,voiceVolume,textScale,textY,zoomSlider,overlayScale,overlayX,overlayY,overlayOpacity;
    private LinearLayout clipStrip,editPanel,audioPanel,textPanel,overlayPanel,canvasPanel;

    private MediaPlayer activeVideoPlayer,musicPlayer,voicePlayer;
    private MediaRecorder voiceRecorder;
    private File voiceRecordingFile;
    private long voiceRecordOffsetMs=0;

    private boolean videoPrepared=false,previewIsVideo=false,timelineTouching=false,restoring=false;
    private boolean playingProject=false,autoStartAfterBind=false,voiceRecording=false;
    private long mediaDurationMs=0,imagePlayStartedAt=0;
    private int colorIndex=0;
    private String activePanel="edit";

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
        showPanel("edit");
        handler.post(ticker);
        if(getIntent().getBooleanExtra("pick_media",false)&&project.clips.isEmpty())previewFrame.postDelayed(this::pickMedia,250);
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
            project=CreatorProject.fromTemplate(t);
            if("blank".equals(id)||t==null){
                project.categoryKey="creator";project.categoryName="مشروع جديد";project.name="مشروع جديد";
                project.hook="";project.body="";project.cta="";project.hashtags="";
            }
        }
        project.ensureClips();
    }

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setPadding(Ui.dp(this,9),Ui.dp(this,7),Ui.dp(this,9),Ui.dp(this,7));
        page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.ghost(this,"‹");back.setTextSize(24);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,42),Ui.dp(this,42)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.addView(Ui.text(this,"Video Studio Pro",18,Ui.TEXT,true));titleBox.addView(Ui.text(this,"المعاينة ثابتة • كل أداة لها استجابة فورية",9,Ui.CYAN,true));top.addView(titleBox,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button undo=Ui.ghost(this,"↶");undo.setOnClickListener(v->undo());top.addView(undo,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));
        Button redo=Ui.ghost(this,"↷");redo.setOnClickListener(v->redo());top.addView(redo,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));
        exportButton=Ui.accent(this,"تصدير");exportButton.setTextSize(12);exportButton.setOnClickListener(v->export());top.addView(exportButton,new LinearLayout.LayoutParams(Ui.dp(this,76),Ui.dp(this,40)));
        page.addView(top,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,46)));

        previewFrame=new FrameLayout(this);previewFrame.setBackground(Ui.gradient(0xFF01050A,0xFF0D1420,18,this));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,286));pp.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,3));page.addView(previewFrame,pp);
        videoPreview=new VideoView(this);videoPreview.setVisibility(View.GONE);videoPreview.setOnClickListener(v->toggleProjectPlay());previewFrame.addView(videoPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        imagePreview=new ImageView(this);imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);imagePreview.setVisibility(View.GONE);imagePreview.setOnClickListener(v->toggleProjectPlay());previewFrame.addView(imagePreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        emptyPreview=Ui.text(this,"＋ اضغط لإضافة فيديو أو صورة",15,Ui.MUTED,true);emptyPreview.setGravity(Gravity.CENTER);emptyPreview.setOnClickListener(v->pickMedia());previewFrame.addView(emptyPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        overlayPreview=new ImageView(this);overlayPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);overlayPreview.setVisibility(View.GONE);previewFrame.addView(overlayPreview,new FrameLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,100)));

        LinearLayout textOverlay=new LinearLayout(this);textOverlay.setOrientation(LinearLayout.VERTICAL);textOverlay.setGravity(Gravity.CENTER);textOverlay.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));previewFrame.addView(textOverlay,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        ratioChip=Ui.chip(this,project.aspectRatio,Ui.CYAN);textOverlay.addView(ratioChip);
        previewHook=Ui.text(this,project.hook,22,project.textColor,true);previewHook.setGravity(Gravity.CENTER);previewHook.setShadowLayer(7,0,2,Color.BLACK);LinearLayout.LayoutParams h=Ui.matchWrap();h.setMargins(0,Ui.dp(this,18),0,Ui.dp(this,6));textOverlay.addView(previewHook,h);
        previewBody=Ui.text(this,project.body,14,project.textColor,false);previewBody.setGravity(Gravity.CENTER);previewBody.setShadowLayer(6,0,2,Color.BLACK);previewBody.setMaxLines(6);textOverlay.addView(previewBody);
        previewCta=Ui.text(this,project.cta,12,project.textColor,true);previewCta.setGravity(Gravity.CENTER);previewCta.setShadowLayer(6,0,2,Color.BLACK);LinearLayout.LayoutParams ct=Ui.matchWrap();ct.setMargins(0,Ui.dp(this,10),0,0);textOverlay.addView(previewCta,ct);

        LinearLayout transport=new LinearLayout(this);transport.setOrientation(LinearLayout.HORIZONTAL);transport.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=Ui.ghost(this,"‹");prev.setOnClickListener(v->selectRelativeClip(-1));transport.addView(prev,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,40)));
        Button minus=Ui.ghost(this,"-5s");minus.setOnClickListener(v->seekRelative(-5000));transport.addView(minus,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,40)));
        playButton=Ui.accent(this,"▶");playButton.setTextSize(17);playButton.setOnClickListener(v->toggleProjectPlay());LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(0,Ui.dp(this,40),1f);pl.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);transport.addView(playButton,pl);
        Button plus=Ui.ghost(this,"+5s");plus.setOnClickListener(v->seekRelative(5000));transport.addView(plus,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,40)));
        Button next=Ui.ghost(this,"›");next.setOnClickListener(v->selectRelativeClip(1));transport.addView(next,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,40)));
        page.addView(transport,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42)));

        timeLabel=Ui.text(this,"00:00.0 / 00:00.0",10,Ui.CYAN,true);timeLabel.setGravity(Gravity.CENTER);page.addView(timeLabel,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,18)));
        timeline=new SeekBar(this);timeline.setMax(1000);timeline.setProgress(0);timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){if(from)seekTo(p);}public void onStartTrackingTouch(SeekBar s){timelineTouching=true;}public void onStopTrackingTouch(SeekBar s){timelineTouching=false;action("تم تحريك المؤشر إلى "+fmt(projectPlayheadMs()));}});page.addView(timeline,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,32)));

        LinearLayout timelineCard=Ui.card(this);timelineCard.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
        clipInfo=Ui.text(this,"",9,Ui.MUTED,false);timelineCard.addView(clipInfo);
        HorizontalScrollView clipScroll=new HorizontalScrollView(this);clipScroll.setHorizontalScrollBarEnabled(false);clipStrip=new LinearLayout(this);clipStrip.setOrientation(LinearLayout.HORIZONTAL);clipScroll.addView(clipStrip,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));timelineCard.addView(clipScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,48)));
        page.addView(timelineCard,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,68)));

        actionStatus=Ui.text(this,"جاهز للمونتاج",10,Ui.GREEN,true);actionStatus.setGravity(Gravity.CENTER);page.addView(actionStatus,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,20)));

        ScrollView controlScroll=new ScrollView(this);controlScroll.setFillViewport(true);
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controlScroll.addView(controls,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        editPanel=buildEditPanel();controls.addView(editPanel);
        audioPanel=buildAudioPanel();controls.addView(audioPanel);
        textPanel=buildTextPanel();controls.addView(textPanel);
        overlayPanel=buildOverlayPanel();controls.addView(overlayPanel);
        canvasPanel=buildCanvasPanel();controls.addView(canvasPanel);
        page.addView(controlScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(0,Ui.dp(this,3),0,0);tabsScroll.addView(tabs,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.MATCH_PARENT));
        tabs.addView(tabButton("edit","✂\nتعديل",v->showPanel("edit")));
        tabs.addView(tabButton("audio","♫\nصوت",v->showPanel("audio")));
        tabs.addView(tabButton("text","T\nنص",v->showPanel("text")));
        tabs.addView(tabButton("overlay","◇\nOverlay",v->showPanel("overlay")));
        tabs.addView(tabButton("canvas","▣\nCanvas",v->showPanel("canvas")));
        tabs.addView(tabButton("media","＋\nميديا",v->pickMedia()));
        page.addView(tabsScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,60)));
        return page;
    }

    private LinearLayout panelBase(String title,String sub){
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(Ui.dp(this,10),Ui.dp(this,7),Ui.dp(this,10),Ui.dp(this,10));
        p.addView(Ui.text(this,title,15,Ui.TEXT,true));TextView s=Ui.text(this,sub,10,Ui.MUTED,false);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,2),0,Ui.dp(this,5));p.addView(s,sp);return p;
    }

    private LinearLayout buildEditPanel(){
        LinearLayout p=panelBase("أدوات المقطع","قص • تقسيم • ترتيب • سرعة • Zoom • Rotate • Filter");
        mediaStatus=Ui.text(this,"",10,Ui.MUTED,false);p.addView(mediaStatus);
        transformStatus=Ui.text(this,"",10,Ui.CYAN,true);p.addView(transformStatus);
        addActionRow(p,new String[]{"✂\nSplit","◁\nTrim In","▷\nTrim Out","⌫\nحذف"},new View.OnClickListener[]{v->splitClip(),v->setTrimStart(),v->setTrimEnd(),v->deleteClip()});
        addActionRow(p,new String[]{"⧉\nنسخ","←\nتحريك","→\nتحريك","＋\nميديا"},new View.OnClickListener[]{v->duplicateClip(),v->moveClip(-1),v->moveClip(1),v->pickMedia()});
        speedButton=Ui.secondary(this,"السرعة 1.0×");speedButton.setOnClickListener(v->cycleSpeed());p.addView(speedButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42)));
        sourceVolume=sliderRow(p,"صوت المقطع",0,100,100,v->{EditorClip c=currentClip();if(c!=null){c.volume=v;applyVideoVolume();action("صوت المقطع "+v+"%");}});
        zoomSlider=sliderRow(p,"Zoom",100,180,100,v->{EditorClip c=currentClip();if(c!=null){c.zoom=v;applyClipPreviewTransform();updateTransformStatus();action("Zoom "+v+"%");}});
        addActionRow(p,new String[]{"↻\nRotate","⇋\nMirror","✦\nFilter","↺\nReset"},new View.OnClickListener[]{v->rotateClip(),v->toggleMirror(),v->cycleFilter(),v->resetClip()});
        LinearLayout img=new LinearLayout(this);img.setOrientation(LinearLayout.HORIZONTAL);img.setGravity(Gravity.CENTER_VERTICAL);TextView l=Ui.text(this,"مدة الصورة",10,Ui.MUTED,true);img.addView(l,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button d3=Ui.ghost(this,"3s");d3.setOnClickListener(v->setImageDuration(3000));img.addView(d3,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,36)));
        Button d5=Ui.ghost(this,"5s");d5.setOnClickListener(v->setImageDuration(5000));img.addView(d5,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,36)));
        Button d10=Ui.ghost(this,"10s");d10.setOnClickListener(v->setImageDuration(10000));img.addView(d10,new LinearLayout.LayoutParams(Ui.dp(this,56),Ui.dp(this,36)));p.addView(img);
        rotateButton=new Button(this);mirrorButton=new Button(this);filterButton=new Button(this);rotateButton.setVisibility(View.GONE);mirrorButton.setVisibility(View.GONE);filterButton.setVisibility(View.GONE);
        return p;
    }

    private LinearLayout buildAudioPanel(){
        LinearLayout p=panelBase("الصوت","موسيقى + صوت الفيديو + Voiceover وتوقيت مستقل");
        audioStatus=Ui.text(this,"لا توجد موسيقى",10,Ui.MUTED,false);p.addView(audioStatus);
        addActionRow(p,new String[]{"♫\nموسيقى","-1s\nتأخير","+1s\nتقديم","⌫\nإزالة"},new View.OnClickListener[]{v->pickAudio(),v->changeAudioOffset(-1000),v->changeAudioOffset(1000),v->removeAudio()});
        audioVolume=sliderRow(p,"صوت الموسيقى",0,100,project.audioVolume,v->{project.audioVolume=v;applyAuxVolumes();action("صوت الموسيقى "+v+"%");});
        addActionRow(p,new String[]{"Fade\nIn","Fade\nOut","●\nVoiceover","⌫\nVoice"},new View.OnClickListener[]{v->cycleMusicFade(true),v->cycleMusicFade(false),v->toggleVoiceoverRecording(),v->removeVoiceover()});
        voiceStatus=Ui.text(this,"لا يوجد Voiceover",10,Ui.MUTED,false);p.addView(voiceStatus);
        voiceVolume=sliderRow(p,"صوت التعليق",0,100,project.voiceoverVolume,v->{project.voiceoverVolume=v;applyAuxVolumes();action("صوت التعليق "+v+"%");});
        return p;
    }

    private LinearLayout buildTextPanel(){
        LinearLayout p=panelBase("النصوص","كل تعديل يظهر مباشرة فوق الفيديو");
        name=field(p,"اسم المشروع",project.name,false);hook=field(p,"العنوان",project.hook,true);body=field(p,"النص",project.body,true);cta=field(p,"النهاية",project.cta,true);hashtags=field(p,"Hashtags",project.hashtags,true);
        textScale=sliderRow(p,"حجم النص",60,180,project.textScale,v->{project.textScale=v;applyTextStyle();action("حجم النص "+v+"%");});
        textY=sliderRow(p,"مكان النص",10,90,project.textYPercent,v->{project.textYPercent=v;applyTextStyle();action("تم تحريك النص");});
        addActionRow(p,new String[]{"●\nلون","Aa\nStyle","F\nFont","T\nإظهار/إخفاء"},new View.OnClickListener[]{v->cycleTextColor(),v->cycleCaptionStyle(),v->cycleTextFont(),v->toggleText()});
        return p;
    }

    private LinearLayout buildOverlayPanel(){
        LinearLayout p=panelBase("Overlay / Logo","صورة أو لوجو فوق الفيديو مع الحجم والمكان والتوقيت");
        overlayStatus=Ui.text(this,"لا يوجد Overlay",10,Ui.MUTED,false);p.addView(overlayStatus);
        addActionRow(p,new String[]{"◇\nاختيار","⌫\nإزالة","⏱\nStart","⏱\nEnd"},new View.OnClickListener[]{v->pickOverlay(),v->removeOverlay(),v->setOverlayTiming(true),v->setOverlayTiming(false)});
        overlayScale=sliderRow(p,"الحجم",10,100,project.overlayScale,v->{project.overlayScale=v;applyOverlayPreview();action("حجم Overlay "+v+"%");});
        overlayX=sliderRow(p,"المكان X",0,100,project.overlayXPercent,v->{project.overlayXPercent=v;applyOverlayPreview();action("تم تحريك Overlay");});
        overlayY=sliderRow(p,"المكان Y",0,100,project.overlayYPercent,v->{project.overlayYPercent=v;applyOverlayPreview();action("تم تحريك Overlay");});
        overlayOpacity=sliderRow(p,"الشفافية",10,100,project.overlayOpacity,v->{project.overlayOpacity=v;applyOverlayPreview();action("شفافية Overlay "+v+"%");});
        return p;
    }

    private LinearLayout buildCanvasPanel(){
        LinearLayout p=panelBase("Canvas & Project","المقاس • الانتقالات • Fade • المسودة");
        ratioButton=Ui.secondary(this,project.aspectRatio+"  المقاس");ratioButton.setOnClickListener(v->cycleRatio());p.addView(ratioButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42)));
        transitionButton=Ui.secondary(this,project.transitionStyle+"  Transition");transitionButton.setOnClickListener(v->cycleTransition());LinearLayout.LayoutParams tr=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42));tr.setMargins(0,Ui.dp(this,5),0,0);p.addView(transitionButton,tr);
        addActionRow(p,new String[]{"Fade\nIn","Fade\nOut","↶\nUndo","↷\nRedo"},new View.OnClickListener[]{v->cycleFade(true),v->cycleFade(false),v->undo(),v->redo()});
        Button save=Ui.secondary(this,"حفظ المشروع كمسودة");save.setOnClickListener(v->saveDraft(true));p.addView(save,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,44)));
        return p;
    }

    private Button tabButton(String key,String text,View.OnClickListener l){Button b=Ui.ghost(this,text);b.setTag(key);b.setTextSize(10);b.setOnClickListener(l);b.setGravity(Gravity.CENTER);b.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this,76),Ui.dp(this,56)));tabButtons.add(b);return b;}
    private Button compactAction(String text,View.OnClickListener l){Button b=Ui.secondary(this,text);b.setTextSize(10);b.setGravity(Gravity.CENTER);b.setAllCaps(false);b.setOnClickListener(l);return b;}
    private void addActionRow(LinearLayout parent,String[] labels,View.OnClickListener[] actions){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=Ui.matchWrap();rp.setMargins(0,Ui.dp(this,5),0,0);for(int i=0;i<labels.length;i++){Button b=compactAction(labels[i],actions[i]);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);if(i>0)bp.setMargins(Ui.dp(this,4),0,0,0);row.addView(b,bp);}parent.addView(row,rp);}

    private void showPanel(String which){
        if(editPanel==null)return;activePanel=which;
        editPanel.setVisibility("edit".equals(which)?View.VISIBLE:View.GONE);audioPanel.setVisibility("audio".equals(which)?View.VISIBLE:View.GONE);textPanel.setVisibility("text".equals(which)?View.VISIBLE:View.GONE);overlayPanel.setVisibility("overlay".equals(which)?View.VISIBLE:View.GONE);canvasPanel.setVisibility("canvas".equals(which)?View.VISIBLE:View.GONE);
        for(Button b:tabButtons){boolean on=which.equals(String.valueOf(b.getTag()));b.setAlpha(on?1f:.62f);b.setTextColor(on?Ui.CYAN:Ui.MUTED);}
        if(!"media".equals(which))action("قسم "+panelName(which));
    }
    private String panelName(String key){if("edit".equals(key))return "التعديل";if("audio".equals(key))return "الصوت";if("text".equals(key))return "النص";if("overlay".equals(key))return "Overlay";if("canvas".equals(key))return "Canvas";return key;}

    private SeekBar sliderRow(LinearLayout parent,String label,int min,int max,int value,SliderListener listener){
        TextView t=Ui.text(this,label+"  "+value,10,Ui.MUTED,true);LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,4),0,0);parent.addView(t,tp);
        SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,value-min)));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean from){int v=progress+min;t.setText(label+"  "+v);if(from)listener.onValue(v);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){saveDraft(false);}});parent.addView(s,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,34)));return s;
    }
    private interface SliderListener{void onValue(int value);}

    private EditText field(LinearLayout parent,String label,String value,boolean multi){TextView l=Ui.text(this,label,10,Ui.MUTED,true);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,3));parent.addView(l,lp);EditText e=Ui.input(this,"",multi);e.setText(value==null?"":value);LinearLayout.LayoutParams ep=Ui.matchWrap();ep.height=Ui.dp(this,multi?64:42);parent.addView(e,ep);e.addTextChangedListener(textWatcher);return e;}
    private final TextWatcher textWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(!restoring){updatePreviewText();action("تم تحديث النص مباشرة");}}public void afterTextChanged(Editable e){}};

    private EditorClip currentClip(){project.ensureClips();if(project.clips.isEmpty())return null;if(project.selectedClipIndex<0||project.selectedClipIndex>=project.clips.size())project.selectedClipIndex=0;return project.clips.get(project.selectedClipIndex);}
    private boolean needClip(String op){if(currentClip()!=null)return true;fail(op+": أضف فيديو أو صورة أولاً");return false;}
    private boolean needVideo(String op){if(!needClip(op))return false;if(!previewIsVideo){fail(op+": الأداة تعمل على الفيديو فقط");return false;}if(!videoPrepared){fail(op+": انتظر حتى تظهر معاينة الفيديو");return false;}return true;}
    private boolean currentIsImage(){EditorClip c=currentClip();return c!=null&&isImageUri(c.uri==null?"":c.uri.toLowerCase(Locale.ROOT));}

    private void refreshClipStrip(){
        if(clipStrip==null)return;clipStrip.removeAllViews();project.ensureClips();
        for(int i=0;i<project.clips.size();i++){final int index=i;EditorClip c=project.clips.get(i);String label=(i+1)+" • "+clipLabel(c);Button b=index==project.selectedClipIndex?Ui.accent(this,label):Ui.secondary(this,label);b.setTextSize(9);b.setOnClickListener(v->{stopProjectPlayback(false);project.selectedClipIndex=index;refreshClipStrip();bindCurrentClip();saveDraft(false);action("تم اختيار المقطع "+(index+1));});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,94),Ui.dp(this,42));p.setMargins(0,0,Ui.dp(this,5),0);clipStrip.addView(b,p);}
        Button add=Ui.ghost(this,"＋");add.setOnClickListener(v->pickMedia());clipStrip.addView(add,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,42)));updateClipInfo();
    }

    private String clipLabel(EditorClip c){String u=c==null||c.uri==null?"":c.uri.toLowerCase(Locale.ROOT);return isImageUri(u)?"IMG":"VIDEO";}

    private void bindCurrentClip(){
        pauseMediaOnly();videoPrepared=false;activeVideoPlayer=null;mediaDurationMs=0;previewIsVideo=false;imagePlayStartedAt=0;
        try{videoPreview.stopPlayback();}catch(Exception ignored){}
        videoPreview.setVisibility(View.GONE);imagePreview.setVisibility(View.GONE);
        EditorClip c=currentClip();
        if(c==null||c.uri==null||c.uri.trim().isEmpty()){
            emptyPreview.setVisibility(View.VISIBLE);timeline.setMax(1000);timeline.setProgress(0);mediaStatus.setText("لا يوجد مقطع");updateClipInfo();applyOverlayPreview();action("أضف فيديو أو صورة لبدء المونتاج");return;
        }
        emptyPreview.setVisibility(View.GONE);Uri uri=Uri.parse(c.uri);String type=mimeOrUri(uri,c.uri);previewIsVideo=type.startsWith("video/")||isVideoUri(type);
        sourceVolume.setProgress(Math.max(0,Math.min(100,c.volume)));zoomSlider.setProgress(Math.max(0,Math.min(80,c.zoom-100)));speedButton.setText(String.format(Locale.US,"السرعة  %.2g×",c.speed));
        if(previewIsVideo){
            videoPreview.setVisibility(View.VISIBLE);videoPreview.setVideoURI(uri);videoPreview.setOnPreparedListener(mp->{activeVideoPlayer=mp;videoPrepared=true;mediaDurationMs=Math.max(1,videoPreview.getDuration());if(c.trimEndMs<=c.trimStartMs||c.trimEndMs>mediaDurationMs)c.trimEndMs=mediaDurationMs;applyVideoVolume();applyPlaybackSpeed();applyClipPreviewTransform();timeline.setMax((int)Math.min(Integer.MAX_VALUE,mediaDurationMs));videoPreview.seekTo((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));timeline.setProgress((int)Math.min(Integer.MAX_VALUE,c.trimStartMs));updateTimeLabel(c.trimStartMs);updateClipInfo();action("الفيديو جاهز للتعديل ✓");if(autoStartAfterBind){autoStartAfterBind=false;startCurrentClipPlayback();}});videoPreview.setOnErrorListener((mp,w,e)->{videoPrepared=false;fail("تعذر تشغيل معاينة هذا الفيديو");if(playingProject)advanceProjectClip();return true;});
        }else{
            imagePreview.setVisibility(View.VISIBLE);imagePreview.setImageURI(uri);mediaDurationMs=Math.max(1000,c.stillDurationMs);timeline.setMax((int)mediaDurationMs);timeline.setProgress(0);updateTimeLabel(0);applyClipPreviewTransform();updateClipInfo();action("الصورة جاهزة للتعديل ✓");if(autoStartAfterBind){autoStartAfterBind=false;handler.postDelayed(this::startCurrentClipPlayback,80);}
        }
        mediaStatus.setText("المقطع "+(project.selectedClipIndex+1)+" من "+project.clips.size()+" • "+clipLabel(c));updateTransformStatus();applyTextStyle();applyOverlayPreview();
    }

    private String mimeOrUri(Uri uri,String fallback){try{String t=getContentResolver().getType(uri);if(t!=null)return t.toLowerCase(Locale.ROOT);}catch(Exception ignored){}return fallback==null?"":fallback.toLowerCase(Locale.ROOT);}
    private boolean isVideoUri(String s){return s.contains(".mp4")||s.contains(".webm")||s.contains(".mkv")||s.contains(".mov")||s.contains(".m4v");}
    private boolean isImageUri(String s){return s.contains("image/")||s.endsWith(".jpg")||s.endsWith(".jpeg")||s.endsWith(".png")||s.endsWith(".webp");}

    private void toggleProjectPlay(){if(project.clips.isEmpty()){fail("أضف فيديو أو صورة أولاً");return;}if(playingProject){stopProjectPlayback(false);action("إيقاف المعاينة");return;}playingProject=true;playButton.setText("❚❚");action("تشغيل المشروع…");startCurrentClipPlayback();}
    private void startCurrentClipPlayback(){EditorClip c=currentClip();if(c==null){stopProjectPlayback(false);return;}if(previewIsVideo){if(!videoPrepared){autoStartAfterBind=true;action("جاري تجهيز الفيديو…");return;}int pos=videoPreview.getCurrentPosition();if(pos<c.trimStartMs||pos>=effectiveEnd(c)-50)videoPreview.seekTo((int)c.trimStartMs);applyPlaybackSpeed();applyVideoVolume();try{videoPreview.start();}catch(Exception e){fail("تعذر بدء التشغيل");}}else{imagePlayStartedAt=System.currentTimeMillis();timeline.setProgress(0);}syncAuxPlayers(true);playButton.setText("❚❚");}
    private void stopProjectPlayback(boolean finished){playingProject=false;autoStartAfterBind=false;imagePlayStartedAt=0;pauseMediaOnly();if(playButton!=null)playButton.setText("▶");if(finished)action("انتهت معاينة المشروع ✓");}
    private void pauseMediaOnly(){try{if(videoPreview!=null&&videoPreview.isPlaying())videoPreview.pause();}catch(Exception ignored){}try{if(musicPlayer!=null&&musicPlayer.isPlaying())musicPlayer.pause();}catch(Exception ignored){}try{if(voicePlayer!=null&&voicePlayer.isPlaying())voicePlayer.pause();}catch(Exception ignored){}}
    private void advanceProjectClip(){if(!playingProject)return;int next=project.selectedClipIndex+1;if(next>=project.clips.size()){stopProjectPlayback(true);return;}project.selectedClipIndex=next;autoStartAfterBind=true;refreshClipStrip();bindCurrentClip();}
    private void selectRelativeClip(int delta){if(project.clips.isEmpty()){fail("Timeline فارغ");return;}stopProjectPlayback(false);int n=Math.max(0,Math.min(project.clips.size()-1,project.selectedClipIndex+delta));if(n==project.selectedClipIndex){action(delta<0?"أنت عند أول مقطع":"أنت عند آخر مقطع");return;}project.selectedClipIndex=n;refreshClipStrip();bindCurrentClip();action("المقطع "+(n+1));}

    private void seekRelative(long delta){if(!needVideo("التقديم/التأخير"))return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition()+delta;p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c)-50,p));videoPreview.seekTo((int)p);timeline.setProgress((int)p);syncAuxPlayers(false);updateTimeLabel(p);action("المؤشر "+fmt(p));}
    private void seekTo(int value){EditorClip c=currentClip();if(c==null)return;if(previewIsVideo&&videoPrepared){long p=Math.max(c.trimStartMs,Math.min(effectiveEnd(c),value));videoPreview.seekTo((int)p);syncAuxPlayers(false);updateTimeLabel(p);}else if(!previewIsVideo){timeline.setProgress(value);imagePlayStartedAt=0;syncAuxPlayers(false);updateTimeLabel(value);}}
    private long effectiveEnd(EditorClip c){if(c==null)return mediaDurationMs;long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;return Math.max(c.trimStartMs+100,Math.min(mediaDurationMs,end));}

    private void setTrimStart(){if(!needVideo("Trim In"))return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p>=effectiveEnd(c)-300){fail("حرّك المؤشر للخلف قبل Trim In");return;}pushUndo();c.trimStartMs=p;saveDraft(false);updateClipInfo();action("Trim In = "+fmt(p));}
    private void setTrimEnd(){if(!needVideo("Trim Out"))return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();if(p<=c.trimStartMs+300){fail("حرّك المؤشر للأمام قبل Trim Out");return;}pushUndo();c.trimEndMs=p;saveDraft(false);updateClipInfo();action("Trim Out = "+fmt(p));}
    private void splitClip(){if(!needVideo("Split"))return;EditorClip c=currentClip();long p=videoPreview.getCurrentPosition();long end=effectiveEnd(c);if(p<=c.trimStartMs+300||p>=end-300){fail("ضع المؤشر داخل المقطع بعيدًا عن البداية والنهاية");return;}pushUndo();EditorClip right=c.copy();right.trimStartMs=p;right.trimEndMs=end;c.trimEndMs=p;project.clips.add(project.selectedClipIndex+1,right);project.selectedClipIndex++;project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();action("تم تقسيم المقطع عند "+fmt(p)+" ✓");}
    private void duplicateClip(){if(!needClip("نسخ المقطع"))return;EditorClip c=currentClip();pushUndo();project.clips.add(project.selectedClipIndex+1,c.copy());project.selectedClipIndex++;project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();action("تم نسخ المقطع ✓");}
    private void moveClip(int delta){if(!needClip("تحريك المقطع"))return;if(project.clips.size()<2){fail("أضف أكثر من مقطع للتحريك");return;}int from=project.selectedClipIndex,to=from+delta;if(to<0||to>=project.clips.size()){action(delta<0?"لا يمكن التحريك أكثر لليسار":"لا يمكن التحريك أكثر لليمين");return;}pushUndo();Collections.swap(project.clips,from,to);project.selectedClipIndex=to;project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();action("تم تغيير ترتيب المقطع ✓");}
    private void deleteClip(){if(!needClip("حذف المقطع"))return;pushUndo();int old=project.selectedClipIndex;project.clips.remove(old);if(project.selectedClipIndex>=project.clips.size())project.selectedClipIndex=Math.max(0,project.clips.size()-1);project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();action("تم حذف المقطع ✓");}
    private void resetClip(){if(!needClip("Reset"))return;EditorClip c=currentClip();pushUndo();c.trimStartMs=0;c.trimEndMs=mediaDurationMs>0?mediaDurationMs:0;c.volume=100;c.speed=1f;c.rotation=0;c.zoom=100;c.mirror=false;c.filter="None";saveDraft(false);bindCurrentClip();action("تمت إعادة إعدادات المقطع ✓");}

    private void pickMedia(){action("اختر فيديو أو صورة من الجهاز…");Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK_MEDIA);}
    private void pickAudio(){action("اختر ملف موسيقى…");Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");startActivityForResult(i,PICK_AUDIO);}
    private void pickOverlay(){action("اختر صورة أو لوجو…");Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_OVERLAY);}

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null){action("تم إلغاء الاختيار");return;}
        if(request==PICK_MEDIA){ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){ClipData clips=data.getClipData();for(int x=0;x<clips.getItemCount();x++)uris.add(clips.getItemAt(x).getUri());}else if(data.getData()!=null)uris.add(data.getData());if(uris.isEmpty()){fail("لم يتم اختيار ميديا");return;}pushUndo();for(Uri uri:uris){persist(uri,data);project.clips.add(new EditorClip(uri.toString()));}project.selectedClipIndex=Math.max(0,project.clips.size()-uris.size());project.syncPrimarySource();saveDraft(false);refreshClipStrip();bindCurrentClip();action("تمت إضافة "+uris.size()+" ملف للمشروع ✓");}
        else if(request==PICK_AUDIO&&data.getData()!=null){pushUndo();Uri uri=data.getData();persist(uri,data);project.audioUri=uri.toString();releaseMusicPlayer();saveDraft(false);updateAudioStatus();action("تمت إضافة الموسيقى ✓");}
        else if(request==PICK_OVERLAY&&data.getData()!=null){pushUndo();Uri uri=data.getData();persist(uri,data);project.overlayUri=uri.toString();project.overlayStartMs=0;project.overlayEndMs=0;saveDraft(false);applyOverlayPreview();updateOverlayStatus();action("تمت إضافة Overlay ✓");}
    }
    private void persist(Uri uri,Intent data){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}}

    private void cycleSpeed(){if(!needClip("السرعة"))return;EditorClip c=currentClip();pushUndo();int index=0;float best=999;for(int i=0;i<speeds.length;i++){float d=Math.abs(speeds[i]-c.speed);if(d<best){best=d;index=i;}}c.speed=speeds[(index+1)%speeds.length];speedButton.setText(String.format(Locale.US,"السرعة  %.2g×",c.speed));applyPlaybackSpeed();saveDraft(false);updateClipInfo();action(String.format(Locale.US,"السرعة %.2g× ✓",c.speed));}
    private void applyPlaybackSpeed(){if(activeVideoPlayer==null)return;EditorClip c=currentClip();try{activeVideoPlayer.setPlaybackParams(activeVideoPlayer.getPlaybackParams().setSpeed(c==null?1f:c.speed));}catch(Exception ignored){}}
    private void applyVideoVolume(){EditorClip c=currentClip();if(activeVideoPlayer==null||c==null)return;float v=c.volume/100f;try{activeVideoPlayer.setVolume(v,v);}catch(Exception ignored){}}
    private void rotateClip(){if(!needClip("Rotate"))return;EditorClip c=currentClip();pushUndo();c.rotation=(c.rotation+90)%360;applyClipPreviewTransform();updateTransformStatus();saveDraft(false);action("Rotate "+c.rotation+"° ✓");}
    private void toggleMirror(){if(!needClip("Mirror"))return;EditorClip c=currentClip();pushUndo();c.mirror=!c.mirror;applyClipPreviewTransform();updateTransformStatus();saveDraft(false);action(c.mirror?"Mirror ON ✓":"Mirror OFF ✓");}
    private void cycleFilter(){if(!needClip("Filter"))return;EditorClip c=currentClip();pushUndo();int idx=0;for(int i=0;i<filters.length;i++)if(filters[i].equals(c.filter))idx=i;c.filter=filters[(idx+1)%filters.length];updateTransformStatus();saveDraft(false);action("Filter: "+c.filter+" • سيطبق في التصدير");}
    private void applyClipPreviewTransform(){EditorClip c=currentClip();if(c==null)return;float z=Math.max(1f,Math.min(1.8f,c.zoom/100f));float sx=c.mirror?-z:z;if(videoPreview!=null){videoPreview.setRotation(c.rotation);videoPreview.setScaleX(sx);videoPreview.setScaleY(z);}if(imagePreview!=null){imagePreview.setRotation(c.rotation);imagePreview.setScaleX(sx);imagePreview.setScaleY(z);}}
    private void updateTransformStatus(){if(transformStatus==null)return;EditorClip c=currentClip();if(c==null){transformStatus.setText("");return;}transformStatus.setText(c.rotation+"° • Zoom "+c.zoom+"% • "+(c.mirror?"Mirror":"Normal")+" • "+c.filter);}

    private void cycleRatio(){pushUndo();int i=0;for(int x=0;x<ratios.length;x++)if(ratios[x].equals(project.aspectRatio))i=x;project.aspectRatio=ratios[(i+1)%ratios.length];ratioButton.setText(project.aspectRatio+"  المقاس");ratioChip.setText(project.aspectRatio);saveDraft(false);action("Canvas "+project.aspectRatio+" ✓");}
    private void cycleTransition(){pushUndo();int i=0;for(int x=0;x<transitions.length;x++)if(transitions[x].equals(project.transitionStyle))i=x;project.transitionStyle=transitions[(i+1)%transitions.length];transitionButton.setText(project.transitionStyle+"  Transition");saveDraft(false);action("Transition: "+project.transitionStyle+" ✓");}
    private void cycleFade(boolean in){pushUndo();int old=in?project.fadeInMs:project.fadeOutMs;int next=old==0?250:old==250?500:old==500?1000:0;if(in)project.fadeInMs=next;else project.fadeOutMs=next;saveDraft(false);action((in?"Fade In ":"Fade Out ")+next+"ms ✓");}
    private void cycleMusicFade(boolean in){pushUndo();int old=in?project.musicFadeInMs:project.musicFadeOutMs;int next=old==0?500:old==500?1000:old==1000?2000:0;if(in)project.musicFadeInMs=next;else project.musicFadeOutMs=next;saveDraft(false);updateAudioStatus();action((in?"Music Fade In ":"Music Fade Out ")+next+"ms ✓");}

    private void toggleText(){pushUndo();project.showText=!project.showText;applyTextStyle();saveDraft(false);action(project.showText?"النص ظاهر ✓":"تم إخفاء النص ✓");}
    private void cycleTextColor(){pushUndo();colorIndex=(colorIndex+1)%textColors.length;project.textColor=textColors[colorIndex];applyTextStyle();saveDraft(false);action("تم تغيير لون النص ✓");}
    private void cycleCaptionStyle(){pushUndo();String s=project.captionStyle==null?"Bold Highlight":project.captionStyle;if("Bold Highlight".equals(s))project.captionStyle="Clean";else if("Clean".equals(s))project.captionStyle="Subtitle";else project.captionStyle="Bold Highlight";applyTextStyle();saveDraft(false);action("Style: "+project.captionStyle+" ✓");}
    private void cycleTextFont(){pushUndo();int i=0;for(int x=0;x<fonts.length;x++)if(fonts[x].equals(project.textFont))i=x;project.textFont=fonts[(i+1)%fonts.length];applyTextStyle();saveDraft(false);action("Font: "+project.textFont+" ✓");}
    private void updatePreviewText(){if(hook==null)return;project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);project.name=text(name);previewHook.setText(project.hook);previewBody.setText(project.body);previewCta.setText(project.cta);}
    private void applyTextStyle(){if(previewHook==null)return;float k=project.textScale/100f;previewHook.setTextSize(22*k);previewBody.setTextSize(14*k);previewCta.setTextSize(12*k);previewHook.setTextColor(project.textColor);previewBody.setTextColor(project.textColor);previewCta.setTextColor(project.textColor);int visibility=project.showText?View.VISIBLE:View.GONE;previewHook.setVisibility(visibility);previewBody.setVisibility(visibility);previewCta.setVisibility(visibility);float y=(project.textYPercent-50)*3.0f;previewHook.setTranslationY(y);previewBody.setTranslationY(y);previewCta.setTranslationY(y);boolean bold="Bold Highlight".equals(project.captionStyle);int style=bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL;android.graphics.Typeface base="Serif".equals(project.textFont)?android.graphics.Typeface.SERIF:"Mono".equals(project.textFont)?android.graphics.Typeface.MONOSPACE:android.graphics.Typeface.SANS_SERIF;previewHook.setTypeface(base,style);previewBody.setTypeface(base,android.graphics.Typeface.NORMAL);previewCta.setTypeface(base,style);}
    private void setImageDuration(long ms){if(!needClip("مدة الصورة"))return;if(!currentIsImage()){fail("حدد صورة لتغيير مدتها");return;}EditorClip c=currentClip();pushUndo();c.stillDurationMs=ms;mediaDurationMs=ms;timeline.setMax((int)ms);saveDraft(false);updateClipInfo();action("مدة الصورة "+(ms/1000)+" ثواني ✓");}
    private void updateClipInfo(){if(clipInfo==null)return;EditorClip c=currentClip();if(c==null){clipInfo.setText("Timeline فارغ");return;}long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:mediaDurationMs;clipInfo.setText("Clip "+(project.selectedClipIndex+1)+"/"+project.clips.size()+" • "+String.format(Locale.US,"%.2g×",c.speed)+" • "+fmt(c.trimStartMs)+" → "+fmt(end)+" • Project "+fmt(totalProjectDurationMs()));}

    private void changeAudioOffset(long delta){if(empty(project.audioUri)){fail("أضف موسيقى أولاً");return;}pushUndo();project.audioOffsetMs=Math.max(-30000,Math.min(30000,project.audioOffsetMs+delta));saveDraft(false);updateAudioStatus();syncAuxPlayers(false);action("توقيت الموسيقى "+String.format(Locale.US,"%.1fs",project.audioOffsetMs/1000f));}
    private void removeAudio(){if(empty(project.audioUri)){action("لا توجد موسيقى لإزالتها");return;}pushUndo();project.audioUri="";releaseMusicPlayer();saveDraft(false);updateAudioStatus();action("تمت إزالة الموسيقى ✓");}
    private void updateAudioStatus(){if(audioStatus==null)return;audioStatus.setText((empty(project.audioUri)?"لا توجد موسيقى":"موسيقى مضافة ✓")+" • offset "+String.format(Locale.US,"%.1fs",project.audioOffsetMs/1000f)+" • Fade "+project.musicFadeInMs+"/"+project.musicFadeOutMs);if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);updateVoiceStatus();}

    private void toggleVoiceoverRecording(){if(voiceRecording){stopVoiceoverRecording();return;}if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){action("نحتاج إذن الميكروفون للـ Voiceover");requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}startVoiceoverRecording();}
    private void startVoiceoverRecording(){try{pushUndo();File dir=new File(getFilesDir(),"voiceovers");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException();voiceRecordingFile=new File(dir,"voice_"+System.currentTimeMillis()+".m4a");voiceRecordOffsetMs=projectPlayheadMs();voiceRecorder=new MediaRecorder();voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);voiceRecorder.setAudioSamplingRate(44100);voiceRecorder.setAudioEncodingBitRate(128000);voiceRecorder.setOutputFile(voiceRecordingFile.getAbsolutePath());voiceRecorder.prepare();voiceRecorder.start();voiceRecording=true;if(voiceStatus!=null)voiceStatus.setText("● جاري تسجيل Voiceover…");action("● تسجيل Voiceover… اضغط مرة ثانية للإيقاف");if(!playingProject)toggleProjectPlay();}catch(Exception e){releaseRecorder();voiceRecording=false;fail("فشل تسجيل Voiceover");}}
    private void stopVoiceoverRecording(){if(!voiceRecording){releaseRecorder();return;}voiceRecording=false;boolean ok=true;try{voiceRecorder.stop();}catch(Exception e){ok=false;}releaseRecorder();if(ok&&voiceRecordingFile!=null&&voiceRecordingFile.exists()&&voiceRecordingFile.length()>512){project.voiceoverUri=Uri.fromFile(voiceRecordingFile).toString();project.voiceoverOffsetMs=voiceRecordOffsetMs;releaseVoicePlayer();saveDraft(false);action("تم حفظ Voiceover ✓");}else{try{if(voiceRecordingFile!=null)voiceRecordingFile.delete();}catch(Exception ignored){}fail("لم يتم حفظ التسجيل");}updateVoiceStatus();}
    private void releaseRecorder(){try{if(voiceRecorder!=null)voiceRecorder.release();}catch(Exception ignored){}voiceRecorder=null;}
    private void removeVoiceover(){if(voiceRecording)stopVoiceoverRecording();if(empty(project.voiceoverUri)){action("لا يوجد Voiceover لإزالته");return;}pushUndo();project.voiceoverUri="";project.voiceoverOffsetMs=0;releaseVoicePlayer();saveDraft(false);updateVoiceStatus();action("تمت إزالة Voiceover ✓");}
    private void updateVoiceStatus(){if(voiceStatus==null)return;voiceStatus.setText((empty(project.voiceoverUri)?"لا يوجد Voiceover":"Voiceover مضاف ✓")+" • يبدأ "+fmt(project.voiceoverOffsetMs));if(voiceVolume!=null)voiceVolume.setProgress(project.voiceoverVolume);}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_MIC){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startVoiceoverRecording();else fail("اسمح بالميكروفون لتسجيل Voiceover");}}

    private void ensureAuxPlayers(){if(!empty(project.audioUri)&&musicPlayer==null){try{musicPlayer=new MediaPlayer();musicPlayer.setDataSource(this,Uri.parse(project.audioUri));musicPlayer.setLooping(true);musicPlayer.prepare();}catch(Exception e){releaseMusicPlayer();fail("تعذر تشغيل الموسيقى في المعاينة");}}if(!empty(project.voiceoverUri)&&voicePlayer==null){try{voicePlayer=new MediaPlayer();voicePlayer.setDataSource(this,Uri.parse(project.voiceoverUri));voicePlayer.setLooping(false);voicePlayer.prepare();}catch(Exception e){releaseVoicePlayer();fail("تعذر تشغيل Voiceover في المعاينة");}}applyAuxVolumes();}
    private void syncAuxPlayers(boolean play){ensureAuxPlayers();syncOnePlayer(musicPlayer,project.audioOffsetMs,play,true);syncOnePlayer(voicePlayer,project.voiceoverOffsetMs,play,false);}
    private void syncOnePlayer(MediaPlayer p,long offset,boolean play,boolean loop){if(p==null)return;try{long target=projectPlayheadMs()-offset;if(target<0){if(p.isPlaying())p.pause();p.seekTo(0);return;}int d=p.getDuration();if(d<=0)return;if(loop)target%=d;else if(target>=d){if(p.isPlaying())p.pause();return;}if(Math.abs(p.getCurrentPosition()-target)>650)p.seekTo((int)target);if(play&&!p.isPlaying())p.start();else if(!play&&p.isPlaying())p.pause();}catch(Exception ignored){}}
    private void applyAuxVolumes(){float m=project.audioVolume/100f,v=project.voiceoverVolume/100f;try{if(musicPlayer!=null)musicPlayer.setVolume(m,m);}catch(Exception ignored){}try{if(voicePlayer!=null)voicePlayer.setVolume(v,v);}catch(Exception ignored){}}
    private void releaseMusicPlayer(){try{if(musicPlayer!=null){musicPlayer.stop();musicPlayer.release();}}catch(Exception ignored){}musicPlayer=null;}
    private void releaseVoicePlayer(){try{if(voicePlayer!=null){voicePlayer.stop();voicePlayer.release();}}catch(Exception ignored){}voicePlayer=null;}
    private void releaseAuxPlayers(){releaseMusicPlayer();releaseVoicePlayer();}

    private void applyOverlayPreview(){if(overlayPreview==null)return;if(empty(project.overlayUri)){overlayPreview.setVisibility(View.GONE);updateOverlayStatus();return;}try{overlayPreview.setImageURI(Uri.parse(project.overlayUri));overlayPreview.setAlpha(project.overlayOpacity/100f);int fw=previewFrame.getWidth(),fh=previewFrame.getHeight();if(fw<=0||fh<=0){previewFrame.postDelayed(this::applyOverlayPreview,100);return;}int size=Math.max(Ui.dp(this,36),(int)(fw*(project.overlayScale/100f)));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(size,size);lp.gravity=Gravity.TOP|Gravity.START;int x=(int)(fw*(project.overlayXPercent/100f)-size/2f),y=(int)(fh*(project.overlayYPercent/100f)-size/2f);lp.leftMargin=Math.max(0,Math.min(fw-size,x));lp.topMargin=Math.max(0,Math.min(fh-size,y));overlayPreview.setLayoutParams(lp);updateOverlayVisibility();updateOverlayStatus();}catch(Exception e){overlayPreview.setVisibility(View.GONE);fail("تعذر عرض Overlay");}}
    private void updateOverlayVisibility(){if(overlayPreview==null||empty(project.overlayUri)){if(overlayPreview!=null)overlayPreview.setVisibility(View.GONE);return;}long p=projectPlayheadMs();long end=project.overlayEndMs>project.overlayStartMs?project.overlayEndMs:Long.MAX_VALUE;overlayPreview.setVisibility(p>=project.overlayStartMs&&p<=end?View.VISIBLE:View.GONE);}
    private void setOverlayTiming(boolean start){if(empty(project.overlayUri)){fail("اختر Overlay أولاً");return;}pushUndo();long p=projectPlayheadMs();if(start){project.overlayStartMs=p;if(project.overlayEndMs>0&&project.overlayEndMs<=p)project.overlayEndMs=0;action("بداية Overlay = "+fmt(p));}else{if(p<=project.overlayStartMs){fail("حرّك المؤشر بعد بداية Overlay");return;}project.overlayEndMs=p;action("نهاية Overlay = "+fmt(p));}saveDraft(false);updateOverlayStatus();updateOverlayVisibility();}
    private void removeOverlay(){if(empty(project.overlayUri)){action("لا يوجد Overlay لإزالته");return;}pushUndo();project.overlayUri="";project.overlayStartMs=0;project.overlayEndMs=0;saveDraft(false);applyOverlayPreview();action("تمت إزالة Overlay ✓");}
    private void updateOverlayStatus(){if(overlayStatus==null)return;String end=project.overlayEndMs>project.overlayStartMs?fmt(project.overlayEndMs):"end";overlayStatus.setText(empty(project.overlayUri)?"لا يوجد Overlay":"Overlay ✓ • "+project.overlayScale+"% • "+fmt(project.overlayStartMs)+" → "+end);}

    private long projectPlayheadMs(){long total=0;for(int i=0;i<project.clips.size();i++){EditorClip c=project.clips.get(i);if(i<project.selectedClipIndex)total+=estimateClipDuration(c);else if(i==project.selectedClipIndex){if(previewIsVideo&&videoPrepared){long pos=videoPreview.getCurrentPosition();total+=(long)(Math.max(0,pos-c.trimStartMs)/Math.max(.5f,c.speed));}else if(!previewIsVideo&&playingProject&&imagePlayStartedAt>0)total+=Math.max(0,System.currentTimeMillis()-imagePlayStartedAt);else if(!previewIsVideo&&timeline!=null)total+=(long)(timeline.getProgress()/Math.max(.5f,c.speed));break;}}return total;}
    private long totalProjectDurationMs(){long total=0;for(EditorClip c:project.clips)total+=estimateClipDuration(c);return total;}
    private long estimateClipDuration(EditorClip c){long raw;if(isImageUri(c.uri==null?"":c.uri.toLowerCase(Locale.ROOT)))raw=c.stillDurationMs;else{long end=c.trimEndMs>c.trimStartMs?c.trimEndMs:c.trimStartMs+5000;raw=Math.max(500,end-c.trimStartMs);}return (long)(raw/Math.max(.5f,c.speed));}

    private final Runnable ticker=new Runnable(){@Override public void run(){try{EditorClip c=currentClip();if(c!=null){if(previewIsVideo&&videoPrepared&&!timelineTouching){long p=videoPreview.getCurrentPosition();timeline.setProgress((int)Math.min(Integer.MAX_VALUE,p));updateTimeLabel(p);if(playingProject&&videoPreview.isPlaying()&&p>=effectiveEnd(c)-80){advanceProjectClip();handler.postDelayed(this,100);return;}}else if(!previewIsVideo&&playingProject&&imagePlayStartedAt>0&&!timelineTouching){long elapsed=Math.max(0,System.currentTimeMillis()-imagePlayStartedAt);long raw=(long)(elapsed*Math.max(.5f,c.speed));timeline.setProgress((int)Math.min(mediaDurationMs,raw));updateTimeLabel(Math.min(mediaDurationMs,raw));if(raw>=mediaDurationMs-30){advanceProjectClip();handler.postDelayed(this,100);return;}}if(playingProject)syncAuxPlayers(true);updateOverlayVisibility();}}catch(Exception ignored){}handler.postDelayed(this,120);}};
    private void updateTimeLabel(long p){if(timeLabel!=null)timeLabel.setText(fmt(p)+" / "+fmt(mediaDurationMs)+"   •   Project "+fmt(projectPlayheadMs())+" / "+fmt(totalProjectDurationMs()));}
    private String fmt(long ms){if(ms<0)ms=0;long min=ms/60000;double sec=(ms%60000)/1000.0;return String.format(Locale.US,"%02d:%04.1f",min,sec);}

    private void pullFields(){if(name!=null){project.name=text(name);project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);}if(project.name==null||project.name.trim().isEmpty())project.name="Video Project";project.syncPrimarySource();}
    private void saveDraft(boolean notify){pullFields();project.updatedAt=System.currentTimeMillis();DraftStore.save(this,project);if(notify)action("تم حفظ المسودة ✓",Ui.GREEN);}
    private void pushUndo(){try{String s=project.toJson().toString();if(undoStack.size()>=30)undoStack.removeFirst();undoStack.addLast(s);redoStack.clear();}catch(Exception ignored){}}
    private void undo(){if(undoStack.isEmpty()){action("لا يوجد شيء للتراجع");return;}stopProjectPlayback(false);try{redoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(undoStack.removeLast()));releaseAuxPlayers();restoreUiFromProject();refreshClipStrip();bindCurrentClip();action("Undo ✓");}catch(Exception e){fail("Undo failed");}}
    private void redo(){if(redoStack.isEmpty()){action("لا يوجد شيء للإعادة");return;}stopProjectPlayback(false);try{undoStack.addLast(project.toJson().toString());project=CreatorProject.fromJson(new JSONObject(redoStack.removeLast()));releaseAuxPlayers();restoreUiFromProject();refreshClipStrip();bindCurrentClip();action("Redo ✓");}catch(Exception e){fail("Redo failed");}}

    private void restoreUiFromProject(){restoring=true;try{project.ensureClips();if(name!=null)name.setText(project.name==null?"":project.name);if(hook!=null)hook.setText(project.hook==null?"":project.hook);if(body!=null)body.setText(project.body==null?"":project.body);if(cta!=null)cta.setText(project.cta==null?"":project.cta);if(hashtags!=null)hashtags.setText(project.hashtags==null?"":project.hashtags);if(textScale!=null)textScale.setProgress(project.textScale-60);if(textY!=null)textY.setProgress(project.textYPercent-10);if(audioVolume!=null)audioVolume.setProgress(project.audioVolume);if(voiceVolume!=null)voiceVolume.setProgress(project.voiceoverVolume);if(overlayScale!=null)overlayScale.setProgress(project.overlayScale-10);if(overlayX!=null)overlayX.setProgress(project.overlayXPercent);if(overlayY!=null)overlayY.setProgress(project.overlayYPercent);if(overlayOpacity!=null)overlayOpacity.setProgress(project.overlayOpacity-10);if(ratioButton!=null)ratioButton.setText(project.aspectRatio+"  المقاس");if(ratioChip!=null)ratioChip.setText(project.aspectRatio);if(transitionButton!=null)transitionButton.setText(project.transitionStyle+"  Transition");updatePreviewText();applyTextStyle();updateAudioStatus();updateVoiceStatus();updateOverlayStatus();applyOverlayPreview();}finally{restoring=false;}}

    private void export(){
        if(voiceRecording)stopVoiceoverRecording();pullFields();project.ensureClips();if(project.clips.isEmpty()){fail("أضف فيديو أو صورة قبل التصدير");return;}DraftStore.save(this,project);stopProjectPlayback(false);exportButton.setEnabled(false);exportButton.setAlpha(.55f);action("جاري تجهيز الفيديو…",Ui.YELLOW);
        executor.execute(()->ReelExporter.export(this,project,new ReelExporter.Callback(){public void onStage(String s){runOnUiThread(()->action(s,Ui.YELLOW));}public void onSuccess(File file){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);action("تم التصدير بنجاح ✓",Ui.GREEN);Intent i=new Intent(TemplateEditorActivity.this,PublishingActivity.class);i.putExtra("project_id",project.id);i.putExtra("video_path",file.getAbsolutePath());startActivity(i);});}public void onError(String m){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);fail(m);});}}));
    }

    private void action(String s){action(s,Ui.CYAN);}
    private void action(String s,int color){if(actionStatus!=null){actionStatus.setTextColor(color);actionStatus.setText(s==null?"":s);}}
    private void fail(String s){action(s,Ui.RED);Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
}
