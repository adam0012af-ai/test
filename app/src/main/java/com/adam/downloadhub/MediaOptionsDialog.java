package com.adam.downloadhub;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MediaOptionsDialog {
    private MediaOptionsDialog() {}

    public interface Callback { void onSelected(MediaOption option); }

    public static void show(Activity activity, String title, List<MediaOption> options, Callback callback) {
        if (activity == null || activity.isFinishing()) return;
        if (options == null || options.isEmpty()) {
            showEmpty(activity);
            return;
        }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        shell.setPadding(Ui.dp(activity,18), Ui.dp(activity,16), Ui.dp(activity,18), Ui.dp(activity,16));
        shell.setBackground(Ui.bordered(Color.rgb(10,17,29), Color.rgb(42,65,94),1,26,activity));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = Ui.text(activity,"↓",25,Color.WHITE,true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.gradient(Ui.BLUE,Color.rgb(34,91,224),16,activity));
        header.addView(icon,new LinearLayout.LayoutParams(Ui.dp(activity,50),Ui.dp(activity,50)));

        LinearLayout hText = new LinearLayout(activity);
        hText.setOrientation(LinearLayout.VERTICAL);
        TextView heading = Ui.text(activity,"اختر طريقة التحميل",20,Ui.TEXT,true);
        TextView sub = Ui.text(activity,options.get(0).platform+"  •  "+options.size()+" خيارات متاحة",12,Ui.CYAN,true);
        hText.addView(heading); hText.addView(sub);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        htp.setMargins(Ui.dp(activity,12),0,0,0); header.addView(hText,htp);
        shell.addView(header);

        String cleanTitle = title == null ? "" : title.replaceAll("\\s+"," ").trim();
        if (!cleanTitle.isEmpty()) {
            TextView mediaTitle = Ui.text(activity,cleanTitle,15,Ui.TEXT,true);
            mediaTitle.setMaxLines(2);
            mediaTitle.setEllipsize(TextUtils.TruncateAt.END);
            mediaTitle.setLineSpacing(0,1.12f);
            LinearLayout.LayoutParams mtp = Ui.matchWrap();
            mtp.setMargins(0,Ui.dp(activity,15),0,Ui.dp(activity,12));
            shell.addView(mediaTitle,mtp);
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(list,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        List<MediaOption> video = new ArrayList<>();
        List<MediaOption> audio = new ArrayList<>();
        for (MediaOption o: options) { if (o.audioOnly) audio.add(o); else video.add(o); }

        if (!video.isEmpty()) {
            list.addView(section(activity,"الفيديو",video.size()+" جودة متاحة"));
            for (MediaOption o:video) list.addView(optionRow(activity,dialog,o,callback,false));
        }
        if (!audio.isEmpty()) {
            TextView sec = section(activity,"الصوت فقط",audio.size()+" خيار متاح");
            LinearLayout.LayoutParams sp = Ui.matchWrap(); sp.setMargins(0,Ui.dp(activity,12),0,0); sec.setLayoutParams(sp);
            list.addView(sec);
            for (MediaOption o:audio) list.addView(optionRow(activity,dialog,o,callback,true));
        }

        int maxH = (int)(activity.getResources().getDisplayMetrics().heightPixels * 0.60f);
        shell.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,maxH));

        Button cancel = Ui.secondary(activity,"إلغاء");
        cancel.setOnClickListener(v->dialog.dismiss());
        LinearLayout.LayoutParams cp = Ui.matchWrap(); cp.height=Ui.dp(activity,48); cp.setMargins(0,Ui.dp(activity,12),0,0);
        shell.addView(cancel,cp);

        dialog.setContentView(shell);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            lp.dimAmount = 0.72f;
            w.setAttributes(lp);
        }
        dialog.show();
    }

    private static TextView section(Activity a,String title,String sub) {
        TextView t = Ui.text(a,title+"   •   "+sub,13,Ui.MUTED,true);
        t.setPadding(Ui.dp(a,2),Ui.dp(a,8),Ui.dp(a,2),Ui.dp(a,8));
        return t;
    }

    private static View optionRow(Activity a, Dialog d, MediaOption o, Callback cb, boolean audio) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(a,12),Ui.dp(a,12),Ui.dp(a,12),Ui.dp(a,12));
        row.setBackground(Ui.bordered(Color.rgb(16,26,42),Color.rgb(41,65,92),1,18,a));

        TextView badge = Ui.text(a,audio?"♪":qualityBadge(o.label),14,audio?Ui.YELLOW:Ui.CYAN,true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.bordered(Color.rgb(9,18,31),audio?Color.rgb(111,87,37):Color.rgb(20,91,121),1,13,a));
        row.addView(badge,new LinearLayout.LayoutParams(Ui.dp(a,58),Ui.dp(a,46)));

        LinearLayout text = new LinearLayout(a); text.setOrientation(LinearLayout.VERTICAL);
        String main = audio ? "الصوت فقط" : cleanLabel(o.label);
        TextView name = Ui.text(a,main,15,Ui.TEXT,true); name.setMaxLines(1); name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        String meta = format(o)+"  •  "+(o.sizeBytes>0?MediaOption.formatBytes(o.sizeBytes):"حجم غير معروف")+(audio?"":"  •  فيديو + صوت");
        TextView detail = Ui.text(a,meta,11,Ui.MUTED,false); detail.setMaxLines(1); detail.setEllipsize(TextUtils.TruncateAt.END); text.addView(detail);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); tp.setMargins(Ui.dp(a,10),0,Ui.dp(a,8),0); row.addView(text,tp);

        TextView action = Ui.text(a,audio?"تحميل الصوت":"تحميل",12,Color.WHITE,true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(Ui.gradient(Ui.BLUE,Color.rgb(35,86,226),13,a));
        row.addView(action,new LinearLayout.LayoutParams(Ui.dp(a,audio?88:70),Ui.dp(a,42)));

        View.OnClickListener click = v->{ d.dismiss(); if(cb!=null) cb.onSelected(o); };
        row.setOnClickListener(click); action.setOnClickListener(click);
        LinearLayout.LayoutParams rp = Ui.matchWrap(); rp.setMargins(0,0,0,Ui.dp(a,8)); row.setLayoutParams(rp);
        return row;
    }

    private static String qualityBadge(String label) {
        String l = label == null ? "" : label.toUpperCase(Locale.ROOT);
        if(l.contains("2160")||l.contains("4K")) return "4K";
        if(l.contains("1440")||l.contains("2K")) return "2K";
        if(l.contains("1080")) return "1080";
        if(l.contains("720")||l.contains("HD")) return "HD";
        if(l.contains("480")||l.contains("SD")) return "SD";
        if(l.contains("360")) return "360";
        return "VIDEO";
    }

    private static String cleanLabel(String label) {
        if(label==null||label.trim().isEmpty()) return "فيديو";
        return label.replace("•"," · ").replaceAll("\\s+"," ").trim();
    }

    private static String format(MediaOption o) {
        String n=o.fileName==null?"":o.fileName.toLowerCase(Locale.ROOT);
        int dot=n.lastIndexOf('.');
        if(dot>=0&&dot+1<n.length()) return n.substring(dot+1).toUpperCase(Locale.ROOT);
        String t=o.contentType==null?"":o.contentType;
        int slash=t.indexOf('/'); if(slash>=0)t=t.substring(slash+1);
        int semi=t.indexOf(';'); if(semi>0)t=t.substring(0,semi);
        return t.isEmpty()?"MEDIA":t.toUpperCase(Locale.ROOT);
    }

    private static void showEmpty(Activity a) {
        final Dialog d=new Dialog(a); d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=Ui.card(a); box.setPadding(Ui.dp(a,20),Ui.dp(a,20),Ui.dp(a,20),Ui.dp(a,18));
        box.addView(Ui.text(a,"لم نجد ملفًا صالحًا",18,Ui.TEXT,true));
        TextView msg=Ui.text(a,"المصدر الذي تم رصده غير مكتمل أو غير قابل للتحميل كملف فيديو/صوت مستقل.",13,Ui.MUTED,false); msg.setPadding(0,Ui.dp(a,8),0,Ui.dp(a,12)); box.addView(msg);
        Button ok=Ui.primary(a,"حسنًا"); ok.setOnClickListener(v->d.dismiss()); LinearLayout.LayoutParams p=Ui.matchWrap();p.height=Ui.dp(a,48);box.addView(ok,p);
        d.setContentView(box); Window w=d.getWindow(); if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(a.getResources().getDisplayMetrics().widthPixels*.88f);lp.height=WindowManager.LayoutParams.WRAP_CONTENT;lp.dimAmount=.72f;w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setAttributes(lp);} d.show();
    }
}
