package com.adam.downloadhub;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
        if (options == null || options.isEmpty()) { showEmpty(activity); return; }

        List<MediaOption> video = new ArrayList<>();
        List<MediaOption> audio = new ArrayList<>();
        for (MediaOption o : options) { if (o.audioOnly) audio.add(o); else video.add(o); }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        shell.setPadding(Ui.dp(activity,18),Ui.dp(activity,17),Ui.dp(activity,18),Ui.dp(activity,16));
        shell.setBackground(Ui.bordered(Color.rgb(8,15,27),Color.rgb(39,66,99),1,28,activity));

        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = Ui.text(activity,"↓",26,Color.WHITE,true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.gradient(Ui.BLUE,Color.rgb(24,91,238),17,activity));
        head.addView(icon,new LinearLayout.LayoutParams(Ui.dp(activity,52),Ui.dp(activity,52)));
        LinearLayout ht = new LinearLayout(activity); ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(Ui.text(activity,"اختر نوع التحميل",21,Ui.TEXT,true));
        ht.addView(Ui.text(activity,options.get(0).platform+" • "+options.size()+" خيارات",12,Ui.CYAN,true));
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); htp.setMargins(Ui.dp(activity,12),0,0,0); head.addView(ht,htp);
        shell.addView(head);

        String cleanTitle = title == null ? "" : title.replaceAll("\\s+"," ").trim();
        if (!cleanTitle.isEmpty()) {
            TextView mediaTitle = Ui.text(activity,cleanTitle,14,Ui.TEXT,true);
            mediaTitle.setMaxLines(2); mediaTitle.setEllipsize(TextUtils.TruncateAt.END); mediaTitle.setLineSpacing(0,1.12f);
            LinearLayout.LayoutParams mtp = Ui.matchWrap(); mtp.setMargins(0,Ui.dp(activity,14),0,Ui.dp(activity,12)); shell.addView(mediaTitle,mtp);
        }

        LinearLayout typeRow = new LinearLayout(activity); typeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button videoButton = typeButton(activity,"🎬  فيديو",!video.isEmpty());
        Button audioButton = typeButton(activity,"🎵  صوت فقط",!audio.isEmpty());
        typeRow.addView(videoButton,new LinearLayout.LayoutParams(0,Ui.dp(activity,50),1f));
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(0,Ui.dp(activity,50),1f); abp.setMargins(Ui.dp(activity,8),0,0,0); typeRow.addView(audioButton,abp);
        shell.addView(typeRow);

        TextView section = Ui.text(activity,"اختر الجودة",14,Ui.MUTED,true);
        LinearLayout.LayoutParams sp = Ui.matchWrap(); sp.setMargins(0,Ui.dp(activity,14),0,Ui.dp(activity,8)); shell.addView(section,sp);

        ScrollView scroll = new ScrollView(activity); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout qualityBox = new LinearLayout(activity); qualityBox.setOrientation(LinearLayout.VERTICAL); qualityBox.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(qualityBox,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        int maxH=(int)(activity.getResources().getDisplayMetrics().heightPixels*.38f);
        shell.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,maxH));

        TextView selectedText = Ui.text(activity,"",12,Ui.CYAN,true);
        selectedText.setGravity(Gravity.CENTER); selectedText.setPadding(0,Ui.dp(activity,8),0,Ui.dp(activity,6)); shell.addView(selectedText);

        Button download = Ui.primary(activity,"تحميل الآن");
        download.setTextSize(16);
        LinearLayout.LayoutParams dlp=Ui.matchWrap();dlp.height=Ui.dp(activity,56);shell.addView(download,dlp);
        Button cancel=Ui.secondary(activity,"إلغاء");cancel.setOnClickListener(v->dialog.dismiss());
        LinearLayout.LayoutParams cp=Ui.matchWrap();cp.height=Ui.dp(activity,46);cp.setMargins(0,Ui.dp(activity,8),0,0);shell.addView(cancel,cp);

        final boolean[] audioMode = {video.isEmpty() && !audio.isEmpty()};
        final MediaOption[] selected = {chooseDefault(activity,audioMode[0]?audio:video)};

        Runnable render = () -> {
            List<MediaOption> current = audioMode[0] ? audio : video;
            section.setText(audioMode[0] ? "اختر جودة الصوت" : "اختر جودة الفيديو");
            paintType(activity,videoButton,!audioMode[0]&&!video.isEmpty());
            paintType(activity,audioButton,audioMode[0]&&!audio.isEmpty());
            renderQualityButtons(activity,qualityBox,current,selected,selectedText,() -> {
                renderSelected(selectedText,selected[0]);
            });
            renderSelected(selectedText,selected[0]);
            download.setEnabled(selected[0]!=null);
            download.setAlpha(selected[0]==null?.45f:1f);
        };

        videoButton.setOnClickListener(v->{ if(video.isEmpty()) return; audioMode[0]=false; selected[0]=chooseDefault(activity,video); render.run(); });
        audioButton.setOnClickListener(v->{ if(audio.isEmpty()) return; audioMode[0]=true; selected[0]=chooseDefault(activity,audio); render.run(); });
        download.setOnClickListener(v->{ if(selected[0]==null)return; dialog.dismiss(); if(callback!=null)callback.onSelected(selected[0]); });
        render.run();

        dialog.setContentView(shell);
        Window w=dialog.getWindow();
        if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(activity.getResources().getDisplayMetrics().widthPixels*.93f);lp.height=WindowManager.LayoutParams.WRAP_CONTENT;lp.gravity=Gravity.CENTER;lp.dimAmount=.75f;w.setAttributes(lp);}
        dialog.show();
    }

    private static Button typeButton(Activity a,String text,boolean enabled){Button b=Ui.secondary(a,text);b.setTextSize(14);b.setEnabled(enabled);b.setAlpha(enabled?1f:.35f);return b;}
    private static void paintType(Activity a,Button b,boolean selected){b.setTextColor(selected?Color.WHITE:Ui.MUTED);b.setBackground(selected?Ui.gradient(Ui.BLUE,Color.rgb(28,89,225),15,a):Ui.bordered(Ui.SURFACE_2,Ui.BORDER,1,15,a));}

    private static void renderQualityButtons(Activity a,LinearLayout box,List<MediaOption> options,MediaOption[] selected,TextView selectedText,Runnable changed){
        box.removeAllViews();
        if(options.isEmpty()){TextView empty=Ui.text(a,"لا توجد خيارات متاحة لهذا النوع",13,Ui.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,Ui.dp(a,22),0,Ui.dp(a,22));box.addView(empty);return;}
        for(int i=0;i<options.size();i+=2){
            LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=0;j<2;j++){
                int idx=i+j;
                if(idx>=options.size()){row.addView(new View(a),new LinearLayout.LayoutParams(0,Ui.dp(a,64),1f));continue;}
                MediaOption o=options.get(idx);
                Button q=new Button(a);q.setAllCaps(false);q.setTextSize(13);q.setGravity(Gravity.CENTER);q.setStateListAnimator(null);
                boolean on=o==selected[0];
                q.setText((on?"✓  ":"")+qualityName(o)+"\n"+metaShort(o));
                q.setTextColor(on?Color.WHITE:Ui.TEXT);
                q.setBackground(on?Ui.gradient(Ui.BLUE,Color.rgb(25,102,235),16,a):Ui.bordered(Color.rgb(14,24,40),Color.rgb(42,66,95),1,16,a));
                q.setOnClickListener(v->{selected[0]=o;renderQualityButtons(a,box,options,selected,selectedText,changed);if(changed!=null)changed.run();});
                LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,Ui.dp(a,66),1f);if(j==1)qp.setMargins(Ui.dp(a,8),0,0,0);row.addView(q,qp);
            }
            LinearLayout.LayoutParams rp=Ui.matchWrap();rp.setMargins(0,0,0,Ui.dp(a,8));box.addView(row,rp);
        }
    }

    private static MediaOption chooseDefault(Activity a,List<MediaOption> xs){
        if(xs==null||xs.isEmpty())return null;
        String profile=AppPrefs.downloadProfile(a);
        if("saver".equals(profile))return xs.get(xs.size()-1);
        if("balanced".equals(profile)&&xs.size()>1){for(MediaOption o:xs){String l=o.label==null?"":o.label.toLowerCase(Locale.ROOT);if(l.contains("720")||l.contains("hd")||l.contains("480"))return o;}}
        return xs.get(0);
    }

    private static void renderSelected(TextView t,MediaOption o){if(o==null){t.setText("اختر جودة أولًا");return;}t.setText("المحدد: "+qualityName(o)+" • "+metaShort(o));}
    private static String qualityName(MediaOption o){if(o.audioOnly){String l=o.label==null?"":o.label.trim();return l.isEmpty()?"الصوت":l;}String l=o.label==null?"":o.label.replace("•"," · ").replaceAll("\\s+"," ").trim();return l.isEmpty()?"فيديو":l;}
    private static String metaShort(MediaOption o){String fmt=format(o);String size=o.sizeBytes>0?MediaOption.formatBytes(o.sizeBytes):"حجم غير معروف";return fmt+" • "+size;}
    private static String format(MediaOption o){String n=o.fileName==null?"":o.fileName.toLowerCase(Locale.ROOT);int dot=n.lastIndexOf('.');if(dot>=0&&dot+1<n.length())return n.substring(dot+1).toUpperCase(Locale.ROOT);String t=o.contentType==null?"":o.contentType;int slash=t.indexOf('/');if(slash>=0)t=t.substring(slash+1);int semi=t.indexOf(';');if(semi>0)t=t.substring(0,semi);return t.isEmpty()?"MEDIA":t.toUpperCase(Locale.ROOT);}

    private static void showEmpty(Activity a){final Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=Ui.card(a);box.setPadding(Ui.dp(a,20),Ui.dp(a,20),Ui.dp(a,20),Ui.dp(a,18));box.addView(Ui.text(a,"لم نجد ملفًا صالحًا",18,Ui.TEXT,true));TextView msg=Ui.text(a,"تعذر العثور على فيديو أو صوت كامل قابل للتنزيل من هذا الرابط.",13,Ui.MUTED,false);msg.setPadding(0,Ui.dp(a,8),0,Ui.dp(a,12));box.addView(msg);Button ok=Ui.primary(a,"حسنًا");ok.setOnClickListener(v->d.dismiss());LinearLayout.LayoutParams p=Ui.matchWrap();p.height=Ui.dp(a,48);box.addView(ok,p);d.setContentView(box);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(a.getResources().getDisplayMetrics().widthPixels*.88f);lp.height=WindowManager.LayoutParams.WRAP_CONTENT;lp.dimAmount=.72f;w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setAttributes(lp);}d.show();}
}
