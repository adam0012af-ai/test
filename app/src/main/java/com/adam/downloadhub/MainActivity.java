package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    public static final String EXTRA_PREFILL_URL = "prefill_url";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText smartUrl;
    private TextView status;
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
                setStatus("تم العثور على رابط في الحافظة — جاهز للتحليل");
            }
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG, Color.rgb(8, 18, 34), 0, this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 34));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.app_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 66), Ui.dp(this, 66)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(Ui.text(this, "Download Hub", 28, Ui.TEXT, true));
        names.addView(Ui.text(this, "V4 PREMIUM • Media Downloader & Manager", 11, Ui.CYAN, true));
        names.addView(Ui.text(this, "Developer • AboAdam", 11, Ui.MUTED, true));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        header.addView(names, nlp);

        Button settings = Ui.secondary(this, "⚙");
        settings.setTextSize(20);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        root.addView(header);

        TextView tagline = Ui.text(this, "حلّل أي رابط مدعوم، اعرض الفيديو والجودات والصوت، ثم اختر ما تريد تنزيله.", 13, Ui.MUTED, false);
        tagline.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams tlp = Ui.matchWrap();
        tlp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 18));
        root.addView(tagline, tlp);

        LinearLayout smart = Ui.card(this);
        smart.setBackground(Ui.gradient(Color.rgb(13, 28, 51), Color.rgb(12, 20, 35), 22, this));
        smart.addView(Ui.text(this, "Smart Media Analyzer", 20, Ui.TEXT, true));
        TextView hint = Ui.text(this, "فيديو • صوت فقط • جودة متعددة • فحص سلامة الملف • بدون علامة مائية", 11, Ui.MUTED, false);
        LinearLayout.LayoutParams hp = Ui.matchWrap();
        hp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        smart.addView(hint, hp);

        smartUrl = new EditText(this);
        smartUrl.setHint("ألصق رابط الفيديو أو المنشور هنا…");
        smartUrl.setTextColor(Ui.TEXT);
        smartUrl.setHintTextColor(Color.rgb(105, 122, 145));
        smartUrl.setTextSize(14);
        smartUrl.setMinLines(3);
        smartUrl.setMaxLines(5);
        smartUrl.setGravity(Gravity.TOP | Gravity.START);
        smartUrl.setTextDirection(View.TEXT_DIRECTION_LTR);
        smartUrl.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        smartUrl.setBackground(Ui.bordered(Color.rgb(8, 14, 25), Ui.BORDER, 1, 16, this));
        smart.addView(smartUrl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 92)));

        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.HORIZONTAL);
        Button paste = Ui.secondary(this, "لصق الرابط");
        paste.setOnClickListener(v -> {
            String u = clipboardUrl();
            if (isHttp(u)) {
                smartUrl.setText(u);
                setStatus("تم لصق الرابط");
            } else show("الحافظة لا تحتوي على رابط صحيح");
        });
        mini.addView(paste, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));

        Button clear = Ui.secondary(this, "مسح");
        clear.setOnClickListener(v -> {
            smartUrl.setText("");
            setStatus("جاهز");
        });
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        cl.setMargins(Ui.dp(this, 8), 0, 0, 0);
        mini.addView(clear, cl);
        LinearLayout.LayoutParams mlp = Ui.matchWrap();
        mlp.setMargins(0, Ui.dp(this, 10), 0, 0);
        smart.addView(mini, mlp);

        Button analyze = Ui.primary(this, "تحليل الرابط واختيار الجودة");
        analyze.setTextSize(15);
        analyze.setOnClickListener(v -> smartProcess());
        LinearLayout.LayoutParams alp = Ui.matchWrap();
        alp.height = Ui.dp(this, 58);
        alp.setMargins(0, Ui.dp(this, 10), 0, 0);
        smart.addView(analyze, alp);
        root.addView(smart);

        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        String[] platforms = {"TikTok", "YouTube", "Instagram", "Facebook", "X", "Reddit", "Pinterest", "Vimeo", "Dailymotion", "Twitch", "Threads", "Snapchat", "Likee", "Kwai", "VK", "Tumblr", "Streamable", "Rumble"};
        for (String s : platforms) {
            TextView chip = Ui.text(this, s, 12, Ui.CYAN, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(this, 13), 0, Ui.dp(this, 13), 0);
            chip.setBackground(Ui.bordered(Ui.SURFACE_2, Ui.BORDER, 1, 18, this));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 38));
            cp.setMargins(0, 0, Ui.dp(this, 7), 0);
            chips.addView(chip, cp);
        }
        chipScroll.addView(chips);
        LinearLayout.LayoutParams csp = Ui.matchWrap();
        csp.setMargins(0, Ui.dp(this, 14), 0, 0);
        root.addView(chipScroll, csp);

        TextView quickTitle = Ui.text(this, "مركز الأدوات", 18, Ui.TEXT, true);
        LinearLayout.LayoutParams qtp = Ui.matchWrap();
        qtp.setMargins(0, Ui.dp(this, 22), 0, Ui.dp(this, 9));
        root.addView(quickTitle, qtp);

        root.addView(quickRow("↓  التحميلات", v -> startActivity(new Intent(this, DownloadsActivity.class)), "☷  Batch", v -> startActivity(new Intent(this, BatchActivity.class))));
        LinearLayout.LayoutParams r2 = Ui.matchWrap();
        r2.setMargins(0, Ui.dp(this, 9), 0, 0);
        root.addView(quickRow("★  السجل والمفضلة", v -> startActivity(new Intent(this, HistoryActivity.class)), "▥  الإحصائيات", v -> startActivity(new Intent(this, StatsActivity.class))), r2);
        LinearLayout.LayoutParams r3 = Ui.matchWrap();
        r3.setMargins(0, Ui.dp(this, 9), 0, 0);
        root.addView(singleQuick("⚙  الإعدادات", v -> startActivity(new Intent(this, SettingsActivity.class))), r3);

        LinearLayout stateCard = Ui.card(this);
        LinearLayout.LayoutParams scp = Ui.matchWrap();
        scp.setMargins(0, Ui.dp(this, 18), 0, 0);
        stateCard.setLayoutParams(scp);
        stateCard.addView(Ui.text(this, "الحالة", 13, Ui.MUTED, true));
        status = Ui.text(this, "جاهز لتحليل أي رابط", 14, Ui.GREEN, false);
        status.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams stp = Ui.matchWrap();
        stp.setMargins(0, Ui.dp(this, 5), 0, 0);
        stateCard.addView(status, stp);
        root.addView(stateCard);

        LinearLayout focus = Ui.card(this);
        LinearLayout.LayoutParams fp = Ui.matchWrap();
        fp.setMargins(0, Ui.dp(this, 14), 0, 0);
        focus.setLayoutParams(fp);
        focus.addView(Ui.text(this, "V4 Focus", 16, Ui.TEXT, true));
        TextView focusText = Ui.text(this, "تنزيل الفيديو والصوت فقط • جودة قبل التنزيل • Batch • إدارة التحميلات • سجل ومفضلة • إحصائيات • تنظيم الملفات", 12, Ui.MUTED, false);
        focusText.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams ftx = Ui.matchWrap();
        ftx.setMargins(0, Ui.dp(this, 7), 0, 0);
        focus.addView(focusText, ftx);
        root.addView(focus);

        TextView footer = Ui.text(this, "Download Hub v4 • Video & Audio Downloader • No Watermark\nDeveloped by AboAdam", 11, Color.rgb(105, 122, 145), true);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams flp = Ui.matchWrap();
        flp.setMargins(0, Ui.dp(this, 22), 0, 0);
        root.addView(footer, flp);
        return scroll;
    }

    private LinearLayout quickRow(String a, View.OnClickListener al, String b, View.OnClickListener bl) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button x = Ui.secondary(this, a);
        x.setTextSize(13);
        x.setOnClickListener(al);
        row.addView(x, new LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f));
        Button y = Ui.secondary(this, b);
        y.setTextSize(13);
        y.setOnClickListener(bl);
        LinearLayout.LayoutParams yp = new LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f);
        yp.setMargins(Ui.dp(this, 9), 0, 0, 0);
        row.addView(y, yp);
        return row;
    }

    private LinearLayout singleQuick(String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button b = Ui.secondary(this, label);
        b.setTextSize(13);
        b.setOnClickListener(listener);
        row.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));
        return row;
    }

    private void consumeIntent(Intent intent) {
        if (intent == null || handledIntent || smartUrl == null) return;
        handledIntent = true;
        String u = intent.getStringExtra(EXTRA_PREFILL_URL);
        if (u == null && Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) u = extractFirstUrl(text.toString());
        }
        if (u == null && intent.getData() != null) u = intent.getData().toString();
        if (isHttp(u)) {
            smartUrl.setText(u);
            setStatus("تم استلام الرابط من تطبيق آخر");
        }
    }

    private void smartProcess() {
        String url = value(smartUrl);
        if (!isHttp(url)) {
            show("ضع رابط HTTP/HTTPS صحيح");
            return;
        }

        String kind = detectKind(url);
        HistoryStore.add(this, url, kind);
        setStatus("جاري تحليل " + kind + " والبحث عن الجودات والصوت…");

        executor.execute(() -> {
            try {
                PlatformExtractor.MediaBundle bundle = PlatformExtractor.extractOptions(url);
                runOnUiThread(() -> presentBundle(bundle));
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("تعذر استخراج ملف فيديو أو صوت صالح من الرابط.\n" + safeMessage(e)));
            }
        });
    }

    private void presentBundle(PlatformExtractor.MediaBundle bundle) {
        List<MediaOption> options = bundle.options;
        setStatus("تم العثور على " + options.size() + " خيار صالح • " + bundle.platform + "\nاختر الفيديو أو الصوت والجودة");
        String mode = AppPrefs.defaultMode(this);
        if ("best".equals(mode)) {
            MediaOption best = firstVideo(options);
            if (best != null) {
                startDownload(best);
                return;
            }
        } else if ("audio".equals(mode)) {
            MediaOption audio = firstAudio(options);
            if (audio != null) {
                startDownload(audio);
                return;
            }
        }
        MediaOptionsDialog.show(this, bundle.title + "\n" + bundle.platform, options, this::startDownload);
    }

    private MediaOption firstVideo(List<MediaOption> xs) {
        for (MediaOption o : xs) if (!o.audioOnly) return o;
        return xs.isEmpty() ? null : xs.get(0);
    }

    private MediaOption firstAudio(List<MediaOption> xs) {
        for (MediaOption o : xs) if (o.audioOnly) return o;
        return null;
    }

    private void startDownload(MediaOption option) {
        try {
            long id = DownloadEngine.enqueue(this, option);
            setStatus("بدأ التحميل ✅\n" + option.displayLabel() + "\n" + option.fileName + (AppPrefs.wifiOnly(this) ? "\nسيبدأ عند توفر Wi‑Fi" : ""));
            show("تمت إضافة التحميل #" + id);
        } catch (Exception e) {
            setStatus("تعذر بدء التحميل: " + safeMessage(e));
        }
    }

    private String detectKind(String url) {
        try {
            String host = URI.create(url).getHost();
            String platform = PlatformExtractor.platformName(host == null ? "" : host.toLowerCase(Locale.ROOT));
            return "Web".equals(platform) ? "Media link" : platform;
        } catch (Exception ignored) {
            return "Media link";
        }
    }

    private String clipboardUrl() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData d = cm.getPrimaryClip();
                if (d != null && d.getItemCount() > 0) {
                    CharSequence c = d.getItemAt(0).coerceToText(this);
                    if (c != null) return extractFirstUrl(c.toString());
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractFirstUrl(String text) {
        if (text == null) return "";
        int a = text.indexOf("https://");
        int b = text.indexOf("http://");
        int start = a >= 0 ? (b >= 0 ? Math.min(a, b) : a) : b;
        if (start < 0) return text.trim();
        int end = text.length();
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                end = i;
                break;
            }
        }
        return text.substring(start, end).trim();
    }

    private String value(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isHttp(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private void setStatus(String s) {
        if (status != null) status.setText(s);
    }

    private void show(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private String safeMessage(Throwable t) {
        String m = t == null ? null : t.getMessage();
        return m == null || m.trim().isEmpty() ? "تعذر تحليل الرابط" : m.trim();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
