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
    private EditText title,caption,hashtags;
    private LinearLayout suggestions;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        String id=getIntent().getStringExtra("project_id");
        project=DraftStore.get(this,id);
        if(project==null)project=CreatorProject.fromTemplate(TemplateCatalog.byId("islamic-0"));
        String path=getIntent().getStringExtra("video_path");
        video=path==null?null:new File(path);
        setContentView(buildUi());
        generate();
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

        root.addView(Ui.topBar(this,"TikTok Publishing","تجهيز الفيديو والنص ثم فتح TikTok",v->finish()));

        LinearLayout done=Ui.card(this);
        LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,14),0,0);done.setLayoutParams(dp);
        done.setBackground(Ui.gradient(0xFF0C5A51,0xFF0A2733,22,this));
        done.addView(Ui.text(this,"✓  الفيديو جاهز للنشر على TikTok",19,Ui.TEXT,true));
        long mb=video!=null&&video.exists()?video.length()/1024/1024:0;
        done.addView(Ui.text(this,(video==null?"ملف غير موجود":video.getName())+(mb>0?" • "+mb+" MB":""),11,0xFFC8EBDD,false));
        TextView mode=Ui.text(this,"سيتم إرسال ملف الفيديو الحقيقي إلى TikTok. آخر ضغطة «نشر» تتم داخل TikTok.",12,Ui.GREEN,true);
        LinearLayout.LayoutParams md=Ui.matchWrap();md.setMargins(0,Ui.dp(this,8),0,0);done.addView(mode,md);
        root.addView(done);

        LinearLayout form=Ui.card(this);
        LinearLayout.LayoutParams fp=Ui.matchWrap();fp.setMargins(0,Ui.dp(this,14),0,0);form.setLayoutParams(fp);
        form.addView(Ui.text(this,"محتوى TikTok",15,Ui.CYAN,true));
        title=field(form,"العنوان","",false);
        caption=field(form,"الوصف / Caption","",true);
        hashtags=field(form,"الهاشتاجات","",true);
        Button regen=Ui.secondary(this,"✦  توليد عنوان وهاشتاج من جديد");
        regen.setOnClickListener(v->generate());
        LinearLayout.LayoutParams rg=Ui.matchWrap();rg.height=Ui.dp(this,48);rg.setMargins(0,Ui.dp(this,10),0,0);form.addView(regen,rg);
        suggestions=new LinearLayout(this);suggestions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sg=Ui.matchWrap();sg.setMargins(0,Ui.dp(this,8),0,0);form.addView(suggestions,sg);
        root.addView(form);

        Button publish=Ui.accent(this,"فتح TikTok وتجهيز النشر");
        publish.setTextSize(16);
        publish.setOnClickListener(v->publishToTikTok());
        LinearLayout.LayoutParams pub=Ui.matchWrap();pub.height=Ui.dp(this,62);pub.setMargins(0,Ui.dp(this,14),0,0);root.addView(publish,pub);

        Button copy=Ui.secondary(this,"نسخ العنوان والوصف والهاشتاجات");
        copy.setOnClickListener(v->{copyPublishText(composeText());toast("تم نسخ النص ✓");});
        LinearLayout.LayoutParams cp=Ui.matchWrap();cp.height=Ui.dp(this,52);cp.setMargins(0,Ui.dp(this,8),0,0);root.addView(copy,cp);

        TextView note=Ui.text(this,"لو TikTok لم يضع الـCaption تلقائيًا، النص يكون منسوخًا بالفعل في الحافظة وتقدر تلصقه فورًا قبل النشر.",11,Ui.MUTED,false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams no=Ui.matchWrap();no.setMargins(0,Ui.dp(this,10),0,0);root.addView(note,no);
        return scroll;
    }

    private EditText field(LinearLayout parent,String label,String value,boolean multi){
        TextView l=Ui.text(this,label,12,Ui.MUTED,true);
        LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,5));parent.addView(l,lp);
        EditText e=Ui.input(this,"",multi);e.setText(value);
        LinearLayout.LayoutParams ep=Ui.matchWrap();ep.height=Ui.dp(this,multi?100:52);parent.addView(e,ep);
        return e;
    }

    private void generate(){
        if(title==null)return;
        ContentAssistant.PublishText p=ContentAssistant.generate(project,"TikTok");
        title.setText(p.titles.isEmpty()?project.name:p.titles.get(0));
        caption.setText(p.caption);
        hashtags.setText(p.hashtags);
        suggestions.removeAllViews();
        List<String> titles=p.titles;
        for(int i=0;i<titles.size();i++){
            String t=titles.get(i);
            Button b=Ui.ghost(this,"عنوان "+(i+1)+": "+t);
            b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v->title.setText(t));
            LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,48);bp.setMargins(0,0,0,Ui.dp(this,6));suggestions.addView(b,bp);
        }
    }

    private void publishToTikTok(){
        if(video==null||!video.exists()||video.length()<1024){toast("ملف الفيديو غير موجود أو غير صالح");return;}
        Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",video);
        String publishText=composeText();
        copyPublishText(publishText);

        Intent base=new Intent(Intent.ACTION_SEND);
        base.setType("video/mp4");
        base.putExtra(Intent.EXTRA_STREAM,uri);
        base.putExtra(Intent.EXTRA_SUBJECT,text(title));
        base.putExtra(Intent.EXTRA_TITLE,text(title));
        base.putExtra(Intent.EXTRA_TEXT,publishText);
        base.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        base.setClipData(ClipData.newRawUri("Download Hub Reel",uri));

        String[] packages={"com.zhiliaoapp.musically","com.ss.android.ugc.trill"};
        for(String pkg:packages){
            try{
                grantUriPermission(pkg,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent targeted=new Intent(base);targeted.setPackage(pkg);
                startActivity(targeted);
                toast("تم فتح TikTok بالفيديو • راجع واضغط نشر");
                return;
            }catch(ActivityNotFoundException ignored){}catch(Exception ignored){}
        }
        toast("TikTok غير مثبت على الجهاز");
    }

    private void copyPublishText(String value){
        try{
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("TikTok caption",value));
        }catch(Exception ignored){}
    }

    private String composeText(){
        StringBuilder b=new StringBuilder();String t=text(title),c=text(caption),h=text(hashtags);
        if(!t.isEmpty())b.append(t);
        if(!c.isEmpty()){if(b.length()>0)b.append("\n\n");b.append(c);}
        if(!h.isEmpty()){if(b.length()>0)b.append("\n\n");b.append(h);}
        return b.toString();
    }
    private String text(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
