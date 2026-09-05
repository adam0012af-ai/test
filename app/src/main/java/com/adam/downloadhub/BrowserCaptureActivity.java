package com.adam.downloadhub;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserCaptureActivity extends Activity {
    public static final String EXTRA_URL="url";
    public static final String EXTRA_TIKTOK_CLEAN="tiktok_clean";

    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String,String> captured=new LinkedHashMap<>();
    private WebView webView;
    private TextView status;
    private EditText address;
    private String sourceUrl;
    private boolean tikTokCleanMode;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);sourceUrl=getIntent().getStringExtra(EXTRA_URL);if(!isHttp(sourceUrl))sourceUrl="https://www.google.com";tikTokCleanMode=getIntent().getBooleanExtra(EXTRA_TIKTOK_CLEAN,false)||isTikTok(sourceUrl);setContentView(buildUi());setupWebView();address.setText(sourceUrl);webView.loadUrl(sourceUrl);}

    private View buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(Ui.gradient(Ui.BG,Color.rgb(8,17,30),0,this));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout top=Ui.card(this);top.setBackground(Ui.bordered(Ui.SURFACE,Ui.BORDER,1,0,this));top.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,9));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.secondary(this,"‹");back.setTextSize(22);back.setOnClickListener(v->{if(webView.canGoBack())webView.goBack();});nav.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,44)));
        Button fwd=Ui.secondary(this,"›");fwd.setTextSize(22);fwd.setOnClickListener(v->{if(webView.canGoForward())webView.goForward();});LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,44));fp.setMargins(Ui.dp(this,6),0,0,0);nav.addView(fwd,fp);
        address=new EditText(this);address.setSingleLine(true);address.setTextSize(12);address.setTextColor(Ui.TEXT);address.setHintTextColor(Ui.MUTED);address.setTextDirection(View.TEXT_DIRECTION_LTR);address.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),0);address.setBackground(Ui.bordered(Color.rgb(7,13,23),Ui.BORDER,1,13,this));LinearLayout.LayoutParams adp=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);adp.setMargins(Ui.dp(this,7),0,0,0);nav.addView(address,adp);
        Button go=Ui.primary(this,"فتح");go.setOnClickListener(v->loadAddress());LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,44));gp.setMargins(Ui.dp(this,7),0,0,0);nav.addView(go,gp);top.addView(nav);

        status=Ui.text(this,tikTokCleanMode?"TikTok Clean • شغّل الفيديو ثم افحص المصادر":"Media Sniffer • شغّل الفيديو ليتم جمع مصادر الوسائط",12,tikTokCleanMode?Ui.CYAN:Ui.MUTED,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,7));top.addView(status,sp);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button scan=Ui.primary(this,"فحص الجودات والصوت");scan.setOnClickListener(v->scanSources());actions.addView(scan,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1.4f));
        Button reload=Ui.secondary(this,"تحديث");reload.setOnClickListener(v->{captured.clear();webView.reload();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),.7f);rp.setMargins(Ui.dp(this,7),0,0,0);actions.addView(reload,rp);
        Button downloads=Ui.secondary(this,"التحميلات");downloads.setOnClickListener(v->startActivity(new android.content.Intent(this,DownloadsActivity.class)));LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);dp.setMargins(Ui.dp(this,7),0,0,0);actions.addView(downloads,dp);top.addView(actions);root.addView(top);
        webView=new WebView(this);root.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));return root;
    }

    private void setupWebView(){
        WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setUserAgentString(NetUtil.USER_AGENT);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);webView.setWebChromeClient(new WebChromeClient());
        webView.setDownloadListener(new DownloadListener(){@Override public void onDownloadStart(String url,String ua,String disposition,String mime,long len){recordCandidate(url,webView.getUrl());}});
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return false;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){try{String u=req.getUrl().toString();if(looksPotential(u))recordCandidate(u,view.getUrl());else if(isSegmentedStream(u))runOnUiThread(()->status.setText("تم رصد بث HLS/DASH مجزأ — لن يتم حفظه كملف تالف"));}catch(Exception ignored){}return super.shouldInterceptRequest(view,req);}
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);address.setText(url);tikTokCleanMode=isTikTok(url)||getIntent().getBooleanExtra(EXTRA_TIKTOK_CLEAN,false);status.setText("الصفحة جاهزة • شغّل الفيديو ثم اضغط فحص الجودات والصوت");}
        });
    }

    private synchronized void recordCandidate(String url,String referer){if(!isHttp(url)||isSegmentedStream(url))return;if(tikTokCleanMode&&looksWatermarked(url))return;if(!looksPotential(url))return;if(captured.size()>=30&&!captured.containsKey(url)){String first=captured.keySet().iterator().next();captured.remove(first);}captured.put(url,isHttp(referer)?referer:sourceUrl);runOnUiThread(()->status.setText("تم رصد "+captured.size()+" مصدر محتمل • اضغط فحص الجودات والصوت"));}

    private void scanSources(){
        status.setText("جاري جمع وفحص مصادر الفيديو والصوت…");
        String js="(function(){var a=[];var vs=document.querySelectorAll('video,audio');for(var i=0;i<vs.length;i++){var x=vs[i].currentSrc||vs[i].src||'';if(x&&x.indexOf('blob:')!==0)a.push(x);}var ss=document.querySelectorAll('video source,audio source');for(var j=0;j<ss.length;j++){var y=ss[j].src||ss[j].getAttribute('src')||'';if(y&&y.indexOf('blob:')!==0)a.push(y);}var ms=document.querySelectorAll('meta[property=\\\"og:video\\\"],meta[property=\\\"og:video:url\\\"],meta[property=\\\"og:video:secure_url\\\"]');for(var k=0;k<ms.length;k++){if(ms[k].content)a.push(ms[k].content);}return a;})()";
        webView.evaluateJavascript(js,value->{for(String u:decodeJsArray(value))recordCandidate(u,webView.getUrl());probeCaptured();});
    }

    private void probeCaptured(){
        final List<Map.Entry<String,String>> candidates; synchronized(this){candidates=new ArrayList<>(captured.entrySet());}
        if(candidates.isEmpty()){status.setText("لم يتم رصد مصدر مباشر بعد. شغّل الفيديو عدة ثوانٍ ثم جرّب مرة أخرى.");return;}
        final String pageTitle=webView.getTitle()==null?"captured_media":webView.getTitle();
        final String current=webView.getUrl()==null?sourceUrl:webView.getUrl();
        executor.execute(()->{
            ArrayList<MediaOption> options=new ArrayList<>();int checked=0;
            for(Map.Entry<String,String> e:candidates){if(checked++>=18)break;String ref=e.getValue();String cookie=CookieManager.getInstance().getCookie(ref==null?current:ref);MediaValidator.ProbeResult p=MediaValidator.probeMedia(e.getKey(),ref,cookie,true);if(!p.valid)continue;String platform=platformFor(current);String label=p.audioOnly?"صوت فقط":inferQuality(p.finalUrl);String ext=extensionFor(p);String base=DownloadUtil.sanitizeFileName(pageTitle);if(base.isEmpty())base=platform+"_media";String name=base+(p.audioOnly?"_audio":"_"+label.replaceAll("[^A-Za-z0-9]+","_"))+"."+ext;options.add(new MediaOption(p.finalUrl,label,name,ref,platform,p.contentType,p.sizeBytes,p.audioOnly));}
            runOnUiThread(()->{if(options.isEmpty()){status.setText("تم فحص المصادر لكن لم يوجد ملف كامل صالح. الصفحة تستخدم بثًا مجزأ أو رابطًا مؤقتًا غير قابل للتحميل مباشرة.");return;}status.setText("تم التحقق من "+options.size()+" خيار صالح ✅");MediaOptionsDialog.show(this,pageTitle,options,this::startDownload);});
        });
    }

    private void startDownload(MediaOption option){try{DownloadEngine.enqueue(this,option);HistoryStore.add(this,webView.getUrl(),"Browser • "+option.platform);status.setText("بدأ التحميل ✅\n"+option.displayLabel());Toast.makeText(this,"تمت إضافة التحميل",Toast.LENGTH_SHORT).show();}catch(Exception e){status.setText("تعذر بدء التحميل: "+(e.getMessage()==null?"خطأ":e.getMessage()));}}

    private void loadAddress(){String u=address.getText()==null?"":address.getText().toString().trim();if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u;captured.clear();sourceUrl=u;tikTokCleanMode=isTikTok(u)||getIntent().getBooleanExtra(EXTRA_TIKTOK_CLEAN,false);webView.loadUrl(u);}
    private boolean looksPotential(String u){if(!isHttp(u))return false;String l=u.toLowerCase(Locale.ROOT);return l.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi|m4a|mp3|aac|ogg|opus)(\\?.*)?$")||l.contains("mime=video")||l.contains("mime%3dvideo")||l.contains("mime=audio")||l.contains("mime%3daudio")||l.contains("video_mp4")||l.contains("mime_type=video_")||l.contains("/video/tos/");}
    private boolean isSegmentedStream(String u){if(!isHttp(u))return false;String l=u.toLowerCase(Locale.ROOT);return l.contains(".m3u8")||l.contains(".mpd")||l.contains(".m4s")||l.contains("/segment/")||l.contains("/segments/")||l.contains("init.mp4")||l.contains("init.m4s")||MediaValidator.structuralProblem(u)!=null;}
    private boolean looksWatermarked(String u){String l=u.toLowerCase(Locale.ROOT);return l.contains("watermark=1")||l.contains("watermark%3d1")||l.contains("downloadaddr")||l.contains("/download/");}
    private boolean isTikTok(String u){String l=u==null?"":u.toLowerCase(Locale.ROOT);return l.contains("tiktok.com")||l.contains("tiktokv.com")||l.contains("tiktokcdn.com");}
    private String platformFor(String url){try{return PlatformExtractor.platformName(URI.create(url).getHost());}catch(Exception e){return"Web";}}
    private String inferQuality(String u){String l=u.toLowerCase(Locale.ROOT);Matcher m=Pattern.compile("(?:^|[^0-9])(2160|1440|1080|720|540|480|360|240)(?:p|[^0-9]|$)").matcher(l);if(m.find())return m.group(1)+"p";if(l.contains("4k"))return"4K";if(l.contains("hd"))return"HD";if(l.contains("sd"))return"SD";return"Video";}
    private String extensionFor(MediaValidator.ProbeResult p){String t=p.contentType==null?"":p.contentType.toLowerCase(Locale.ROOT),u=p.finalUrl.toLowerCase(Locale.ROOT);if(p.audioOnly){if(t.contains("mpeg")||u.contains(".mp3"))return"mp3";if(t.contains("ogg")||u.contains(".ogg"))return"ogg";if(t.contains("opus")||u.contains(".opus"))return"opus";if(t.contains("aac")||u.contains(".aac"))return"aac";return"m4a";}if(t.contains("webm")||u.contains(".webm"))return"webm";if(t.contains("quicktime")||u.contains(".mov"))return"mov";return"mp4";}
    private List<String> decodeJsArray(String value){ArrayList<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(value);for(int i=0;i<a.length();i++){String s=a.optString(i,"");if(isHttp(s))out.add(s);}}catch(Exception ignored){}return out;}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){executor.shutdownNow();if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
