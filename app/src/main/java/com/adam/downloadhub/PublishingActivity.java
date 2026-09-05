package com.adam.downloadhub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

public class PublishingActivity extends Activity {
    private CreatorProject project;
    private File video;
    private String platform="Instagram";
    private EditText title,caption,hashtags;
    private LinearLayout suggestions;
    private TextView platformLabel;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        String id=getIntent().getStringExtra("project_id");project=DraftStore.get(this,id);
        if(project==null)project=CreatorProject.fromTemplate(TemplateCatalog.byId("cinematic-0"));
        String path=getIntent().getStringExtra("video_path");video=path==null?null:new File(path);
        setContentView(buildUi());generate();
    }

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,34));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.topBar(this,"Publishing Center","جهّز كل شيء وافتح شاشة النشر الرسمية",v->finish()));

        LinearLayout done=Ui.card(this);LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,14),0,0);done.setLayoutParams(dp);done.setBackground(Ui.gradient(0xFF0C5A51,0xFF0A2733,22,this));
        TextView ok=Ui.text(this,"✓  الفيديو جاهز للنشر الفعلي",19,Ui.TEXT,true);done.addView(ok);
        long mb=video!=null&&video.exists()?video.length()/1024/1024:0;done.addView(Ui.text(this,(video==null?"ملف غير موجود":video.getName())+(mb>0?" • "+mb+" MB":""),11,0xFFC8EBDD,false));
        TextView mode=Ui.text(this,"آخر خطوة فقط: اضغط نشر داخل المنصة",12,Ui.GREEN,true);LinearLayout.LayoutParams md=Ui.matchWrap();md.setMargins(0,Ui.dp(this,8),0,0);done.addView(mode,md);root.addView(done);

        TextView ptitle=Ui.sectionTitle(this,"اختار المنصة");LinearLayout.LayoutParams pp=Ui.matchWrap();pp.setMargins(0,Ui.dp(this,20),0,Ui.dp(this,9));root.addView(ptitle,pp);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);r1.addView(platformButton("Instagram"),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));LinearLayout.LayoutParams t1=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);t1.setMargins(Ui.dp(this,8),0,0,0);r1.addView(platformButton("TikTok"),t1);root.addView(r1);
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams r2p=Ui.matchWrap();r2p.setMargins(0,Ui.dp(this,8),0,0);r2.addView(platformButton("YouTube Shorts"),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));LinearLayout.LayoutParams f1=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);f1.setMargins(Ui.dp(this,8),0,0,0);r2.addView(platformButton("Facebook Reels"),f1);root.addView(r2,r2p);

        LinearLayout form=Ui.card(this);LinearLayout.LayoutParams fp=Ui.matchWrap();fp.setMargins(0,Ui.dp(this,14),0,0);form.setLayoutParams(fp);
        platformLabel=Ui.text(this,"محتوى مناسب لـ Instagram",14,Ui.CYAN,true);form.addView(platformLabel);
        title=field(form,"العنوان","",false);caption=field(form,"الوصف / Caption","",true);hashtags=field(form,"الهاشتاجات","",true);
        Button regen=Ui.secondary(this,"✦  اقتراح عنوان وهاشتاج من جديد");regen.setOnClickListener(v->generate());LinearLayout.LayoutParams rg=Ui.matchWrap();rg.height=Ui.dp(this,48);rg.setMargins(0,Ui.dp(this,10),0,0);form.addView(regen,rg);
        suggestions=new LinearLayout(this);suggestions.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams sg=Ui.matchWrap();sg.setMargins(0,Ui.dp(this,8),0,0);form.addView(suggestions,sg);root.addView(form);

        Button publish=Ui.accent(this,"تجهيز وفتح شاشة النشر");publish.setTextSize(16);publish.setOnClickListener(v->publish());LinearLayout.LayoutParams pub=Ui.matchWrap();pub.height=Ui.dp(this,60);pub.setMargins(0,Ui.dp(this,14),0,0);root.addView(publish,pub);
        TextView note=Ui.text(this,"سيتم إرسال ملف الفيديو فعليًا للتطبيق المختار، ونسخ العنوان والوصف والهاشتاجات للحافظة احتياطياً. بعد فتح شاشة النشر راجع المحتوى واضغط «نشر» داخل المنصة.",11,Ui.MUTED,false);note.setGravity(Gravity.CENTER);LinearLayout.LayoutParams no=Ui.matchWrap();no.setMargins(0,Ui.dp(this,10),0,0);root.addView(note,no);
        return scroll;
    }

    private Button platformButton(String label){Button b=Ui.secondary(this,label);b.setOnClickListener(v->{platform=label;generate();});return b;}
    private EditText field(LinearLayout parent,String label,String value,boolean multi){TextView l=Ui.text(this,label,12,Ui.MUTED,true);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,5));parent.addView(l,lp);EditText e=Ui.input(this,"",multi);e.setText(value);LinearLayout.LayoutParams ep=Ui.matchWrap();ep.height=Ui.dp(this,multi?100:52);parent.addView(e,ep);return e;}

    private void generate(){
        if(title==null)return;ContentAssistant.PublishText p=ContentAssistant.generate(project,platform);platformLabel.setText("محتوى مناسب لـ "+platform);title.setText(p.titles.isEmpty()?project.name:p.titles.get(0));caption.setText(p.caption);hashtags.setText(p.hashtags);suggestions.removeAllViews();
        List<String> titles=p.titles;for(int i=0;i<titles.size();i++){String t=titles.get(i);Button b=Ui.ghost(this,"عنوان "+(i+1)+": "+t);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setOnClickListener(v->title.setText(t));LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,48);bp.setMargins(0,0,0,Ui.dp(this,6));suggestions.addView(b,bp);}
    }

    private void publish(){
        if(video==null||!video.exists()||video.length()<1024){toast("ملف الفيديو غير موجود أو غير صالح");return;}
        Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",video);
        String publishText=composeText();copyPublishText(publishText);

        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("video/mp4");
        send.putExtra(Intent.EXTRA_STREAM,uri);
        send.putExtra(Intent.EXTRA_SUBJECT,text(title));
        send.putExtra(Intent.EXTRA_TITLE,text(title));
        send.putExtra(Intent.EXTRA_TEXT,publishText);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri("Download Hub Reel",uri));

        String[] packages=packagesFor(platform);
        for(String pkg:packages){
            try{
                grantUriPermission(pkg,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent targeted=new Intent(send);targeted.setPackage(pkg);startActivity(targeted);
                toast("تم تجهيز الفيديو والنص • اضغط نشر داخل "+platform);
                return;
            }catch(ActivityNotFoundException ignored){}catch(Exception ignored){}
        }

        try{
            Intent chooser=Intent.createChooser(send,"نشر الريل");
            startActivity(chooser);
            toast("التطبيق المختار غير مثبت • اختر منصة النشر من القائمة");
        }catch(Exception ex){toast("لا يوجد تطبيق متاح لاستقبال الفيديو");}
    }

    private void copyPublishText(String value){
        try{
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("Reel title and hashtags",value));
        }catch(Exception ignored){}
    }

    private String[] packagesFor(String p){
        if(p.startsWith("Instagram"))return new String[]{"com.instagram.android"};
        if(p.startsWith("TikTok"))return new String[]{"com.zhiliaoapp.musically","com.ss.android.ugc.trill"};
        if(p.startsWith("YouTube"))return new String[]{"com.google.android.youtube"};
        if(p.startsWith("Facebook"))return new String[]{"com.facebook.katana"};
        return new String[0];
    }

    private String composeText(){StringBuilder b=new StringBuilder();String t=text(title),c=text(caption),h=text(hashtags);if(!t.isEmpty())b.append(t);if(!c.isEmpty()){if(b.length()>0)b.append("\n\n");b.append(c);}if(!h.isEmpty()){if(b.length()>0)b.append("\n\n");b.append(h);}return b.toString();}
    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
