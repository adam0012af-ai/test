package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackground(Ui.gradient(Ui.BG,Ui.SURFACE,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,22),Ui.dp(this,16),Ui.dp(this,30));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=Ui.text(this,"الإحصائيات",27,Ui.TEXT,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=Ui.text(this,"ملخص استخدام Download Hub على هذا الجهاز",13,Ui.MUTED,false);sub.setGravity(Gravity.CENTER_HORIZONTAL);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,18));root.addView(sub,sp);

        List<DownloadStore.Item> downloads=DownloadStore.list(this);List<HistoryStore.Item> history=HistoryStore.list(this);DownloadManager dm=(DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        int complete=0,active=0,failed=0;long totalBytes=0;
        for(DownloadStore.Item item:downloads){try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(item.id))){if(c!=null&&c.moveToFirst()){int st=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));if(st==DownloadManager.STATUS_SUCCESSFUL){complete++;long n=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));if(n>0)totalBytes+=n;}else if(st==DownloadManager.STATUS_FAILED)failed++;else if(st==DownloadManager.STATUS_RUNNING||st==DownloadManager.STATUS_PENDING||st==DownloadManager.STATUS_PAUSED)active++;}}catch(Exception ignored){}}
        int fav=0;Map<String,Integer> kinds=new HashMap<>();for(HistoryStore.Item h:history){if(h.favorite)fav++;String k=h.kind==null?"Other":h.kind;kinds.put(k,kinds.getOrDefault(k,0)+1);}String top="—";int topN=0;for(Map.Entry<String,Integer> e:kinds.entrySet()){if(e.getValue()>topN){top=e.getKey();topN=e.getValue();}}

        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);grid.addView(pair("إجمالي التحميلات",String.valueOf(downloads.size()),"مكتمل",String.valueOf(complete)));LinearLayout.LayoutParams p2=Ui.matchWrap();p2.setMargins(0,Ui.dp(this,9),0,0);grid.addView(pair("جاري الآن",String.valueOf(active),"فشل",String.valueOf(failed)),p2);LinearLayout.LayoutParams p3=Ui.matchWrap();p3.setMargins(0,Ui.dp(this,9),0,0);grid.addView(pair("حجم المكتمل",MediaOption.formatBytes(totalBytes),"المفضلة",String.valueOf(fav)),p3);root.addView(grid);

        LinearLayout card=Ui.card(this);LinearLayout.LayoutParams cp=Ui.matchWrap();cp.setMargins(0,Ui.dp(this,14),0,0);card.setLayoutParams(cp);card.addView(Ui.text(this,"السجل",17,Ui.TEXT,true));card.addView(line("روابط محفوظة",String.valueOf(history.size())));card.addView(line("الأكثر استخدامًا",topN>0?top+" • "+topN:"—"));card.addView(line("الوضع الخاص",AppPrefs.privateMode(this)?"مفعّل":"غير مفعّل"));card.addView(line("تنظيم المجلدات",AppPrefs.organizeFolders(this)?"مفعّل":"غير مفعّل"));root.addView(card);
        return scroll;
    }

    private LinearLayout pair(String a,String av,String b,String bv){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.addView(stat(a,av),new LinearLayout.LayoutParams(0,Ui.dp(this,100),1f));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,100),1f);p.setMargins(Ui.dp(this,9),0,0,0);row.addView(stat(b,bv),p);return row;}
    private LinearLayout stat(String title,String value){LinearLayout c=Ui.card(this);c.setGravity(Gravity.CENTER);c.addView(Ui.text(this,value,23,Ui.CYAN,true));TextView t=Ui.text(this,title,12,Ui.MUTED,false);t.setGravity(Gravity.CENTER);c.addView(t);return c;}
    private View line(String a,String b){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(0,Ui.dp(this,11),0,0);row.addView(Ui.text(this,a,13,Ui.MUTED,false),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));TextView v=Ui.text(this,b,13,Ui.TEXT,true);v.setGravity(Gravity.END);row.addView(v);return row;}
}
