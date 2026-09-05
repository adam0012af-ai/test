package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    public static final String EXTRA_PREFILL_URL = "prefill_url";
    private static final int PICK_M3U = 4001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText smartUrl;
    private TextView status;
    private boolean handledIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        consumeIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handledIntent = false;
        consumeIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppPrefs.autoClipboard(this) && smartUrl != null && value(smartUrl).isEmpty()) {
            String clip = clipboardUrl();
            if (isHttp(clip)) {
                smartUrl.setText(clip);
                setStatus("تم العثور على رابط في الحافظة — اضغط تحليل وتحميل");
            }
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG, Color.rgb(8, 18, 34), 0, this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 32));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.app_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62));
        header.addView(logo, logoLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, "Download Hub", 26, Ui.TEXT, true);
        TextView premium = Ui.text(this, "PREMIUM  •  Smart Downloader", 11, Ui.CYAN, true);
        names.addView(title);
        names.addView(premium);
        LinearLayout.LayoutParams namesLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        namesLp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        header.addView(names, namesLp);
        Button settings = Ui.secondary(this, "⚙");
        settings.setTextSize(20);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        root.addView(header);

        TextView tagline = Ui.text(this, "رابط واحد. التطبيق يتعرف على النوع ويختار طريقة التحميل تلقائيًا.", 13, Ui.MUTED, false);
        tagline.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams tlp = Ui.matchWrap();
        tlp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 18));
        root.addView(tagline, tlp);

        LinearLayout smartCard = Ui.card(this);
        smartCard.setBackground(Ui.gradient(Color.rgb(13, 28, 51), Color.rgb(12, 20, 35), 22, this));
        smartCard.addView(Ui.text(this, "Smart Link", 19, Ui.TEXT, true));
        TextView hint = Ui.text(this, "TikTok • Instagram • Facebook • X • Reddit • Vimeo • ملفات مباشرة • M3U", 11, Ui.MUTED, false);
        LinearLayout.LayoutParams hp = Ui.matchWrap(); hp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 10)); smartCard.addView(hint, hp);

        smartUrl = new EditText(this);
        smartUrl.setHint("ألصق الرابط هنا…");
        smartUrl.setTextColor(Ui.TEXT);
        smartUrl.setHintTextColor(Color.rgb(105, 122, 145));
        smartUrl.setTextSize(14);
        smartUrl.setMinLines(3);
        smartUrl.setMaxLines(5);
        smartUrl.setGravity(Gravity.TOP | Gravity.START);
        smartUrl.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);
        smartUrl.setBackground(Ui.bordered(Color.rgb(8, 14, 25), Ui.BORDER, 1, 16, this));
        smartCard.addView(smartUrl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 92)));

        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.HORIZONTAL);
        Button paste = Ui.secondary(this, "لصق");
        paste.setOnClickListener(v -> {
            String u = clipboardUrl();
            if (isHttp(u)) { smartUrl.setText(u); setStatus("تم لصق الرابط"); }
            else show("الحافظة لا تحتوي على رابط HTTP");
        });
        mini.addView(paste, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        Button clear = Ui.secondary(this, "مسح");
        clear.setOnClickListener(v -> { smartUrl.setText(""); setStatus("جاهز"); });
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f); cl.setMargins(Ui.dp(this, 8),0,0,0); mini.addView(clear,cl);
        Button browser = Ui.secondary(this, "متصفح الالتقاط");
        browser.setOnClickListener(v -> openBrowser(value(smartUrl)));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1.35f); bl.setMargins(Ui.dp(this,8),0,0,0); mini.addView(browser,bl);
        LinearLayout.LayoutParams mlp = Ui.matchWrap(); mlp.setMargins(0,Ui.dp(this,10),0,0); smartCard.addView(mini,mlp);

        Button analyze = Ui.primary(this, "تحليل الرابط وتحميله");
        analyze.setTextSize(15);
        analyze.setOnClickListener(v -> smartProcess());
        LinearLayout.LayoutParams alp = Ui.matchWrap(); alp.height = Ui.dp(this, 56); alp.setMargins(0,Ui.dp(this,10),0,0); smartCard.addView(analyze,alp);
        root.addView(smartCard);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); chips.setPadding(0,0,0,0);
        for (String s : new String[]{"TikTok", "Instagram", "Facebook", "X", "Reddit", "Pinterest", "Vimeo", "Dailymotion", "Twitch"}) {
            TextView chip = Ui.text(this, s, 12, Ui.CYAN, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(this,13),0,Ui.dp(this,13),0);
            chip.setBackground(Ui.bordered(Ui.SURFACE_2, Ui.BORDER, 1, 18, this));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this,38)); cp.setMargins(0,0,Ui.dp(this,7),0); chips.addView(chip,cp);
        }
        chipsScroll.addView(chips);
        LinearLayout.LayoutParams csp=Ui.matchWrap();csp.setMargins(0,Ui.dp(this,14),0,0);root.addView(chipsScroll,csp);

        TextView quickTitle = Ui.text(this, "الوصول السريع", 17, Ui.TEXT, true);
        LinearLayout.LayoutParams qtp=Ui.matchWrap();qtp.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,9));root.addView(quickTitle,qtp);
        LinearLayout q1 = new LinearLayout(this); q1.setOrientation(LinearLayout.HORIZONTAL);
        q1.addView(quick("↓  التحميلات", v -> startActivity(new Intent(this, DownloadsActivity.class))), new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f));
        LinearLayout.LayoutParams qb2=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f);qb2.setMargins(Ui.dp(this,9),0,0,0);q1.addView(quick("◉  المتصفح",v->openBrowser(value(smartUrl))),qb2);
        root.addView(q1);
        LinearLayout q2 = new LinearLayout(this); q2.setOrientation(LinearLayout.HORIZONTAL);
        q2.addView(quick("★  السجل والمفضلة",v->startActivity(new Intent(this,HistoryActivity.class))),new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f));
        LinearLayout.LayoutParams qs2=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f);qs2.setMargins(Ui.dp(this,9),0,0,0);q2.addView(quick("⚙  الإعدادات",v->startActivity(new Intent(this,SettingsActivity.class))),qs2);
        LinearLayout.LayoutParams q2p=Ui.matchWrap();q2p.setMargins(0,Ui.dp(this,9),0,0);root.addView(q2,q2p);

        LinearLayout statusCard = Ui.card(this);
        LinearLayout.LayoutParams scp=Ui.matchWrap();scp.setMargins(0,Ui.dp(this,18),0,0);statusCard.setLayoutParams(scp);
        TextView stTitle = Ui.text(this, "الحالة", 13, Ui.MUTED, true); statusCard.addView(stTitle);
        status = Ui.text(this, "جاهز", 14, Ui.GREEN, false); status.setLineSpacing(0,1.2f);
        LinearLayout.LayoutParams stp=Ui.matchWrap();stp.setMargins(0,Ui.dp(this,5),0,0);statusCard.addView(status,stp);
        root.addView(statusCard);

        LinearLayout m3u = Ui.card(this);
        LinearLayout.LayoutParams m3p=Ui.matchWrap();m3p.setMargins(0,Ui.dp(this,14),0,0);m3u.setLayoutParams(m3p);
        LinearLayout mrow = new LinearLayout(this);mrow.setOrientation(LinearLayout.HORIZONTAL);mrow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout mt = new LinearLayout(this);mt.setOrientation(LinearLayout.VERTICAL);mt.addView(Ui.text(this,"M3U Tools",15,Ui.TEXT,true));mt.addView(Ui.text(this,"مكتملة — نفس محرك القوائم الضخمة بدون تغييرات",11,Ui.MUTED,false));
        mrow.addView(mt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button choose=Ui.secondary(this,"اختيار ملف");choose.setOnClickListener(v->pickM3uFile());mrow.addView(choose,new LinearLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,44)));m3u.addView(mrow);root.addView(m3u);

        TextView footer=Ui.text(this,"Download Hub 2.0 • تحميل ذكي • إدارة • التقاط • سجل",11,Color.rgb(100,116,138),false);footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp=Ui.matchWrap();flp.setMargins(0,Ui.dp(this,20),0,0);root.addView(footer,flp);
        return scroll;
    }

    private Button quick(String label, View.OnClickListener listener) {
        Button b = Ui.secondary(this,label); b.setTextSize(13); b.setOnClickListener(listener); return b;
    }

    private void consumeIntent(Intent intent) {
        if (intent == null || handledIntent || smartUrl == null) return;
        handledIntent = true;
        String u = intent.getStringExtra(EXTRA_PREFILL_URL);
        if (u == null && Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) u = extractFirstUrl(text.toString());
        }
        if (u == null && intent.getData() != null) u = intent.getData().toString();
        if (isHttp(u)) { smartUrl.setText(u); setStatus("تم استلام الرابط من تطبيق آخر"); }
    }

    private void smartProcess() {
        String url = value(smartUrl);
        if (!isHttp(url)) { show("ضع رابط HTTP/HTTPS صحيح"); return; }
        String kind = detectKind(url);
        HistoryStore.add(this,url,kind);
        if (isM3u(url)) { fetchM3u(url); return; }
        if (looksDirect(url)) {
            setStatus("تم اكتشاف رابط مباشر — بدء التحميل…");
            enqueue(url,DownloadUtil.guessFileName(url,"download.bin"),null);
            return;
        }
        setStatus("جاري تحليل " + kind + "…");
        executor.execute(() -> {
            try {
                PlatformExtractor.VideoCandidate result = PlatformExtractor.extract(url);
                runOnUiThread(() -> {
                    setStatus("تم العثور على الفيديو ✅\n" + result.fileName);
                    enqueue(result.url,result.fileName,result.referer);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    String msg = safeMessage(e);
                    if (AppPrefs.autoBrowser(this)) {
                        setStatus("الاستخراج المباشر لم ينجح — فتح متصفح الالتقاط…\n" + msg);
                        openBrowser(url);
                    } else setStatus("تعذر الاستخراج: " + msg);
                });
            }
        });
    }

    private void openBrowser(String url) {
        if (!isHttp(url)) { show("ضع رابطًا أولًا"); return; }
        HistoryStore.add(this,url,"Browser");
        Intent i=new Intent(this,BrowserCaptureActivity.class);i.putExtra(BrowserCaptureActivity.EXTRA_URL,url);startActivity(i);
    }

    private void fetchM3u(String url) {
        setStatus("تم اكتشاف M3U — جاري المعالجة Streaming…");
        executor.execute(() -> {
            try (NetUtil.StreamResult source=NetUtil.openTextStream(url,url)) { processM3uStream(source.stream); }
            catch(Exception e){runOnUiThread(()->setStatus("فشل M3U: "+safeMessage(e)));}
        });
    }

    private void pickM3uFile(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_M3U);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_M3U&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();setStatus("جاري قراءة ملف M3U Streaming…");
            executor.execute(()->{try(InputStream input=NetUtil.openUriStream(this,uri)){processM3uStream(input);}catch(Exception e){runOnUiThread(()->setStatus("فشل M3U: "+safeMessage(e)));}});
        }
    }

    private void processM3uStream(InputStream input) throws Exception {
        try(TextExporter.StreamingSession output=TextExporter.openStreaming(this)){
            final long[] processed={0L};
            long total=M3uParser.parseStream(input,entry->{output.write(entry,M3uParser.classifyOne(entry));processed[0]++;if(processed[0]%5000L==0L){long n=processed[0];runOnUiThread(()->setStatus("M3U — تمت معالجة "+String.format(Locale.US,"%,d",n)+" عنصر"));}});
            if(total==0L)throw new IllegalArgumentException("لم يتم العثور على عناصر M3U صالحة");
            TextExporter.StreamingExportResult result=output.finish();
            runOnUiThread(()->setStatus(String.format(Locale.US,"M3U اكتمل ✅\nقنوات: %,d | أفلام: %,d | مسلسلات: %,d",result.channelsCount,result.moviesCount,result.seriesCount)));
        }
    }

    private void enqueue(String url,String name,String referer){
        try{
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);request.setDescription("Download Hub Premium");request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            if(AppPrefs.wifiOnly(this)) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            else { request.setAllowedOverMetered(true); request.setAllowedOverRoaming(true); }
            request.addRequestHeader("User-Agent",NetUtil.USER_AGENT);if(referer!=null&&!referer.isEmpty())request.addRequestHeader("Referer",referer);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"DownloadHub/"+DownloadUtil.sanitizeFileName(name));
            DownloadManager dm=(DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);long id=dm.enqueue(request);DownloadStore.add(this,id,name,url);
            setStatus("بدأ التحميل ✅\n"+name+(AppPrefs.wifiOnly(this)?"\nسيبدأ عند توفر Wi‑Fi":""));Toast.makeText(this,"بدأ التحميل",Toast.LENGTH_SHORT).show();
        }catch(Exception e){setStatus("تعذر بدء التحميل: "+safeMessage(e));}
    }

    private String detectKind(String url){
        try{String h=URI.create(url).getHost();h=h==null?"":h.toLowerCase(Locale.ROOT);if(h.contains("tiktok"))return"TikTok";if(h.contains("instagram"))return"Instagram";if(h.contains("facebook")||h.contains("fb.watch"))return"Facebook";if(h.equals("x.com")||h.contains("twitter"))return"X / Twitter";if(h.contains("reddit"))return"Reddit";if(h.contains("pinterest"))return"Pinterest";if(h.contains("vimeo"))return"Vimeo";if(h.contains("dailymotion"))return"Dailymotion";if(h.contains("twitch"))return"Twitch";}catch(Exception ignored){}
        if(isM3u(url))return"M3U";if(looksDirect(url))return"Direct file";return"Web video";
    }

    private boolean isM3u(String s){String l=s.toLowerCase(Locale.ROOT);return l.matches(".*\\.m3u8?(\\?.*)?$")||l.contains("type=m3u")||l.contains("output=m3u");}
    private boolean looksDirect(String s){String l=s.toLowerCase(Locale.ROOT);return l.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi|mp3|m4a|aac|flac|wav|zip|rar|7z|pdf|apk|jpg|jpeg|png)(\\?.*)?$");}
    private String clipboardUrl(){try{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm!=null&&cm.hasPrimaryClip()){ClipData d=cm.getPrimaryClip();if(d!=null&&d.getItemCount()>0){CharSequence c=d.getItemAt(0).coerceToText(this);if(c!=null)return extractFirstUrl(c.toString());}}}catch(Exception ignored){}return"";}
    private String extractFirstUrl(String text){if(text==null)return"";int a=text.indexOf("https://");int b=text.indexOf("http://");int start=a>=0?(b>=0?Math.min(a,b):a):b;if(start<0)return text.trim();int end=text.length();for(int i=start;i<text.length();i++){char c=text.charAt(i);if(Character.isWhitespace(c)){end=i;break;}}return text.substring(start,end).trim();}
    private String value(EditText e){return e.getText()==null?"":e.getText().toString().trim();}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}
    private void setStatus(String s){if(status!=null)status.setText(s);}
    private void show(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private String safeMessage(Throwable t){String m=t.getMessage();return(m==null||m.trim().isEmpty())?t.getClass().getSimpleName():m;}

    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
