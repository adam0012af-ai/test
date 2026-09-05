package com.adam.downloadhub;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchActivity extends Activity {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private EditText input;
    private TextView status;
    private volatile boolean running;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackground(Ui.gradient(Ui.BG,Ui.SURFACE,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,22),Ui.dp(this,16),Ui.dp(this,30));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=Ui.text(this,"Batch Downloader",27,Ui.TEXT,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=Ui.text(this,"ضع رابطًا في كل سطر. التطبيق يجرب المحرك السريع ثم المحرك المتقدم تلقائيًا لكل رابط.",13,Ui.MUTED,false);sub.setGravity(Gravity.CENTER);sub.setLineSpacing(0,1.2f);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,7),0,Ui.dp(this,16));root.addView(sub,sp);

        LinearLayout card=Ui.card(this);input=new EditText(this);input.setHint("https://...\nhttps://...\nhttps://...");input.setTextColor(Ui.TEXT);input.setHintTextColor(Ui.MUTED);input.setTextDirection(View.TEXT_DIRECTION_LTR);input.setGravity(Gravity.TOP|Gravity.START);input.setMinLines(10);input.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12));input.setBackground(Ui.bordered(Ui.BG,Ui.BORDER,1,14,this));card.addView(input,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,250)));
        Button start=Ui.primary(this,"إضافة أفضل الخيارات");start.setOnClickListener(v->startBatch());LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,54);bp.setMargins(0,Ui.dp(this,12),0,0);card.addView(start,bp);root.addView(card);

        LinearLayout state=Ui.card(this);LinearLayout.LayoutParams stp=Ui.matchWrap();stp.setMargins(0,Ui.dp(this,14),0,0);state.setLayoutParams(stp);state.addView(Ui.text(this,"الحالة",13,Ui.MUTED,true));status=Ui.text(this,"جاهز",14,Ui.GREEN,false);status.setLineSpacing(0,1.2f);LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,7),0,0);state.addView(status,tp);root.addView(state);
        return scroll;
    }

    private void startBatch(){
        if(running){Toast.makeText(this,"هناك Batch يعمل بالفعل",Toast.LENGTH_SHORT).show();return;}
        String raw=input.getText()==null?"":input.getText().toString();String[] lines=raw.split("\\r?\\n");List<String> urls=new ArrayList<>();for(String s:lines){s=s.trim();if(s.startsWith("http://")||s.startsWith("https://")){if(AppPrefs.linkCleaner(this))s=LinkTools.clean(s);urls.add(s);}if(urls.size()>=100)break;}
        if(urls.isEmpty()){Toast.makeText(this,"أضف روابط أولًا",Toast.LENGTH_SHORT).show();return;}
        running=true;status.setText("بدء معالجة "+urls.size()+" رابط…");
        executor.execute(()->{
            int ok=0,fail=0;
            for(int i=0;i<urls.size();i++){
                String u=urls.get(i);final int pos=i+1;
                runOnUiThread(()->status.setText("الرابط "+pos+" / "+urls.size()+"…"));
                try{
                    PlatformExtractor.MediaBundle b;
                    try{b=PlatformExtractor.extractOptions(u);}catch(Exception first){b=YtDlpResolver.extractOptions(this,u);}
                    MediaOption pick=pick(b.options);if(pick==null)throw new IllegalStateException("لا يوجد خيار مناسب");
                    DownloadEngine.enqueue(this,pick);HistoryStore.add(this,u,"Batch • "+b.platform);ok++;
                }catch(Exception e){fail++;}
            }
            int a=ok,f=fail;running=false;
            runOnUiThread(()->status.setText("اكتمل Batch ✅\nتمت الإضافة: "+a+"\nتعذر: "+f+"\nراجع مدير التحميلات لمتابعة التقدم."));
        });
    }

    private MediaOption pick(List<MediaOption> options){if(options==null||options.isEmpty())return null;if("audio".equals(AppPrefs.defaultMode(this))){for(MediaOption o:options)if(o.audioOnly)return o;}for(MediaOption o:options)if(!o.audioOnly)return o;return options.get(0);}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
