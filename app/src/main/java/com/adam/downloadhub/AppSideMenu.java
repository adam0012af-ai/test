package com.adam.downloadhub;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AppSideMenu {
    private AppSideMenu() {}

    public static void show(Activity a) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(a,18),Ui.dp(a,22),Ui.dp(a,18),Ui.dp(a,28));
        root.setBackground(Ui.gradient(0xFF07111E,0xFF0D1A2D,0,a));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView brand=Ui.text(a,"Download Hub Studio",23,Ui.TEXT,true);root.addView(brand);
        TextView sub=Ui.text(a,"MEDIA • EDIT • TEMPLATES • PUBLISH",10,Ui.CYAN,true);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(a,3),0,Ui.dp(a,16));root.addView(sub,sp);

        add(root,a,d,"⌂  الرئيسية",MainActivity.class);
        root.addView(section(a,"إنشاء"));
        addNewProject(root,a,d,"✂  مشروع مونتاج جديد");
        add(root,a,d,"▦  القوالب الجاهزة",TemplateLibraryActivity.class);
        add(root,a,d,"☾  صانع الريلز الإسلامي",IslamicReelsActivity.class);
        add(root,a,d,"✦  Creator Studio",CreatorStudioActivity.class);

        root.addView(section(a,"الميديا"));
        add(root,a,d,"↓  التحميلات",DownloadsActivity.class);
        add(root,a,d,"▣  مكتبة الملفات",LibraryActivity.class);
        add(root,a,d,"★  السجل",HistoryActivity.class);
        add(root,a,d,"☷  Batch",BatchActivity.class);

        root.addView(section(a,"التطبيق"));
        add(root,a,d,"▥  الإحصائيات",StatsActivity.class);
        add(root,a,d,"⚙  الإعدادات",SettingsActivity.class);

        TextView note=Ui.text(a,"TikTok: التصدير يجهّز الفيديو ويفتح تطبيق TikTok. الربط الرسمي بالحساب يحتاج Client Key + Backend.",10,Ui.MUTED,false);note.setLineSpacing(0,1.2f);LinearLayout.LayoutParams np=Ui.matchWrap();np.setMargins(0,Ui.dp(a,18),0,0);root.addView(note,np);

        d.setContentView(scroll);
        Window w=d.getWindow();
        if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));WindowManager.LayoutParams p=new WindowManager.LayoutParams();p.copyFrom(w.getAttributes());p.width=(int)(a.getResources().getDisplayMetrics().widthPixels*.86f);p.height=WindowManager.LayoutParams.MATCH_PARENT;p.gravity=Gravity.START;w.setAttributes(p);w.setDimAmount(.58f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
        d.show();
    }

    private static TextView section(Activity a,String s){TextView t=Ui.text(a,s,11,Ui.MUTED_2,true);LinearLayout.LayoutParams p=Ui.matchWrap();p.setMargins(0,Ui.dp(a,16),0,Ui.dp(a,6));t.setLayoutParams(p);return t;}

    private static void add(LinearLayout root,Activity a,Dialog d,String label,Class<?> target){Button b=Ui.ghost(a,label);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setOnClickListener(v->{d.dismiss();if(!target.isInstance(a)){Intent i=new Intent(a,target);if(target==MainActivity.class)i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);a.startActivity(i);}});LinearLayout.LayoutParams p=Ui.matchWrap();p.height=Ui.dp(a,52);p.setMargins(0,0,0,Ui.dp(a,4));root.addView(b,p);}

    private static void addNewProject(LinearLayout root,Activity a,Dialog d,String label){Button b=Ui.accent(a,label);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setOnClickListener(v->{d.dismiss();Intent i=new Intent(a,TemplateEditorActivity.class);i.putExtra("template_id","blank");i.putExtra("pick_media",true);a.startActivity(i);});LinearLayout.LayoutParams p=Ui.matchWrap();p.height=Ui.dp(a,54);p.setMargins(0,0,0,Ui.dp(a,6));root.addView(b,p);}
}
