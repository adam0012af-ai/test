package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
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
        ScrollView scroll=new ScrollView(this);scroll.setBackground(Ui.gradient(Ui.BG,Ui.SURFACE,0,this));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(Ui.dp(this,16),Ui.dp(this,22),Ui.dp(this,16),Ui.dp(this,30));scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=Ui.text(this,"إعدادات Download Hub",26,Ui.TEXT,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=Ui.text(this,"الجودة • الشبكة • الحماية • الخصوصية",13,Ui.MUTED,false);sub.setGravity(Gravity.CENTER_HORIZONTAL);LinearLayout.LayoutParams spl=Ui.matchWrap();spl.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,18));root.addView(sub,spl);

        LinearLayout profile=Ui.card(this);profile.addView(Ui.text(this,"Download Profile",17,Ui.TEXT,true));
        TextView current=Ui.text(this,"الحالي: "+profileName(AppPrefs.downloadProfile(this)),12,Ui.CYAN,true);LinearLayout.LayoutParams cpl=Ui.matchWrap();cpl.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,10));profile.addView(current,cpl);
        TextView pinfo=Ui.text(this,"يحدد الجودة التي يتم تعليمها افتراضيًا. لن يبدأ التحميل إلا بعد ضغط «تحميل الآن».",12,Ui.MUTED,false);LinearLayout.LayoutParams pi=Ui.matchWrap();pi.setMargins(0,0,0,Ui.dp(this,10));profile.addView(pinfo,pi);
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.addView(profileButton("أقصى جودة","max",current),new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f));
        LinearLayout.LayoutParams m2=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);m2.setMargins(Ui.dp(this,7),0,0,0);modes.addView(profileButton("متوازن","balanced",current),m2);
        LinearLayout.LayoutParams m3=new LinearLayout.LayoutParams(0,Ui.dp(this,46),1f);m3.setMargins(Ui.dp(this,7),0,0,0);modes.addView(profileButton("توفير بيانات","saver",current),m3);profile.addView(modes);root.addView(profile);

        LinearLayout downloads=Ui.card(this);LinearLayout.LayoutParams dp=Ui.matchWrap();dp.setMargins(0,Ui.dp(this,14),0,0);downloads.setLayoutParams(dp);downloads.addView(Ui.text(this,"التحميل والحماية",17,Ui.TEXT,true));
        downloads.addView(toggle("Wi‑Fi فقط","بدء التحميلات الجديدة عند توفر Wi‑Fi",AppPrefs.wifiOnly(this),(b,v)->AppPrefs.setWifiOnly(this,v)));
        downloads.addView(toggle("Link Cleaner","تنظيف روابط التتبع قبل تجهيز التحميل",AppPrefs.linkCleaner(this),(b,v)->AppPrefs.setLinkCleaner(this,v)));
        downloads.addView(toggle("Duplicate Shield","تحذير ومنع إضافة نفس الملف مرة أخرى خلال فترة قصيرة",AppPrefs.duplicateShield(this),(b,v)->AppPrefs.setDuplicateShield(this,v)));
        downloads.addView(toggle("اقتراح رابط الحافظة","التقاط آخر رابط نسخته عند فتح التطبيق",AppPrefs.autoClipboard(this),(b,v)->AppPrefs.setAutoClipboard(this,v)));
        downloads.addView(toggle("تنظيم المجلدات","Downloads/DownloadHub/المنصة/Video أو Audio",AppPrefs.organizeFolders(this),(b,v)->AppPrefs.setOrganizeFolders(this,v)));
        downloads.addView(toggle("إعادة المحاولة","إظهار Retry للتحميلات الفاشلة",AppPrefs.autoRetry(this),(b,v)->AppPrefs.setAutoRetry(this,v)));root.addView(downloads);

        LinearLayout privacy=Ui.card(this);LinearLayout.LayoutParams pp=Ui.matchWrap();pp.setMargins(0,Ui.dp(this,14),0,0);privacy.setLayoutParams(pp);privacy.addView(Ui.text(this,"الخصوصية",17,Ui.TEXT,true));
        privacy.addView(toggle("حفظ سجل الروابط","حفظ الروابط والمفضلة داخل التطبيق",AppPrefs.saveHistory(this),(b,v)->AppPrefs.setSaveHistory(this,v)));
        privacy.addView(toggle("Private Mode","عدم إضافة روابط جديدة إلى السجل",AppPrefs.privateMode(this),(b,v)->AppPrefs.setPrivateMode(this,v)));root.addView(privacy);

        LinearLayout manage=Ui.card(this);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,14),0,0);manage.setLayoutParams(mp);manage.addView(Ui.text(this,"إدارة التطبيق",17,Ui.TEXT,true));
        Button sys=Ui.secondary(this,"إعدادات النظام");sys.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception e){toast("تعذر فتح الإعدادات");}});LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,50);bp.setMargins(0,Ui.dp(this,12),0,0);manage.addView(sys,bp);
        Button clear=Ui.secondary(this,"مسح سجل الروابط");clear.setOnClickListener(v->{HistoryStore.clear(this);toast("تم مسح السجل");});LinearLayout.LayoutParams ch=Ui.matchWrap();ch.height=Ui.dp(this,50);ch.setMargins(0,Ui.dp(this,8),0,0);manage.addView(clear,ch);root.addView(manage);

        LinearLayout about=Ui.card(this);LinearLayout.LayoutParams ap=Ui.matchWrap();ap.setMargins(0,Ui.dp(this,14),0,0);about.setLayoutParams(ap);
        about.addView(Ui.text(this,"Download Hub v5 Premium",18,Ui.CYAN,true));
        TextView dev=Ui.text(this,"Developer: AboAdam",14,Ui.TEXT,true);LinearLayout.LayoutParams dv=Ui.matchWrap();dv.setMargins(0,Ui.dp(this,7),0,0);about.addView(dev,dv);
        TextView info=Ui.text(this,"Video & Audio • Quality Selector • Batch • Stats • History • Favorites • Link Cleaner • Duplicate Shield • Storage Guard • Smart Download Profiles",13,Ui.MUTED,false);info.setLineSpacing(0,1.25f);LinearLayout.LayoutParams ip=Ui.matchWrap();ip.setMargins(0,Ui.dp(this,8),0,0);about.addView(info,ip);root.addView(about);
        return scroll;
    }

    private Button profileButton(String label,String mode,TextView current){Button b=Ui.secondary(this,label);b.setTextSize(11);b.setOnClickListener(v->{AppPrefs.setDownloadProfile(this,mode);current.setText("الحالي: "+profileName(mode));toast("تم اختيار "+label);});return b;}
    private String profileName(String m){if("max".equals(m))return"أقصى جودة";if("saver".equals(m))return"توفير بيانات";return"متوازن";}
    private View toggle(String title,String sub,boolean checked,CompoundButton.OnCheckedChangeListener listener){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,4));LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.addView(Ui.text(this,title,14,Ui.TEXT,true));texts.addView(Ui.text(this,sub,12,Ui.MUTED,false));row.addView(texts,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Switch s=new Switch(this);s.setChecked(checked);s.setOnCheckedChangeListener(listener);row.addView(s);return row;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
