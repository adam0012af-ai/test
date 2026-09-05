package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

public class BrowserCaptureActivity extends Activity {
    public static final String EXTRA_URL = "url";

    private WebView webView;
    private TextView status;
    private String sourceUrl;
    private boolean autoTried;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 13, 18));
        getWindow().setNavigationBarColor(Color.rgb(10, 13, 18));

        sourceUrl = getIntent().getStringExtra(EXTRA_URL);
        if (sourceUrl == null || (!sourceUrl.startsWith("http://") && !sourceUrl.startsWith("https://"))) {
            finish();
            return;
        }
        setContentView(buildUi());
        setupWebView();
        webView.loadUrl(sourceUrl);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 13, 18));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(Color.rgb(17, 22, 30));

        status = new TextView(this);
        status.setText("افتح/شغّل الفيديو ثم اضغط التقاط الفيديو");
        status.setTextColor(Color.rgb(190, 200, 215));
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        bar.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button capture = button("التقاط الفيديو");
        capture.setOnClickListener(v -> captureCurrentVideo(false));
        buttons.addView(capture, new LinearLayout.LayoutParams(0, dp(46), 1f));

        Button downloads = button("التحميلات");
        downloads.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(55, 63, 78)));
        downloads.setOnClickListener(v -> startActivity(new android.content.Intent(this, DownloadsActivity.class)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        dlp.setMargins(dp(7), 0, 0, 0);
        buttons.addView(downloads, dlp);

        bar.addView(buttons);
        root.addView(bar);

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(NetUtil.USER_AGENT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    String name = DownloadUtil.guessFileName(url, "platform_video.mp4");
                    enqueue(url, name, webView.getUrl());
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                status.setText("تم تحميل الصفحة — شغّل الفيديو لو لم يبدأ تلقائيًا");
                if (!autoTried) {
                    autoTried = true;
                    view.postDelayed(() -> captureCurrentVideo(true), 1800L);
                }
            }
        });
    }

    private void captureCurrentVideo(boolean automatic) {
        status.setText(automatic ? "محاولة التقاط الفيديو تلقائيًا…" : "جاري البحث عن مصدر الفيديو…");
        String js = "(function(){" +
                "var u='';" +
                "var vs=document.querySelectorAll('video');" +
                "for(var i=0;i<vs.length;i++){var x=vs[i].currentSrc||vs[i].src||'';if(x&&x.indexOf('blob:')!==0){u=x;break;}}" +
                "if(!u){var ss=document.querySelectorAll('video source');for(var j=0;j<ss.length;j++){var y=ss[j].src||ss[j].getAttribute('src')||'';if(y&&y.indexOf('blob:')!==0){u=y;break;}}}" +
                "if(!u){var m=document.querySelector('meta[property=\\\"og:video:secure_url\\\"],meta[property=\\\"og:video:url\\\"],meta[property=\\\"og:video\\\"]');if(m)u=m.content||'';}" +
                "return u;})()";

        webView.evaluateJavascript(js, value -> {
            String url = decodeJsString(value);
            if (url == null || url.isEmpty() || url.startsWith("blob:")) {
                if (!automatic) status.setText("لم يظهر رابط مباشر بعد. شغّل الفيديو ثم جرّب مرة أخرى.");
                return;
            }
            String lower = url.toLowerCase();
            if (lower.contains(".m3u8")) {
                status.setText("تم العثور على HLS (.m3u8)، لكنه يحتاج محرك دمج خاص وليس DownloadManager المباشر.");
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (!automatic) status.setText("المصدر ليس رابط HTTP مباشرًا.");
                return;
            }
            String name = DownloadUtil.guessFileName(url, "platform_video.mp4");
            status.setText("تم العثور على الفيديو وبدأ التحميل ✅");
            enqueue(url, name, webView.getUrl());
        });
    }

    private String decodeJsString(String value) {
        if (value == null || "null".equals(value) || "undefined".equals(value)) return "";
        try {
            return new JSONArray("[" + value + "]").optString(0, "");
        } catch (Exception e) {
            return value.replace("\\\"", "\"").replace("\\/", "/").replaceAll("^\"|\"$", "");
        }
    }

    private void enqueue(String url, String name, String referer) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setDescription("Download Hub");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.addRequestHeader("User-Agent", NetUtil.USER_AGENT);
            if (referer != null && !referer.isEmpty()) request.addRequestHeader("Referer", referer);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "DownloadHub/" + DownloadUtil.sanitizeFileName(name));

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long id = dm.enqueue(request);
            DownloadStore.add(this, id, name, url);
            Toast.makeText(this, "بدأ التحميل", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("تعذر بدء التحميل: " + (e.getMessage() == null ? "خطأ" : e.getMessage()));
        }
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(34, 116, 232)));
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
