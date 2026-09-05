package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.List;
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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        consumeIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); handledIntent=false; consumeIntent(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        if (AppPrefs.autoClipboard(this) && smartUrl != null && value(smartUrl).isEmpty()) {
            String clip=clipboardUrl(); if(isHttp(clip)){smartUrl.setText(clip);setStatus("تم العثور على رابط في الحافظة — جاهز للتحليل");}
        }
    }

    private View buildUi() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(Ui.gradient(Ui.BG,Color.rgb(8,18,34),0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,32));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_logo);logo.setScaleType(ImageView.ScaleType.CENTER_CROP);header.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,64)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.addView(Ui.text(this,"Download Hub",27,Ui.TEXT,true));names.addView(Ui.text(this,"V3 PREMIUM • Universal Media Downloader",11,Ui.CYAN,true));
        LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);nlp.setMargins(Ui.dp(this,12),0,0,0);header.addView(names,nlp);
        Button settings=Ui.secondary(this,"⚙");settings.setTextSize(20);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(settings,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,48)));root.addView(header);

        TextView tagline=Ui.text(this,"حلّل الرابط، اعرض الجودات والصوت المتاح، ثم اختر ما تريد تحميله.",13,Ui.MUTED,false);tagline.setLineSpacing(0,1.2f);LinearLayout.LayoutParams tlp=Ui.matchWrap();tlp.setMargins(0,Ui.dp(this,10),0,Ui.dp(this,18));root.addView(tagline,tlp);

        LinearLayout smart=Ui.card(this);smart.setBackground(Ui.gradient(Color.rgb(13,28,51),Color.rgb(12,20,35),22,this));smart.addView(Ui.text(this,"Smart Media Analyzer",19,Ui.TEXT,true));
        TextView hint=Ui.text(this,"فيديو • صوت فقط • جودة متعددة • فحص سلامة المصدر • بدون علامة مائية عند توفر نسخة نظيفة",11,Ui.MUTED,false);LinearLayout.LayoutParams hp=Ui.matchWrap();hp.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,10));smart.addView(hint,hp);
        smartUrl=new EditText(this);smartUrl.setHint("ألصق رابط الفيديو أو الصفحة هنا…");smartUrl.setTextColor(Ui.TEXT);smartUrl.setHintTextColor(Color.rgb(105,122,145));smartUrl.setTextSize(14);smartUrl.setMinLines(3);smartUrl.setMaxLines(5);smartUrl.setGravity(Gravity.TOP|Gravity.START);smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);smartUrl.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));smartUrl.setBackground(Ui.bordered(Color.rgb(8,14,25),Ui.BORDER,1,16,this));smart.addView(smartUrl,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,92)));

        LinearLayout mini=new LinearLayout(this);mini.setOrientation(LinearLayout.HORIZONTAL);
        Button paste=Ui.secondary(this,"لصق");paste.setOnClickListener(v->{String u=clipboardUrl();if(isHttp(u)){smartUrl.setText(u);setStatus("تم لصق الرابط");}else show("الحافظة لا تحتوي على رابط صحيح");});mini.addView(paste,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f));
        Button clear=Ui.secondary(this,"مسح");clear.setOnClickListener(v->{smartUrl.setText("");setStatus("جاهز");});LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);cl.setMargins(Ui.dp(this,8),0,0,0);mini.addView(clear,cl);
        Button browser=Ui.secondary(this,"متصفح الالتقاط");browser.setOnClickListener(v->openBrowser(value(smartUrl)));LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1.35f);bl.setMargins(Ui.dp(this,8),0,0,0);mini.addView(browser,bl);LinearLayout.LayoutParams mlp=Ui.matchWrap();mlp.setMargins(0,Ui.dp(this,10),0,0);smart.addView(mini,mlp);
        Button analyze=Ui.primary(this,"تحليل واختيار الجودة");analyze.setTextSize(15);analyze.setOnClickListener(v->smartProcess());LinearLayout.LayoutParams alp=Ui.matchWrap();alp.height=Ui.dp(this,56);alp.setMargins(0,Ui.dp(this,10),0,0);smart.addView(analyze,alp);root.addView(smart);

        HorizontalScrollView chipScroll=new HorizontalScrollView(this);chipScroll.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);
        String[] platforms={"TikTok","YouTube","Instagram","Facebook","X","Reddit","Pinterest","Vimeo","Dailymotion","Twitch","Threads","Snapchat","Likee","Kwai","VK","Tumblr","Streamable","Rumble"};
        for(String s:platforms){TextView chip=Ui.text(this,s,12,Ui.CYAN,true);chip.setGravity(Gravity.CENTER);chip.setPadding(Ui.dp(this,13),0,Ui.dp(this,13),0);chip.setBackground(Ui.bordered(Ui.SURFACE_2,Ui.BORDER,1,18,this));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(this,38));cp.setMargins(0,0,Ui.dp(this,7),0);chips.addView(chip,cp);}chipScroll.addView(chips);LinearLayout.LayoutParams csp=Ui.matchWrap();csp.setMargins(0,Ui.dp(this,14),0,0);root.addView(chipScroll,csp);

        TextView quickTitle=Ui.text(this,"مركز الأدوات",17,Ui.TEXT,true);LinearLayout.LayoutParams qtp=Ui.matchWrap();qtp.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,9));root.addView(quickTitle,qtp);
        root.addView(quickRow("↓  التحميلات",v->startActivity(new Intent(this,DownloadsActivity.class)),"◉  المتصفح",v->openBrowser(value(smartUrl))));
        LinearLayout.LayoutParams r2=Ui.matchWrap();r2.setMargins(0,Ui.dp(this,9),0,0);LinearLayout row2=quickRow("☷  Batch",v->startActivity(new Intent(this,BatchActivity.class)),"★  السجل",v->startActivity(new Intent(this,HistoryActivity.class)));root.addView(row2,r2);
        LinearLayout.LayoutParams r3=Ui.matchWrap();r3.setMargins(0,Ui.dp(this,9),0,0);LinearLayout row3=quickRow("▥  الإحصائيات",v->startActivity(new Intent(this,StatsActivity.class)),"⚙  الإعدادات",v->startActivity(new Intent(this,SettingsActivity.class)));root.addView(row3,r3);

        LinearLayout stateCard=Ui.card(this);LinearLayout.LayoutParams scp=Ui.matchWrap();scp.setMargins(0,Ui.dp(this,18),0,0);stateCard.setLayoutParams(scp);stateCard.addView(Ui.text(this,"الحالة",13,Ui.MUTED,true));status=Ui.text(this,"جاهز لتحليل أي رابط",14,Ui.GREEN,false);status.setLineSpacing(0,1.2f);LinearLayout.LayoutParams stp=Ui.matchWrap();stp.setMargins(0,Ui.dp(this,5),0,0);stateCard.addView(status,stp);root.addView(stateCard);

        LinearLayout m3u=Ui.card(this);LinearLayout.LayoutParams m3p=Ui.matchWrap();m3p.setMargins(0,Ui.dp(this,14),0,0);m3u.setLayoutParams(m3p);LinearLayout mrow=new LinearLayout(this);mrow.setOrientation(LinearLayout.HORIZONTAL);mrow.setGravity(Gravity.CENTER_VERTICAL);LinearLayout mt=new LinearLayout(this);mt.setOrientation(LinearLayout.VERTICAL);mt.addView(Ui.text(this,"M3U Tools",15,Ui.TEXT,true));mt.addView(Ui.text(this,"مكتملة — نفس محرك القوائم الضخمة بدون أي تغيير",11,Ui.MUTED,false));mrow.addView(mt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Button choose=Ui.secondary(this,"اختيار ملف");choose.setOnClickListener(v->pickM3uFile());mrow.addView(choose,new LinearLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,44)));m3u.addView(mrow);root.addView(m3u);

        TextView footer=Ui.text(this,"Download Hub v3 • تحميل الفيديوهات والصوت من المنصات المدعومة — بدون علامة مائية عند توفر نسخة نظيفة من المصدر.",11,Color.rgb(105,122,145),false);footer.setGravity(Gravity.CENTER);footer.setLineSpacing(0,1.15f);LinearLayout.LayoutParams flp=Ui.matchWrap();flp.setMargins(0,Ui.dp(this,20),0,0);root.addView(footer,flp);
        return scroll;
    }

    private LinearLayout quickRow(String a,View.OnClickListener al,String b,View.OnClickListener bl){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button x=Ui.secondary(this,a);x.setTextSize(13);x.setOnClickListener(al);row.addView(x,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f));Button y=Ui.secondary(this,b);y.setTextSize(13);y.setOnClickListener(bl);LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f);yp.setMargins(Ui.dp(this,9),0,0,0);row.addView(y,yp);return row;}

    private void consumeIntent(Intent intent){if(intent==null||handledIntent||smartUrl==null)return;handledIntent=true;String u=intent.getStringExtra(EXTRA_PREFILL_URL);if(u==null&&Intent.ACTION_SEND.equals(intent.getAction())){CharSequence text=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(text!=null)u=extractFirstUrl(text.toString());}if(u==null&&intent.getData()!=null)u=intent.getData().toString();if(isHttp(u)){smartUrl.setText(u);setStatus("تم استلام الرابط من تطبيق آخر");}}

    private void smartProcess(){String url=value(smartUrl);if(!isHttp(url)){show("ضع رابط HTTP/HTTPS صحيح");return;}String kind=detectKind(url);HistoryStore.add(this,url,kind);if(isM3u(url)){fetchM3u(url);return;}if(looksDirectNonMedia(url)){MediaOption o=new MediaOption(url,"ملف مباشر",DownloadUtil.guessFileName(url,"download.bin"),null,"Files","",-1,false);startDownload(o);return;}setStatus("جاري تحليل "+kind+" والبحث عن الجودات…");executor.execute(()->{try{PlatformExtractor.MediaBundle bundle=PlatformExtractor.extractOptions(url);runOnUiThread(()->presentBundle(bundle));}catch(Exception e){runOnUiThread(()->{String msg=safeMessage(e);if(AppPrefs.autoBrowser(this)){setStatus("الاستخراج المباشر لم يجد ملفًا كاملًا — فتح متصفح الالتقاط…\n"+msg);openBrowser(url);}else setStatus("تعذر الاستخراج: "+msg);});}});}

    private void presentBundle(PlatformExtractor.MediaBundle bundle){List<MediaOption> options=bundle.options;setStatus("تم العثور على "+options.size()+" خيار صالح • "+bundle.platform+"\nاختر الجودة أو الصوت");String mode=AppPrefs.defaultMode(this);if("best".equals(mode)){MediaOption best=firstVideo(options);if(best!=null){startDownload(best);return;}}else if("audio".equals(mode)){MediaOption audio=firstAudio(options);if(audio!=null){startDownload(audio);return;}}MediaOptionsDialog.show(this,bundle.title+"\n"+bundle.platform,options,this::startDownload);}
    private MediaOption firstVideo(List<MediaOption> xs){for(MediaOption o:xs)if(!o.audioOnly)return o;return xs.isEmpty()?null:xs.get(0);}private MediaOption firstAudio(List<MediaOption> xs){for(MediaOption o:xs)if(o.audioOnly)return o;return null;}

    private void startDownload(MediaOption option){try{long id=DownloadEngine.enqueue(this,option);setStatus("بدأ التحميل ✅\n"+option.displayLabel()+"\n"+option.fileName+(AppPrefs.wifiOnly(this)?"\nسيبدأ عند توفر Wi‑Fi":""));show("تمت إضافة التحميل #"+id);}catch(Exception e){setStatus("تعذر بدء التحميل: "+safeMessage(e));}}

    private void openBrowser(String url){if(!isHttp(url)){show("ضع رابطًا أولًا");return;}HistoryStore.add(this,url,"Browser");Intent i=new Intent(this,BrowserCaptureActivity.class);i.putExtra(BrowserCaptureActivity.EXTRA_URL,url);startActivity(i);}

    private void fetchM3u(String url){setStatus("تم اكتشاف M3U — جاري المعالجة Streaming…");executor.execute(()->{try(NetUtil.StreamResult source=NetUtil.openTextStream(url,url)){processM3uStream(source.stream);}catch(Exception e){runOnUiThread(()->setStatus("فشل M3U: "+safeMessage(e)));}});}
    private void pickM3uFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_M3U);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_M3U&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();setStatus("جاري قراءة ملف M3U Streaming…");executor.execute(()->{try(InputStream input=NetUtil.openUriStream(this,uri)){processM3uStream(input);}catch(Exception e){runOnUiThread(()->setStatus("فشل M3U: "+safeMessage(e)));}});}}
    private void processM3uStream(InputStream input)throws Exception{try(TextExporter.StreamingSession output=TextExporter.openStreaming(this)){final long[] processed={0};long total=M3uParser.parseStream(input,entry->{output.write(entry,M3uParser.classifyOne(entry));processed[0]++;if(processed[0]%5000==0){long n=processed[0];runOnUiThread(()->setStatus("M3U — تمت معالجة "+String.format(Locale.US,"%,d",n)+" عنصر"));}});if(total==0)throw new IllegalArgumentException("لم يتم العثور على عناصر M3U صالحة");TextExporter.StreamingExportResult result=output.finish();runOnUiThread(()->setStatus(String.format(Locale.US,"M3U اكتمل ✅\nقنوات: %,d | أفلام: %,d | مسلسلات: %,d",result.channelsCount,result.moviesCount,result.seriesCount)));}}

    private String detectKind(String url){try{String h=URI.create(url).getHost();String p=PlatformExtractor.platformName(h);if(!"Web".equals(p))return p;}catch(Exception ignored){}if(isM3u(url))return"M3U";if(looksDirectNonMedia(url))return"Direct file";return"Web media";}
    private boolean isM3u(String s){String l=s.toLowerCase(Locale.ROOT);return l.matches(".*\\.m3u8?(\\?.*)?$")||l.contains("type=m3u")||l.contains("output=m3u");}
    private boolean looksDirectNonMedia(String s){String l=s.toLowerCase(Locale.ROOT);return l.matches(".*\\.(zip|rar|7z|pdf|apk|jpg|jpeg|png|gif|txt)(\\?.*)?$");}
    private String clipboardUrl(){try{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm!=null&&cm.hasPrimaryClip()){ClipData d=cm.getPrimaryClip();if(d!=null&&d.getItemCount()>0){CharSequence c=d.getItemAt(0).coerceToText(this);if(c!=null)return extractFirstUrl(c.toString());}}}catch(Exception ignored){}return"";}
    private String extractFirstUrl(String text){if(text==null)return"";int a=text.indexOf("https://"),b=text.indexOf("http://");int start=a>=0?(b>=0?Math.min(a,b):a):b;if(start<0)return text.trim();int end=text.length();for(int i=start;i<text.length();i++){if(Character.isWhitespace(text.charAt(i))){end=i;break;}}return text.substring(start,end).trim();}
    private String value(EditText e){return e.getText()==null?"":e.getText().toString().trim();}private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}private void setStatus(String s){if(status!=null)status.setText(s);}private void show(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}private String safeMessage(Throwable t){String m=t.getMessage();return m==null||m.trim().isEmpty()?t.getClass().getSimpleName():m;}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
