package com.adam.downloadhub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IslamicReelsActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String[] SURAH_NAMES = {
            "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
            "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه",
            "الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم",
            "لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر",
            "فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق",
            "الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة",
            "الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج",
            "نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس",
            "التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد",
            "الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات",
            "القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر",
            "المسد","الإخلاص","الفلق","الناس"
    };

    private static final String[] RECITER_NAMES = {
            "مشاري راشد العفاسي",
            "عبد الباسط عبد الصمد - مرتل",
            "محمود خليل الحصري",
            "محمد صديق المنشاوي"
    };

    private static final String[] RECITER_IDS = {
            "ar.alafasy",
            "ar.abdulbasitmurattal",
            "ar.husary",
            "ar.minshawi"
    };

    private int surahIndex = 0;
    private int reciterIndex = 0;
    private Button surahButton, reciterButton, createQuranButton;
    private EditText fromAyah, toAyah;
    private TextView quranStatus;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG, Ui.BG_2, 0, this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 36));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(Ui.topBar(this, "صانع الريلز الإسلامي", "قرآن • أذكار • أدعية • حديث • تذكير", v -> finish()));

        LinearLayout hero = Ui.card(this);
        LinearLayout.LayoutParams hp = Ui.matchWrap();
        hp.setMargins(0, Ui.dp(this, 14), 0, 0);
        hero.setLayoutParams(hp);
        hero.setBackground(Ui.gradient3(0xFF0B695D, 0xFF123E4B, 0xFF081B2A, 24, this));
        hero.addView(Ui.text(this, "اعمل Reel فعلي من الصفر", 22, Ui.TEXT, true));
        TextView sub = Ui.text(this, "اختار المحتوى والصوت، وبعدها اختار فيديو/صورة خلفية، عدّل النص والمدة، صدّر MP4 وافتح TikTok للنشر.", 12, 0xFFCBECE5, false);
        sub.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams sp = Ui.matchWrap();
        sp.setMargins(0, Ui.dp(this, 7), 0, 0);
        hero.addView(sub, sp);
        root.addView(hero);

        TextView qt = Ui.sectionTitle(this, "☾ قرآن كريم — أونلاين");
        LinearLayout.LayoutParams qtp = Ui.matchWrap();
        qtp.setMargins(0, Ui.dp(this, 22), 0, Ui.dp(this, 9));
        root.addView(qt, qtp);
        root.addView(quranCard());

        TextView other = Ui.sectionTitle(this, "محتوى إسلامي آخر");
        LinearLayout.LayoutParams op = Ui.matchWrap();
        op.setMargins(0, Ui.dp(this, 22), 0, Ui.dp(this, 9));
        root.addView(other, op);

        root.addView(contentButton("◉  أذكار", "ذكر واستغفار", "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "اجعل لسانك رطبًا بذكر الله", "#أذكار #استغفار #اسلاميات #ذكر"));
        root.addView(contentButton("🤲  دعاء", "دعاء من القرآن", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "آمين يا رب العالمين", "#دعاء #قرآن #اسلاميات #دعاء_اليوم"));
        root.addView(contentButton("❝  حديث صحيح", "إنما الأعمال بالنيات", "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى", "متفق عليه", "#حديث #السنة #اسلاميات #تذكير"));
        root.addView(contentButton("✦  تذكير", "صلِّ على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ ﷺ", "شاركها لعلها تكون لك أجرًا", "#الصلاة_على_النبي #اسلاميات #تذكير"));

        Button oldLibrary = Ui.secondary(this, "▦  فتح مكتبة القوالب الإسلامية الحالية");
        oldLibrary.setOnClickListener(v -> {
            Intent i = new Intent(this, TemplateLibraryActivity.class);
            i.putExtra("category", "islamic");
            startActivity(i);
        });
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.height = Ui.dp(this, 54);
        lp.setMargins(0, Ui.dp(this, 12), 0, 0);
        root.addView(oldLibrary, lp);

        TextView note = Ui.text(this, "في نسخة الاختبار: القرآن يُجلب من Al Quran Cloud، والتلاوة تُحمّل للآيات المختارة ثم تُجهز داخل المشروع. اختر حتى 6 آيات في الريل الواحد.", 11, Ui.MUTED, false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = Ui.matchWrap();
        np.setMargins(0, Ui.dp(this, 16), 0, 0);
        root.addView(note, np);

        return scroll;
    }

    private View quranCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "اختيار التلاوة", 17, Ui.TEXT, true));

        surahButton = Ui.secondary(this, "السورة: " + SURAH_NAMES[surahIndex]);
        surahButton.setOnClickListener(v -> chooseSurah());
        LinearLayout.LayoutParams sb = Ui.matchWrap();
        sb.height = Ui.dp(this, 50);
        sb.setMargins(0, Ui.dp(this, 10), 0, 0);
        card.addView(surahButton, sb);

        reciterButton = Ui.secondary(this, "القارئ: " + RECITER_NAMES[reciterIndex]);
        reciterButton.setOnClickListener(v -> chooseReciter());
        LinearLayout.LayoutParams rb = Ui.matchWrap();
        rb.height = Ui.dp(this, 50);
        rb.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(reciterButton, rb);

        LinearLayout range = new LinearLayout(this);
        range.setOrientation(LinearLayout.HORIZONTAL);
        fromAyah = Ui.input(this, "من آية", false);
        fromAyah.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        fromAyah.setText("1");
        range.addView(fromAyah, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        toAyah = Ui.input(this, "إلى آية", false);
        toAyah.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        toAyah.setText("3");
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f);
        tp.setMargins(Ui.dp(this, 8), 0, 0, 0);
        range.addView(toAyah, tp);
        LinearLayout.LayoutParams rp = Ui.matchWrap();
        rp.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(range, rp);

        createQuranButton = Ui.accent(this, "▶  تجهيز Reel القرآن");
        createQuranButton.setTextSize(16);
        createQuranButton.setOnClickListener(v -> prepareQuran());
        LinearLayout.LayoutParams cp = Ui.matchWrap();
        cp.height = Ui.dp(this, 58);
        cp.setMargins(0, Ui.dp(this, 10), 0, 0);
        card.addView(createQuranButton, cp);

        quranStatus = Ui.text(this, "جاهز — بعد التجهيز سيُفتح المحرر لاختيار الخلفية", 11, Ui.GREEN, true);
        quranStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qs = Ui.matchWrap();
        qs.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(quranStatus, qs);
        return card;
    }

    private Button contentButton(String label, String hook, String body, String cta, String hashtags) {
        Button b = Ui.secondary(this, label);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setOnClickListener(v -> openSimpleProject(label.replaceAll("^[^ ]+\\s+", ""), hook, body, cta, hashtags));
        LinearLayout.LayoutParams p = Ui.matchWrap();
        p.height = Ui.dp(this, 56);
        p.setMargins(0, 0, 0, Ui.dp(this, 8));
        b.setLayoutParams(p);
        return b;
    }

    private void chooseSurah() {
        new AlertDialog.Builder(this)
                .setTitle("اختار السورة")
                .setSingleChoiceItems(SURAH_NAMES, surahIndex, (d, which) -> {
                    surahIndex = which;
                    surahButton.setText("السورة: " + SURAH_NAMES[which]);
                    d.dismiss();
                }).show();
    }

    private void chooseReciter() {
        new AlertDialog.Builder(this)
                .setTitle("اختار القارئ")
                .setSingleChoiceItems(RECITER_NAMES, reciterIndex, (d, which) -> {
                    reciterIndex = which;
                    reciterButton.setText("القارئ: " + RECITER_NAMES[which]);
                    d.dismiss();
                }).show();
    }

    private void prepareQuran() {
        int from = number(fromAyah, 1);
        int to = number(toAyah, from);
        if (from < 1 || to < from) {
            toast("راجع أرقام الآيات");
            return;
        }
        if (to - from + 1 > 6) {
            toast("للتجربة اختار حتى 6 آيات في الريل الواحد");
            return;
        }
        setQuranBusy(true, "جاري تحميل نص الآيات والتلاوة…");
        final int surah = surahIndex + 1;
        final int reciter = reciterIndex;
        executor.execute(() -> {
            try {
                JSONObject root = getJson("https://api.alquran.cloud/v1/surah/" + surah + "/" + RECITER_IDS[reciter]);
                JSONObject data = root.optJSONObject("data");
                if (data == null) throw new IllegalStateException("تعذر قراءة بيانات السورة");
                JSONArray ayahs = data.optJSONArray("ayahs");
                if (ayahs == null || ayahs.length() == 0) throw new IllegalStateException("لم تصل آيات من المصدر");
                if (to > ayahs.length()) throw new IllegalArgumentException("السورة تحتوي على " + ayahs.length() + " آية فقط");

                File dir = new File(getFilesDir(), "islamic_reels/quran_" + System.currentTimeMillis());
                if (!dir.mkdirs() && !dir.exists()) throw new IllegalStateException("تعذر إنشاء ملفات المشروع");

                List<File> audioFiles = new ArrayList<>();
                StringBuilder body = new StringBuilder();
                for (int i = from - 1; i < to; i++) {
                    JSONObject ayah = ayahs.optJSONObject(i);
                    if (ayah == null) continue;
                    int numberInSurah = ayah.optInt("numberInSurah", i + 1);
                    String text = ayah.optString("text", "").trim();
                    if (!text.isEmpty()) {
                        if (body.length() > 0) body.append("\n");
                        body.append(text).append(" ﴿").append(numberInSurah).append("﴾");
                    }
                    String audio = ayah.optString("audio", "").trim();
                    if (audio.isEmpty()) {
                        int global = ayah.optInt("number", 0);
                        if (global > 0) audio = "https://cdn.islamic.network/quran/audio/64/" + RECITER_IDS[reciter] + "/" + global + ".mp3";
                    }
                    if (audio.isEmpty()) throw new IllegalStateException("تعذر إيجاد ملف التلاوة للآية " + numberInSurah);
                    File part = new File(dir, String.format(java.util.Locale.US, "ayah_%03d.mp3", numberInSurah));
                    download(audio, part);
                    audioFiles.add(part);
                    final int progress = numberInSurah;
                    runOnUiThread(() -> quranStatus.setText("تم تحميل الآية " + progress + "…"));
                }

                if (audioFiles.isEmpty()) throw new IllegalStateException("لم يتم تحميل أي تلاوة");
                File joined = new File(dir, "recitation.m4a");
                runOnUiThread(() -> quranStatus.setText("جاري دمج التلاوة وتجهيز المشروع…"));
                ReelExporter.concatAudio(this, audioFiles, joined);
                if (!joined.exists() || joined.length() < 1024) throw new IllegalStateException("تعذر تجهيز ملف التلاوة");

                CreatorProject p = CreatorProject.fromTemplate(TemplateCatalog.byId("islamic-0"));
                p.id = "quran-" + System.currentTimeMillis();
                p.templateId = "quran-online";
                p.categoryKey = "islamic";
                p.categoryName = "القرآن الكريم";
                p.name = "سورة " + SURAH_NAMES[surahIndex] + " • " + from + "-" + to;
                p.hook = "﴿ سورة " + SURAH_NAMES[surahIndex] + " ﴾";
                p.body = body.toString();
                p.cta = RECITER_NAMES[reciter] + " • القرآن الكريم";
                p.hashtags = "#قرآن #القرآن_الكريم #تلاوة #اسلاميات #Quran";
                p.audioUri = joined.toURI().toString();
                p.durationSec = audioDuration(joined);
                p.startColor = 0xFF0A665A;
                p.endColor = 0xFF071C2B;
                DraftStore.save(this, p);

                runOnUiThread(() -> {
                    setQuranBusy(false, "تم تجهيز التلاوة ✓ اختار الخلفية وابدأ المونتاج");
                    Intent i = new Intent(this, TemplateEditorActivity.class);
                    i.putExtra("draft_id", p.id);
                    i.putExtra("pick_media", true);
                    startActivity(i);
                });
            } catch (Throwable e) {
                String m = e.getMessage() == null ? "تعذر تجهيز Reel القرآن" : e.getMessage();
                runOnUiThread(() -> {
                    setQuranBusy(false, m);
                    toast(m);
                });
            }
        });
    }

    private void openSimpleProject(String name, String hook, String body, String cta, String hashtags) {
        CreatorProject p = CreatorProject.fromTemplate(TemplateCatalog.byId("islamic-0"));
        p.id = "islamic-" + System.currentTimeMillis();
        p.categoryName = "ريلز إسلامي";
        p.name = name;
        p.hook = hook;
        p.body = body;
        p.cta = cta;
        p.hashtags = hashtags;
        p.durationSec = 15;
        p.startColor = 0xFF0B665C;
        p.endColor = 0xFF071C2B;
        DraftStore.save(this, p);
        Intent i = new Intent(this, TemplateEditorActivity.class);
        i.putExtra("draft_id", p.id);
        i.putExtra("pick_media", true);
        startActivity(i);
    }

    private void setQuranBusy(boolean busy, String message) {
        createQuranButton.setEnabled(!busy);
        createQuranButton.setAlpha(busy ? .55f : 1f);
        createQuranButton.setText(busy ? "جاري التجهيز…" : "▶  تجهيز Reel القرآن");
        quranStatus.setText(message);
        quranStatus.setTextColor(busy ? Ui.YELLOW : Ui.GREEN);
    }

    private int number(EditText e, int fallback) {
        try {
            String s = e.getText() == null ? "" : e.getText().toString().trim();
            return s.isEmpty() ? fallback : Integer.parseInt(s);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private JSONObject getJson(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "DownloadHub-IslamicReels/1.0");
        try {
            int code = c.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            if (in == null) throw new IllegalStateException("خطأ اتصال " + code);
            StringBuilder b = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) b.append(line);
            }
            JSONObject o = new JSONObject(b.toString());
            if (code < 200 || code >= 300 || o.optInt("code", 200) != 200) throw new IllegalStateException("مصدر القرآن لم يستجب الآن");
            return o;
        } finally {
            c.disconnect();
        }
    }

    private void download(String url, File out) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "DownloadHub-IslamicReels/1.0");
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("فشل تحميل التلاوة: " + code);
            try (InputStream in = new BufferedInputStream(c.getInputStream()); FileOutputStream f = new FileOutputStream(out)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) f.write(buf, 0, n);
            }
        } finally {
            c.disconnect();
        }
    }

    private int audioDuration(File f) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int sec = ms == null ? 20 : (int) Math.ceil(Long.parseLong(ms) / 1000.0);
            return Math.max(6, Math.min(60, sec));
        } catch (Exception e) {
            return 20;
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
