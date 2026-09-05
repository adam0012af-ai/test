package com.adam.downloadhub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Final export hub: preview, save to Gallery, share/publish, TikTok and re-edit. */
public class PublishingActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private CreatorProject project;
    private File video;
    private Uri galleryUri;
    private EditText title, caption, hashtags;
    private TextView saveState;
    private Button saveButton;
    private VideoView preview;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        String id = getIntent().getStringExtra("project_id");
        project = DraftStore.get(this, id);
        if (project == null) project = CreatorProject.fromTemplate(null);
        String path = getIntent().getStringExtra("video_path");
        video = path == null ? null : new File(path);
        setContentView(buildUi());
        generate();
        if (validVideo()) {
            preview.setVideoURI(Uri.fromFile(video));
            preview.setOnPreparedListener(mp -> { mp.setLooping(true); try { preview.start(); } catch (Exception ignored) {} });
            saveToGallery(true);
        } else {
            saveState.setText("ملف الفيديو غير موجود أو غير صالح");
            saveState.setTextColor(Ui.RED);
        }
    }

    @Override protected void onDestroy() {
        try { preview.stopPlayback(); } catch (Exception ignored) {}
        io.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG, Ui.BG_2, 0, this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 30));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(Ui.topBar(this, "Export Center", "حفظ • مشاركة • نشر • رجوع للتعديل", v -> finish()));

        FrameLayout previewCard = new FrameLayout(this);
        previewCard.setBackground(Ui.bordered(0xFF010409, Ui.BORDER_SOFT, 1, 18, this));
        preview = new VideoView(this);
        previewCard.addView(preview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        TextView tap = Ui.text(this, "اضغط للتشغيل / الإيقاف", 10, 0xCCFFFFFF, true);
        tap.setGravity(Gravity.CENTER);
        tap.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28), Gravity.BOTTOM);
        previewCard.addView(tap, tp);
        previewCard.setOnClickListener(v -> togglePreview());
        LinearLayout.LayoutParams pv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 280));
        pv.setMargins(0, Ui.dp(this, 10), 0, 0);
        root.addView(previewCard, pv);

        LinearLayout status = Ui.card(this);
        LinearLayout.LayoutParams sp = Ui.matchWrap(); sp.setMargins(0, Ui.dp(this, 10), 0, 0);
        status.setLayoutParams(sp);
        status.setBackground(Ui.gradient(0xFF0C443F, 0xFF0A1D2C, 20, this));
        status.addView(Ui.text(this, "✓ تم إنشاء الفيديو", 17, Ui.TEXT, true));
        long mb = validVideo() ? Math.max(1, video.length() / 1024 / 1024) : 0;
        String info = validVideo() ? video.getName() + " • " + mb + " MB • " + AppPrefs.exportQuality(this) + "p" : "ملف غير صالح";
        status.addView(Ui.text(this, info, 11, 0xFFC8EBDD, false));
        saveState = Ui.text(this, "جاري الحفظ في الاستوديو…", 11, Ui.YELLOW, true);
        LinearLayout.LayoutParams ss = Ui.matchWrap(); ss.setMargins(0, Ui.dp(this, 7), 0, 0);
        status.addView(saveState, ss);
        root.addView(status);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ap = Ui.matchWrap(); ap.setMargins(0, Ui.dp(this, 10), 0, 0);
        actions.setLayoutParams(ap);
        root.addView(actions);

        LinearLayout row1 = actionRow();
        saveButton = actionButton("⬇", "حفظ في الجهاز", v -> saveToGallery(false));
        row1.addView(saveButton, cell());
        row1.addView(actionButton("↗", "مشاركة", v -> shareGeneral()), cell());
        row1.addView(actionButton("♪", "TikTok", v -> publishToTikTok()), cell());
        actions.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 76)));

        LinearLayout row2 = actionRow();
        row2.addView(actionButton("▶", "فتح الفيديو", v -> openVideo()), cell());
        row2.addView(actionButton("✎", "رجوع للتعديل", v -> editAgain()), cell());
        row2.addView(actionButton("⧉", "نسخ النص", v -> { copyPublishText(composeText()); toast("تم نسخ النص ✓"); }), cell());
        LinearLayout.LayoutParams r2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 76)); r2.setMargins(0, Ui.dp(this, 7), 0, 0);
        actions.addView(row2, r2);

        LinearLayout form = Ui.card(this);
        LinearLayout.LayoutParams fp = Ui.matchWrap(); fp.setMargins(0, Ui.dp(this, 12), 0, 0); form.setLayoutParams(fp);
        form.addView(Ui.text(this, "محتوى النشر", 15, Ui.CYAN, true));
        title = field(form, "العنوان", "", false);
        caption = field(form, "الوصف / Caption", "", true);
        hashtags = field(form, "الهاشتاجات", "", true);
        Button regen = Ui.secondary(this, "✦ توليد عنوان ووصف وهاشتاج جديد");
        regen.setOnClickListener(v -> generate());
        LinearLayout.LayoutParams rg = Ui.matchWrap(); rg.height = Ui.dp(this, 46); rg.setMargins(0, Ui.dp(this, 9), 0, 0); form.addView(regen, rg);
        root.addView(form);

        Button publish = Ui.accent(this, "نشر / مشاركة الفيديو");
        publish.setTextSize(15);
        publish.setOnClickListener(v -> shareGeneral());
        LinearLayout.LayoutParams pp = Ui.matchWrap(); pp.height = Ui.dp(this, 58); pp.setMargins(0, Ui.dp(this, 12), 0, 0); root.addView(publish, pp);

        TextView note = Ui.text(this, "الفيديو يُحفظ تلقائيًا داخل Movies/DownloadHub ويظهر في Gallery. النشر المباشر النهائي يعتمد على التطبيق المستهدف؛ TikTok يفتح بالفيديو وتؤكد آخر ضغطة نشر داخله.", 10, Ui.MUTED, false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = Ui.matchWrap(); np.setMargins(0, Ui.dp(this, 9), 0, 0); root.addView(note, np);
        return scroll;
    }

    private LinearLayout actionRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER);
        return r;
    }

    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        return p;
    }

    private View actionButton(String icon, String label, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(Ui.bordered(Ui.SURFACE_2, Ui.BORDER_SOFT, 1, 15, this));
        box.setOnClickListener(click);
        TextView i = Ui.text(this, icon, 19, Ui.CYAN, true); i.setGravity(Gravity.CENTER);
        TextView l = Ui.text(this, label, 10, Ui.TEXT, true); l.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 36)));
        box.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28)));
        return box;
    }

    private EditText field(LinearLayout parent, String label, String value, boolean multi) {
        TextView l = Ui.text(this, label, 11, Ui.MUTED, true);
        LinearLayout.LayoutParams lp = Ui.matchWrap(); lp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 4)); parent.addView(l, lp);
        EditText e = Ui.input(this, "", multi); e.setText(value);
        LinearLayout.LayoutParams ep = Ui.matchWrap(); ep.height = Ui.dp(this, multi ? 78 : 46); parent.addView(e, ep);
        return e;
    }

    private void generate() {
        if (title == null) return;
        ContentAssistant.PublishText p = ContentAssistant.generate(project, "TikTok");
        title.setText(p.titles.isEmpty() ? project.name : p.titles.get(0));
        caption.setText(p.caption);
        hashtags.setText(p.hashtags);
    }

    private void saveToGallery(boolean automatic) {
        if (!validVideo()) { toast("ملف الفيديو غير صالح"); return; }
        if (galleryUri != null) {
            saveState.setText("✓ محفوظ في Gallery • Movies/DownloadHub");
            saveState.setTextColor(Ui.GREEN);
            if (!automatic) toast("الفيديو محفوظ بالفعل ✓");
            return;
        }
        saveButton.setEnabled(false);
        saveButton.setAlpha(.55f);
        saveState.setText("جاري الحفظ في Gallery…");
        saveState.setTextColor(Ui.YELLOW);
        io.execute(() -> {
            try {
                Uri uri = MediaStoreSaver.saveVideo(this, video);
                runOnUiThread(() -> {
                    galleryUri = uri;
                    saveButton.setEnabled(true); saveButton.setAlpha(1f);
                    saveState.setText("✓ محفوظ في Gallery • Movies/DownloadHub");
                    saveState.setTextColor(Ui.GREEN);
                    if (!automatic) toast("تم حفظ الفيديو في الجهاز ✓");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true); saveButton.setAlpha(1f);
                    saveState.setText("تعذر الحفظ: " + safeMessage(e));
                    saveState.setTextColor(Ui.RED);
                    if (!automatic) toast("تعذر حفظ الفيديو");
                });
            }
        });
    }

    private Uri shareUri() {
        if (galleryUri != null) return galleryUri;
        return FileProvider.getUriForFile(this, getPackageName() + ".files", video);
    }

    private void shareGeneral() {
        if (!validVideo()) { toast("ملف الفيديو غير صالح"); return; }
        try {
            Uri uri = shareUri();
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("video/mp4");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_TEXT, composeText());
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setClipData(ClipData.newRawUri("Exported video", uri));
            startActivity(Intent.createChooser(i, "نشر / مشاركة الفيديو"));
        } catch (Exception e) { toast("تعذر فتح المشاركة"); }
    }

    private void publishToTikTok() {
        if (!validVideo()) { toast("ملف الفيديو غير صالح"); return; }
        try {
            Uri uri = shareUri();
            copyPublishText(composeText());
            Intent base = new Intent(Intent.ACTION_SEND);
            base.setType("video/mp4");
            base.putExtra(Intent.EXTRA_STREAM, uri);
            base.putExtra(Intent.EXTRA_TEXT, composeText());
            base.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            base.setClipData(ClipData.newRawUri("Download Hub Reel", uri));
            String[] pkgs = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};
            for (String pkg : pkgs) {
                try {
                    grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Intent t = new Intent(base); t.setPackage(pkg); startActivity(t);
                    toast("تم فتح TikTok بالفيديو • النص منسوخ");
                    return;
                } catch (ActivityNotFoundException ignored) {} catch (Exception ignored) {}
            }
            toast("TikTok غير مثبت على الجهاز");
        } catch (Exception e) { toast("تعذر تجهيز TikTok"); }
    }

    private void openVideo() {
        if (!validVideo()) return;
        try {
            Uri uri = shareUri();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "video/mp4");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "فتح الفيديو"));
        } catch (Exception e) { togglePreview(); }
    }

    private void editAgain() {
        Intent i = new Intent(this, TemplateEditorActivity.class);
        i.putExtra("draft_id", project.id);
        startActivity(i);
        finish();
    }

    private void togglePreview() {
        try { if (preview.isPlaying()) preview.pause(); else preview.start(); } catch (Exception ignored) {}
    }

    private boolean validVideo() { return video != null && video.exists() && video.length() >= 1024; }

    private void copyPublishText(String value) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Publish caption", value));
        } catch (Exception ignored) {}
    }

    private String composeText() {
        StringBuilder b = new StringBuilder();
        String t = text(title), c = text(caption), h = text(hashtags);
        if (!t.isEmpty()) b.append(t);
        if (!c.isEmpty()) { if (b.length() > 0) b.append("\n\n"); b.append(c); }
        if (!h.isEmpty()) { if (b.length() > 0) b.append("\n\n"); b.append(h); }
        return b.toString();
    }

    private String text(EditText e) { return e == null || e.getText() == null ? "" : e.getText().toString().trim(); }
    private String safeMessage(Exception e) { String m = e.getMessage(); return m == null || m.trim().isEmpty() ? "خطأ غير معروف" : m.trim(); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
