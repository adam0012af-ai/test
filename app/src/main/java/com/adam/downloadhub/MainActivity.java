package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_M3U = 4001;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText directUrl;
    private EditText platformUrl;
    private EditText m3uUrl;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 13, 18));
        getWindow().setNavigationBarColor(Color.rgb(10, 13, 18));
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(10, 13, 18));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(30));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Download Hub", 28, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView sub = text("تحميل روابط مباشرة + استخراج فيديو من المنصات + فرز M3U الضخم", 14,
                Color.rgb(170, 180, 195), false);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subLp = matchWrap();
        subLp.setMargins(0, dp(6), 0, dp(24));
        root.addView(sub, subLp);

        root.addView(section("تحميل رابط مباشر"));
        directUrl = input("ضع رابط ملف أو فيديو مباشر https://...");
        root.addView(directUrl, fieldLp());
        Button directBtn = button("تحميل الرابط");
        directBtn.setOnClickListener(v -> startDirectDownload());
        root.addView(directBtn, buttonLp());

        root.addView(section("تحميل من المنصات"));
        platformUrl = input("ضع رابط TikTok أو صفحة فيديو عامة");
        root.addView(platformUrl, fieldLp());
        Button extractBtn = button("استخراج أفضل فيديو وتحميله");
        extractBtn.setOnClickListener(v -> startPlatformExtraction());
        root.addView(extractBtn, buttonLp());

        root.addView(section("M3U / M3U8"));
        m3uUrl = input("ضع رابط M3U / M3U8 مهما كان حجمه");
        root.addView(m3uUrl, fieldLp());
        Button fetchBtn = button("جلب الرابط وفرز المحتوى");
        fetchBtn.setOnClickListener(v -> fetchM3u());
        root.addView(fetchBtn, buttonLp());

        Button fileBtn = button("اختيار ملف M3U من الجهاز");
        fileBtn.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(46, 55, 70)));
        fileBtn.setOnClickListener(v -> pickM3uFile());
        root.addView(fileBtn, buttonLp());

        status = text("جاهز", 14, Color.rgb(115, 220, 160), false);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams statusLp = matchWrap();
        statusLp.setMargins(0, dp(16), 0, 0);
        root.addView(status, statusLp);

        TextView note = text(
                "M3U يتم قراءته وكتابته Streaming سطرًا بسطر لدعم القوائم الضخمة بدون تحميلها كاملة في الذاكرة. التطبيق لا يتجاوز DRM أو تسجيل الدخول أو حماية المنصات.",
                12, Color.rgb(130, 140, 155), false);
        LinearLayout.LayoutParams noteLp = matchWrap();
        noteLp.setMargins(0, dp(14), 0, 0);
        root.addView(note, noteLp);

        return scroll;
    }

    private void startDirectDownload() {
        String url = value(directUrl);
        if (!isHttp(url)) {
            show("اكتب رابط HTTP/HTTPS صحيح");
            return;
        }
        String name = DownloadUtil.guessFileName(url, "download.bin");
        enqueue(url, name, null);
    }

    private void startPlatformExtraction() {
        String url = value(platformUrl);
        if (!isHttp(url)) {
            show("اكتب رابط منصة صحيح");
            return;
        }
        setStatus("جاري تحليل صفحة الفيديو…");
        executor.execute(() -> {
            try {
                PlatformExtractor.VideoCandidate result = PlatformExtractor.extract(url);
                runOnUiThread(() -> {
                    setStatus("تم العثور على الفيديو: " + result.fileName);
                    enqueue(result.url, result.fileName, result.referer);
                });
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("تعذر استخراج الفيديو: " + safeMessage(e)));
            }
        });
    }

    private void fetchM3u() {
        String url = value(m3uUrl);
        if (!isHttp(url)) {
            show("اكتب رابط M3U/M3U8 صحيح");
            return;
        }
        setStatus("جاري فتح قائمة M3U وبدء المعالجة Streaming…");
        executor.execute(() -> {
            try (NetUtil.StreamResult source = NetUtil.openTextStream(url, url)) {
                processM3uStream(source.stream);
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("فشل جلب/فرز M3U: " + safeMessage(e)));
            }
        });
    }

    private void pickM3uFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, PICK_M3U);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_M3U && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            setStatus("جاري قراءة الملف وفرزه Streaming…");
            executor.execute(() -> {
                try (InputStream input = NetUtil.openUriStream(this, uri)) {
                    processM3uStream(input);
                } catch (Exception e) {
                    runOnUiThread(() -> setStatus("فشل قراءة/فرز الملف: " + safeMessage(e)));
                }
            });
        }
    }

    private void processM3uStream(InputStream input) throws Exception {
        try (TextExporter.StreamingSession output = TextExporter.openStreaming(this)) {
            final long[] processed = {0L};

            long total = M3uParser.parseStream(input, entry -> {
                output.write(entry, M3uParser.classifyOne(entry));
                processed[0]++;

                if (processed[0] % 5000L == 0L) {
                    long current = processed[0];
                    runOnUiThread(() -> setStatus(
                            "جاري الفرز… تمت معالجة " + String.format(Locale.US, "%,d", current) + " عنصر"));
                }
            });

            if (total == 0L) {
                throw new IllegalArgumentException("لم يتم العثور على عناصر M3U صالحة");
            }

            TextExporter.StreamingExportResult result = output.finish();
            runOnUiThread(() -> setStatus(String.format(Locale.US,
                    "تم الفرز بنجاح ✅\nقنوات: %,d | أفلام: %,d | مسلسلات: %,d\nالإجمالي: %,d\nتم الحفظ داخل Downloads/DownloadHub",
                    result.channelsCount,
                    result.moviesCount,
                    result.seriesCount,
                    result.channelsCount + result.moviesCount + result.seriesCount)));
        }
    }

    private void enqueue(String url, String name, String referer) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setDescription("Download Hub");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.addRequestHeader("User-Agent", NetUtil.USER_AGENT);
            if (referer != null && !referer.isEmpty()) {
                request.addRequestHeader("Referer", referer);
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "DownloadHub/" + DownloadUtil.sanitizeFileName(name));

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long id = dm.enqueue(request);
            setStatus("بدأ التحميل. رقم العملية: " + id);
            Toast.makeText(this, "بدأ التحميل", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            setStatus("تعذر بدء التحميل: " + safeMessage(e));
        }
    }

    private TextView section(String s) {
        TextView t = text(s, 18, Color.WHITE, true);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(22), 0, dp(8));
        t.setLayoutParams(lp);
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(125, 135, 150));
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setMinLines(2);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(80, 95, 115)));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(34, 116, 232)));
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.height = dp(64);
        return lp;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.height = dp(52);
        lp.setMargins(0, dp(9), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private String value(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isHttp(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void show(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private String safeMessage(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.trim().isEmpty()) ? t.getClass().getSimpleName() : m;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
