package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UniversalSearchActivity extends Activity {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private EditText query;
    private LinearLayout results;
    private TextView status;
    private Button searchButton;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());
    }

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));page.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),0);
        page.addView(Ui.topBar(this,"بحث وتحميل","ابحث باسم الفيديو بدون رابط",v->finish()));
        LinearLayout box=Ui.card(this);LinearLayout.LayoutParams bp=Ui.matchWrap();bp.setMargins(0,Ui.dp(this,14),0,0);box.setLayoutParams(bp);
        query=Ui.input(this,"اكتب اسم فيديو، قناة أو موضوع…",false);box.addView(query,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,54)));
        searchButton=Ui.accent(this,"⌕  بحث");searchButton.setOnClickListener(v->search());LinearLayout.LayoutParams sp=Ui.matchWrap();sp.height=Ui.dp(this,54);sp.setMargins(0,Ui.dp(this,9),0,0);box.addView(searchButton,sp);page.addView(box);
        status=Ui.text(this,"اكتب ما تبحث عنه ثم اضغط بحث",12,Ui.MUTED,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams st=Ui.matchWrap();st.setMargins(0,Ui.dp(this,10),0,Ui.dp(this,8));page.addView(status,st);
        ScrollView scroll=new ScrollView(this);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);results.setPadding(0,0,0,Ui.dp(this,28));scroll.addView(results,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));return page;
    }

    private void search(){
        String q=query.getText().toString().trim();if(q.length()<2){toast("اكتب كلمة بحث");return;}setBusy(true);status.setText("جاري البحث بالمحرك المتقدم…");results.removeAllViews();
        executor.execute(()->{
            try{
                YoutubeDL.getInstance().init(getApplicationContext());
                YoutubeDLRequest r=new YoutubeDLRequest("ytsearch20:"+q);r.addOption("--flat-playlist");r.addOption("--dump-json");r.addOption("--no-warnings");r.addOption("--ignore-errors");
                YoutubeDLResponse response=YoutubeDL.getInstance().execute(r);List<Result> found=parse(response.getOut());
                runOnUiThread(()->{setBusy(false);render(found);});
            }catch(Throwable e){String m=e.getMessage();runOnUiThread(()->{setBusy(false);status.setText("تعذر البحث: "+shortMsg(m));});}
        });
    }

    private List<Result> parse(String out){
        List<Result> list=new ArrayList<>();if(out==null)return list;String[] lines=out.split("\\r?\\n");
        for(String line:lines){line=line.trim();if(!line.startsWith("{"))continue;try{JSONObject o=new JSONObject(line);String id=o.optString("id","");String title=o.optString("title","فيديو");String uploader=o.optString("uploader",o.optString("channel",""));long duration=Math.round(o.optDouble("duration",0));String url=o.optString("webpage_url","");if(!url.startsWith("http")){String raw=o.optString("url","");if(raw.startsWith("http"))url=raw;else if(!id.isEmpty())url="https://www.youtube.com/watch?v="+id;}if(url.startsWith("http"))list.add(new Result(title,uploader,url,duration));}catch(Exception ignored){}
        }return list;
    }

    private void render(List<Result> list){
        if(list.isEmpty()){status.setText("لم يتم العثور على نتائج");return;}status.setText("تم العثور على "+list.size()+" نتيجة • اختر فيديو لتجهيز الجودة");int n=1;
        for(Result r:list){LinearLayout card=Ui.card(this);LinearLayout.LayoutParams cp=Ui.matchWrap();cp.setMargins(0,0,0,Ui.dp(this,9));card.setLayoutParams(cp);
            LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);TextView num=Ui.chip(this,String.valueOf(n++),Ui.CYAN);head.addView(num);TextView title=Ui.text(this,r.title,15,Ui.TEXT,true);title.setMaxLines(2);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);tp.setMargins(Ui.dp(this,9),0,0,0);head.addView(title,tp);card.addView(head);
            String meta=(r.uploader.isEmpty()?"YouTube":r.uploader)+(r.duration>0?" • "+formatDuration(r.duration):"");TextView m=Ui.text(this,meta,11,Ui.MUTED,false);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,7),0,Ui.dp(this,10));card.addView(m,mp);
            Button dl=Ui.primary(this,"اختيار الجودة وتحميل");dl.setOnClickListener(v->openDownload(r.url));card.addView(dl,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,49)));results.addView(card);}
    }

    private void openDownload(String url){Intent i=new Intent(this,MainActivity.class);i.putExtra(MainActivity.EXTRA_PREFILL_URL,url);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);}
    private void setBusy(boolean b){searchButton.setEnabled(!b);searchButton.setAlpha(b?.55f:1f);searchButton.setText(b?"جاري البحث…":"⌕  بحث");}
    private String formatDuration(long s){return String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private String shortMsg(String s){if(s==null||s.trim().isEmpty())return"خطأ في المحرك";s=s.trim().replace('\n',' ');return s.length()>140?s.substring(0,140)+"…":s;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static final class Result{final String title,uploader,url;final long duration;Result(String t,String u,String x,long d){title=t;uploader=u;url=x;duration=d;}}
}
