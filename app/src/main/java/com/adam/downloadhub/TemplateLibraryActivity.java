package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemplateLibraryActivity extends Activity {
    private static final String CATALOG_URL="https://raw.githubusercontent.com/adam0012af-ai/test/main/online/islamic-templates.json";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private LinearLayout listBox;
    private TextView status;
    private ProgressBar loading;

    private static final class OnlineTemplate {
        String id,name,description,hook,body,cta,hashtags,mediaUrl,mediaExt,credit,license,sourcePage;
        int durationSec;
        double approxSizeMb;
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        loadCatalog();
    }

    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));

        LinearLayout top=Ui.topBar(this,"☾  مكتبة الريلز الإسلامي","قوالب فيديو Online • تحميل عند الطلب",v->finish());
        top.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,8));
        page.addView(top);

        LinearLayout info=Ui.card(this);
        LinearLayout.LayoutParams ip=Ui.matchWrap();ip.setMargins(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,10));info.setLayoutParams(ip);
        info.addView(Ui.text(this,"المكتبة الجديدة أونلاين",18,Ui.TEXT,true));
        TextView sub=Ui.text(this,"الفيديوهات نفسها مش جوه الـAPK. كل قالب بيتحمّل لوحده، وبعد التحميل يفتح مباشرة في المونتاج ويشتغل Offline.",12,Ui.MUTED,false);
        sub.setLineSpacing(0,1.25f);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,6),0,0);info.addView(sub,sp);
        page.addView(info);

        LinearLayout state=new LinearLayout(this);state.setOrientation(LinearLayout.HORIZONTAL);state.setGravity(Gravity.CENTER_VERTICAL);state.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),Ui.dp(this,8));
        loading=new ProgressBar(this);state.addView(loading,new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28)));
        status=Ui.text(this,"جاري تحميل المكتبة…",12,Ui.CYAN,true);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);st.setMargins(Ui.dp(this,8),0,0,0);state.addView(status,st);
        Button refresh=Ui.ghost(this,"↻ تحديث");refresh.setOnClickListener(v->loadCatalog());state.addView(refresh,new LinearLayout.LayoutParams(Ui.dp(this,94),Ui.dp(this,44)));
        page.addView(state);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        listBox=new LinearLayout(this);listBox.setOrientation(LinearLayout.VERTICAL);listBox.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),Ui.dp(this,28));
        scroll.addView(listBox,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        return page;
    }

    private void loadCatalog(){
        loading.setVisibility(View.VISIBLE);status.setText("جاري تحميل المكتبة الأونلاين…");listBox.removeAllViews();
        executor.execute(()->{
            try{
                JSONObject root=getJson(CATALOG_URL);
                JSONArray arr=root.optJSONArray("templates");
                if(arr==null||arr.length()==0)throw new IllegalStateException("المكتبة الأونلاين فارغة حاليًا");
                List<OnlineTemplate> items=new ArrayList<>();
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.optJSONObject(i);if(o==null)continue;
                    OnlineTemplate t=new OnlineTemplate();
                    t.id=o.optString("id","");t.name=o.optString("name","Reel");t.description=o.optString("description","");
                    t.hook=o.optString("hook","");t.body=o.optString("body","");t.cta=o.optString("cta","");t.hashtags=o.optString("hashtags","");
                    t.durationSec=Math.max(6,o.optInt("durationSec",15));t.approxSizeMb=o.optDouble("approxSizeMb",0);
                    t.mediaUrl=o.optString("mediaUrl","");t.mediaExt=o.optString("mediaExt",".mp4");t.credit=o.optString("credit","");
                    t.license=o.optString("license","");t.sourcePage=o.optString("sourcePage","");
                    if(!t.id.isEmpty()&&!t.mediaUrl.isEmpty())items.add(t);
                }
                if(items.isEmpty())throw new IllegalStateException("لم يتم العثور على قوالب صالحة");
                runOnUiThread(()->render(items));
            }catch(Throwable e){
                String m=e.getMessage()==null?"تعذر تحميل المكتبة الأونلاين":e.getMessage();
                runOnUiThread(()->showLoadError(m));
            }
        });
    }

    private void render(List<OnlineTemplate> items){
        loading.setVisibility(View.GONE);listBox.removeAllViews();status.setText(items.size()+" قوالب فيديو Online جاهزة للتجربة");
        for(OnlineTemplate t:items)listBox.addView(templateCard(t));
    }

    private void showLoadError(String message){
        loading.setVisibility(View.GONE);status.setText(message);
        LinearLayout card=Ui.card(this);card.addView(Ui.text(this,"تعذر فتح المكتبة الأونلاين",17,Ui.RED,true));
        TextView text=Ui.text(this,message+"\nتأكد من الإنترنت واضغط تحديث.",12,Ui.MUTED,false);LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,8),0,0);card.addView(text,tp);listBox.addView(card);
    }

    private View templateCard(OnlineTemplate t){
        LinearLayout card=Ui.card(this);LinearLayout.LayoutParams cp=Ui.matchWrap();cp.setMargins(0,0,0,Ui.dp(this,10));card.setLayoutParams(cp);
        card.setBackground(Ui.gradient(0xFF0B4F48,0xFF0A1D2E,21,this));

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=Ui.text(this,t.name,16,Ui.TEXT,true);head.addView(name,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView dur=Ui.chip(this,t.durationSec+"ث",Ui.CYAN);head.addView(dur);card.addView(head);

        TextView desc=Ui.text(this,t.description,12,0xFFD6E5F8,false);desc.setLineSpacing(0,1.2f);LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,8),0,0);card.addView(desc,dp);
        String size=t.approxSizeMb>0?String.format(Locale.US,"%.1f MB",t.approxSizeMb):"حسب المصدر";
        TextView meta=Ui.text(this,"فيديو حقيقي • "+size+" • "+t.license,11,Ui.GREEN,true);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,8),0,0);card.addView(meta,mp);
        if(!t.credit.isEmpty()){TextView credit=Ui.text(this,"المصدر: "+t.credit,10,Ui.MUTED_2,false);LinearLayout.LayoutParams cr=Ui.matchWrap();cr.setMargins(0,Ui.dp(this,4),0,0);card.addView(credit,cr);}

        File local=templateFile(t);
        TextView localState=Ui.text(this,local.exists()&&local.length()>1024?"✓ تم تحميل القالب — متاح Offline":"Online — لم يتم تحميله بعد",11,local.exists()?Ui.GREEN:Ui.YELLOW,true);
        LinearLayout.LayoutParams ls=Ui.matchWrap();ls.setMargins(0,Ui.dp(this,10),0,0);card.addView(localState,ls);

        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams bp=Ui.matchWrap();bp.setMargins(0,Ui.dp(this,10),0,0);
        Button preview=Ui.secondary(this,"▶ معاينة");preview.setOnClickListener(v->previewOnline(t));buttons.addView(preview,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f));
        Button action=Ui.accent(this,local.exists()&&local.length()>1024?"فتح في المونتاج":"↓ تحميل القالب");
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f);ap.setMargins(Ui.dp(this,8),0,0,0);buttons.addView(action,ap);card.addView(buttons,bp);

        action.setOnClickListener(v->{
            File f=templateFile(t);
            if(f.exists()&&f.length()>1024)openEditor(t,f);else downloadTemplate(t,action,localState);
        });

        if(local.exists()&&local.length()>1024){
            Button delete=Ui.ghost(this,"حذف الملفات المحمّلة وإبقاء القالب Online");
            delete.setOnClickListener(v->{if(templateFile(t).delete()){toast("تم حذف القالب من الجهاز");loadCatalog();}else toast("تعذر حذف الملف");});
            LinearLayout.LayoutParams dl=Ui.matchWrap();dl.height=Ui.dp(this,45);dl.setMargins(0,Ui.dp(this,6),0,0);card.addView(delete,dl);
        }
        return card;
    }

    private void previewOnline(OnlineTemplate t){
        try{
            Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(t.mediaUrl));
            i.setDataAndType(Uri.parse(t.mediaUrl),"video/*");
            startActivity(i);
        }catch(Exception e){toast("تعذر فتح المعاينة على الجهاز");}
    }

    private void downloadTemplate(OnlineTemplate t,Button action,TextView localState){
        action.setEnabled(false);action.setAlpha(.55f);action.setText("جاري التحميل…");localState.setTextColor(Ui.YELLOW);localState.setText("بدء تحميل الفيديو…");
        executor.execute(()->{
            try{
                File out=templateFile(t);File parent=out.getParentFile();if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IllegalStateException("تعذر إنشاء مجلد القوالب");
                HttpURLConnection c=(HttpURLConnection)new URL(t.mediaUrl).openConnection();
                c.setInstanceFollowRedirects(true);c.setConnectTimeout(20000);c.setReadTimeout(60000);c.setRequestProperty("User-Agent","DownloadHub/6 Android");c.connect();
                int code=c.getResponseCode();if(code<200||code>=400)throw new IllegalStateException("فشل تحميل الفيديو: HTTP "+code);
                long total=c.getContentLengthLong();long done=0;int last=-1;
                try(InputStream in=new BufferedInputStream(c.getInputStream());FileOutputStream fo=new FileOutputStream(out)){
                    byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0){fo.write(buf,0,n);done+=n;if(total>0){int pct=(int)(done*100/total);if(pct>=last+5){last=pct;final int p=pct;runOnUiThread(()->localState.setText("تحميل القالب… "+p+"%"));}}}
                }finally{c.disconnect();}
                if(!out.exists()||out.length()<1024)throw new IllegalStateException("ملف القالب غير صالح");
                runOnUiThread(()->{action.setEnabled(true);action.setAlpha(1f);action.setText("فتح في المونتاج");localState.setTextColor(Ui.GREEN);localState.setText("✓ تم تحميل القالب — متاح Offline");openEditor(t,out);});
            }catch(Throwable e){
                try{templateFile(t).delete();}catch(Exception ignored){}
                String m=e.getMessage()==null?"تعذر تحميل القالب":e.getMessage();
                runOnUiThread(()->{action.setEnabled(true);action.setAlpha(1f);action.setText("↓ إعادة التحميل");localState.setTextColor(Ui.RED);localState.setText(m);toast(m);});
            }
        });
    }

    private void openEditor(OnlineTemplate t,File file){
        CreatorProject p=CreatorProject.fromTemplate(TemplateCatalog.byId("islamic-0"));
        p.id="online-"+t.id+"-"+System.currentTimeMillis();p.templateId=t.id;p.categoryKey="islamic";p.categoryName="مكتبة الريلز الإسلامي Online";
        p.name=t.name;p.hook=t.hook;p.body=t.body;p.cta=t.cta;p.hashtags=t.hashtags;p.durationSec=t.durationSec;
        p.sourceUri=Uri.fromFile(file).toString();p.startColor=0xFF0A554D;p.endColor=0xFF071C2B;
        DraftStore.save(this,p);
        Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("draft_id",p.id);startActivity(i);
    }

    private File templateFile(OnlineTemplate t){
        File root=getExternalFilesDir(Environment.DIRECTORY_MOVIES);if(root==null)root=new File(getFilesDir(),"movies");
        String ext=t.mediaExt==null||t.mediaExt.trim().isEmpty()?".mp4":t.mediaExt.trim();if(!ext.startsWith("."))ext="."+ext;
        return new File(new File(root,"DownloadHub/Templates"),DownloadUtil.sanitizeFileName(t.id)+ext);
    }

    private JSONObject getJson(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","DownloadHub/6 Android");
        int code=c.getResponseCode();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code);
        try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);return new JSONObject(b.toString());}finally{c.disconnect();}
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
