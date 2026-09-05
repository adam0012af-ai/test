package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.util.Locale;

public class BrowserCaptureActivity extends Activity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TIKTOK_CLEAN = "tiktok_clean";
    private WebView webView;
    private TextView status;
    private EditText address;
    private String sourceUrl;
    private volatile String lastMediaUrl = "";
    private volatile String lastMediaReferer = "";
    private boolean autoTried;
    private boolean tikTokCleanMode;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        sourceUrl=getIntent().getStringExtra(EXTRA_URL);if(sourceUrl==null||(!sourceUrl.startsWith("http://")&&!sourceUrl.startsWith("https://")))sourceUrl="https://www.google.com";tikTokCleanMode=getIntent().getBooleanExtra(EXTRA_TIKTOK_CLEAN,false)||isTikTokUrl(sourceUrl);
        setContentView(buildUi());setupWebView();address.setText(sourceUrl);webView.loadUrl(sourceUrl);
    }

    private View buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(Ui.gradient(Ui.BG,Color.rgb(8,17,30),0,this));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout top=Ui.card(this);top.setBackground(Ui.bordered(Ui.SURFACE,Ui.BORDER,1,0,this));top.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,9));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.secondary(this,"‹");back.setTextSize(22);back.setOnClickListener(v->{if(webView.canGoBack())webView.goBack();});nav.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,44)));
        Button forward=Ui.secondary(this,"›");forward.setTextSize(22);forward.setOnClickListener(v->{if(webView.canGoForward())webView.goForward();});LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,44));fp.setMargins(Ui.dp(this,6),0,0,0);nav.addView(forward,fp);
        address=new EditText(this);address.setSingleLine(true);address.setTextSize(12);address.setTextColor(Ui.TEXT);address.setHintTextColor(Ui.MUTED);address.setTextDirection(View.TEXT_DIRECTION_LTR);address.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),0);address.setBackground(Ui.bordered(Color.rgb(7,13,23),Ui.BORDER,1,13,this));LinearLayout.LayoutParams adp=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);adp.setMargins(Ui.dp(this,7),0,0,0);nav.addView(address,adp);
        Button go=Ui.primary(this,"فتح");go.setOnClickListener(v->loadAddress());LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,44));gp.setMargins(Ui.dp(this,7),0,0,0);nav.addView(go,gp);top.addView(nav);

        status=Ui.text(this,tikTokCleanMode?"TikTok Clean Mode — شغّل الفيديو وسيتم التقاط مصدر التشغيل بدون علامة مائية عند توفره.":"شغّل الفيديو؛ التطبيق يراقب روابط الوسائط تلقائيًا.",12,tikTokCleanMode?Ui.CYAN:Ui.MUTED,false);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,7));top.addView(status,sp);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button capture=Ui.primary(this,tikTokCleanMode?"تحميل بدون علامة":"التقاط وتحميل");capture.setOnClickListener(v->captureCurrentVideo(false));actions.addView(capture,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1.3f));
        Button reload=Ui.secondary(this,"تحديث");reload.setOnClickListener(v->webView.reload());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),.8f);rp.setMargins(Ui.dp(this,7),0,0,0);actions.addView(reload,rp);
        Button downloads=Ui.secondary(this,"التحميلات");downloads.setOnClickListener(v->startActivity(new android.content.Intent(this,DownloadsActivity.class)));LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);dp.setMargins(Ui.dp(this,7),0,0,0);actions.addView(downloads,dp);top.addView(actions);root.addView(top);
        webView=new WebView(this);root.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));return root;
    }

    private void setupWebView(){
        WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setUserAgentString(NetUtil.USER_AGENT);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);webView.setWebChromeClient(new WebChromeClient());
        webView.setDownloadListener(new DownloadListener(){@Override public void onDownloadStart(String url,String userAgent,String contentDisposition,String mimetype,long contentLength){if(isHttp(url)){lastMediaUrl=url;lastMediaReferer=webView.getUrl();String name=DownloadUtil.guessFileName(url,"browser_download.mp4");enqueue(url,name,lastMediaReferer);}}});
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return false;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                try{String u=request.getUrl().toString();if(tikTokCleanMode?looksTikTokCleanMedia(u):looksMedia(u)){lastMediaUrl=u;lastMediaReferer=view.getUrl()==null?sourceUrl:view.getUrl();runOnUiThread(()->status.setText(tikTokCleanMode?"تم رصد مصدر تشغيل TikTok نظيف ✅ اضغط تحميل بدون علامة":"تم رصد مصدر وسائط — يمكنك الضغط على التقاط وتحميل"));}}catch(Exception ignored){}
                return super.shouldInterceptRequest(view,request);
            }
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);address.setText(url);status.setText(tikTokCleanMode?"TikTok جاهز — شغّل الفيديو عدة ثوانٍ ليظهر مصدر التشغيل النظيف":"الصفحة جاهزة — شغّل الفيديو ليظهر مصدره");if(!autoTried){autoTried=true;view.postDelayed(()->captureCurrentVideo(true),tikTokCleanMode?3200L:2000L);}}
        });
    }

    private void loadAddress(){String u=address.getText()==null?"":address.getText().toString().trim();if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u;lastMediaUrl="";autoTried=false;tikTokCleanMode=isTikTokUrl(u)||getIntent().getBooleanExtra(EXTRA_TIKTOK_CLEAN,false);webView.loadUrl(u);}

    private void captureCurrentVideo(boolean automatic){
        status.setText(automatic?"فحص مصدر الفيديو تلقائيًا…":"جاري فحص مصدر الفيديو…");
        String js="(function(){var u='';var vs=document.querySelectorAll('video');for(var i=0;i<vs.length;i++){var x=vs[i].currentSrc||vs[i].src||'';if(x&&x.indexOf('blob:')!==0){u=x;break;}}if(!u){var ss=document.querySelectorAll('video source');for(var j=0;j<ss.length;j++){var y=ss[j].src||ss[j].getAttribute('src')||'';if(y&&y.indexOf('blob:')!==0){u=y;break;}}}if(!u){var m=document.querySelector('meta[property=\\\"og:video:secure_url\\\"],meta[property=\\\"og:video:url\\\"],meta[property=\\\"og:video\\\"]');if(m)u=m.content||'';}return u;})()";
        webView.evaluateJavascript(js,value->{String url=decodeJsString(value);if(tikTokCleanMode){if(isHttp(lastMediaUrl))url=lastMediaUrl;else if(!looksTikTokCleanMedia(url))url="";}else if(!isHttp(url)&&isHttp(lastMediaUrl))url=lastMediaUrl;if(!isHttp(url)){if(!automatic)status.setText(tikTokCleanMode?"لم يظهر مصدر تشغيل نظيف بعد. شغّل فيديو TikTok لعدة ثوانٍ ثم جرّب مرة أخرى.":"لم يتم رصد رابط مباشر بعد. شغّل الفيديو ثم جرّب مرة أخرى.");return;}String lower=url.toLowerCase(Locale.ROOT);if(lower.contains(".m3u8")){status.setText("تم رصد بث HLS. يحتاج محرك تنزيل ودمج منفصل وسنضيفه في مرحلة تنزيلات البث.");return;}String name=tikTokCleanMode?"TikTok_no_watermark_"+System.currentTimeMillis()+".mp4":DownloadUtil.guessFileName(url,"captured_video.mp4");status.setText(tikTokCleanMode?"تم العثور على نسخة التشغيل بدون علامة وبدأ التحميل ✅":"تم رصد الفيديو وبدأ التحميل ✅");enqueue(url,name,isHttp(lastMediaReferer)?lastMediaReferer:webView.getUrl());});
    }

    private boolean looksMedia(String u){String l=u.toLowerCase(Locale.ROOT);return l.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi|mp3|m4a|aac)(\\?.*)?$")||l.contains(".m3u8")||l.contains("mime=video")||l.contains("video_mp4")||l.contains("mime_type=video_")||l.contains("/video/tos/");}
    private boolean looksTikTokCleanMedia(String u){if(!isHttp(u))return false;String l=u.toLowerCase(Locale.ROOT);boolean cdn=l.contains("tiktokcdn")||l.contains("tiktokv")||l.contains("bytevc")||l.contains("tos-maliva")||l.contains("tos-useast")||l.contains("/video/tos/")||l.contains("mime_type=video_")||l.contains("video_mp4");boolean blocked=l.contains("watermark=1")||l.contains("watermark%3d1")||l.contains("downloadaddr")||l.contains("/download/");return cdn&&!blocked&&!l.contains(".m3u8");}
    private boolean isTikTokUrl(String u){if(!isHttp(u))return false;String l=u.toLowerCase(Locale.ROOT);return l.contains("tiktok.com")||l.contains("tiktokv.com")||l.contains("tiktokcdn.com");}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}
    private String decodeJsString(String value){if(value==null||"null".equals(value)||"undefined".equals(value))return"";try{return new JSONArray("["+value+"]").optString(0,"");}catch(Exception e){return value.replace("\\\"","\"").replace("\\/","/").replaceAll("^\"|\"$","");}}

    private void enqueue(String url,String name,String referer){try{DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));request.setTitle(name);request.setDescription("Download Hub Premium");request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);if(AppPrefs.wifiOnly(this))request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);else{request.setAllowedOverMetered(true);request.setAllowedOverRoaming(true);}request.addRequestHeader("User-Agent",NetUtil.USER_AGENT);String cookies=CookieManager.getInstance().getCookie(referer==null?url:referer);if(cookies!=null&&!cookies.isEmpty())request.addRequestHeader("Cookie",cookies);if(referer!=null&&!referer.isEmpty())request.addRequestHeader("Referer",referer);request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"DownloadHub/"+DownloadUtil.sanitizeFileName(name));DownloadManager dm=(DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);long id=dm.enqueue(request);DownloadStore.add(this,id,name,url);HistoryStore.add(this,webView.getUrl(),tikTokCleanMode?"TikTok Clean Capture":"Browser Capture");Toast.makeText(this,"بدأ التحميل",Toast.LENGTH_SHORT).show();}catch(Exception e){status.setText("تعذر بدء التحميل: "+(e.getMessage()==null?"خطأ":e.getMessage()));}}

    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
