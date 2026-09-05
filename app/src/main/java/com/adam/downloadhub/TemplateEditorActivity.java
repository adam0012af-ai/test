package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateEditorActivity extends Activity {
    private static final int PICK_MEDIA=501;
    private static final int PICK_AUDIO=502;

    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private CreatorProject project;
    private EditText name,hook,body,cta,hashtags;
    private FrameLayout previewFrame;
    private VideoView videoPreview;
    private ImageView imagePreview;
    private TextView emptyPreview,previewHook,previewBody,previewCta,mediaStatus,exportStatus;
    private Button exportButton,previewPlayButton;
    private boolean previewIsVideo=false;
    private boolean videoPrepared=false;
    private int styleIndex=0;
    private final int[][] styles={{0xFF0C4C8B,0xFF07162D},{0xFF126C61,0xFF092A32},{0xFF5B42C5,0xFF141B3F},{0xFF7A334D,0xFF281425},{0xFF74521C,0xFF261B0D}};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        loadProject();
        setContentView(buildUi());
        syncFields();
        showSelectedMedia();
        if(getIntent().getBooleanExtra("pick_media",false))previewFrame.postDelayed(this::pickMedia,350);
    }

    @Override protected void onPause(){
        if(videoPreview!=null&&videoPreview.isPlaying())videoPreview.pause();
        super.onPause();
    }

    @Override protected void onDestroy(){
        try{if(videoPreview!=null)videoPreview.stopPlayback();}catch(Exception ignored){}
        executor.shutdownNow();
        super.onDestroy();
    }

    private void loadProject(){
        String draft=getIntent().getStringExtra("draft_id");
        if(draft!=null)project=DraftStore.get(this,draft);
        if(project==null){
            String id=getIntent().getStringExtra("template_id");
            project=CreatorProject.fromTemplate(TemplateCatalog.byId(id));
        }
    }

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,34));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.topBar(this,"محرر الريلز",project.categoryName+" • معاينة ومونتاج فعلي",v->finish()));

        previewFrame=new FrameLayout(this);
        previewFrame.setBackground(Ui.gradient(project.startColor,project.endColor,25,this));
        LinearLayout.LayoutParams pp=Ui.matchWrap();
        pp.height=Ui.dp(this,500);
        pp.setMargins(0,Ui.dp(this,14),0,0);
        root.addView(previewFrame,pp);

        videoPreview=new VideoView(this);
        videoPreview.setVisibility(View.GONE);
        previewFrame.addView(videoPreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        imagePreview=new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);
        previewFrame.addView(imagePreview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        emptyPreview=Ui.text(this,"اختار فيديو أو صورة علشان تظهر المعاينة هنا",15,Ui.MUTED,true);
        emptyPreview.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams em=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        em.setMargins(Ui.dp(this,26),Ui.dp(this,26),Ui.dp(this,26),Ui.dp(this,26));
        previewFrame.addView(emptyPreview,em);

        LinearLayout overlay=new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setPadding(Ui.dp(this,22),Ui.dp(this,30),Ui.dp(this,22),Ui.dp(this,30));
        FrameLayout.LayoutParams ov=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        previewFrame.addView(overlay,ov);

        TextView ratio=Ui.chip(this,"9:16 • "+project.durationSec+" ثانية",Ui.CYAN);
        overlay.addView(ratio);
        previewHook=Ui.text(this,project.hook,23,Ui.TEXT,true);
        previewHook.setGravity(Gravity.CENTER);
        previewHook.setShadowLayer(7,0,2,0xFF000000);
        LinearLayout.LayoutParams ph=Ui.matchWrap();ph.setMargins(0,Ui.dp(this,38),0,Ui.dp(this,14));overlay.addView(previewHook,ph);
        previewBody=Ui.text(this,project.body,14,0xFFF1F5FA,false);
        previewBody.setGravity(Gravity.CENTER);
        previewBody.setShadowLayer(6,0,2,0xFF000000);
        previewBody.setMaxLines(7);
        overlay.addView(previewBody);
        previewCta=Ui.text(this,project.cta,13,Ui.CYAN,true);
        previewCta.setGravity(Gravity.CENTER);
        previewCta.setShadowLayer(6,0,2,0xFF000000);
        LinearLayout.LayoutParams pc=Ui.matchWrap();pc.setMargins(0,Ui.dp(this,28),0,0);overlay.addView(previewCta,pc);

        LinearLayout media=Ui.card(this);
        LinearLayout.LayoutParams me=Ui.matchWrap();me.setMargins(0,Ui.dp(this,12),0,0);media.setLayoutParams(me);
        media.addView(Ui.text(this,"الميديا والصوت",17,Ui.TEXT,true));
        mediaStatus=Ui.text(this,"لا توجد خلفية بعد",12,Ui.MUTED,false);
        LinearLayout.LayoutParams ms=Ui.matchWrap();ms.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,10));media.addView(mediaStatus,ms);

        LinearLayout mr=new LinearLayout(this);mr.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=Ui.secondary(this,"▣  اختيار فيديو / صورة");
        pick.setOnClickListener(v->pickMedia());
        mr.addView(pick,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f));
        Button aud=Ui.secondary(this,"♫  إضافة صوت");
        aud.setOnClickListener(v->pickAudio());
        LinearLayout.LayoutParams au=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f);au.setMargins(Ui.dp(this,8),0,0,0);mr.addView(aud,au);
        media.addView(mr);

        previewPlayButton=Ui.primary(this,"▶  تشغيل المعاينة");
        previewPlayButton.setOnClickListener(v->togglePreview());
        LinearLayout.LayoutParams pb=Ui.matchWrap();pb.height=Ui.dp(this,48);pb.setMargins(0,Ui.dp(this,8),0,0);media.addView(previewPlayButton,pb);
        root.addView(media);

        LinearLayout edit=Ui.card(this);
        LinearLayout.LayoutParams ep=Ui.matchWrap();ep.setMargins(0,Ui.dp(this,12),0,0);edit.setLayoutParams(ep);
        edit.addView(Ui.text(this,"النص والمحتوى",17,Ui.TEXT,true));
        name=field(edit,"اسم المشروع",project.name,false);
        hook=field(edit,"العنوان الرئيسي / Hook",project.hook,true);
        body=field(edit,"النص",project.body,true);
        cta=field(edit,"النهاية / CTA",project.cta,true);
        hashtags=field(edit,"هاشتاجات أولية",project.hashtags,true);
        root.addView(edit);

        LinearLayout style=Ui.card(this);
        LinearLayout.LayoutParams st=Ui.matchWrap();st.setMargins(0,Ui.dp(this,12),0,0);style.setLayoutParams(st);
        style.addView(Ui.text(this,"الشكل والمدة",17,Ui.TEXT,true));
        Button change=Ui.secondary(this,"✦  تغيير ستايل الألوان");
        change.setOnClickListener(v->cycleStyle());
        LinearLayout.LayoutParams ch=Ui.matchWrap();ch.height=Ui.dp(this,48);ch.setMargins(0,Ui.dp(this,10),0,0);style.addView(change,ch);
        LinearLayout times=new LinearLayout(this);times.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ts=Ui.matchWrap();ts.setMargins(0,Ui.dp(this,8),0,0);
        times.addView(duration("15 ث",15),new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));
        LinearLayout.LayoutParams d2=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);d2.setMargins(Ui.dp(this,7),0,0,0);times.addView(duration("30 ث",30),d2);
        LinearLayout.LayoutParams d3=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);d3.setMargins(Ui.dp(this,7),0,0,0);times.addView(duration("45 ث",45),d3);
        style.addView(times,ts);root.addView(style);

        Button save=Ui.secondary(this,"حفظ كمسودة");
        save.setOnClickListener(v->saveDraft(true));
        LinearLayout.LayoutParams sv=Ui.matchWrap();sv.height=Ui.dp(this,50);sv.setMargins(0,Ui.dp(this,14),0,0);root.addView(save,sv);

        exportButton=Ui.accent(this,"تصدير الفيديو MP4 ثم فتح TikTok");
        exportButton.setTextSize(16);
        exportButton.setOnClickListener(v->export());
        LinearLayout.LayoutParams ex=Ui.matchWrap();ex.height=Ui.dp(this,62);ex.setMargins(0,Ui.dp(this,8),0,0);root.addView(exportButton,ex);

        exportStatus=Ui.text(this,"الحالة: جاهز للتعديل والتصدير",12,Ui.GREEN,true);
        exportStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams es=Ui.matchWrap();es.setMargins(0,Ui.dp(this,10),0,0);root.addView(exportStatus,es);
        return scroll;
    }

    private EditText field(LinearLayout parent,String label,String value,boolean multi){
        TextView l=Ui.text(this,label,12,Ui.MUTED,true);
        LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,5));parent.addView(l,lp);
        EditText e=Ui.input(this,"",multi);e.setText(value);
        LinearLayout.LayoutParams ip=Ui.matchWrap();ip.height=Ui.dp(this,multi?88:52);parent.addView(e,ip);
        e.addTextChangedListener(watcher);return e;
    }

    private final TextWatcher watcher=new TextWatcher(){
        public void beforeTextChanged(CharSequence s,int st,int c,int a){}
        public void onTextChanged(CharSequence s,int st,int b,int c){if(previewFrame!=null)updatePreviewText();}
        public void afterTextChanged(Editable e){}
    };

    private void syncFields(){updateMediaStatus();updatePreviewText();}
    private void updatePreviewText(){
        if(hook!=null)previewHook.setText(text(hook));
        if(body!=null)previewBody.setText(text(body));
        if(cta!=null)previewCta.setText(text(cta));
    }

    private void showSelectedMedia(){
        if(videoPreview==null||imagePreview==null||emptyPreview==null)return;
        videoPrepared=false;
        previewIsVideo=false;
        try{videoPreview.stopPlayback();}catch(Exception ignored){}
        videoPreview.setVisibility(View.GONE);
        imagePreview.setVisibility(View.GONE);

        if(empty(project.sourceUri)){
            emptyPreview.setVisibility(View.VISIBLE);
            if(previewPlayButton!=null){previewPlayButton.setText("▶  تشغيل المعاينة");previewPlayButton.setEnabled(false);previewPlayButton.setAlpha(.55f);}
            return;
        }

        Uri uri=Uri.parse(project.sourceUri);
        String type="";
        try{String t=getContentResolver().getType(uri);if(t!=null)type=t.toLowerCase(Locale.ROOT);}catch(Exception ignored){}
        if(type.isEmpty())type=project.sourceUri.toLowerCase(Locale.ROOT);
        previewIsVideo=type.startsWith("video/")||type.contains(".mp4")||type.contains(".mov")||type.contains(".mkv")||type.contains(".webm");
        emptyPreview.setVisibility(View.GONE);

        if(previewIsVideo){
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoURI(uri);
            videoPreview.setOnPreparedListener(mp->{
                videoPrepared=true;
                mp.setLooping(true);
                mp.setVolume(0f,0f);
                try{videoPreview.start();}catch(Exception ignored){}
                if(previewPlayButton!=null){previewPlayButton.setEnabled(true);previewPlayButton.setAlpha(1f);previewPlayButton.setText("❚❚  إيقاف المعاينة");}
            });
            videoPreview.setOnErrorListener((mp,what,extra)->{
                videoPrepared=false;
                toast("تعذر تشغيل معاينة الفيديو، لكنه سيظل متاحًا للتصدير");
                return true;
            });
        }else{
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageURI(uri);
            if(previewPlayButton!=null){previewPlayButton.setEnabled(true);previewPlayButton.setAlpha(1f);previewPlayButton.setText("✓  الصورة ظاهرة في المعاينة");}
        }
    }

    private void togglePreview(){
        if(empty(project.sourceUri)){toast("اختار فيديو أو صورة أولًا");return;}
        if(!previewIsVideo){toast("الصورة ظاهرة بالفعل في المعاينة");return;}
        if(!videoPrepared){toast("المعاينة لسه بتتجهز");return;}
        if(videoPreview.isPlaying()){
            videoPreview.pause();
            previewPlayButton.setText("▶  تشغيل المعاينة");
        }else{
            videoPreview.start();
            previewPlayButton.setText("❚❚  إيقاف المعاينة");
        }
    }

    private void pickMedia(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/*"});
        startActivityForResult(i,PICK_MEDIA);
    }

    private void pickAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        startActivityForResult(i,PICK_AUDIO);
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
        if(request==PICK_MEDIA){
            project.sourceUri=uri.toString();
            showSelectedMedia();
        }else if(request==PICK_AUDIO){
            project.audioUri=uri.toString();
        }
        updateMediaStatus();
        saveDraft(false);
    }

    private void updateMediaStatus(){
        if(mediaStatus==null)return;
        String m=empty(project.sourceUri)?"لا توجد خلفية":"فيديو/صورة مختارة ✓";
        String a=empty(project.audioUri)?"بدون صوت مخصص":"صوت مضاف ✓";
        mediaStatus.setText("الخلفية: "+m+" • الصوت: "+a);
    }

    private Button duration(String label,int sec){
        Button b=Ui.secondary(this,label);
        b.setOnClickListener(v->{project.durationSec=sec;toast("المدة: "+sec+" ثانية");});
        return b;
    }

    private void cycleStyle(){
        styleIndex=(styleIndex+1)%styles.length;
        project.startColor=styles[styleIndex][0];project.endColor=styles[styleIndex][1];
        previewFrame.setBackground(Ui.gradient(project.startColor,project.endColor,25,this));
    }

    private void pullFields(){
        project.name=text(name);project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);
        if(project.name.isEmpty())project.name="Reel Project";
    }

    private void saveDraft(boolean notify){
        pullFields();DraftStore.save(this,project);if(notify)toast("تم حفظ المسودة ✓");
    }

    private void export(){
        if(empty(project.sourceUri)){
            toast("اختار فيديو أو صورة للخلفية قبل التصدير");
            return;
        }
        pullFields();DraftStore.save(this,project);
        exportButton.setEnabled(false);exportButton.setAlpha(.55f);
        exportStatus.setTextColor(Ui.YELLOW);exportStatus.setText("الحالة: جاري تجهيز الفيديو…");
        executor.execute(()->ReelExporter.export(this,project,new ReelExporter.Callback(){
            public void onStage(String s){runOnUiThread(()->exportStatus.setText("الحالة: "+s));}
            public void onSuccess(File file){runOnUiThread(()->{
                exportButton.setEnabled(true);exportButton.setAlpha(1f);
                exportStatus.setTextColor(Ui.GREEN);exportStatus.setText("الحالة: تم التصدير ✓");
                Intent i=new Intent(TemplateEditorActivity.this,PublishingActivity.class);
                i.putExtra("project_id",project.id);i.putExtra("video_path",file.getAbsolutePath());startActivity(i);
            });}
            public void onError(String m){runOnUiThread(()->{
                exportButton.setEnabled(true);exportButton.setAlpha(1f);
                exportStatus.setTextColor(Ui.RED);exportStatus.setText("الحالة: "+m);toast(m);
            });}
        }));
    }

    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
