package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class DownloadsActivity extends Activity {
    private LinearLayout listBox;
    private TextView summary;
    private DownloadManager dm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int filter = 0;
    private boolean resumed;
    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            refresh();
            handler.postDelayed(this, 1800L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG); getWindow().setNavigationBarColor(Ui.BG);
        dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        setContentView(buildUi());
    }

    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(refresher);handler.post(refresher);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(refresher);super.onPause();}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackground(Ui.gradient(Ui.BG,Ui.SURFACE,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,20),Ui.dp(this,16),Ui.dp(this,30));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=Ui.text(this,"مدير التحميلات",27,Ui.TEXT,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        summary=Ui.text(this,"",13,Ui.MUTED,false);summary.setGravity(Gravity.CENTER_HORIZONTAL);LinearLayout.LayoutParams sm=Ui.matchWrap();sm.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,16));root.addView(summary,sm);

        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.addView(filterButton("الكل",0),new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f));
        LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);p1.setMargins(Ui.dp(this,6),0,0,0);filters.addView(filterButton("جاري",1),p1);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);p2.setMargins(Ui.dp(this,6),0,0,0);filters.addView(filterButton("مكتمل",2),p2);
        LinearLayout.LayoutParams p3=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);p3.setMargins(Ui.dp(this,6),0,0,0);filters.addView(filterButton("فشل",3),p3);
        root.addView(filters);

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh=Ui.primary(this,"تحديث الآن");refresh.setOnClickListener(v->refresh());actions.addView(refresh,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        Button clear=Ui.secondary(this,"مسح السجل");clear.setOnClickListener(v->{DownloadStore.clear(this);refresh();});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);cp.setMargins(Ui.dp(this,8),0,0,0);actions.addView(clear,cp);
        LinearLayout.LayoutParams ap=Ui.matchWrap();ap.setMargins(0,Ui.dp(this,10),0,0);root.addView(actions,ap);

        listBox=new LinearLayout(this);listBox.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams lp=Ui.matchWrap();lp.setMargins(0,Ui.dp(this,14),0,0);root.addView(listBox,lp);
        return scroll;
    }

    private Button filterButton(String label,int f){Button b=Ui.secondary(this,label);b.setTextSize(12);b.setOnClickListener(v->{filter=f;refresh();});return b;}

    private void refresh(){
        if(listBox==null)return;listBox.removeAllViews();List<DownloadStore.Item> items=DownloadStore.list(this);
        int active=0,done=0,failed=0,shown=0;
        for(DownloadStore.Item item:items){StatusInfo info=query(item.id);if(info.active())active++;if(info.status==DownloadManager.STATUS_SUCCESSFUL)done++;if(info.status==DownloadManager.STATUS_FAILED)failed++;if(matches(info)){listBox.addView(buildRow(item,info));shown++;}}
        summary.setText(String.format(Locale.US,"الإجمالي %d  •  جاري %d  •  مكتمل %d  •  فشل %d",items.size(),active,done,failed));
        if(shown==0){TextView empty=Ui.text(this,"لا توجد تحميلات في هذا القسم.",15,Ui.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,Ui.dp(this,36),0,Ui.dp(this,36));listBox.addView(empty);}
    }

    private boolean matches(StatusInfo i){if(filter==0)return true;if(filter==1)return i.active();if(filter==2)return i.status==DownloadManager.STATUS_SUCCESSFUL;return i.status==DownloadManager.STATUS_FAILED;}

    private View buildRow(DownloadStore.Item item,StatusInfo info){
        LinearLayout card=Ui.card(this);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,0,0,Ui.dp(this,10));card.setLayoutParams(mp);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=Ui.text(this,info.status==DownloadManager.STATUS_SUCCESSFUL?"✓":info.active()?"↓":"!",22,info.color,true);icon.setGravity(Gravity.CENTER);top.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,38)));
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);TextView name=Ui.text(this,item.name,15,Ui.TEXT,true);name.setMaxLines(2);texts.addView(name);texts.addView(Ui.text(this,info.label,12,info.color,false));LinearLayout.LayoutParams tx=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);tx.setMargins(Ui.dp(this,8),0,0,0);top.addView(texts,tx);card.addView(top);

        if(info.progress>=0&&info.status!=DownloadManager.STATUS_SUCCESSFUL){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(info.progress);LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,8);bp.setMargins(0,Ui.dp(this,10),0,0);card.addView(bar,bp);}
        TextView bytes=Ui.text(this,info.bytesText,11,Ui.MUTED,false);LinearLayout.LayoutParams byp=Ui.matchWrap();byp.setMargins(0,Ui.dp(this,7),0,Ui.dp(this,9));card.addView(bytes,byp);

        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
        if(info.status==DownloadManager.STATUS_SUCCESSFUL){Button open=Ui.primary(this,"فتح");open.setOnClickListener(v->openDownload(item.id));buttons.addView(open,new LinearLayout.LayoutParams(0,Ui.dp(this,43),1f));}
        else if(info.active()){Button cancel=Ui.secondary(this,"إلغاء");cancel.setOnClickListener(v->{dm.remove(item.id);Toast.makeText(this,"تم إلغاء التحميل",Toast.LENGTH_SHORT).show();refresh();});buttons.addView(cancel,new LinearLayout.LayoutParams(0,Ui.dp(this,43),1f));}
        else if(info.status==DownloadManager.STATUS_FAILED){Button retry=Ui.primary(this,"إعادة المحاولة");retry.setOnClickListener(v->retry(item));buttons.addView(retry,new LinearLayout.LayoutParams(0,Ui.dp(this,43),1f));}
        Button remove=Ui.secondary(this,"إزالة من السجل");remove.setOnClickListener(v->{DownloadStore.remove(this,item.id);refresh();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,Ui.dp(this,43),1f);rp.setMargins(Ui.dp(this,7),0,0,0);buttons.addView(remove,rp);card.addView(buttons);
        return card;
    }

    private void retry(DownloadStore.Item item){
        Intent i=new Intent(this,MainActivity.class);i.putExtra(MainActivity.EXTRA_PREFILL_URL,item.url);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);
    }

    private StatusInfo query(long id){
        DownloadManager.Query q=new DownloadManager.Query().setFilterById(id);
        try(Cursor c=dm.query(q)){
            if(c==null||!c.moveToFirst())return new StatusInfo(-1,"غير موجود في مدير التحميلات",Ui.YELLOW,-1,"—");
            int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));long done=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));long total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));int progress=total>0?(int)Math.min(100L,(done*100L)/total):-1;String size=formatBytes(done)+(total>0?" / "+formatBytes(total):"");
            switch(status){case DownloadManager.STATUS_SUCCESSFUL:return new StatusInfo(status,"مكتمل ✅",Ui.GREEN,100,size);case DownloadManager.STATUS_RUNNING:return new StatusInfo(status,progress>=0?"جاري التحميل • "+progress+"%":"جاري التحميل",Ui.CYAN,progress,size);case DownloadManager.STATUS_PENDING:return new StatusInfo(status,"في الانتظار",Ui.YELLOW,progress,size);case DownloadManager.STATUS_PAUSED:return new StatusInfo(status,"متوقف مؤقتًا"+(progress>=0?" • "+progress+"%":""),Ui.YELLOW,progress,size);case DownloadManager.STATUS_FAILED:int reason=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));return new StatusInfo(status,"فشل • كود "+reason,Ui.RED,progress,size);default:return new StatusInfo(status,"حالة "+status,Ui.MUTED,progress,size);}
        }catch(Exception e){return new StatusInfo(-1,"تعذر قراءة الحالة",Ui.RED,-1,"—");}
    }

    private void openDownload(long id){try{Uri uri=dm.getUriForDownloadedFile(id);if(uri==null)throw new IllegalStateException();Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){Toast.makeText(this,"تعذر فتح الملف",Toast.LENGTH_SHORT).show();}}
    private String formatBytes(long b){if(b<0)return"—";if(b<1024)return b+" B";double k=b/1024.0;if(k<1024)return new DecimalFormat("0.0").format(k)+" KB";double m=k/1024.0;if(m<1024)return new DecimalFormat("0.0").format(m)+" MB";return new DecimalFormat("0.00").format(m/1024.0)+" GB";}

    private static final class StatusInfo{final int status;final String label;final int color;final int progress;final String bytesText;StatusInfo(int status,String label,int color,int progress,String bytesText){this.status=status;this.label=label;this.color=color;this.progress=progress;this.bytesText=bytesText;}boolean active(){return status==DownloadManager.STATUS_RUNNING||status==DownloadManager.STATUS_PENDING||status==DownloadManager.STATUS_PAUSED;}}
}
