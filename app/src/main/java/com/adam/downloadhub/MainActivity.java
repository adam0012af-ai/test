package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    public static final String EXTRA_PREFILL_URL = "prefill_url";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText smartUrl;
    private TextView status;
    private Button downloadButton;
    private boolean handledIntent;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        consumeIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handledIntent = false;
        consumeIntent(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        if (AppPrefs.autoClipboard(this) && smartUrl != null && value(smartUrl).isEmpty()) {
            String clip = clipboardUrl();
            if (isHttp(clip)) {
                smartUrl.setText(clip);
                setStatus("الرابط جاهز للتحميل");
            }
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG, Color.rgb(5, 16, 31), 0, this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,30));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        // Compact premium header.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0,0,0,Ui.dp(this,8));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.app_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,58),Ui.dp(this,58)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView app = Ui.text(this,"Download Hub",26,Ui.TEXT,true);
        TextView premium = Ui.text(this,"PREMIUM MEDIA DOWNLOADER",10,Ui.CYAN,true);
        brand.addView(app);brand.addView(premium);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        bp.setMargins(Ui.dp(this,11),0,0,0);header.addView(brand,bp);

        Button settings=Ui.secondary(this,"⚙");settings.setTextSize(19);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        header.addView(settings,new LinearLayout.LayoutParams(Ui.dp(this,50),Ui.dp(this,46)));
        root.addView(header);

        // Hero.
        LinearLayout hero = Ui.card(this);
        hero.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18));
        hero.setBackground(Ui.gradient(Color.rgb(10,34,66),Color.rgb(8,22,43),24,this));
        TextView heroTitle=Ui.text(this,"حمّل الفيديو والصوت بسهولة",24,Ui.TEXT,true);hero.addView(heroTitle);
        TextView heroSub=Ui.text(this,"ألصق الرابط فقط — Download Hub يتعرّف على المنصة ويعرض الخيارات المناسبة تلقائيًا.",13,Ui.MUTED,false);heroSub.setLineSpacing(0,1.25f);LinearLayout.LayoutParams hs=Ui.matchWrap();hs.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,13));hero.addView(heroSub,hs);

        LinearLayout badges=new LinearLayout(this);badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.addView(badge("بدون علامة مائية"),new LinearLayout.LayoutParams(0,Ui.dp(this,36),1.2f));
        LinearLayout.LayoutParams b2=new LinearLayout.LayoutParams(0,Ui.dp(this,36),1f);b2.setMargins(Ui.dp(this,6),0,0,0);badges.addView(badge("اختيار الجودة"),b2);
        LinearLayout.LayoutParams b3=new LinearLayout.LayoutParams(0,Ui.dp(this,36),.9f);b3.setMargins(Ui.dp(this,6),0,0,0);badges.addView(badge("صوت فقط"),b3);
        hero.addView(badges);
        root.addView(hero);

        LinearLayout downloader=Ui.card(this);
        LinearLayout.LayoutParams dcp=Ui.matchWrap();dcp.setMargins(0,Ui.dp(this,14),0,0);downloader.setLayoutParams(dcp);
        downloader.addView(Ui.text(this,"تحميل الوسائط",19,Ui.TEXT,true));
        TextView shortSupport=Ui.text(this,"يدعم أشهر منصات الفيديو والروابط المباشرة",12,Ui.CYAN,true);LinearLayout.LayoutParams ssp=Ui.matchWrap();ssp.setMargins(0,Ui.dp(this,3),0,Ui.dp(this,10));downloader.addView(shortSupport,ssp);

        smartUrl=new EditText(this);
        smartUrl.setHint("ألصق رابط الفيديو هنا");smartUrl.setTextColor(Ui.TEXT);smartUrl.setHintTextColor(Color.rgb(111,130,157));smartUrl.setTextSize(14);smartUrl.setSingleLine(false);smartUrl.setMinLines(2);smartUrl.setMaxLines(3);smartUrl.setGravity(Gravity.TOP|Gravity.START);smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);smartUrl.setPadding(Ui.dp(this,14),Ui.dp(this,13),Ui.dp(this,14),Ui.dp(this,13));smartUrl.setBackground(Ui.bordered(Color.rgb(6,14,26),Color.rgb(42,69,101),1,17,this));
        downloader.addView(smartUrl,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,76)));

        LinearLayout linkActions=new LinearLayout(this);linkActions.setOrientation(LinearLayout.HORIZONTAL);
        Button paste=Ui.secondary(this,"لصق");paste.setOnClickListener(v->pasteLink());linkActions.addView(paste,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f));
        Button clear=Ui.secondary(this,"مسح");clear.setOnClickListener(v->{smartUrl.setText("");setStatus("جاهز");});LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);c.setMargins(Ui.dp(this,8),0,0,0);linkActions.addView(clear,c);
        LinearLayout.LayoutParams lap=Ui.matchWrap();lap.setMargins(0,Ui.dp(this,9),0,0);downloader.addView(linkActions,lap);

        downloadButton=Ui.primary(this,"↓  تحميل");downloadButton.setTextSize(17);downloadButton.setOnClickListener(v->smartProcess());LinearLayout.LayoutParams dl=Ui.matchWrap();dl.height=Ui.dp(this,58);dl.setMargins(0,Ui.dp(this,10),0,0);downloader.addView(downloadButton,dl);
        root.addView(downloader);

        LinearLayout state=Ui.card(this);LinearLayout.LayoutParams stc=Ui.matchWrap();stc.setMargins(0,Ui.dp(this,12),0,0);state.setLayoutParams(stc);
        status=Ui.text(this,"جاهز",13,Ui.GREEN,false);status.setGravity(Gravity.CENTER);status.setLineSpacing(0,1.2f);state.addView(status);root.addView(state);

        TextView toolsTitle=Ui.text(this,"مركز الأدوات",18,Ui.TEXT,true);LinearLayout.LayoutParams ttp=Ui.matchWrap();ttp.setMargins(0,Ui.dp(this,18),0,Ui.dp(this,9));root.addView(toolsTitle,ttp);
        root.addView(quickRow("↓  التحميلات",v->startActivity(new Intent(this,DownloadsActivity.class)),"☷  Batch",v->startActivity(new Intent(this,BatchActivity.class))));
        LinearLayout.LayoutParams row2p=Ui.matchWrap();row2p.setMargins(0,Ui.dp(this,9),0,0);root.addView(quickRow("★  السجل والمفضلة",v->startActivity(new Intent(this,HistoryActivity.class)),"▥  الإحصائيات",v->startActivity(new Intent(this,StatsActivity.class))),row2p);
        LinearLayout.LayoutParams row3p=Ui.matchWrap();row3p.setMargins(0,Ui.dp(this,9),0,0);root.addView(singleQuick("⚙  الإعدادات",v->startActivity(new Intent(this,SettingsActivity.class))),row3p);

        TextView footer=Ui.text(this,"Download Hub v5 • Developed by AboAdam",11,Color.rgb(96,118,148),true);footer.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=Ui.matchWrap();fp.setMargins(0,Ui.dp(this,22),0,0);root.addView(footer,fp);
        return scroll;
    }

    private TextView badge(String value){TextView t=Ui.text(this,value,10,Ui.CYAN,true);t.setGravity(Gravity.CENTER);t.setBackground(Ui.bordered(Color.rgb(8,24,43),Color.rgb(25,80,112),1,14,this));return t;}

    private LinearLayout quickRow(String a,View.OnClickListener al,String b,View.OnClickListener bl){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button x=Ui.secondary(this,a);x.setTextSize(13);x.setOnClickListener(al);row.addView(x,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f));Button y=Ui.secondary(this,b);y.setTextSize(13);y.setOnClickListener(bl);LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1f);yp.setMargins(Ui.dp(this,9),0,0,0);row.addView(y,yp);return row;}
    private LinearLayout singleQuick(String label,View.OnClickListener listener){LinearLayout row=new LinearLayout(this);Button b=Ui.secondary(this,label);b.setOnClickListener(listener);row.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,54)));return row;}

    private void pasteLink(){String u=clipboardUrl();if(isHttp(u)){smartUrl.setText(u);setStatus("الرابط جاهز للتحميل");}else show("الحافظة لا تحتوي على رابط صحيح");}

    private void consumeIntent(Intent intent){if(intent==null||handledIntent||smartUrl==null)return;handledIntent=true;String u=intent.getStringExtra(EXTRA_PREFILL_URL);if(u==null&&Intent.ACTION_SEND.equals(intent.getAction())){CharSequence text=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(text!=null)u=extractFirstUrl(text.toString());}if(u==null&&intent.getData()!=null)u=intent.getData().toString();if(isHttp(u)){smartUrl.setText(u);setStatus("تم استلام الرابط — اضغط تحميل");}}

    private void smartProcess(){
        String raw=value(smartUrl);if(!isHttp(raw)){show("ضع رابط HTTP/HTTPS صحيح");return;}
        String url=AppPrefs.linkCleaner(this)?LinkTools.clean(raw):raw;
        if(!url.equals(raw))smartUrl.setText(url);
        String platform=detectKind(url);HistoryStore.add(this,url,platform);
        setBusy(true);setStatus("جاري تجهيز خيارات التحميل…");
        executor.execute(()->{try{PlatformExtractor.MediaBundle bundle=PlatformExtractor.extractOptions(url);runOnUiThread(()->{setBusy(false);presentBundle(bundle);});}catch(Exception e){runOnUiThread(()->{setBusy(false);setStatus("تعذر تجهيز التحميل\n"+safeMessage(e));});}});
    }

    private void presentBundle(PlatformExtractor.MediaBundle bundle){List<MediaOption> options=bundle.options;if(options==null||options.isEmpty()){setStatus("لم يتم العثور على فيديو أو صوت قابل للتحميل");return;}setStatus(bundle.platform+" • "+options.size()+" خيارات متاحة");MediaOptionsDialog.show(this,bundle.title,options,this::startDownload);}

    private void startDownload(MediaOption option){try{long id=DownloadEngine.enqueue(this,option);setStatus("تمت إضافة التحميل ✓\n"+option.label+(option.sizeBytes>0?" • "+MediaOption.formatBytes(option.sizeBytes):""));show("بدأ التحميل #"+id);}catch(Exception e){setStatus(safeMessage(e));show(safeMessage(e));}}

    private void setBusy(boolean busy){if(downloadButton==null)return;downloadButton.setEnabled(!busy);downloadButton.setAlpha(busy?.55f:1f);downloadButton.setText(busy?"جاري التجهيز…":"↓  تحميل");}
    private void setStatus(String s){if(status!=null)status.setText(s==null?"":s);}
    private String value(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}
    private String clipboardUrl(){try{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm==null||!cm.hasPrimaryClip())return"";ClipData c=cm.getPrimaryClip();if(c==null||c.getItemCount()==0)return"";CharSequence t=c.getItemAt(0).coerceToText(this);return t==null?"":extractFirstUrl(t.toString());}catch(Exception e){return"";}}
    private String extractFirstUrl(String text){if(text==null)return"";Matcher m=Pattern.compile("https?://\\S+").matcher(text);if(!m.find())return"";String u=m.group();while(u.endsWith(")")||u.endsWith("]")||u.endsWith("}")||u.endsWith(",")||u.endsWith("."))u=u.substring(0,u.length()-1);return u;}
    private String detectKind(String url){try{String h=new URI(url).getHost();String p=PlatformExtractor.platformName(h);return "Web".equals(p)?"رابط وسائط":p;}catch(Exception e){return"رابط وسائط";}}
    private String safeMessage(Throwable e){String m=e==null?null:e.getMessage();return TextUtils.isEmpty(m)?"حدث خطأ أثناء تجهيز التحميل":m;}
    private void show(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
