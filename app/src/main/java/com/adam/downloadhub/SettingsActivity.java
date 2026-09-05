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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setBackground(Ui.gradient(Ui.BG,Ui.SURFACE,0,this));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,22),Ui.dp(this,16),Ui.dp(this,30));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=Ui.text(this,"الإعدادات",27,Ui.TEXT,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=Ui.text(this,"تحكم في سلوك التحميل والاستخراج",13,Ui.MUTED,false);sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams spl=Ui.matchWrap();spl.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,18));root.addView(sub,spl);

        LinearLayout downloads=Ui.card(this); downloads.addView(Ui.text(this,"إعدادات التحميل",17,Ui.TEXT,true));
        downloads.addView(toggle("Wi‑Fi فقط", "منع بدء تحميلات جديدة على بيانات الهاتف", AppPrefs.wifiOnly(this), (b,v)->AppPrefs.setWifiOnly(this,v)));
        downloads.addView(toggle("الانتقال للمتصفح تلقائيًا", "إذا فشل الاستخراج المباشر افتح متصفح الالتقاط", AppPrefs.autoBrowser(this), (b,v)->AppPrefs.setAutoBrowser(this,v)));
        downloads.addView(toggle("اقتراح رابط الحافظة", "يظهر الرابط المنسوخ في الصفحة الرئيسية", AppPrefs.autoClipboard(this), (b,v)->AppPrefs.setAutoClipboard(this,v)));
        downloads.addView(toggle("حفظ سجل الروابط", "يمكن تعطيله للخصوصية", AppPrefs.saveHistory(this), (b,v)->AppPrefs.setSaveHistory(this,v)));
        root.addView(downloads);

        LinearLayout manage=Ui.card(this); LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,14),0,0);manage.setLayoutParams(mp);
        manage.addView(Ui.text(this,"إدارة التطبيق",17,Ui.TEXT,true));
        Button downloadSettings=Ui.secondary(this,"إعدادات التنزيل في النظام");
        downloadSettings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception e){Toast.makeText(this,"تعذر فتح الإعدادات",Toast.LENGTH_SHORT).show();}});
        LinearLayout.LayoutParams bp=Ui.matchWrap();bp.height=Ui.dp(this,50);bp.setMargins(0,Ui.dp(this,12),0,0);manage.addView(downloadSettings,bp);
        Button clearHistory=Ui.secondary(this,"مسح سجل الروابط"); clearHistory.setOnClickListener(v->{HistoryStore.clear(this);Toast.makeText(this,"تم مسح السجل",Toast.LENGTH_SHORT).show();});
        LinearLayout.LayoutParams ch=Ui.matchWrap();ch.height=Ui.dp(this,50);ch.setMargins(0,Ui.dp(this,8),0,0);manage.addView(clearHistory,ch);
        root.addView(manage);

        LinearLayout about=Ui.card(this); LinearLayout.LayoutParams ap=Ui.matchWrap();ap.setMargins(0,Ui.dp(this,14),0,0);about.setLayoutParams(ap);
        about.addView(Ui.text(this,"Download Hub Premium",18,Ui.CYAN,true));
        TextView info=Ui.text(this,"الإصدار 2.0.0\nSmart Link • Download Manager • Browser Capture • History • Favorites\n\nالتطبيق يتعامل مع المحتوى العام والروابط التي لديك صلاحية تنزيلها، ولا يتجاوز DRM أو حماية الحسابات.",13,Ui.MUTED,false);
        info.setLineSpacing(0,1.25f); LinearLayout.LayoutParams ip=Ui.matchWrap();ip.setMargins(0,Ui.dp(this,8),0,0);about.addView(info,ip);
        root.addView(about);
        return scroll;
    }

    private View toggle(String title,String sub,boolean checked,CompoundButton.OnCheckedChangeListener listener){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,4));
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this,title,14,Ui.TEXT,true));texts.addView(Ui.text(this,sub,12,Ui.MUTED,false));
        row.addView(texts,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Switch s=new Switch(this);s.setChecked(checked);s.setOnCheckedChangeListener(listener);row.addView(s);
        return row;
    }
}
