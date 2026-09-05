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
import android.widget.HorizontalScrollView;
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
    private static final String CATALOG_URL="https://raw.githubusercontent.com/adam0012af-ai/test/main/online/templates.json";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private LinearLayout listBox,filters;
    private TextView status;
    private ProgressBar loading;
    private String selectedCategory="all";
    private final List<OnlineTemplate> cache=new ArrayList<>();

    private static final class OnlineTemplate {
        String id,category,categoryName,name,description,hook,body,cta,hashtags,mediaUrl,mediaExt,credit,license,sourcePage;
        int durationSec;
        double approxSizeMb;
    }

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);String incoming=getIntent().getStringExtra("category");if(incoming!=null&&!incoming.trim().isEmpty())selectedCategory=incoming;setContentView(buildUi());loadCatalog();}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,8));Button back=Ui.ghost(this,"‹");back.setTextSize(24);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.addView(Ui.text(this,"Templates Hub",22,Ui.TEXT,true));title.addView(Ui.text(this,"REAL VIDEO TEMPLATES • ONLINE",9,Ui.CYAN,true));top.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Button menu=Ui.ghost(this,"☰");menu.setOnClickListener(v->AppSideMenu.show(this));top.addView(menu,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,46)));page.addView(top);

        LinearLayout hero=Ui.card(this);LinearLayout.LayoutParams hp=Ui.matchWrap();hp.setMargins(Ui.dp(this,14),Ui.dp(this,6),Ui.dp(this,14),Ui.dp(this,8));hero.setLayoutParams(hp);hero.setBackground(Ui.gradient3(0xFF143A62,0xFF15294A,0xFF09131F,22,this));hero.addView(Ui.text(this,"قالب كامل، مش مجرد نص",19,Ui.TEXT,true));TextView d=Ui.text(this,"عاين القالب، نزّله عند الطلب، استخدمه كما هو أو افتحه في Video Studio وبدّل الفيديوهات والنص والصوت والقص والسرعة.",11,0xFFD8E7F8,false);d.setLineSpacing(0,1.25f);LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,6),0,0);hero.addView(d,dp);page.addView(hero);

        HorizontalScrollView fs=new HorizontalScrollView(this);fs.setHorizontalScrollBarEnabled(false);filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);filters.setPadding(Ui.dp(this,14),Ui.dp(this,4),Ui.dp(this,14),Ui.dp(this,8));fs.addView(filters,new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(fs);

        LinearLayout state=new LinearLayout(this);state.setOrientation(LinearLayout.HORIZONTAL);state.setGravity(Gravity.CENTER_VERTICAL);state.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),Ui.dp(this,8));loading=new ProgressBar(this);state.addView(loading,new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28)));status=Ui.text(this,"جاري تحميل المكتبة…",11,Ui.CYAN,true);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);st.setMargins(Ui.dp(this,8),0,0,0);state.addView(status,st);Button refresh=Ui.ghost(this,"↻");refresh.setOnClickListener(v->loadCatalog());state.addView(refresh,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,44)));page.addView(state);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);listBox=new LinearLayout(this);listBox.setOrientation(LinearLayout.VERTICAL);listBox.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),Ui.dp(this,30));scroll.addView(listBox,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));return page;
    }

    private void loadCatalog(){loading.setVisibility(View.VISIBLE);status.setText("جاري تحديث القوالب Online…");listBox.removeAllViews();executor.execute(()->{try{JSONObject root=getJson(CATALOG_URL);JSONArray arr=root.optJSONArray("templates");if(arr==null||arr.length()==0)throw new IllegalStateException("المكتبة Online فارغة");List<OnlineTemplate> items=new ArrayList<>();for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;OnlineTemplate t=new OnlineTemplate();t.id=o.optString("id","");t.category=o.optString("category","other");t.categoryName=o.optString("categoryName",t.category);t.name=o.optString("name","Reel");t.description=o.optString("description","");t.hook=o.optString("hook","");t.body=o.optString("body","");t.cta=o.optString("cta","");t.hashtags=o.optString("hashtags","");t.durationSec=Math.max(1,o.optInt("durationSec",15));t.approxSizeMb=o.optDouble("approxSizeMb",0);t.mediaUrl=o.optString("mediaUrl","");t.mediaExt=o.optString("mediaExt",".mp4");t.credit=o.optString("credit","");t.license=o.optString("license","");t.sourcePage=o.optString("sourcePage","");if(!t.id.isEmpty()&&!t.mediaUrl.isEmpty())items.add(t);}if(items.isEmpty())throw new IllegalStateException("لا توجد قوالب صالحة");runOnUiThread(()->{cache.clear();cache.addAll(items);buildFilters();render();});}catch(Throwable e){String m=e.getMessage()==null?"تعذر تحميل المكتبة":e.getMessage();runOnUiThread(()->showLoadError(m));}});}

    private void buildFilters(){filters.removeAllViews();addFilter("all","الكل");List<String> seen=new ArrayList<>();for(OnlineTemplate t:cache){if(seen.contains(t.category))continue;seen.add(t.category);addFilter(t.category,t.categoryName);}}
    private void addFilter(String key,String label){Button b=key.equals(selectedCategory)?Ui.accent(this,label):Ui.secondary(this,label);b.setOnClickListener(v->{selectedCategory=key;buildFilters();render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,108),Ui.dp(this,44));p.setMargins(0,0,Ui.dp(this,7),0);filters.addView(b,p);}

    private void render(){loading.setVisibility(View.GONE);listBox.removeAllViews();int count=0;for(OnlineTemplate t:cache){if(!"all".equals(selectedCategory)&&!selectedCategory.equals(t.category))continue;listBox.addView(templateCard(t));count++;}status.setText(count+" قوالب فيديو حقيقية • تحميل عند الطلب");if(count==0){LinearLayout c=Ui.card(this);c.addView(Ui.text(this,"لا توجد قوالب في القسم حاليًا",14,Ui.MUTED,true));listBox.addView(c);}}
    private void showLoadError(String message){loading.setVisibility(View.GONE);status.setText(message);LinearLayout c=Ui.card(this);c.addView(Ui.text(this,"تعذر فتح Templates Hub",17,Ui.RED,true));TextView t=Ui.text(this,message+"\nتأكد من الإنترنت واضغط تحديث.",11,Ui.MUTED,false);LinearLayout.LayoutParams p=Ui.matchWrap();p.setMargins(0,Ui.dp(this,6),0,0);c.addView(t,p);listBox.addView(c);}

    private View templateCard(OnlineTemplate t){
        LinearLayout card=Ui.card(this);LinearLayout.LayoutParams cp=Ui.matchWrap();cp.setMargins(0,0,0,Ui.dp(this,10));card.setLayoutParams(cp);card.setBackground(Ui.gradient(0xFF0D263E,0xFF091622,21,this));
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);TextView name=Ui.text(this,t.name,16,Ui.TEXT,true);head.addView(name,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));TextView cat=Ui.chip(this,t.categoryName,Ui.CYAN);head.addView(cat);card.addView(head);
        TextView desc=Ui.text(this,t.description,11,0xFFD6E5F8,false);desc.setLineSpacing(0,1.22f);LinearLayout.LayoutParams ds=Ui.matchWrap();ds.setMargins(0,Ui.dp(this,7),0,0);card.addView(desc,ds);
        String size=t.approxSizeMb>0?String.format(Locale.US,"%.1f MB",t.approxSizeMb):"حسب المصدر";TextView meta=Ui.text(this,t.durationSec+"ث • "+size+" • "+t.license,10,Ui.GREEN,true);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,7),0,0);card.addView(meta,mp);if(!t.credit.isEmpty()){TextView cr=Ui.text(this,"Credit: "+t.credit,9,Ui.MUTED_2,false);LinearLayout.LayoutParams crp=Ui.matchWrap();crp.setMargins(0,Ui.dp(this,3),0,0);card.addView(cr,crp);}
        File local=templateFile(t);boolean ready=local.exists()&&local.length()>1024;TextView localState=Ui.text(this,ready?"✓ Downloaded • Offline ready":"Online • not downloaded",10,ready?Ui.GREEN:Ui.YELLOW,true);LinearLayout.LayoutParams ls=Ui.matchWrap();ls.setMargins(0,Ui.dp(this,9),0,0);card.addView(localState,ls);

        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);Button preview=Ui.secondary(this,"▶ Preview");preview.setOnClickListener(v->previewOnline(t));r1.addView(preview,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f));Button download=Ui.accent(this,ready?"Open Editor":"↓ Download");LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f);dl.setMargins(Ui.dp(this,8),0,0,0);r1.addView(download,dl);LinearLayout.LayoutParams rp=Ui.matchWrap();rp.setMargins(0,Ui.dp(this,9),0,0);card.addView(r1,rp);
        download.setOnClickListener(v->{File f=templateFile(t);if(f.exists()&&f.length()>1024)openEditor(t,f);else downloadTemplate(t,download,localState,false);});

        if(ready){LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);Button asIs=Ui.secondary(this,"Use as-is");asIs.setOnClickListener(v->exportAsIs(t,templateFile(t),asIs));r2.addView(asIs,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button delete=Ui.ghost(this,"Delete local");delete.setOnClickListener(v->{if(templateFile(t).delete()){toast("تم حذف الملف المحلي");render();}else toast("تعذر حذف الملف");});LinearLayout.LayoutParams de=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);de.setMargins(Ui.dp(this,8),0,0,0);r2.addView(delete,de);LinearLayout.LayoutParams r2p=Ui.matchWrap();r2p.setMargins(0,Ui.dp(this,6),0,0);card.addView(r2,r2p);}
        return card;
    }

    private void previewOnline(OnlineTemplate t){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(t.mediaUrl),"video/*");startActivity(i);}catch(Exception e){toast("تعذر فتح المعاينة");}}

    private void downloadTemplate(OnlineTemplate t,Button action,TextView localState,boolean exportAfter){action.setEnabled(false);action.setAlpha(.55f);action.setText("Downloading…");localState.setTextColor(Ui.YELLOW);localState.setText("0%");executor.execute(()->{try{File out=templateFile(t);File parent=out.getParentFile();if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IllegalStateException("تعذر إنشاء مجلد القوالب");HttpURLConnection c=(HttpURLConnection)new URL(t.mediaUrl).openConnection();c.setInstanceFollowRedirects(true);c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setRequestProperty("User-Agent","DownloadHubStudio/6 Android");c.connect();int code=c.getResponseCode();if(code<200||code>=400)throw new IllegalStateException("HTTP "+code);long total=c.getContentLengthLong(),done=0;int last=-1;try(InputStream in=new BufferedInputStream(c.getInputStream());FileOutputStream fo=new FileOutputStream(out)){byte[]buf=new byte[65536];int n;while((n=in.read(buf))>0){fo.write(buf,0,n);done+=n;if(total>0){int pct=(int)(done*100/total);if(pct>=last+3){last=pct;final int p=pct;runOnUiThread(()->localState.setText("Downloading… "+p+"%"));}}}}finally{c.disconnect();}if(!out.exists()||out.length()<1024)throw new IllegalStateException("ملف القالب غير صالح");runOnUiThread(()->{action.setEnabled(true);action.setAlpha(1f);action.setText("Open Editor");localState.setTextColor(Ui.GREEN);localState.setText("✓ Downloaded • Offline ready");if(exportAfter)exportAsIs(t,out,action);else openEditor(t,out);});}catch(Throwable e){try{templateFile(t).delete();}catch(Exception ignored){}String m=e.getMessage()==null?"تعذر تحميل القالب":e.getMessage();runOnUiThread(()->{action.setEnabled(true);action.setAlpha(1f);action.setText("Retry");localState.setTextColor(Ui.RED);localState.setText(m);toast(m);});}});}

    private CreatorProject projectFrom(OnlineTemplate t,File file){CreatorProject p=CreatorProject.fromTemplate(TemplateCatalog.byId("islamic-0"));p.id="template-"+t.id+"-"+System.currentTimeMillis();p.templateId=t.id;p.categoryKey=t.category;p.categoryName=t.categoryName;p.name=t.name;p.hook=t.hook;p.body=t.body;p.cta=t.cta;p.hashtags=t.hashtags;p.durationSec=t.durationSec;p.sourceUri=Uri.fromFile(file).toString();p.clips.clear();EditorClip c=new EditorClip(p.sourceUri);c.trimEndMs=t.durationSec*1000L;p.clips.add(c);p.selectedClipIndex=0;p.aspectRatio="9:16";p.syncPrimarySource();return p;}
    private void openEditor(OnlineTemplate t,File file){CreatorProject p=projectFrom(t,file);DraftStore.save(this,p);Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("draft_id",p.id);startActivity(i);}

    private void exportAsIs(OnlineTemplate t,File file,Button button){CreatorProject p=projectFrom(t,file);DraftStore.save(this,p);button.setEnabled(false);button.setText("Exporting…");executor.execute(()->ReelExporter.export(this,p,new ReelExporter.Callback(){public void onStage(String s){runOnUiThread(()->button.setText(s));}public void onSuccess(File out){runOnUiThread(()->{button.setEnabled(true);button.setText("Use as-is");Intent i=new Intent(TemplateLibraryActivity.this,PublishingActivity.class);i.putExtra("project_id",p.id);i.putExtra("video_path",out.getAbsolutePath());startActivity(i);});}public void onError(String m){runOnUiThread(()->{button.setEnabled(true);button.setText("Use as-is");toast(m);});}}));}

    private File templateFile(OnlineTemplate t){File root=getExternalFilesDir(Environment.DIRECTORY_MOVIES);if(root==null)root=new File(getFilesDir(),"movies");String ext=t.mediaExt==null||t.mediaExt.trim().isEmpty()?".mp4":t.mediaExt.trim();if(!ext.startsWith("."))ext="."+ext;return new File(new File(root,"DownloadHub/Templates"),DownloadUtil.sanitizeFileName(t.id)+ext);}
    private JSONObject getJson(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","DownloadHubStudio/6 Android");int code=c.getResponseCode();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code);try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);return new JSONObject(b.toString());}finally{c.disconnect();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
