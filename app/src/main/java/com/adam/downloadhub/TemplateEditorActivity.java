package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateEditorActivity extends Activity {
    private static final int PICK_MEDIA=501;
    private static final int PICK_AUDIO=502;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private CreatorProject project;
    private EditText name,hook,body,cta,hashtags;
    private LinearLayout preview;
    private TextView previewHook,previewBody,previewCta,mediaStatus,exportStatus;
    private Button exportButton;
    private int styleIndex=0;
    private final int[][] styles={{0xFF0C4C8B,0xFF07162D},{0xFF126C61,0xFF092A32},{0xFF5B42C5,0xFF141B3F},{0xFF7A334D,0xFF281425},{0xFF74521C,0xFF261B0D}};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        loadProject();setContentView(buildUi());syncFields();
        if(getIntent().getBooleanExtra("pick_media",false))preview.postDelayed(v->pickMedia(),350);
    }

    private void loadProject(){
        String draft=getIntent().getStringExtra("draft_id");
        if(draft!=null)project=DraftStore.get(this,draft);
        if(project==null){String id=getIntent().getStringExtra("template_id");project=CreatorProject.fromTemplate(TemplateCatalog.byId(id));}
    }

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,34));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.topBar(this,"محرر الريلز",project.categoryName+" • مشروع قابل للتعديل",v->finish()));

        preview=new LinearLayout(this);preview.setOrientation(LinearLayout.VERTICAL);preview.setGravity(Gravity.CENTER);preview.setPadding(Ui.dp(this,22),Ui.dp(this,30),Ui.dp(this,22),Ui.dp(this,30));preview.setBackground(Ui.gradient(project.startColor,project.endColor,25,this));
        LinearLayout.LayoutParams pp=Ui.matchWrap();pp.height=Ui.dp(this,430);pp.setMargins(0,Ui.dp(this,14),0,0);root.addView(preview,pp);
        TextView ratio=Ui.chip(this,"9:16 • "+project.durationSec+" ثانية",Ui.CYAN);preview.addView(ratio);
        previewHook=Ui.text(this,project.hook,23,Ui.TEXT,true);previewHook.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ph=Ui.matchWrap();ph.setMargins(0,Ui.dp(this,40),0,Ui.dp(this,14));preview.addView(previewHook,ph);
        previewBody=Ui.text(this,project.body,14,0xFFE2ECFA,false);previewBody.setGravity(Gravity.CENTER);previewBody.setMaxLines(6);preview.addView(previewBody);
        previewCta=Ui.text(this,project.cta,13,Ui.CYAN,true);previewCta.setGravity(Gravity.CENTER);LinearLayout.LayoutParams pc=Ui.matchWrap();pc.setMargins(0,Ui.dp(this,28),0,0);preview.addView(previewCta,pc);

        LinearLayout media=Ui.card(this);LinearLayout.LayoutParams me=Ui.matchWrap();me.setMargins(0,Ui.dp(this,12),0,0);media.setLayoutParams(me);media.addView(Ui.text(this,"الميديا والصوت",17,Ui.TEXT,true));
        mediaStatus=Ui.text(this,"الخلفية: تصميم القالب • الصوت: بدون صوت مخصص",12,Ui.MUTED,false);LinearLayout.LayoutParams ms=Ui.matchWrap();ms.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,10));media.addView(mediaStatus,ms);
        LinearLayout mr=new LinearLayout(this);mr.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=Ui.secondary(this,"▣  فيديو / صورة");pick.setOnClickListener(v->pickMedia());mr.addView(pick,new LinearLayout.LayoutParams(0,Ui.dp(this,49),1f));
        Button aud=Ui.secondary(this,"♫  إضافة صوت");aud.setOnClickListener(v->pickAudio());LinearLayout.LayoutParams au=new LinearLayout.LayoutParams(0,Ui.dp(this,49),1f);au.setMargins(Ui.dp(this,8),0,0,0);mr.addView(aud,au);media.addView(mr);root.addView(media);

        LinearLayout edit=Ui.card(this);LinearLayout.LayoutParams ep=Ui.matchWrap();ep.setMargins(0,Ui.dp(this,12),0,0);edit.setLayoutParams(ep);edit.addView(Ui.text(this,"النص والمحتوى",17,Ui.TEXT,true));
        name=field(edit,"اسم المشروع",project.name,false);
        hook=field(edit,"العنوان الرئيسي / Hook",project.hook,true);
        body=field(edit,"النص",project.body,true);
        cta=field(edit,"النهاية / CTA",project.cta,true);
        hashtags=field(edit,"هاشتاجات أولية",project.hashtags,true);
        root.addView(edit);

        LinearLayout style=Ui.card(this);LinearLayout.LayoutParams st=Ui.matchWrap();st.setMargins(0,Ui.dp(this,12),0,0);style.setLayoutParams(st);style.addView(Ui.text(this,"الشكل والمدة",17,Ui.TEXT,true));
        Button change=Ui.secondary(this,"✦  تغيير ستايل الألوان");change.setOnClickListener(v->cycleStyle());LinearLayout.LayoutParams ch=Ui.matchWrap();ch.height=Ui.dp(this,48);ch.setMargins(0,Ui.dp(this,10),0,0);style.addView(change,ch);
        LinearLayout times=new LinearLayout(this);times.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams ts=Ui.matchWrap();ts.setMargins(0,Ui.dp(this,8),0,0);
        times.addView(duration("15 ث",15),new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));LinearLayout.LayoutParams d2=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);d2.setMargins(Ui.dp(this,7),0,0,0);times.addView(duration("30 ث",30),d2);LinearLayout.LayoutParams d3=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);d3.setMargins(Ui.dp(this,7),0,0,0);times.addView(duration("45 ث",45),d3);style.addView(times,ts);root.addView(style);

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams ac=Ui.matchWrap();ac.setMargins(0,Ui.dp(this,14),0,0);
        Button save=Ui.secondary(this,"حفظ كمسودة");save.setOnClickListener(v->saveDraft(true));actions.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1f));
        exportButton=Ui.accent(this,"تصدير Reel");exportButton.setOnClickListener(v->export());LinearLayout.LayoutParams ex=new LinearLayout.LayoutParams(0,Ui.dp(this,54),1f);ex.setMargins(Ui.dp(this,8),0,0,0);actions.addView(exportButton,ex);root.addView(actions,ac);
        exportStatus=Ui.text(this,"جاهز للتعديل والتصدير",12,Ui.GREEN,true);exportStatus.setGravity(Gravity.CENTER);LinearLayout.LayoutParams es=Ui.matchWrap();es.setMargins(0,Ui.dp(this,10),0,0);root.addView(exportStatus,es);
        return scroll;
    }

    private EditText field(LinearLayout parent,String label,String value,boolean multi){
        TextView l=Ui.text(this,label,12,Ui.MUTED,true);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,5));parent.addView(l,lp);
        EditText e=Ui.input(this,"",multi);e.setText(value);LinearLayout.LayoutParams ip=Ui.matchWrap();ip.height=Ui.dp(this,multi?88:52);parent.addView(e,ip);e.addTextChangedListener(watcher);return e;
    }

    private final TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(preview!=null)updatePreview();}public void afterTextChanged(Editable e){}};

    private void syncFields(){updateMediaStatus();updatePreview();}
    private void updatePreview(){if(hook!=null)previewHook.setText(text(hook));if(body!=null)previewBody.setText(text(body));if(cta!=null)previewCta.setText(text(cta));}

    private void pickMedia(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/*"});startActivityForResult(i,PICK_MEDIA);}
    private void pickAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");startActivityForResult(i,PICK_AUDIO);}

    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}if(request==PICK_MEDIA)project.sourceUri=uri.toString();else if(request==PICK_AUDIO)project.audioUri=uri.toString();updateMediaStatus();saveDraft(false);}

    private void updateMediaStatus(){if(mediaStatus==null)return;String m=empty(project.sourceUri)?"تصميم القالب":"فيديو/صورة محددة";String a=empty(project.audioUri)?"بدون صوت مخصص":"صوت مخصص";mediaStatus.setText("الخلفية: "+m+" • الصوت: "+a);}
    private Button duration(String label,int sec){Button b=Ui.secondary(this,label);b.setOnClickListener(v->{project.durationSec=sec;toast("المدة: "+sec+" ثانية");});return b;}
    private void cycleStyle(){styleIndex=(styleIndex+1)%styles.length;project.startColor=styles[styleIndex][0];project.endColor=styles[styleIndex][1];preview.setBackground(Ui.gradient(project.startColor,project.endColor,25,this));}

    private void pullFields(){project.name=text(name);project.hook=text(hook);project.body=text(body);project.cta=text(cta);project.hashtags=text(hashtags);if(project.name.isEmpty())project.name="Reel Project";}
    private void saveDraft(boolean notify){pullFields();DraftStore.save(this,project);if(notify)toast("تم حفظ المسودة ✓");}

    private void export(){
        pullFields();DraftStore.save(this,project);exportButton.setEnabled(false);exportButton.setAlpha(.55f);exportStatus.setTextColor(Ui.YELLOW);exportStatus.setText("جاري تجهيز التصدير…");
        executor.execute(()->ReelExporter.export(this,project,new ReelExporter.Callback(){
            public void onStage(String s){runOnUiThread(()->exportStatus.setText(s));}
            public void onSuccess(File file){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.GREEN);exportStatus.setText("تم التصدير ✓");Intent i=new Intent(TemplateEditorActivity.this,PublishingActivity.class);i.putExtra("project_id",project.id);i.putExtra("video_path",file.getAbsolutePath());startActivity(i);});}
            public void onError(String m){runOnUiThread(()->{exportButton.setEnabled(true);exportButton.setAlpha(1f);exportStatus.setTextColor(Ui.RED);exportStatus.setText(m);toast(m);});}
        }));
    }

    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
