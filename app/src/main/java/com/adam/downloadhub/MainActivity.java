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
    public static final String EXTRA_PREFILL_URL = "prefill_url";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText smartUrl;
    private TextView status;
    private Button downloadButton;
    private boolean handledIntent;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        consumeIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handledIntent = false;
        consumeIntent(i);
    }

    @Override protected void onResume() {
        super.onResume();
        if (AppPrefs.autoClipboard(this) && smartUrl != null && value(smartUrl).isEmpty()) {
            String u = clipboardUrl();
            if (isHttp(u)) {
                smartUrl.setText(u);
                setStatus("رابط من الحافظة جاهز");
            }
        }
    }

    private View buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setBackground(Ui.gradient(Ui.BG, Ui.BG_2, 0, this));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(Ui.text(this, "Download Hub", 24, Ui.TEXT, true));
        brand.addView(Ui.text(this, "ISLAMIC REELS • CREATOR • TIKTOK", 10, Ui.CYAN, true));
        brand.addView(Ui.text(this, "AboAdam", 10, Ui.MUTED, false));
        LinearLayout.LayoutParams br = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        br.setMargins(Ui.dp(this, 10), 0, 0, 0);
        header.addView(brand, br);
        Button settings = Ui.ghost(this, "⚙");
        settings.setTextSize(18);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 46)));
        root.addView(header);

        LinearLayout creator = Ui.card(this);
        LinearLayout.LayoutParams cr = Ui.matchWrap();
        cr.setMargins(0, Ui.dp(this, 14), 0, 0);
        creator.setLayoutParams(cr);
        creator.setBackground(Ui.gradient3(0xFF0C4D9B, 0xFF143462, 0xFF0A1B32, 24, this));
        creator.addView(Ui.text(this, "اصنع Reel حقيقي وانشره على TikTok", 22, Ui.TEXT, true));
        TextView hs = Ui.text(this, "صانع ريلز إسلامي + مونتاج فيديو فعلي + تصدير MP4. البحث موقوف مؤقتًا في نسخة الاختبار الخفيفة.", 12, 0xFFC9DDF6, false);
        hs.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams hsp = Ui.matchWrap();
        hsp.setMargins(0, Ui.dp(this, 7), 0, Ui.dp(this, 13));
        creator.addView(hs, hsp);

        Button islamicStudio = Ui.accent(this, "☾  صانع الريلز الإسلامي");
        islamicStudio.setTextSize(17);
        islamicStudio.setOnClickListener(v -> startActivity(new Intent(this, IslamicReelsActivity.class)));
        LinearLayout.LayoutParams islp = Ui.matchWrap();
        islp.height = Ui.dp(this, 60);
        creator.addView(islamicStudio, islp);

        LinearLayout hr = new LinearLayout(this);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams hrp = Ui.matchWrap();
        hrp.setMargins(0, Ui.dp(this, 8), 0, 0);
        Button homeDownload = Ui.primary(this, "↓  تحميل بالرابط");
        homeDownload.setOnClickListener(v -> {
            scroll.post(() -> scroll.smoothScrollTo(0, Ui.dp(this, 470)));
            if (smartUrl != null) smartUrl.requestFocus();
        });
        hr.addView(homeDownload, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f));
        Button montage = Ui.secondary(this, "✂  مونتاج فيديو");
        montage.setOnClickListener(v -> {
            Intent i = new Intent(this, TemplateEditorActivity.class);
            i.putExtra("template_id", "islamic-0");
            i.putExtra("pick_media", true);
            startActivity(i);
        });
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
        ml.setMargins(Ui.dp(this, 8), 0, 0, 0);
        hr.addView(montage, ml);
        creator.addView(hr, hrp);
        root.addView(creator);

        TextView dlTitle = Ui.sectionTitle(this, "تحميل الوسائط");
        LinearLayout.LayoutParams dlt = Ui.matchWrap();
        dlt.setMargins(0, Ui.dp(this, 20), 0, Ui.dp(this, 9));
        root.addView(dlTitle, dlt);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button linkMode = Ui.primary(this, "🔗  بالرابط");
        linkMode.setEnabled(false);
        modes.addView(linkMode, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        Button searchMode = Ui.secondary(this, "⌕  البحث موقوف مؤقتًا");
        searchMode.setEnabled(false);
        searchMode.setAlpha(.55f);
        LinearLayout.LayoutParams sm = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
        sm.setMargins(Ui.dp(this, 8), 0, 0, 0);
        modes.addView(searchMode, sm);
        root.addView(modes);

        LinearLayout downloader = Ui.card(this);
        LinearLayout.LayoutParams dc = Ui.matchWrap();
        dc.setMargins(0, Ui.dp(this, 9), 0, 0);
        downloader.setLayoutParams(dc);
        downloader.addView(Ui.text(this, "ألصق رابط فيديو أو صوت", 16, Ui.TEXT, true));
        downloader.addView(Ui.text(this, "نسخة الاختبار تستخدم المحرك السريع فقط. yt-dlp والبحث المتقدم متوقفان مؤقتًا.", 11, Ui.MUTED, false));
        smartUrl = Ui.input(this, "https://…", true);
        smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);
        LinearLayout.LayoutParams su = Ui.matchWrap();
        su.height = Ui.dp(this, 80);
        su.setMargins(0, Ui.dp(this, 10), 0, 0);
        downloader.addView(smartUrl, su);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ar = Ui.matchWrap();
        ar.setMargins(0, Ui.dp(this, 8), 0, 0);
        Button paste = Ui.secondary(this, "لصق");
        paste.setOnClickListener(v -> pasteLink());
        actions.addView(paste, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        Button clear = Ui.ghost(this, "مسح");
        clear.setOnClickListener(v -> {
            smartUrl.setText("");
            setStatus("جاهز");
        });
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f);
        cl.setMargins(Ui.dp(this, 7), 0, 0, 0);
        actions.addView(clear, cl);
        downloader.addView(actions, ar);

        downloadButton = Ui.accent(this, "↓  تجهيز خيارات التحميل");
        downloadButton.setTextSize(16);
        downloadButton.setOnClickListener(v -> smartProcess());
        LinearLayout.LayoutParams db = Ui.matchWrap();
        db.height = Ui.dp(this, 58);
        db.setMargins(0, Ui.dp(this, 9), 0, 0);
        downloader.addView(downloadButton, db);
        root.addView(downloader);

        LinearLayout state = Ui.card(this);
        LinearLayout.LayoutParams st = Ui.matchWrap();
        st.setMargins(0, Ui.dp(this, 9), 0, 0);
        state.setLayoutParams(st);
        status = Ui.text(this, "جاهز", 12, Ui.GREEN, true);
        status.setGravity(Gravity.CENTER);
        state.addView(status);
        root.addView(state);

        TextView quick = Ui.sectionTitle(this, "وصول سريع");
        LinearLayout.LayoutParams qt = Ui.matchWrap();
        qt.setMargins(0, Ui.dp(this, 20), 0, Ui.dp(this, 9));
        root.addView(quick, qt);
        root.addView(quickRow("↓  التحميلات", v -> startActivity(new Intent(this, DownloadsActivity.class)), "▦  المكتبة", v -> startActivity(new Intent(this, LibraryActivity.class))));
        LinearLayout.LayoutParams q2 = Ui.matchWrap();
        q2.setMargins(0, Ui.dp(this, 8), 0, 0);
        root.addView(quickRow("★  السجل", v -> startActivity(new Intent(this, HistoryActivity.class)), "☷  Batch", v -> startActivity(new Intent(this, BatchActivity.class))), q2);

        TextView footer = Ui.text(this, "Download Hub • v6.0.3 Test • Developed by AboAdam", 10, Ui.MUTED_2, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ft = Ui.matchWrap();
        ft.setMargins(0, Ui.dp(this, 22), 0, 0);
        root.addView(footer, ft);

        page.addView(bottomNav(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 68)));
        return page;
    }

    private View bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 8));
        nav.setBackground(Ui.bordered(Ui.SURFACE, Ui.BORDER_SOFT, 1, 0, this));
        nav.addView(navButton("⌂\nالرئيسية", v -> {}), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navButton("↓\nالتحميلات", v -> startActivity(new Intent(this, DownloadsActivity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navButton("✂\nمونتاج", v -> {
            Intent i = new Intent(this, TemplateEditorActivity.class);
            i.putExtra("template_id", "islamic-0");
            i.putExtra("pick_media", true);
            startActivity(i);
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navButton("☾\nإسلامي", v -> startActivity(new Intent(this, IslamicReelsActivity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navButton("⚙\nالضبط", v -> startActivity(new Intent(this, SettingsActivity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return nav;
    }

    private Button navButton(String s, View.OnClickListener l) {
        Button b = Ui.ghost(this, s);
        b.setTextSize(10);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout quickRow(String a, View.OnClickListener al, String b, View.OnClickListener bl) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button x = Ui.secondary(this, a);
        x.setOnClickListener(al);
        row.addView(x, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        Button y = Ui.secondary(this, b);
        y.setOnClickListener(bl);
        LinearLayout.LayoutParams yp = new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f);
        yp.setMargins(Ui.dp(this, 8), 0, 0, 0);
        row.addView(y, yp);
        return row;
    }

    private void pasteLink() {
        String u = clipboardUrl();
        if (isHttp(u)) {
            smartUrl.setText(u);
            setStatus("الرابط جاهز للتحميل");
        } else show("الحافظة لا تحتوي على رابط صحيح");
    }

    private void consumeIntent(Intent intent) {
        if (intent == null || handledIntent || smartUrl == null) return;
        handledIntent = true;
        String u = intent.getStringExtra(EXTRA_PREFILL_URL);
        if (u == null && Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence t = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (t != null) u = extractFirstUrl(t.toString());
        }
        if (u == null && intent.getData() != null) u = intent.getData().toString();
        if (isHttp(u)) {
            smartUrl.setText(u);
            setStatus("تم استلام الرابط — اضغط تجهيز خيارات التحميل");
        }
    }

    private void smartProcess() {
        String raw = value(smartUrl);
        if (!isHttp(raw)) {
            show("ضع رابط HTTP/HTTPS صحيح");
            return;
        }
        String url = AppPrefs.linkCleaner(this) ? LinkTools.clean(raw) : raw;
        if (!url.equals(raw)) smartUrl.setText(url);
        HistoryStore.add(this, url, detectKind(url));
        setBusy(true);
        setStatus("المحرك السريع: جاري تحليل الرابط…");
        executor.execute(() -> {
            Throwable first;
            try {
                PlatformExtractor.MediaBundle b = PlatformExtractor.extractOptions(url);
                runOnUiThread(() -> {
                    setBusy(false);
                    presentBundle(b, "المحرك السريع");
                });
                return;
            } catch (Throwable e) {
                first = e;
            }

            if (!BuildConfig.YTDLP_ENABLED) {
                String error = safeMessage(first);
                runOnUiThread(() -> {
                    setBusy(false);
                    setStatus("المحرك المتقدم موقوف مؤقتًا في نسخة الاختبار.\n" + shortError(error, ""));
                });
                return;
            }

            runOnUiThread(() -> setStatus("المحرك المتقدم yt-dlp: جاري تجربة الرابط…"));
            try {
                PlatformExtractor.MediaBundle b = YtDlpResolver.extractOptions(this, url);
                runOnUiThread(() -> {
                    setBusy(false);
                    presentBundle(b, "yt-dlp");
                });
            } catch (Throwable second) {
                String a = safeMessage(first), bb = safeMessage(second);
                runOnUiThread(() -> {
                    setBusy(false);
                    setStatus("تعذر تجهيز ملف كامل من الرابط.\n" + shortError(a, bb));
                });
            }
        });
    }

    private void presentBundle(PlatformExtractor.MediaBundle b, String engine) {
        List<MediaOption> opts = b.options;
        if (opts == null || opts.isEmpty()) {
            setStatus("لم يتم العثور على فيديو أو صوت قابل للتحميل");
            return;
        }
        setStatus(b.platform + " • " + opts.size() + " خيارات • " + engine);
        MediaOptionsDialog.show(this, b.title, opts, this::startDownload);
    }

    private void startDownload(MediaOption o) {
        try {
            long id = DownloadEngine.enqueue(this, o);
            setStatus("تمت إضافة التحميل ✓\n" + o.label + (o.sizeBytes > 0 ? " • " + MediaOption.formatBytes(o.sizeBytes) : ""));
            show("بدأ التحميل #" + id);
        } catch (Exception e) {
            String m = safeMessage(e);
            setStatus(m);
            show(m);
        }
    }

    private void setBusy(boolean busy) {
        if (downloadButton == null) return;
        downloadButton.setEnabled(!busy);
        downloadButton.setAlpha(busy ? .55f : 1f);
        downloadButton.setText(busy ? "جاري التجهيز…" : "↓  تجهيز خيارات التحميل");
    }

    private void setStatus(String s) { if (status != null) status.setText(s == null ? "" : s); }
    private String value(EditText e) { return e == null || e.getText() == null ? "" : e.getText().toString().trim(); }
    private boolean isHttp(String s) { return s != null && (s.startsWith("http://") || s.startsWith("https://")); }

    private String clipboardUrl() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return "";
            ClipData c = cm.getPrimaryClip();
            if (c == null || c.getItemCount() == 0) return "";
            CharSequence t = c.getItemAt(0).coerceToText(this);
            return t == null ? "" : extractFirstUrl(t.toString());
        } catch (Exception e) {
            return "";
        }
    }

    private String extractFirstUrl(String text) {
        if (text == null) return "";
        Matcher m = Pattern.compile("https?://\\S+").matcher(text);
        if (!m.find()) return "";
        String u = m.group();
        while (u.endsWith(")") || u.endsWith("]") || u.endsWith("}") || u.endsWith(",") || u.endsWith(".")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private String detectKind(String url) {
        try {
            String h = new URI(url).getHost();
            return PlatformExtractor.platformName(h);
        } catch (Exception e) {
            return "Link";
        }
    }

    private String safeMessage(Throwable e) {
        if (e == null) return "";
        String s = e.getMessage();
        if (TextUtils.isEmpty(s)) s = e.getClass().getSimpleName();
        s = s.replace('\n', ' ').trim();
        return s.length() > 180 ? s.substring(0, 180) + "…" : s;
    }

    private String shortError(String a, String b) {
        if (!TextUtils.isEmpty(b)) return b;
        if (!TextUtils.isEmpty(a)) return a;
        return "الرابط غير متاح كملف وسائط حاليًا";
    }

    private void show(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
