package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class LibraryActivity extends Activity {
    private LinearLayout content;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());}
    @Override protected void onResume(){super.onResume();render();}

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));page.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),0);
        page.addView(Ui.topBar(this,"مكتبتي","المشاريع والريلز المصدرة",v->finish()));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=Ui.matchWrap();rp.setMargins(0,Ui.dp(this,14),0,Ui.dp(this,10));
        Button downloads=Ui.secondary(this,"↓  التحميلات");downloads.setOnClickListener(v->startActivity(new Intent(this,DownloadsActivity.class)));row.addView(downloads,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));
        Button creator=Ui.primary(this,"✦  Creator Studio");creator.setOnClickListener(v->startActivity(new Intent(this,CreatorStudioActivity.class)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);cp.setMargins(Ui.dp(this,8),0,0,0);row.addView(creator,cp);page.addView(row,rp);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,0,0,Ui.dp(this,32));scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));return page;
    }

    private void render(){
        if(content==null)return;content.removeAllViews();List<CreatorProject> drafts=DraftStore.list(this);TextView dt=Ui.sectionTitle(this,"المسودات • "+drafts.size());LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,8));content.addView(dt,dp);
        if(drafts.isEmpty())content.addView(empty("لا توجد مسودات بعد"));else for(int i=0;i<Math.min(12,drafts.size());i++)content.addView(draftCard(drafts.get(i)));
        File dir=ReelExporter.getOutputDir(this);File[] files=dir.listFiles((d,n)->n.toLowerCase().endsWith(".mp4"));if(files==null)files=new File[0];Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));TextView vt=Ui.sectionTitle(this,"الريلز المصدرة • "+files.length);LinearLayout.LayoutParams vp=Ui.matchWrap();vp.setMargins(0,Ui.dp(this,20),0,Ui.dp(this,8));content.addView(vt,vp);
        if(files.length==0)content.addView(empty("صدّر أول Reel وسيظهر هنا"));else for(File f:files)content.addView(videoCard(f));
    }

    private View draftCard(CreatorProject p){LinearLayout c=Ui.card(this);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,0,0,Ui.dp(this,8));c.setLayoutParams(lp);c.addView(Ui.text(this,p.name,15,Ui.TEXT,true));c.addView(Ui.text(this,p.categoryName+" • "+p.durationSec+"ث",11,Ui.MUTED,false));Button edit=Ui.secondary(this,"متابعة التعديل");edit.setOnClickListener(v->{Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("draft_id",p.id);startActivity(i);});LinearLayout.LayoutParams ep=Ui.matchWrap();ep.height=Ui.dp(this,45);ep.setMargins(0,Ui.dp(this,8),0,0);c.addView(edit,ep);return c;}
    private View videoCard(File f){LinearLayout c=Ui.card(this);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,0,0,Ui.dp(this,8));c.setLayoutParams(lp);c.addView(Ui.text(this,f.getName(),14,Ui.TEXT,true));String meta=MediaOption.formatBytes(f.length())+" • "+ DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(f.lastModified()));c.addView(Ui.text(this,meta,11,Ui.MUTED,false));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=Ui.matchWrap();rp.setMargins(0,Ui.dp(this,8),0,0);Button open=Ui.secondary(this,"تشغيل");open.setOnClickListener(v->open(f));row.addView(open,new LinearLayout.LayoutParams(0,Ui.dp(this,45),1f));Button share=Ui.primary(this,"مشاركة / نشر");share.setOnClickListener(v->share(f));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,Ui.dp(this,45),1f);sp.setMargins(Ui.dp(this,7),0,0,0);row.addView(share,sp);c.addView(row,rp);return c;}
    private View empty(String text){LinearLayout c=Ui.card(this);TextView t=Ui.text(this,text,12,Ui.MUTED,false);t.setGravity(Gravity.CENTER);c.addView(t);return c;}
    private Uri uri(File f){return FileProvider.getUriForFile(this,getPackageName()+".files",f);}
    private void open(File f){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri(f),"video/mp4");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("لا يوجد مشغل متاح");}}
    private void share(File f){try{Intent i=new Intent(Intent.ACTION_SEND);i.setType("video/mp4");i.putExtra(Intent.EXTRA_STREAM,uri(f));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"مشاركة الفيديو"));}catch(Exception e){toast("تعذر مشاركة الملف");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
