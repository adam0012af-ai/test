package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
    public static final String EXTRA_PREFILL_URL="prefill_url";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private EditText smartUrl;
    private TextView status;
    private Button downloadButton;
    private boolean handledIntent;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());consumeIntent(getIntent());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handledIntent=false;consumeIntent(i);}
    @Override protected void onResume(){super.onResume();if(AppPrefs.autoClipboard(this)&&smartUrl!=null&&value(smartUrl).isEmpty()){String u=clipboardUrl();if(isHttp(u)){smartUrl.setText(u);setStatus("رابط من الحافظة جاهز");}}}

    private View buildUi(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,30));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        Button menu=Ui.ghost(this,"☰");menu.setTextSize(20);menu.setOnClickListener(v->AppSideMenu.show(this));header.addView(menu,new LinearLayout.LayoutParams(Ui.dp(this,50),Ui.dp(this,48)));
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams lg=new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48));lg.setMargins(Ui.dp(this,8),0,Ui.dp(this,8),0);header.addView(logo,lg);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.addView(Ui.text(this,"Download Hub Studio",22,Ui.TEXT,true));brand.addView(Ui.text(this,"CREATE • EDIT • TEMPLATES • PUBLISH",9,Ui.CYAN,true));header.addView(brand,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button settings=Ui.ghost(this,"⚙");settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(settings,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));root.addView(header);

        LinearLayout hero=Ui.card(this);LinearLayout.LayoutParams hp=Ui.matchWrap();hp.setMargins(0,Ui.dp(this,14),0,0);hero.setLayoutParams(hp);hero.setPadding(Ui.dp(this,18),Ui.dp(this,20),Ui.dp(this,18),Ui.dp(this,20));hero.setBackground(Ui.gradient3(0xFF113A73,0xFF14284C,0xFF081322,28,this));
        TextView badge=Ui.chip(this,"CREATOR STUDIO",Ui.CYAN);hero.addView(badge);
        TextView title=Ui.text(this,"حوّل فكرتك إلى فيديو جاهز للنشر",25,Ui.TEXT,true);title.setLineSpacing(0,1.05f);LinearLayout.LayoutParams tt=Ui.matchWrap();tt.setMargins(0,Ui.dp(this,12),0,0);hero.addView(title,tt);
        TextView desc=Ui.text(this,"مونتاج Timeline متعدد المقاطع، قص وتقسيم وسرعة وصوت ونصوص ومقاسات مختلفة، مع قوالب Online جاهزة وصانع ريلز إسلامي.",12,0xFFD7E7FA,false);desc.setLineSpacing(0,1.3f);LinearLayout.LayoutParams ds=Ui.matchWrap();ds.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,15));hero.addView(desc,ds);
        Button newProject=Ui.accent(this,"＋  مشروع مونتاج جديد");newProject.setTextSize(17);newProject.setOnClickListener(v->openBlankEditor());hero.addView(newProject,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,60)));
        LinearLayout heroRow=new LinearLayout(this);heroRow.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams hrp=Ui.matchWrap();hrp.setMargins(0,Ui.dp(this,8),0,0);Button templates=Ui.primary(this,"▦  القوالب");templates.setOnClickListener(v->startActivity(new Intent(this,TemplateLibraryActivity.class)));heroRow.addView(templates,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));Button islamic=Ui.secondary(this,"☾  إسلامي");islamic.setOnClickListener(v->startActivity(new Intent(this,IslamicReelsActivity.class)));LinearLayout.LayoutParams isl=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);isl.setMargins(Ui.dp(this,8),0,0,0);heroRow.addView(islamic,isl);hero.addView(heroRow,hrp);root.addView(hero);

        TextView what=Ui.sectionTitle(this,"كل حاجة في مكان واحد");LinearLayout.LayoutParams wp=Ui.matchWrap();wp.setMargins(0,Ui.dp(this,20),0,Ui.dp(this,8));root.addView(what,wp);
        root.addView(featureRow("✂","Video Editor","Timeline • Split • Trim • Speed",v->openBlankEditor(),"▦","Templates","Online • Download • Edit",v->startActivity(new Intent(this,TemplateLibraryActivity.class))));
        LinearLayout.LayoutParams fr2=Ui.matchWrap();fr2.setMargins(0,Ui.dp(this,8),0,0);root.addView(featureRow("♫","Audio Mix","Original + Music + Timing",v->openBlankEditor(),"☾","Islamic Studio","Quran • Dhikr • Dua",v->startActivity(new Intent(this,IslamicReelsActivity.class))),fr2);
        LinearLayout.LayoutParams fr3=Ui.matchWrap();fr3.setMargins(0,Ui.dp(this,8),0,0);root.addView(featureRow("↓","Downloader","Direct links",v->scroll.post(()->scroll.smoothScrollTo(0,Ui.dp(this,1050))),"↗","Publishing","Export then TikTok",v->toast("صدّر أي مشروع وبعدها هتفتح شاشة TikTok Publishing")),fr3);

        LinearLayout pro=Ui.card(this);LinearLayout.LayoutParams pr=Ui.matchWrap();pr.setMargins(0,Ui.dp(this,18),0,0);pro.setLayoutParams(pr);pro.addView(Ui.text(this,"Video Studio — النسخة الحالية",16,Ui.TEXT,true));TextView proText=Ui.text(this,"المحرر دلوقتي يدعم أكثر من Clip، Split، Trim In/Out، تقديم وتأخير، Speed، Original Volume، موسيقى منفصلة مع Timing، Text styling، Ratio 9:16 / 1:1 / 16:9، Fade، Undo/Redo وتصدير MP4.",11,Ui.MUTED,false);proText.setLineSpacing(0,1.28f);LinearLayout.LayoutParams pt=Ui.matchWrap();pt.setMargins(0,Ui.dp(this,6),0,0);pro.addView(proText,pt);root.addView(pro);

        TextView dlTitle=Ui.sectionTitle(this,"تحميل الوسائط");LinearLayout.LayoutParams dlt=Ui.matchWrap();dlt.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,8));root.addView(dlTitle,dlt);
        LinearLayout downloader=Ui.card(this);downloader.addView(Ui.text(this,"ألصق رابط فيديو أو صوت",16,Ui.TEXT,true));downloader.addView(Ui.text(this,BuildConfig.YTDLP_ENABLED?"المحرك السريع + yt-dlp متاحان.":"البحث وyt-dlp متوقفان مؤقتًا لتخفيف APK الاختبار.",11,Ui.MUTED,false));smartUrl=Ui.input(this,"https://…",true);smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);LinearLayout.LayoutParams su=Ui.matchWrap();su.height=Ui.dp(this,78);su.setMargins(0,Ui.dp(this,10),0,0);downloader.addView(smartUrl,su);
        LinearLayout ar=new LinearLayout(this);ar.setOrientation(LinearLayout.HORIZONTAL);Button paste=Ui.secondary(this,"لصق");paste.setOnClickListener(v->pasteLink());ar.addView(paste,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button clear=Ui.ghost(this,"مسح");clear.setOnClickListener(v->{smartUrl.setText("");setStatus("جاهز");});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);cp.setMargins(Ui.dp(this,8),0,0,0);ar.addView(clear,cp);LinearLayout.LayoutParams arp=Ui.matchWrap();arp.setMargins(0,Ui.dp(this,8),0,0);downloader.addView(ar,arp);
        downloadButton=Ui.accent(this,"↓  تجهيز خيارات التحميل");downloadButton.setOnClickListener(v->smartProcess());LinearLayout.LayoutParams db=Ui.matchWrap();db.height=Ui.dp(this,56);db.setMargins(0,Ui.dp(this,8),0,0);downloader.addView(downloadButton,db);status=Ui.text(this,"جاهز",11,Ui.GREEN,true);status.setGravity(Gravity.CENTER);LinearLayout.LayoutParams st=Ui.matchWrap();st.setMargins(0,Ui.dp(this,8),0,0);downloader.addView(status,st);root.addView(downloader);

        TextView quick=Ui.sectionTitle(this,"الوصول السريع");LinearLayout.LayoutParams q=Ui.matchWrap();q.setMargins(0,Ui.dp(this,20),0,Ui.dp(this,8));root.addView(quick,q);root.addView(quickRow("↓ التحميلات",v->startActivity(new Intent(this,DownloadsActivity.class)),"▣ ملفاتك",v->startActivity(new Intent(this,LibraryActivity.class))));LinearLayout.LayoutParams q2=Ui.matchWrap();q2.setMargins(0,Ui.dp(this,8),0,0);root.addView(quickRow("★ السجل",v->startActivity(new Intent(this,HistoryActivity.class)),"⚙ الإعدادات",v->startActivity(new Intent(this,SettingsActivity.class))),q2);

        TextView footer=Ui.text(this,"Download Hub Studio • Developed by AboAdam",10,Ui.MUTED_2,true);footer.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=Ui.matchWrap();fp.setMargins(0,Ui.dp(this,22),0,0);root.addView(footer,fp);
        page.addView(bottomNav(),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,70)));return page;
    }

    private LinearLayout featureRow(String i1,String t1,String s1,View.OnClickListener l1,String i2,String t2,String s2,View.OnClickListener l2){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.addView(featureCard(i1,t1,s1,l1),new LinearLayout.LayoutParams(0,Ui.dp(this,128),1f));LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,Ui.dp(this,128),1f);b.setMargins(Ui.dp(this,8),0,0,0);row.addView(featureCard(i2,t2,s2,l2),b);return row;}
    private View featureCard(String icon,String title,String sub,View.OnClickListener l){LinearLayout c=Ui.card(this);c.setGravity(Gravity.CENTER);c.setOnClickListener(l);TextView ic=Ui.text(this,icon,24,Ui.CYAN,true);ic.setGravity(Gravity.CENTER);c.addView(ic);TextView t=Ui.text(this,title,14,Ui.TEXT,true);t.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,5),0,0);c.addView(t,tp);TextView s=Ui.text(this,sub,9,Ui.MUTED,false);s.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,4),0,0);c.addView(s,sp);return c;}

    private View bottomNav(){LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,8));nav.setBackground(Ui.bordered(Ui.SURFACE,Ui.BORDER_SOFT,1,0,this));nav.addView(navButton("⌂\nالرئيسية",v->{}),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));nav.addView(navButton("✂\nمونتاج",v->openBlankEditor()),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));nav.addView(navButton("▦\nقوالب",v->startActivity(new Intent(this,TemplateLibraryActivity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));nav.addView(navButton("☾\nإسلامي",v->startActivity(new Intent(this,IslamicReelsActivity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));nav.addView(navButton("☰\nالمزيد",v->AppSideMenu.show(this)),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));return nav;}
    private Button navButton(String s,View.OnClickListener l){Button b=Ui.ghost(this,s);b.setTextSize(10);b.setOnClickListener(l);return b;}

    private LinearLayout quickRow(String a,View.OnClickListener al,String b,View.OnClickListener bl){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button x=Ui.secondary(this,a);x.setOnClickListener(al);row.addView(x,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f));Button y=Ui.secondary(this,b);y.setOnClickListener(bl);LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1f);yp.setMargins(Ui.dp(this,8),0,0,0);row.addView(y,yp);return row;}
    private void openBlankEditor(){Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("template_id","blank");i.putExtra("pick_media",true);startActivity(i);}

    private void pasteLink(){String u=clipboardUrl();if(isHttp(u)){smartUrl.setText(u);setStatus("الرابط جاهز للتحميل");}else toast("الحافظة لا تحتوي على رابط صحيح");}
    private void consumeIntent(Intent intent){if(intent==null||handledIntent||smartUrl==null)return;handledIntent=true;String u=intent.getStringExtra(EXTRA_PREFILL_URL);if(u==null&&Intent.ACTION_SEND.equals(intent.getAction())){CharSequence t=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(t!=null)u=extractFirstUrl(t.toString());}if(u==null&&intent.getData()!=null)u=intent.getData().toString();if(isHttp(u)){smartUrl.setText(u);setStatus("تم استلام الرابط — اضغط تجهيز خيارات التحميل");}}

    private void smartProcess(){String raw=value(smartUrl);if(!isHttp(raw)){toast("ضع رابط HTTP/HTTPS صحيح");return;}String url=AppPrefs.linkCleaner(this)?LinkTools.clean(raw):raw;if(!url.equals(raw))smartUrl.setText(url);HistoryStore.add(this,url,detectKind(url));setBusy(true);setStatus("جاري تحليل الرابط…");executor.execute(()->{Throwable first=null;try{PlatformExtractor.MediaBundle b=PlatformExtractor.extractOptions(url);runOnUiThread(()->{setBusy(false);presentBundle(b,"المحرك السريع");});return;}catch(Throwable e){first=e;}if(BuildConfig.YTDLP_ENABLED){try{PlatformExtractor.MediaBundle b=YtDlpResolver.extractOptions(this,url);runOnUiThread(()->{setBusy(false);presentBundle(b,"yt-dlp");});return;}catch(Throwable second){String m=safeMessage(second);runOnUiThread(()->{setBusy(false);setStatus(m);});return;}}String m=safeMessage(first);runOnUiThread(()->{setBusy(false);setStatus("تعذر تجهيز الرابط بالمحرك الخفيف. "+m);});});}
    private void presentBundle(PlatformExtractor.MediaBundle b,String engine){List<MediaOption> opts=b.options;if(opts==null||opts.isEmpty()){setStatus("لم يتم العثور على فيديو أو صوت قابل للتحميل");return;}setStatus(b.platform+" • "+opts.size()+" خيارات • "+engine);MediaOptionsDialog.show(this,b.title,opts,this::startDownload);}
    private void startDownload(MediaOption o){try{long id=DownloadEngine.enqueue(this,o);setStatus("تمت إضافة التحميل ✓  "+o.label);toast("بدأ التحميل #"+id);}catch(Exception e){String m=safeMessage(e);setStatus(m);toast(m);}}
    private void setBusy(boolean busy){if(downloadButton==null)return;downloadButton.setEnabled(!busy);downloadButton.setAlpha(busy?.55f:1f);downloadButton.setText(busy?"جاري التجهيز…":"↓  تجهيز خيارات التحميل");}
    private void setStatus(String s){if(status!=null)status.setText(s==null?"":s);}
    private String value(EditText e){return e==null||e.getText()==null?"":e.getText().toString().trim();}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}
    private String clipboardUrl(){try{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm==null||!cm.hasPrimaryClip())return"";ClipData c=cm.getPrimaryClip();if(c==null||c.getItemCount()==0)return"";CharSequence t=c.getItemAt(0).coerceToText(this);return t==null?"":extractFirstUrl(t.toString());}catch(Exception e){return"";}}
    private String extractFirstUrl(String text){if(text==null)return"";Matcher m=Pattern.compile("https?://\\S+").matcher(text);if(!m.find())return"";String u=m.group();while(u.endsWith(")")||u.endsWith("]")||u.endsWith("}")||u.endsWith(",")||u.endsWith("."))u=u.substring(0,u.length()-1);return u;}
    private String detectKind(String url){try{String h=new URI(url).getHost();return PlatformExtractor.platformName(h);}catch(Exception e){return"Link";}}
    private String safeMessage(Throwable e){if(e==null)return"";String s=e.getMessage();if(TextUtils.isEmpty(s))s=e.getClass().getSimpleName();s=s.replace('\n',' ').trim();return s.length()>160?s.substring(0,160)+"…":s;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
