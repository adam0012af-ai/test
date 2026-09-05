package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);setContentView(buildUi());}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,34));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.topBar(this,"الإعدادات","Download • Creator • Privacy • Storage",v->finish()));

        LinearLayout identity=Ui.card(this);LinearLayout.LayoutParams ip=Ui.matchWrap();ip.setMargins(0,Ui.dp(this,14),0,0);identity.setLayoutParams(ip);identity.setBackground(Ui.gradient3(0xFF0A4EA3,0xFF12386D,0xFF0A1B32,24,this));identity.addView(Ui.text(this,"Download Hub v6.0",21,Ui.TEXT,true));identity.addView(Ui.text(this,"Premium Media & Creator Center",12,0xFFC8E3FF,false));TextView dev=Ui.chip(this,"Developed by AboAdam",Ui.CYAN);LinearLayout.LayoutParams dv=Ui.matchWrap();dv.setMargins(0,Ui.dp(this,10),0,0);identity.addView(dev,dv);root.addView(identity);

        root.addView(section("الجودة والتحميل","إعدادات التحميل الفعلية"),marginTop(14));
        LinearLayout profile=Ui.card(this);TextView current=Ui.text(this,"الجودة الافتراضية: "+profileName(AppPrefs.downloadProfile(this)),13,Ui.CYAN,true);profile.addView(current);LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,10),0,0);modes.addView(profileButton("أقصى","max",current),new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));LinearLayout.LayoutParams m2=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);m2.setMargins(Ui.dp(this,7),0,0,0);modes.addView(profileButton("متوازن","balanced",current),m2);LinearLayout.LayoutParams m3=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);m3.setMargins(Ui.dp(this,7),0,0,0);modes.addView(profileButton("توفير","saver",current),m3);profile.addView(modes,mp);root.addView(profile);

        LinearLayout download=Ui.card(this);LinearLayout.LayoutParams dl=Ui.matchWrap();dl.setMargins(0,Ui.dp(this,9),0,0);download.setLayoutParams(dl);download.addView(toggle("Wi‑Fi فقط","لا تبدأ التحميلات الجديدة على بيانات الهاتف",AppPrefs.wifiOnly(this),(b,v)->AppPrefs.setWifiOnly(this,v)));download.addView(toggle("Link Cleaner","ينظف روابط التتبع قبل التحليل",AppPrefs.linkCleaner(this),(b,v)->AppPrefs.setLinkCleaner(this,v)));download.addView(toggle("Duplicate Shield","يمنع تنزيل نفس الملف بالخطأ",AppPrefs.duplicateShield(this),(b,v)->AppPrefs.setDuplicateShield(this,v)));download.addView(toggle("اقتراح رابط الحافظة","يجهز آخر رابط منسوخ عند فتح التطبيق",AppPrefs.autoClipboard(this),(b,v)->AppPrefs.setAutoClipboard(this,v)));download.addView(toggle("تنظيم المجلدات","يرتب التحميلات حسب المصدر والنوع",AppPrefs.organizeFolders(this),(b,v)->AppPrefs.setOrganizeFolders(this,v)));download.addView(toggle("إعادة المحاولة","يتيح إعادة تشغيل التحميل الفاشل",AppPrefs.autoRetry(this),(b,v)->AppPrefs.setAutoRetry(this,v)));root.addView(download);

        root.addView(section("Creator Studio","إعدادات التصدير والنشر"),marginTop(18));
        LinearLayout creator=Ui.card(this);TextView quality=Ui.text(this,"جودة التصدير: "+AppPrefs.exportQuality(this)+"p",13,Ui.CYAN,true);creator.addView(quality);LinearLayout qrow=new LinearLayout(this);qrow.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams qr=Ui.matchWrap();qr.setMargins(0,Ui.dp(this,9),0,Ui.dp(this,6));Button q720=Ui.secondary(this,"720p • أسرع");q720.setOnClickListener(v->{AppPrefs.setExportQuality(this,720);quality.setText("جودة التصدير: 720p");});qrow.addView(q720,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));Button q1080=Ui.primary(this,"1080p • أفضل");q1080.setOnClickListener(v->{AppPrefs.setExportQuality(this,1080);quality.setText("جودة التصدير: 1080p");});LinearLayout.LayoutParams q2=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);q2.setMargins(Ui.dp(this,7),0,0,0);qrow.addView(q1080,q2);creator.addView(qrow,qr);creator.addView(toggle("عنوان وهاشتاج تلقائي","يولّد محتوى النشر المناسب حسب نوع الريل والمنصة",AppPrefs.autoPublishText(this),(b,v)->AppPrefs.setAutoPublishText(this,v)));creator.addView(toggle("علامة Download Hub","إظهار توقيع صغير أسفل الفيديو المُصدّر",AppPrefs.creatorWatermark(this),(b,v)->AppPrefs.setCreatorWatermark(this,v)));root.addView(creator);

        root.addView(section("الخصوصية","كل هذه البيانات محفوظة محليًا"),marginTop(18));
        LinearLayout privacy=Ui.card(this);privacy.addView(toggle("حفظ سجل الروابط","السجل والمفضلة داخل التطبيق",AppPrefs.saveHistory(this),(b,v)->AppPrefs.setSaveHistory(this,v)));privacy.addView(toggle("Private Mode","لا يحفظ الروابط الجديدة في السجل",AppPrefs.privateMode(this),(b,v)->AppPrefs.setPrivateMode(this,v)));root.addView(privacy);

        root.addView(section("الإدارة","مكتبتك ومساحة التطبيق"),marginTop(18));
        LinearLayout manage=Ui.card(this);Button lib=Ui.secondary(this,"▦  فتح مكتبتي");lib.setOnClickListener(v->startActivity(new Intent(this,LibraryActivity.class)));manage.addView(lib,height(50));Button downloads=Ui.secondary(this,"↓  إدارة التحميلات");downloads.setOnClickListener(v->startActivity(new Intent(this,DownloadsActivity.class)));LinearLayout.LayoutParams dlp=height(50);dlp.setMargins(0,Ui.dp(this,8),0,0);manage.addView(downloads,dlp);Button clear=Ui.secondary(this,"مسح سجل الروابط");clear.setOnClickListener(v->{HistoryStore.clear(this);toast("تم مسح السجل");});LinearLayout.LayoutParams ch=height(50);ch.setMargins(0,Ui.dp(this,8),0,0);manage.addView(clear,ch);Button system=Ui.ghost(this,"إعدادات التطبيق في Android");system.setOnClickListener(v->{try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){toast("تعذر فتح إعدادات النظام");}});LinearLayout.LayoutParams sy=height(50);sy.setMargins(0,Ui.dp(this,8),0,0);manage.addView(system,sy);root.addView(manage);

        TextView foot=Ui.text(this,"Download Hub v6.0 • AboAdam",11,Ui.MUTED_2,true);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ft=Ui.matchWrap();ft.setMargins(0,Ui.dp(this,24),0,0);root.addView(foot,ft);return scroll;
    }

    private View section(String title,String sub){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.addView(Ui.sectionTitle(this,title));l.addView(Ui.text(this,sub,11,Ui.MUTED,false));return l;}
    private LinearLayout.LayoutParams marginTop(int n){LinearLayout.LayoutParams p=Ui.matchWrap();p.setMargins(0,Ui.dp(this,n),0,Ui.dp(this,8));return p;}
    private LinearLayout.LayoutParams height(int h){return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,h));}
    private Button profileButton(String label,String mode,TextView current){Button b=Ui.secondary(this,label);b.setOnClickListener(v->{AppPrefs.setDownloadProfile(this,mode);current.setText("الجودة الافتراضية: "+profileName(mode));});return b;}
    private String profileName(String m){if("max".equals(m))return"أقصى جودة";if("saver".equals(m))return"توفير بيانات";return"متوازن";}
    private View toggle(String title,String sub,boolean checked,CompoundButton.OnCheckedChangeListener listener){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,11),0,Ui.dp(this,5));LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.addView(Ui.text(this,title,14,Ui.TEXT,true));TextView s=Ui.text(this,sub,11,Ui.MUTED,false);s.setMaxLines(2);texts.addView(s);row.addView(texts,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Switch sw=new Switch(this);sw.setChecked(checked);sw.setOnCheckedChangeListener(listener);row.addView(sw);return row;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
