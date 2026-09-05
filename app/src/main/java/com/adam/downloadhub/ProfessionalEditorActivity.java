package com.adam.downloadhub;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfessionalEditorActivity extends Activity {
    private static final int PICK_MEDIA = 901;
    private static final int PICK_AUDIO = 902;
    private static final int PICK_OVERLAY = 903;
    private static final int REQ_MIC = 904;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService thumbExecutor = Executors.newFixedThreadPool(2);
    private final ArrayDeque<String> undoStack = new ArrayDeque<>();
    private final ArrayDeque<String> redoStack = new ArrayDeque<>();

    private CreatorProject project;

    private FrameLayout previewStage;
    private FrameLayout previewFrame;
    private VideoView videoPreview;
    private ImageView imagePreview;
    private ImageView overlayPreview;
    private LinearLayout previewTextBox;
    private TextView previewHook;
    private TextView previewBody;
    private TextView previewCta;
    private TextView timeLabel;
    private TextView statusLabel;
    private TextView timelineTitle;
    private LinearLayout clipStrip;
    private LinearLayout layersRow;
    private SeekBar projectSeek;
    private Button playButton;
    private Button exportButton;

    private MediaPlayer activeVideoPlayer;
    private MediaPlayer musicPlayer;
    private MediaPlayer voicePlayer;
    private MediaRecorder voiceRecorder;
    private File voiceFile;

    private boolean prepared = false;
    private boolean selectedIsVideo = false;
    private boolean playing = false;
    private boolean seekTouching = false;
    private boolean autoStartAfterBind = false;
    private boolean voiceRecording = false;
    private long rawDurationMs = 0;
    private long imageOutputPositionMs = 0;
    private long imagePlayStartedAt = 0;
    private long pendingRawSeekMs = -1;
    private long voiceRecordOffsetMs = 0;

    private final float[] speeds = {.5f, .75f, 1f, 1.25f, 1.5f, 2f};
    private final String[] filters = {"None", "Vivid", "Mono", "Soft", "Contrast"};
    private final String[] filterLabels = {"بدون فلتر", "Vivid", "أبيض وأسود", "Soft", "Contrast"};
    private final String[] fontKeys = {"Sans", "Medium", "Condensed", "Black", "Serif", "Mono"};
    private final String[] fontLabels = {"عصري Sans", "متوسط Medium", "مضغوط Condensed", "عريض Black", "Serif", "Monospace"};
    private final String[] ratios = {"9:16", "1:1", "16:9"};
    private final String[] transitions = {"None", "Fade", "Soft"};
    private final int[] textColors = {Color.WHITE, 0xFF8DEBFF, 0xFFFFE38A, 0xFFFFA7D7, 0xFF9DFFB1, 0xFFFF8A80, 0xFFC7A7FF, 0xFFB0BEC5};
    private final String[] textColorLabels = {"أبيض", "سماوي", "ذهبي", "وردي", "أخضر", "أحمر فاتح", "بنفسجي", "رمادي"};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        loadProject();
        setContentView(buildWorkspace());
        applyProjectVisuals();
        refreshTimeline();
        bindCurrentClip();
        handler.post(ticker);
        if (getIntent().getBooleanExtra("pick_media", false) && project.clips.isEmpty()) {
            previewStage.postDelayed(this::pickMedia, 250);
        }
    }

    @Override protected void onPause() {
        if (voiceRecording) stopVoiceover();
        stopPlayback(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseAuxPlayers();
        releaseRecorder();
        try { if (videoPreview != null) videoPreview.stopPlayback(); } catch (Exception ignored) {}
        exportExecutor.shutdownNow();
        thumbExecutor.shutdownNow();
        super.onDestroy();
    }

    private void loadProject() {
        String draft = getIntent().getStringExtra("draft_id");
        if (draft != null) project = DraftStore.get(this, draft);
        if (project == null) {
            String templateId = getIntent().getStringExtra("template_id");
            ReelTemplate t = TemplateCatalog.byId(templateId);
            project = CreatorProject.fromTemplate(t);
            if (t == null || "blank".equals(templateId)) {
                project.categoryKey = "creator";
                project.categoryName = "مشروع جديد";
                project.name = "مشروع جديد";
                project.hook = "";
                project.body = "";
                project.cta = "";
                project.hashtags = "";
            }
        }
        project.ensureClips();
    }

    private View buildWorkspace() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 9), Ui.dp(this, 6), Ui.dp(this, 9), Ui.dp(this, 6));
        page.setBackground(Ui.gradient(Ui.BG, Ui.BG_2, 0, this));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = Ui.ghost(this, "‹");
        back.setTextSize(24);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));

        TextView title = Ui.text(this, "Video Studio Pro", 18, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f);
        titleP.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        top.addView(title, titleP);

        Button undo = Ui.ghost(this, "↶");
        undo.setOnClickListener(v -> undo());
        top.addView(undo, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        Button redo = Ui.ghost(this, "↷");
        redo.setOnClickListener(v -> redo());
        top.addView(redo, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        exportButton = Ui.accent(this, "تصدير");
        exportButton.setTextSize(12);
        exportButton.setOnClickListener(v -> exportProject());
        top.addView(exportButton, new LinearLayout.LayoutParams(Ui.dp(this, 78), Ui.dp(this, 40)));
        page.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));

        float density = getResources().getDisplayMetrics().density;
        int screenDp = Math.round(getResources().getDisplayMetrics().heightPixels / density);
        int previewHeight = Math.max(220, Math.min(310, screenDp - 340));

        previewStage = new FrameLayout(this);
        previewStage.setBackground(Ui.bordered(0xFF010409, Ui.BORDER_SOFT, 1, 20, this));
        previewStage.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout.LayoutParams stageP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, previewHeight));
        stageP.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        page.addView(previewStage, stageP);

        previewFrame = new FrameLayout(this);
        previewFrame.setBackgroundColor(Color.BLACK);
        previewFrame.setClipToOutline(true);
        previewFrame.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        FrameLayout.LayoutParams pf = new FrameLayout.LayoutParams(Ui.dp(this, 168), Ui.dp(this, 298), Gravity.CENTER);
        previewStage.addView(previewFrame, pf);

        videoPreview = new VideoView(this);
        videoPreview.setVisibility(View.GONE);
        videoPreview.setOnClickListener(v -> togglePlay());
        previewFrame.addView(videoPreview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);
        imagePreview.setOnClickListener(v -> togglePlay());
        previewFrame.addView(imagePreview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        TextView empty = Ui.text(this, "＋\nأضف فيديو أو صورة", 15, Ui.MUTED, true);
        empty.setGravity(Gravity.CENTER);
        empty.setTag("empty-preview");
        empty.setOnClickListener(v -> pickMedia());
        previewFrame.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlayPreview = new ImageView(this);
        overlayPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        overlayPreview.setVisibility(View.GONE);
        previewFrame.addView(overlayPreview, new FrameLayout.LayoutParams(Ui.dp(this, 90), Ui.dp(this, 90), Gravity.TOP | Gravity.START));

        previewTextBox = new LinearLayout(this);
        previewTextBox.setOrientation(LinearLayout.VERTICAL);
        previewTextBox.setGravity(Gravity.CENTER);
        previewTextBox.setPadding(Ui.dp(this, 10), Ui.dp(this, 7), Ui.dp(this, 10), Ui.dp(this, 7));
        previewTextBox.setOnClickListener(v -> showTextMenu());
        FrameLayout.LayoutParams textP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        textP.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        previewFrame.addView(previewTextBox, textP);

        previewHook = Ui.text(this, project.hook, 21, project.textColor, true);
        previewHook.setGravity(Gravity.CENTER);
        previewHook.setShadowLayer(5, 0, 2, Color.BLACK);
        previewTextBox.addView(previewHook, Ui.matchWrap());
        previewBody = Ui.text(this, project.body, 13, project.textColor, false);
        previewBody.setGravity(Gravity.CENTER);
        previewBody.setMaxLines(5);
        previewBody.setShadowLayer(4, 0, 2, Color.BLACK);
        LinearLayout.LayoutParams bodyP = Ui.matchWrap();
        bodyP.setMargins(0, Ui.dp(this, 4), 0, 0);
        previewTextBox.addView(previewBody, bodyP);
        previewCta = Ui.text(this, project.cta, 11, project.textColor, true);
        previewCta.setGravity(Gravity.CENTER);
        previewCta.setShadowLayer(4, 0, 2, Color.BLACK);
        LinearLayout.LayoutParams ctaP = Ui.matchWrap();
        ctaP.setMargins(0, Ui.dp(this, 5), 0, 0);
        previewTextBox.addView(previewCta, ctaP);

        LinearLayout transport = new LinearLayout(this);
        transport.setOrientation(LinearLayout.HORIZONTAL);
        transport.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = Ui.ghost(this, "‹");
        prev.setOnClickListener(v -> selectRelative(-1));
        transport.addView(prev, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 40)));
        Button back1 = Ui.ghost(this, "-1s");
        back1.setOnClickListener(v -> seekRelativeProject(-1000));
        transport.addView(back1, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 40)));
        playButton = Ui.accent(this, "▶");
        playButton.setTextSize(17);
        playButton.setOnClickListener(v -> togglePlay());
        LinearLayout.LayoutParams playP = new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1f);
        playP.setMargins(Ui.dp(this, 5), 0, Ui.dp(this, 5), 0);
        transport.addView(playButton, playP);
        Button next1 = Ui.ghost(this, "+1s");
        next1.setOnClickListener(v -> seekRelativeProject(1000));
        transport.addView(next1, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 40)));
        Button next = Ui.ghost(this, "›");
        next.setOnClickListener(v -> selectRelative(1));
        transport.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 40)));
        page.addView(transport, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 42)));

        timeLabel = Ui.text(this, "00:00.0 / 00:00.0", 10, Ui.CYAN, true);
        timeLabel.setGravity(Gravity.CENTER);
        page.addView(timeLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 18)));

        projectSeek = new SeekBar(this);
        projectSeek.setMax(1000);
        projectSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) seekProjectTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { seekTouching = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { seekTouching = false; }
        });
        page.addView(projectSeek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 30)));

        LinearLayout timelineCard = Ui.card(this);
        timelineCard.setPadding(Ui.dp(this, 8), Ui.dp(this, 5), Ui.dp(this, 8), Ui.dp(this, 5));
        timelineTitle = Ui.text(this, "Timeline", 10, Ui.MUTED, true);
        timelineCard.addView(timelineTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 18)));

        FrameLayout timelineViewport = new FrameLayout(this);
        HorizontalScrollView clipScroll = new HorizontalScrollView(this);
        clipScroll.setHorizontalScrollBarEnabled(false);
        clipScroll.setFillViewport(false);
        clipStrip = new LinearLayout(this);
        clipStrip.setOrientation(LinearLayout.HORIZONTAL);
        clipStrip.setGravity(Gravity.CENTER_VERTICAL);
        clipStrip.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));
        clipScroll.addView(clipStrip, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        timelineViewport.addView(clipScroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View playhead = new View(this);
        playhead.setBackgroundColor(Ui.CYAN);
        FrameLayout.LayoutParams ph = new FrameLayout.LayoutParams(Ui.dp(this, 2), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        timelineViewport.addView(playhead, ph);
        timelineCard.addView(timelineViewport, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));

        layersRow = new LinearLayout(this);
        layersRow.setOrientation(LinearLayout.HORIZONTAL);
        layersRow.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView layerScroll = new HorizontalScrollView(this);
        layerScroll.setHorizontalScrollBarEnabled(false);
        layerScroll.addView(layersRow, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        timelineCard.addView(layerScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 32)));
        page.addView(timelineCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 118)));

        statusLabel = Ui.text(this, "جاهز للمونتاج", 10, Ui.GREEN, true);
        statusLabel.setGravity(Gravity.CENTER);
        page.addView(statusLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 21)));

        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(0, Ui.dp(this, 3), 0, 0);
        toolsScroll.addView(tools, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tools.addView(toolButton("✂", "تعديل", v -> showEditMenu()));
        tools.addView(toolButton("T", "نص", v -> showTextMenu()));
        tools.addView(toolButton("♫", "صوت", v -> showAudioMenu()));
        tools.addView(toolButton("◇", "Overlay", v -> showOverlayMenu()));
        tools.addView(toolButton("✦", "Effects", v -> showEffectsMenu()));
        tools.addView(toolButton("▣", "Canvas", v -> showCanvasMenu()));
        tools.addView(toolButton("＋", "ميديا", v -> pickMedia()));
        page.addView(toolsScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 64)));

        previewStage.post(this::updatePreviewBounds);
        return page;
    }

    private View toolButton(String icon, String label, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(Ui.bordered(Ui.SURFACE_2, Ui.BORDER_SOFT, 1, 14, this));
        box.setOnClickListener(click);
        TextView i = Ui.text(this, icon, 18, Ui.CYAN, true);
        i.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 31)));
        TextView l = Ui.text(this, label, 10, Ui.TEXT, true);
        l.setGravity(Gravity.CENTER);
        box.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 25)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(this, 78), Ui.dp(this, 58));
        p.setMargins(0, 0, Ui.dp(this, 6), 0);
        box.setLayoutParams(p);
        return box;
    }

    private void showEditMenu() {
        if (!requireClip()) return;
        EditorSheets.menu(this, "تعديل المقطع", new String[]{
                "✂  تقسيم عند المؤشر", "◁  تعيين بداية القص", "▷  تعيين نهاية القص",
                "⏱  السرعة", "🔊  صوت المقطع", "⧉  نسخ المقطع",
                "←  تحريك لليسار", "→  تحريك لليمين", "⌫  حذف المقطع", "↺  إعادة ضبط المقطع"
        }, new Runnable[]{
                this::splitClip, this::setTrimStart, this::setTrimEnd,
                this::showSpeedSheet, this::showClipVolumeSheet, this::duplicateClip,
                () -> moveClip(-1), () -> moveClip(1), this::deleteClip, this::resetClip
        });
    }

    private void showEffectsMenu() {
        if (!requireClip()) return;
        EditorSheets.menu(this, "المؤثرات والتحويل", new String[]{
                "✦  الفلاتر", "🔍  Zoom", "↻  تدوير 90°", "⇋  Mirror"
        }, new Runnable[]{this::showFilterSheet, this::showZoomSheet, this::rotateClip, this::toggleMirror});
    }

    private void showTextMenu() {
        EditorSheets.menu(this, "النص", new String[]{
                "T  تحرير النص", "Aa  الخط", "●  اللون", "▤  شكل النص",
                "↕  حجم النص", "⌖  مكان النص", project.showText ? "◉  إخفاء النص" : "○  إظهار النص"
        }, new Runnable[]{
                this::showTextEditor, this::showFontSheet, this::showTextColorSheet, this::showCaptionStyleSheet,
                this::showTextSizeSheet, this::showTextPositionSheet, this::toggleText
        });
    }

    private void showAudioMenu() {
        EditorSheets.menu(this, "الصوت", new String[]{
                "♫  اختيار موسيقى", "🔊  صوت الفيديو الأصلي", "♪  صوت الموسيقى",
                "⏱  توقيت بداية الموسيقى", "↗  Fade In للموسيقى", "↘  Fade Out للموسيقى",
                voiceRecording ? "■  إيقاف Voiceover" : "●  تسجيل Voiceover", "🎙  صوت Voiceover",
                "⌫  إزالة الموسيقى", "⌫  إزالة Voiceover"
        }, new Runnable[]{
                this::pickAudio, this::showClipVolumeSheet, this::showMusicVolumeSheet,
                this::showMusicOffsetSheet, () -> showMusicFadeSheet(true), () -> showMusicFadeSheet(false),
                this::toggleVoiceover, this::showVoiceVolumeSheet, this::removeMusic, this::removeVoice
        });
    }

    private void showOverlayMenu() {
        EditorSheets.menu(this, "Overlay / Logo", new String[]{
                "◇  اختيار صورة أو Logo", "↔  الحجم", "↔  المكان أفقي", "↕  المكان رأسي",
                "◐  الشفافية", "⏱  البداية عند المؤشر", "⏱  النهاية عند المؤشر", "⌫  إزالة Overlay"
        }, new Runnable[]{
                this::pickOverlay, this::showOverlayScaleSheet, this::showOverlayXSheet, this::showOverlayYSheet,
                this::showOverlayOpacitySheet, () -> setOverlayTiming(true), () -> setOverlayTiming(false), this::removeOverlay
        });
    }

    private void showCanvasMenu() {
        EditorSheets.menu(this, "Canvas & Project", new String[]{
                "▣  المقاس / Ratio", "⇄  الانتقال بين المقاطع", "↗  Fade In للمشروع", "↘  Fade Out للمشروع",
                "💾  حفظ المشروع كمسودة"
        }, new Runnable[]{this::showRatioSheet, this::showTransitionSheet, () -> showProjectFadeSheet(true), () -> showProjectFadeSheet(false), () -> saveDraft(true)});
    }

    private void showSpeedSheet() {
        EditorClip c = currentClip(); if (c == null) return;
        String[] labels = {"0.5×", "0.75×", "1.0×", "1.25×", "1.5×", "2.0×"};
        int selected = nearestSpeed(c.speed);
        EditorSheets.choice(this, "السرعة", labels, selected, null, i -> {
            pushUndo(); c.speed = speeds[i]; applyPlaybackSpeed(); saveDraft(false); refreshTimeline(); action("السرعة " + labels[i]);
        });
    }

    private void showFilterSheet() {
        EditorClip c = currentClip(); if (c == null) return;
        int selected = indexOf(filters, c.filter);
        EditorSheets.choice(this, "الفلاتر", filterLabels, selected, null, i -> {
            pushUndo(); c.filter = filters[i]; saveDraft(false); applyClipTransform(); refreshTimeline(); action("Filter: " + filterLabels[i]);
        });
    }

    private void showZoomSheet() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo();
        EditorSheets.slider(this, "Zoom", 100, 180, c.zoom, "%", v -> {
            c.zoom = v; applyClipTransform(); saveDraft(false);
        });
    }

    private void showClipVolumeSheet() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo();
        EditorSheets.slider(this, "صوت المقطع", 0, 100, c.volume, "%", v -> {
            c.volume = v; applyVideoVolume(); saveDraft(false);
        });
    }

    private void showMusicVolumeSheet() {
        pushUndo();
        EditorSheets.slider(this, "صوت الموسيقى", 0, 100, project.audioVolume, "%", v -> {
            project.audioVolume = v; applyAuxVolumes(); saveDraft(false); refreshLayers();
        });
    }

    private void showVoiceVolumeSheet() {
        pushUndo();
        EditorSheets.slider(this, "صوت Voiceover", 0, 100, project.voiceoverVolume, "%", v -> {
            project.voiceoverVolume = v; applyAuxVolumes(); saveDraft(false); refreshLayers();
        });
    }

    private void showMusicOffsetSheet() {
        int sec = (int) Math.max(-30, Math.min(30, project.audioOffsetMs / 1000));
        pushUndo();
        EditorSheets.slider(this, "بداية الموسيقى", -30, 30, sec, "s", v -> {
            project.audioOffsetMs = v * 1000L; saveDraft(false); syncAux(false); refreshLayers();
        });
    }

    private void showMusicFadeSheet(boolean in) {
        String[] labels = {"بدون", "0.5 ثانية", "1 ثانية", "2 ثانية"};
        int[] values = {0, 500, 1000, 2000};
        int current = in ? project.musicFadeInMs : project.musicFadeOutMs;
        int selected = closest(values, current);
        EditorSheets.choice(this, in ? "Music Fade In" : "Music Fade Out", labels, selected, null, i -> {
            pushUndo(); if (in) project.musicFadeInMs = values[i]; else project.musicFadeOutMs = values[i]; saveDraft(false); action(labels[i]);
        });
    }

    private void showProjectFadeSheet(boolean in) {
        String[] labels = {"بدون", "0.25 ثانية", "0.5 ثانية", "1 ثانية"};
        int[] values = {0, 250, 500, 1000};
        int current = in ? project.fadeInMs : project.fadeOutMs;
        int selected = closest(values, current);
        EditorSheets.choice(this, in ? "Project Fade In" : "Project Fade Out", labels, selected, null, i -> {
            pushUndo(); if (in) project.fadeInMs = values[i]; else project.fadeOutMs = values[i]; saveDraft(false); action(labels[i]);
        });
    }

    private void showRatioSheet() {
        int selected = indexOf(ratios, project.aspectRatio);
        EditorSheets.choice(this, "مقاس الفيديو", ratios, selected, null, i -> {
            pushUndo(); project.aspectRatio = ratios[i]; updatePreviewBounds(); saveDraft(false); action("المقاس " + ratios[i]);
        });
    }

    private void showTransitionSheet() {
        String[] labels = {"بدون Transition", "Fade", "Soft Fade"};
        int selected = indexOf(transitions, project.transitionStyle);
        EditorSheets.choice(this, "الانتقالات", labels, selected, null, i -> {
            pushUndo(); project.transitionStyle = transitions[i]; saveDraft(false); action(labels[i]);
        });
    }

    private void showFontSheet() {
        Typeface[] faces = new Typeface[fontKeys.length];
        for (int i = 0; i < faces.length; i++) faces[i] = typefaceFor(fontKeys[i]);
        int selected = indexOf(fontKeys, project.textFont);
        EditorSheets.choice(this, "اختيار الخط", fontLabels, selected, faces, i -> {
            pushUndo(); project.textFont = fontKeys[i]; applyTextStyle(); saveDraft(false); action("الخط: " + fontLabels[i]);
        });
    }

    private void showTextColorSheet() {
        int selected = 0;
        for (int i = 0; i < textColors.length; i++) if (textColors[i] == project.textColor) selected = i;
        EditorSheets.choice(this, "لون النص", textColorLabels, selected, null, i -> {
            pushUndo(); project.textColor = textColors[i]; applyTextStyle(); saveDraft(false); action("لون النص: " + textColorLabels[i]);
        });
    }

    private void showCaptionStyleSheet() {
        String[] keys = {"Bold Highlight", "Clean", "Subtitle"};
        String[] labels = {"Bold Highlight", "Clean", "Subtitle"};
        int selected = indexOf(keys, project.captionStyle);
        EditorSheets.choice(this, "شكل النص", labels, selected, null, i -> {
            pushUndo(); project.captionStyle = keys[i]; applyTextStyle(); saveDraft(false); action(labels[i]);
        });
    }

    private void showTextSizeSheet() {
        pushUndo();
        EditorSheets.slider(this, "حجم النص", 60, 180, project.textScale, "%", v -> {
            project.textScale = v; applyTextStyle(); saveDraft(false);
        });
    }

    private void showTextPositionSheet() {
        pushUndo();
        EditorSheets.slider(this, "مكان النص رأسيًا", 10, 90, project.textYPercent, "%", v -> {
            project.textYPercent = v; applyTextStyle(); saveDraft(false);
        });
    }

    private void showTextEditor() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, 0, 0, Ui.dp(this, 6));
        scroll.addView(form, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText hook = addField(form, "العنوان", project.hook, true);
        EditText body = addField(form, "النص", project.body, true);
        EditText cta = addField(form, "النهاية / CTA", project.cta, true);
        EditText tags = addField(form, "Hashtags", project.hashtags, true);
        Button save = Ui.accent(this, "حفظ النص");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        sp.setMargins(0, Ui.dp(this, 10), 0, 0);
        form.addView(save, sp);

        Dialog d = EditorSheets.open(this, "تحرير النص", scroll);
        save.setOnClickListener(v -> {
            pushUndo();
            project.hook = value(hook);
            project.body = value(body);
            project.cta = value(cta);
            project.hashtags = value(tags);
            project.showText = true;
            applyTextStyle();
            saveDraft(false);
            refreshLayers();
            action("تم تحديث النص");
            d.dismiss();
        });
    }

    private EditText addField(LinearLayout parent, String label, String text, boolean multiline) {
        TextView l = Ui.text(this, label, 11, Ui.MUTED, true);
        LinearLayout.LayoutParams lp = Ui.matchWrap(); lp.setMargins(0, Ui.dp(this, 7), 0, Ui.dp(this, 4));
        parent.addView(l, lp);
        EditText e = Ui.input(this, "", multiline);
        e.setText(text == null ? "" : text);
        LinearLayout.LayoutParams ep = Ui.matchWrap(); ep.height = Ui.dp(this, multiline ? 74 : 46);
        parent.addView(e, ep);
        return e;
    }

    private void showOverlayScaleSheet() { pushUndo(); EditorSheets.slider(this, "حجم Overlay", 10, 100, project.overlayScale, "%", v -> { project.overlayScale = v; applyOverlayPreview(); saveDraft(false); }); }
    private void showOverlayXSheet() { pushUndo(); EditorSheets.slider(this, "المكان أفقي", 0, 100, project.overlayXPercent, "%", v -> { project.overlayXPercent = v; applyOverlayPreview(); saveDraft(false); }); }
    private void showOverlayYSheet() { pushUndo(); EditorSheets.slider(this, "المكان رأسي", 0, 100, project.overlayYPercent, "%", v -> { project.overlayYPercent = v; applyOverlayPreview(); saveDraft(false); }); }
    private void showOverlayOpacitySheet() { pushUndo(); EditorSheets.slider(this, "شفافية Overlay", 10, 100, project.overlayOpacity, "%", v -> { project.overlayOpacity = v; applyOverlayPreview(); saveDraft(false); }); }

    private void toggleText() {
        pushUndo(); project.showText = !project.showText; applyTextStyle(); saveDraft(false); refreshLayers(); action(project.showText ? "تم إظهار النص" : "تم إخفاء النص");
    }

    private void applyProjectVisuals() {
        applyTextStyle();
        previewStage.post(this::updatePreviewBounds);
        previewFrame.post(this::applyOverlayPreview);
    }

    private void applyTextStyle() {
        if (previewHook == null) return;
        float k = Math.max(.6f, Math.min(1.8f, project.textScale / 100f));
        Typeface face = typefaceFor(project.textFont);
        previewHook.setTypeface(face, Typeface.BOLD);
        previewBody.setTypeface(face, Typeface.NORMAL);
        previewCta.setTypeface(face, Typeface.BOLD);
        previewHook.setTextSize(21 * k);
        previewBody.setTextSize(13 * k);
        previewCta.setTextSize(11 * k);
        previewHook.setTextColor(project.textColor);
        previewBody.setTextColor(project.textColor);
        previewCta.setTextColor(project.textColor);
        previewHook.setText(project.hook == null ? "" : project.hook);
        previewBody.setText(project.body == null ? "" : project.body);
        previewCta.setText(project.cta == null ? "" : project.cta);
        previewTextBox.setVisibility(project.showText ? View.VISIBLE : View.GONE);

        if ("Clean".equals(project.captionStyle)) {
            previewTextBox.setBackgroundColor(Color.TRANSPARENT);
        } else if ("Subtitle".equals(project.captionStyle)) {
            previewTextBox.setBackground(Ui.bg(0x88000000, 10, this));
        } else {
            previewTextBox.setBackground(Ui.bordered(0x99030C19, 0x553CEBFF, 1, 14, this));
        }
        int h = previewFrame == null ? 0 : previewFrame.getHeight();
        float move = h <= 0 ? (project.textYPercent - 50) * 2.5f : (project.textYPercent - 50) * h / 100f;
        previewTextBox.setTranslationY(move);
    }

    private Typeface typefaceFor(String key) {
        if ("Medium".equals(key)) return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        if ("Condensed".equals(key)) return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        if ("Black".equals(key)) return Typeface.create("sans-serif-black", Typeface.NORMAL);
        if ("Serif".equals(key)) return Typeface.SERIF;
        if ("Mono".equals(key)) return Typeface.MONOSPACE;
        return Typeface.SANS_SERIF;
    }

    private void updatePreviewBounds() {
        if (previewStage == null || previewFrame == null) return;
        int sw = previewStage.getWidth() - Ui.dp(this, 16);
        int sh = previewStage.getHeight() - Ui.dp(this, 12);
        if (sw <= 0 || sh <= 0) return;
        int w, h;
        if ("1:1".equals(project.aspectRatio)) {
            w = h = Math.min(sw, sh);
        } else if ("16:9".equals(project.aspectRatio)) {
            w = Math.min(sw, Math.round(sh * 16f / 9f));
            h = Math.round(w * 9f / 16f);
        } else {
            h = sh;
            w = Math.round(h * 9f / 16f);
            if (w > sw) { w = sw; h = Math.round(w * 16f / 9f); }
        }
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(Math.max(1, w), Math.max(1, h), Gravity.CENTER);
        previewFrame.setLayoutParams(p);
        previewFrame.post(() -> { applyTextStyle(); applyOverlayPreview(); });
    }

    private EditorClip currentClip() {
        project.ensureClips();
        if (project.clips.isEmpty()) return null;
        if (project.selectedClipIndex < 0 || project.selectedClipIndex >= project.clips.size()) project.selectedClipIndex = 0;
        return project.clips.get(project.selectedClipIndex);
    }

    private boolean requireClip() {
        if (currentClip() != null) return true;
        action("أضف فيديو أو صورة أولاً");
        pickMedia();
        return false;
    }

    private void bindCurrentClip() {
        prepared = false;
        activeVideoPlayer = null;
        rawDurationMs = 0;
        selectedIsVideo = false;
        imageOutputPositionMs = 0;
        imagePlayStartedAt = 0;
        try { videoPreview.stopPlayback(); } catch (Exception ignored) {}
        videoPreview.setVisibility(View.GONE);
        imagePreview.setVisibility(View.GONE);
        View empty = previewFrame.findViewWithTag("empty-preview");

        EditorClip c = currentClip();
        if (c == null || c.uri == null || c.uri.trim().isEmpty()) {
            if (empty != null) empty.setVisibility(View.VISIBLE);
            updateProjectSeek();
            refreshTimeline();
            return;
        }
        if (empty != null) empty.setVisibility(View.GONE);
        Uri uri = Uri.parse(c.uri);
        selectedIsVideo = isVideo(uri, c.uri);

        if (selectedIsVideo) {
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoURI(uri);
            videoPreview.setOnPreparedListener(mp -> {
                activeVideoPlayer = mp;
                prepared = true;
                rawDurationMs = Math.max(1, videoPreview.getDuration());
                if (c.trimEndMs <= c.trimStartMs || c.trimEndMs > rawDurationMs) c.trimEndMs = rawDurationMs;
                applyVideoVolume();
                applyPlaybackSpeed();
                applyClipTransform();
                long target = pendingRawSeekMs >= 0 ? pendingRawSeekMs : c.trimStartMs;
                pendingRawSeekMs = -1;
                target = Math.max(c.trimStartMs, Math.min(effectiveEnd(c), target));
                videoPreview.seekTo((int) Math.min(Integer.MAX_VALUE, target));
                updateProjectSeek();
                refreshTimeline();
                if (autoStartAfterBind) { autoStartAfterBind = false; startSelectedPlayback(); }
            });
            videoPreview.setOnErrorListener((mp, what, extra) -> {
                prepared = false;
                action("تعذر تشغيل معاينة هذا الملف");
                if (playing) advanceClip();
                return true;
            });
        } else {
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageURI(uri);
            rawDurationMs = Math.max(500, c.stillDurationMs);
            applyClipTransform();
            if (autoStartAfterBind) { autoStartAfterBind = false; startSelectedPlayback(); }
        }
        applyTextStyle();
        applyOverlayPreview();
        updateProjectSeek();
        refreshTimeline();
    }

    private void togglePlay() {
        if (project.clips.isEmpty()) { pickMedia(); return; }
        if (playing) stopPlayback(false); else {
            playing = true;
            playButton.setText("❚❚");
            startSelectedPlayback();
        }
    }

    private void startSelectedPlayback() {
        EditorClip c = currentClip();
        if (c == null) { stopPlayback(false); return; }
        if (selectedIsVideo) {
            if (!prepared) { autoStartAfterBind = true; return; }
            long p = videoPreview.getCurrentPosition();
            if (p < c.trimStartMs || p >= effectiveEnd(c) - 60) videoPreview.seekTo((int)c.trimStartMs);
            applyPlaybackSpeed();
            applyVideoVolume();
            try { videoPreview.start(); } catch (Exception ignored) {}
        } else {
            imagePlayStartedAt = System.currentTimeMillis() - imageOutputPositionMs;
        }
        syncAux(true);
    }

    private void stopPlayback(boolean finished) {
        playing = false;
        autoStartAfterBind = false;
        try { if (videoPreview.isPlaying()) videoPreview.pause(); } catch (Exception ignored) {}
        pauseAux();
        if (playButton != null) playButton.setText("▶");
        if (finished) action("انتهت المعاينة");
    }

    private void advanceClip() {
        if (!playing) return;
        int next = project.selectedClipIndex + 1;
        if (next >= project.clips.size()) { stopPlayback(true); return; }
        project.selectedClipIndex = next;
        autoStartAfterBind = true;
        bindCurrentClip();
    }

    private void selectRelative(int delta) {
        if (project.clips.isEmpty()) return;
        int n = Math.max(0, Math.min(project.clips.size() - 1, project.selectedClipIndex + delta));
        if (n == project.selectedClipIndex) return;
        stopPlayback(false);
        project.selectedClipIndex = n;
        bindCurrentClip();
    }

    private void selectClip(int index) {
        if (index < 0 || index >= project.clips.size()) return;
        stopPlayback(false);
        project.selectedClipIndex = index;
        bindCurrentClip();
        saveDraft(false);
    }

    private void seekRelativeProject(long delta) {
        long total = totalProjectDurationMs();
        long target = Math.max(0, Math.min(total, projectPositionMs() + delta));
        seekProjectTo((int)Math.min(Integer.MAX_VALUE, target));
    }

    private void seekProjectTo(int targetInt) {
        long target = Math.max(0, Math.min(totalProjectDurationMs(), (long)targetInt));
        long cursor = 0;
        int index = project.clips.isEmpty() ? -1 : project.clips.size() - 1;
        long localOut = 0;
        for (int i = 0; i < project.clips.size(); i++) {
            long d = estimateClipDuration(project.clips.get(i));
            if (target <= cursor + d || i == project.clips.size() - 1) {
                index = i;
                localOut = Math.max(0, target - cursor);
                break;
            }
            cursor += d;
        }
        if (index < 0) return;
        boolean wasPlaying = playing;
        if (index != project.selectedClipIndex) {
            project.selectedClipIndex = index;
            EditorClip c = currentClip();
            if (isImageClip(c)) {
                imageOutputPositionMs = localOut;
                pendingRawSeekMs = -1;
            } else {
                pendingRawSeekMs = c.trimStartMs + (long)(localOut * Math.max(.5f, c.speed));
            }
            autoStartAfterBind = wasPlaying;
            bindCurrentClip();
        } else {
            EditorClip c = currentClip();
            if (selectedIsVideo && prepared) {
                long raw = c.trimStartMs + (long)(localOut * Math.max(.5f, c.speed));
                raw = Math.max(c.trimStartMs, Math.min(effectiveEnd(c), raw));
                videoPreview.seekTo((int)Math.min(Integer.MAX_VALUE, raw));
            } else if (!selectedIsVideo) {
                imageOutputPositionMs = localOut;
                if (playing) imagePlayStartedAt = System.currentTimeMillis() - imageOutputPositionMs;
            }
            syncAux(playing);
        }
        updateProjectSeek();
    }

    private long projectPositionMs() {
        long total = projectStartFor(project.selectedClipIndex);
        EditorClip c = currentClip();
        if (c == null) return total;
        if (selectedIsVideo && prepared) {
            long raw = Math.max(c.trimStartMs, videoPreview.getCurrentPosition());
            total += (long)((raw - c.trimStartMs) / Math.max(.5f, c.speed));
        } else if (!selectedIsVideo) {
            total += Math.max(0, imageOutputPositionMs);
        }
        return Math.max(0, total);
    }

    private long projectStartFor(int index) {
        long x = 0;
        for (int i = 0; i < project.clips.size() && i < index; i++) x += estimateClipDuration(project.clips.get(i));
        return x;
    }

    private long totalProjectDurationMs() {
        long t = 0;
        for (EditorClip c : project.clips) t += estimateClipDuration(c);
        return Math.max(0, t);
    }

    private long estimateClipDuration(EditorClip c) {
        if (c == null) return 0;
        if (isImageClip(c)) return (long)(Math.max(500, c.stillDurationMs) / Math.max(.5f, c.speed));
        long end = c.trimEndMs > c.trimStartMs ? c.trimEndMs : c.trimStartMs + 5000;
        return (long)(Math.max(500, end - c.trimStartMs) / Math.max(.5f, c.speed));
    }

    private long effectiveEnd(EditorClip c) {
        long end = c.trimEndMs > c.trimStartMs ? c.trimEndMs : rawDurationMs;
        return Math.max(c.trimStartMs + 100, Math.min(rawDurationMs, end));
    }

    private void updateProjectSeek() {
        long total = Math.max(1, totalProjectDurationMs());
        int max = (int)Math.min(Integer.MAX_VALUE, total);
        projectSeek.setMax(Math.max(1, max));
        if (!seekTouching) projectSeek.setProgress((int)Math.min(max, projectPositionMs()));
        timeLabel.setText(fmt(projectPositionMs()) + "  /  " + fmt(total));
        timelineTitle.setText("Timeline  •  " + project.clips.size() + " clips  •  " + fmt(total));
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            try {
                EditorClip c = currentClip();
                if (playing && c != null) {
                    if (selectedIsVideo && prepared) {
                        long pos = videoPreview.getCurrentPosition();
                        if (videoPreview.isPlaying() && pos >= effectiveEnd(c) - 80) {
                            advanceClip();
                        }
                    } else if (!selectedIsVideo) {
                        imageOutputPositionMs = Math.max(0, System.currentTimeMillis() - imagePlayStartedAt);
                        if (imageOutputPositionMs >= estimateClipDuration(c)) advanceClip();
                    }
                    syncAux(true);
                }
                updateOverlayVisibility();
                updateProjectSeek();
            } catch (Exception ignored) {}
            handler.postDelayed(this, 100);
        }
    };

    private void refreshTimeline() {
        if (clipStrip == null) return;
        clipStrip.removeAllViews();
        for (int i = 0; i < project.clips.size(); i++) {
            final int index = i;
            EditorClip c = project.clips.get(i);
            boolean selected = index == project.selectedClipIndex;
            FrameLayout tile = new FrameLayout(this);
            tile.setBackground(Ui.bordered(selected ? 0xFF102A3B : Ui.SURFACE_2, selected ? Ui.CYAN : Ui.BORDER_SOFT, selected ? 2 : 1, 13, this));
            tile.setPadding(Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2));
            tile.setOnClickListener(v -> selectClip(index));

            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundColor(0xFF070D17);
            tile.addView(thumb, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView badge = Ui.text(this, (index + 1) + "  " + fmt(estimateClipDuration(c)), 9, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundColor(0x99000000);
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 19), Gravity.BOTTOM);
            tile.addView(badge, bp);

            int seconds = (int)Math.max(1, estimateClipDuration(c) / 1000);
            int width = Ui.dp(this, Math.max(78, Math.min(145, 68 + seconds * 5)));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(width, Ui.dp(this, 54));
            tp.setMargins(0, 0, Ui.dp(this, 5), 0);
            clipStrip.addView(tile, tp);
            loadThumbnail(thumb, c);
        }
        Button add = Ui.ghost(this, "＋");
        add.setTextSize(20);
        add.setOnClickListener(v -> pickMedia());
        clipStrip.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 54)));
        refreshLayers();
        updateProjectSeek();
    }

    private void refreshLayers() {
        if (layersRow == null) return;
        layersRow.removeAllViews();
        if (project.showText && (!empty(project.hook) || !empty(project.body) || !empty(project.cta))) layersRow.addView(layerChip("T  Text", Ui.CYAN, v -> showTextMenu()));
        if (!empty(project.audioUri)) layersRow.addView(layerChip("♫  Music " + fmt(project.audioOffsetMs), Ui.PURPLE, v -> showAudioMenu()));
        if (!empty(project.voiceoverUri)) layersRow.addView(layerChip("●  Voice " + fmt(project.voiceoverOffsetMs), Ui.GREEN, v -> showAudioMenu()));
        if (!empty(project.overlayUri)) layersRow.addView(layerChip("◇  Overlay", Ui.YELLOW, v -> showOverlayMenu()));
        if (layersRow.getChildCount() == 0) layersRow.addView(layerChip("＋  أضف Text / Audio / Overlay", Ui.MUTED, v -> showTextMenu()));
    }

    private TextView layerChip(String label, int color, View.OnClickListener click) {
        TextView chip = Ui.chip(this, label, color);
        chip.setOnClickListener(click);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 27));
        p.setMargins(0, 0, Ui.dp(this, 6), 0);
        chip.setLayoutParams(p);
        return chip;
    }

    private void loadThumbnail(ImageView view, EditorClip clip) {
        if (clip == null || empty(clip.uri)) return;
        Uri uri = Uri.parse(clip.uri);
        if (isImageClip(clip)) {
            try { view.setImageURI(uri); } catch (Exception ignored) {}
            return;
        }
        thumbExecutor.execute(() -> {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            Bitmap frame = null;
            try {
                r.setDataSource(this, uri);
                frame = r.getFrameAtTime(Math.max(0, clip.trimStartMs * 1000L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } catch (Exception ignored) {
            } finally {
                try { r.release(); } catch (Exception ignored) {}
            }
            Bitmap result = frame;
            if (result != null) handler.post(() -> { try { view.setImageBitmap(result); } catch (Exception ignored) {} });
        });
    }

    private void setTrimStart() {
        EditorClip c = currentClip();
        if (!selectedIsVideo || !prepared || c == null) { action("Trim متاح للفيديو بعد تجهيز المعاينة"); return; }
        long p = videoPreview.getCurrentPosition();
        if (p >= effectiveEnd(c) - 300) { action("حرّك المؤشر للخلف"); return; }
        pushUndo(); c.trimStartMs = p; saveDraft(false); refreshTimeline(); action("تم تعيين بداية القص " + fmt(p));
    }

    private void setTrimEnd() {
        EditorClip c = currentClip();
        if (!selectedIsVideo || !prepared || c == null) { action("Trim متاح للفيديو بعد تجهيز المعاينة"); return; }
        long p = videoPreview.getCurrentPosition();
        if (p <= c.trimStartMs + 300) { action("حرّك المؤشر للأمام"); return; }
        pushUndo(); c.trimEndMs = p; saveDraft(false); refreshTimeline(); action("تم تعيين نهاية القص " + fmt(p));
    }

    private void splitClip() {
        EditorClip c = currentClip();
        if (!selectedIsVideo || !prepared || c == null) { action("Split متاح للفيديو عند المؤشر"); return; }
        long p = videoPreview.getCurrentPosition();
        long end = effectiveEnd(c);
        if (p <= c.trimStartMs + 300 || p >= end - 300) { action("ضع المؤشر داخل المقطع"); return; }
        pushUndo();
        EditorClip right = c.copy();
        right.trimStartMs = p;
        right.trimEndMs = end;
        c.trimEndMs = p;
        project.clips.add(project.selectedClipIndex + 1, right);
        project.selectedClipIndex++;
        project.syncPrimarySource();
        saveDraft(false);
        bindCurrentClip();
        action("تم تقسيم المقطع");
    }

    private void duplicateClip() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo(); project.clips.add(project.selectedClipIndex + 1, c.copy()); project.selectedClipIndex++; project.syncPrimarySource(); saveDraft(false); bindCurrentClip(); action("تم نسخ المقطع");
    }

    private void moveClip(int delta) {
        if (project.clips.size() < 2) { action("لا يوجد مقطع آخر للتحريك"); return; }
        int from = project.selectedClipIndex, to = from + delta;
        if (to < 0 || to >= project.clips.size()) { action("وصل المقطع لنهاية الترتيب"); return; }
        pushUndo(); Collections.swap(project.clips, from, to); project.selectedClipIndex = to; project.syncPrimarySource(); saveDraft(false); refreshTimeline(); action("تم تغيير ترتيب المقطع");
    }

    private void deleteClip() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo(); project.clips.remove(project.selectedClipIndex); if (project.selectedClipIndex >= project.clips.size()) project.selectedClipIndex = Math.max(0, project.clips.size() - 1); project.syncPrimarySource(); saveDraft(false); bindCurrentClip(); action("تم حذف المقطع");
    }

    private void resetClip() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo(); c.trimStartMs = 0; c.trimEndMs = rawDurationMs > 0 ? rawDurationMs : c.trimEndMs; c.speed = 1f; c.volume = 100; c.rotation = 0; c.zoom = 100; c.mirror = false; c.filter = "None"; applyClipTransform(); applyPlaybackSpeed(); applyVideoVolume(); saveDraft(false); refreshTimeline(); action("تمت إعادة ضبط المقطع");
    }

    private void rotateClip() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo(); c.rotation = (c.rotation + 90) % 360; applyClipTransform(); saveDraft(false); action("Rotate " + c.rotation + "°");
    }

    private void toggleMirror() {
        EditorClip c = currentClip(); if (c == null) return;
        pushUndo(); c.mirror = !c.mirror; applyClipTransform(); saveDraft(false); action(c.mirror ? "Mirror ON" : "Mirror OFF");
    }

    private void applyClipTransform() {
        EditorClip c = currentClip(); if (c == null) return;
        float z = Math.max(1f, Math.min(1.8f, c.zoom / 100f));
        float sx = c.mirror ? -z : z;
        videoPreview.setScaleX(sx); videoPreview.setScaleY(z); videoPreview.setRotation(c.rotation);
        imagePreview.setScaleX(sx); imagePreview.setScaleY(z); imagePreview.setRotation(c.rotation);
        if ("Mono".equals(c.filter)) { imagePreview.setColorFilter(Color.GRAY); videoPreview.setAlpha(.92f); }
        else if ("Soft".equals(c.filter)) { imagePreview.setColorFilter(0x18FFFFFF); videoPreview.setAlpha(.94f); }
        else { imagePreview.clearColorFilter(); videoPreview.setAlpha(1f); }
    }

    private void applyPlaybackSpeed() {
        EditorClip c = currentClip();
        if (activeVideoPlayer == null || c == null) return;
        try { activeVideoPlayer.setPlaybackParams(activeVideoPlayer.getPlaybackParams().setSpeed(c.speed)); } catch (Exception ignored) {}
    }

    private void applyVideoVolume() {
        EditorClip c = currentClip();
        if (activeVideoPlayer == null || c == null) return;
        float v = c.volume / 100f;
        try { activeVideoPlayer.setVolume(v, v); } catch (Exception ignored) {}
    }

    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "image/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK_MEDIA);
    }

    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        startActivityForResult(i, PICK_AUDIO);
    }

    private void pickOverlay() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, PICK_OVERLAY);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == PICK_MEDIA) {
            ArrayList<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData cd = data.getClipData();
                for (int i = 0; i < cd.getItemCount(); i++) uris.add(cd.getItemAt(i).getUri());
            } else if (data.getData() != null) uris.add(data.getData());
            if (uris.isEmpty()) return;
            pushUndo();
            int firstNew = project.clips.size();
            for (Uri uri : uris) {
                persist(uri, data);
                project.clips.add(makeClip(uri));
            }
            project.selectedClipIndex = firstNew;
            project.syncPrimarySource();
            saveDraft(false);
            bindCurrentClip();
            action("تمت إضافة " + uris.size() + " ميديا");
        } else if (requestCode == PICK_AUDIO && data.getData() != null) {
            pushUndo(); Uri uri = data.getData(); persist(uri, data); project.audioUri = uri.toString(); releaseMusic(); saveDraft(false); refreshLayers(); action("تمت إضافة الموسيقى");
        } else if (requestCode == PICK_OVERLAY && data.getData() != null) {
            pushUndo(); Uri uri = data.getData(); persist(uri, data); project.overlayUri = uri.toString(); project.overlayStartMs = projectPositionMs(); project.overlayEndMs = 0; applyOverlayPreview(); saveDraft(false); refreshLayers(); action("تمت إضافة Overlay");
        }
    }

    private EditorClip makeClip(Uri uri) {
        EditorClip c = new EditorClip(uri.toString());
        if (isVideo(uri, uri.toString())) {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            try {
                r.setDataSource(this, uri);
                String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (d != null) c.trimEndMs = Math.max(0, Long.parseLong(d));
            } catch (Exception ignored) {
            } finally { try { r.release(); } catch (Exception ignored) {} }
        } else c.stillDurationMs = 3000;
        return c;
    }

    private void persist(Uri uri, Intent data) {
        try { getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
    }

    private boolean isVideo(Uri uri, String fallback) {
        try { String t = getContentResolver().getType(uri); if (t != null) return t.toLowerCase(Locale.ROOT).startsWith("video/"); } catch (Exception ignored) {}
        String s = fallback == null ? "" : fallback.toLowerCase(Locale.ROOT);
        return s.contains(".mp4") || s.contains(".webm") || s.contains(".mkv") || s.contains(".mov") || s.contains(".m4v");
    }

    private boolean isImageClip(EditorClip c) {
        if (c == null || empty(c.uri)) return false;
        Uri u = Uri.parse(c.uri);
        try { String t = getContentResolver().getType(u); if (t != null) return t.toLowerCase(Locale.ROOT).startsWith("image/"); } catch (Exception ignored) {}
        String s = c.uri.toLowerCase(Locale.ROOT);
        return s.contains(".jpg") || s.contains(".jpeg") || s.contains(".png") || s.contains(".webp");
    }

    private void toggleVoiceover() {
        if (voiceRecording) { stopVoiceover(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        startVoiceover();
    }

    private void startVoiceover() {
        try {
            pushUndo();
            File dir = new File(getFilesDir(), "voiceovers");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException();
            voiceFile = new File(dir, "voice_" + System.currentTimeMillis() + ".m4a");
            voiceRecordOffsetMs = projectPositionMs();
            voiceRecorder = new MediaRecorder();
            voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            voiceRecorder.setAudioSamplingRate(44100);
            voiceRecorder.setAudioEncodingBitRate(128000);
            voiceRecorder.setOutputFile(voiceFile.getAbsolutePath());
            voiceRecorder.prepare();
            voiceRecorder.start();
            voiceRecording = true;
            action("● جاري تسجيل Voiceover");
            if (!playing) togglePlay();
        } catch (Exception e) {
            releaseRecorder(); voiceRecording = false; action("فشل بدء تسجيل Voiceover");
        }
    }

    private void stopVoiceover() {
        if (!voiceRecording) { releaseRecorder(); return; }
        voiceRecording = false;
        boolean ok = true;
        try { voiceRecorder.stop(); } catch (Exception e) { ok = false; }
        releaseRecorder();
        if (ok && voiceFile != null && voiceFile.exists() && voiceFile.length() > 512) {
            project.voiceoverUri = Uri.fromFile(voiceFile).toString();
            project.voiceoverOffsetMs = voiceRecordOffsetMs;
            releaseVoice();
            saveDraft(false);
            refreshLayers();
            action("تم حفظ Voiceover");
        } else action("لم يتم حفظ التسجيل");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startVoiceover();
            else action("اسمح بالميكروفون لتسجيل Voiceover");
        }
    }

    private void releaseRecorder() { try { if (voiceRecorder != null) voiceRecorder.release(); } catch (Exception ignored) {} voiceRecorder = null; }

    private void ensureAuxPlayers() {
        if (!empty(project.audioUri) && musicPlayer == null) {
            try { musicPlayer = new MediaPlayer(); musicPlayer.setDataSource(this, Uri.parse(project.audioUri)); musicPlayer.setLooping(true); musicPlayer.prepare(); } catch (Exception e) { releaseMusic(); }
        }
        if (!empty(project.voiceoverUri) && voicePlayer == null) {
            try { voicePlayer = new MediaPlayer(); voicePlayer.setDataSource(this, Uri.parse(project.voiceoverUri)); voicePlayer.setLooping(false); voicePlayer.prepare(); } catch (Exception e) { releaseVoice(); }
        }
        applyAuxVolumes();
    }

    private void syncAux(boolean play) {
        ensureAuxPlayers();
        syncPlayer(musicPlayer, project.audioOffsetMs, play, true);
        syncPlayer(voicePlayer, project.voiceoverOffsetMs, play, false);
    }

    private void syncPlayer(MediaPlayer p, long offset, boolean play, boolean loop) {
        if (p == null) return;
        try {
            long target = projectPositionMs() - offset;
            if (target < 0) { if (p.isPlaying()) p.pause(); p.seekTo(0); return; }
            int d = p.getDuration(); if (d <= 0) return;
            if (loop) target %= d; else if (target >= d) { if (p.isPlaying()) p.pause(); return; }
            if (Math.abs(p.getCurrentPosition() - target) > 650) p.seekTo((int)target);
            if (play && !p.isPlaying()) p.start(); else if (!play && p.isPlaying()) p.pause();
        } catch (Exception ignored) {}
    }

    private void applyAuxVolumes() {
        float m = project.audioVolume / 100f, v = project.voiceoverVolume / 100f;
        try { if (musicPlayer != null) musicPlayer.setVolume(m, m); } catch (Exception ignored) {}
        try { if (voicePlayer != null) voicePlayer.setVolume(v, v); } catch (Exception ignored) {}
    }

    private void pauseAux() {
        try { if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause(); } catch (Exception ignored) {}
        try { if (voicePlayer != null && voicePlayer.isPlaying()) voicePlayer.pause(); } catch (Exception ignored) {}
    }

    private void removeMusic() { pushUndo(); project.audioUri = ""; releaseMusic(); saveDraft(false); refreshLayers(); action("تمت إزالة الموسيقى"); }
    private void removeVoice() { if (voiceRecording) stopVoiceover(); pushUndo(); project.voiceoverUri = ""; project.voiceoverOffsetMs = 0; releaseVoice(); saveDraft(false); refreshLayers(); action("تمت إزالة Voiceover"); }
    private void releaseMusic() { try { if (musicPlayer != null) { musicPlayer.stop(); musicPlayer.release(); } } catch (Exception ignored) {} musicPlayer = null; }
    private void releaseVoice() { try { if (voicePlayer != null) { voicePlayer.stop(); voicePlayer.release(); } } catch (Exception ignored) {} voicePlayer = null; }
    private void releaseAuxPlayers() { releaseMusic(); releaseVoice(); }

    private void applyOverlayPreview() {
        if (overlayPreview == null) return;
        if (empty(project.overlayUri)) { overlayPreview.setVisibility(View.GONE); return; }
        try {
            overlayPreview.setImageURI(Uri.parse(project.overlayUri));
            overlayPreview.setAlpha(project.overlayOpacity / 100f);
            int fw = previewFrame.getWidth(), fh = previewFrame.getHeight();
            if (fw <= 0 || fh <= 0) { previewFrame.postDelayed(this::applyOverlayPreview, 80); return; }
            int size = Math.max(Ui.dp(this, 30), (int)(fw * project.overlayScale / 100f));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.START);
            int x = (int)(fw * project.overlayXPercent / 100f - size / 2f);
            int y = (int)(fh * project.overlayYPercent / 100f - size / 2f);
            lp.leftMargin = Math.max(0, Math.min(fw - size, x));
            lp.topMargin = Math.max(0, Math.min(fh - size, y));
            overlayPreview.setLayoutParams(lp);
            updateOverlayVisibility();
        } catch (Exception e) { overlayPreview.setVisibility(View.GONE); }
    }

    private void updateOverlayVisibility() {
        if (overlayPreview == null || empty(project.overlayUri)) { if (overlayPreview != null) overlayPreview.setVisibility(View.GONE); return; }
        long p = projectPositionMs();
        long end = project.overlayEndMs > project.overlayStartMs ? project.overlayEndMs : Long.MAX_VALUE;
        overlayPreview.setVisibility(p >= project.overlayStartMs && p <= end ? View.VISIBLE : View.GONE);
    }

    private void setOverlayTiming(boolean start) {
        if (empty(project.overlayUri)) { action("اختر Overlay أولاً"); return; }
        pushUndo();
        long p = projectPositionMs();
        if (start) { project.overlayStartMs = p; if (project.overlayEndMs > 0 && project.overlayEndMs <= p) project.overlayEndMs = 0; action("بداية Overlay: " + fmt(p)); }
        else { if (p <= project.overlayStartMs) { action("حرّك المؤشر بعد البداية"); return; } project.overlayEndMs = p; action("نهاية Overlay: " + fmt(p)); }
        saveDraft(false); updateOverlayVisibility(); refreshLayers();
    }

    private void removeOverlay() { pushUndo(); project.overlayUri = ""; project.overlayStartMs = 0; project.overlayEndMs = 0; applyOverlayPreview(); saveDraft(false); refreshLayers(); action("تمت إزالة Overlay"); }

    private void pushUndo() {
        try {
            String s = project.toJson().toString();
            if (undoStack.size() >= 40) undoStack.removeFirst();
            undoStack.addLast(s);
            redoStack.clear();
        } catch (Exception ignored) {}
    }

    private void undo() {
        if (undoStack.isEmpty()) { action("لا يوجد Undo"); return; }
        stopPlayback(false);
        try {
            redoStack.addLast(project.toJson().toString());
            project = CreatorProject.fromJson(new JSONObject(undoStack.removeLast()));
            releaseAuxPlayers();
            applyProjectVisuals();
            bindCurrentClip();
            action("Undo");
        } catch (Exception e) { action("تعذر Undo"); }
    }

    private void redo() {
        if (redoStack.isEmpty()) { action("لا يوجد Redo"); return; }
        stopPlayback(false);
        try {
            undoStack.addLast(project.toJson().toString());
            project = CreatorProject.fromJson(new JSONObject(redoStack.removeLast()));
            releaseAuxPlayers();
            applyProjectVisuals();
            bindCurrentClip();
            action("Redo");
        } catch (Exception e) { action("تعذر Redo"); }
    }

    private void saveDraft(boolean notify) {
        project.syncPrimarySource();
        DraftStore.save(this, project);
        if (notify) action("تم حفظ المشروع ✓");
    }

    private void exportProject() {
        if (voiceRecording) stopVoiceover();
        project.ensureClips();
        if (project.clips.isEmpty()) { action("أضف فيديو أو صورة قبل التصدير"); return; }
        saveDraft(false);
        stopPlayback(false);
        exportButton.setEnabled(false);
        exportButton.setAlpha(.55f);
        statusLabel.setTextColor(Ui.YELLOW);
        statusLabel.setText("جاري تجهيز التصدير…");
        exportExecutor.execute(() -> ReelExporter.export(this, project, new ReelExporter.Callback() {
            @Override public void onStage(String status) { runOnUiThread(() -> statusLabel.setText(status)); }
            @Override public void onSuccess(File file) {
                runOnUiThread(() -> {
                    exportButton.setEnabled(true); exportButton.setAlpha(1f); statusLabel.setTextColor(Ui.GREEN); statusLabel.setText("تم التصدير ✓");
                    Intent i = new Intent(ProfessionalEditorActivity.this, PublishingActivity.class);
                    i.putExtra("project_id", project.id);
                    i.putExtra("video_path", file.getAbsolutePath());
                    startActivity(i);
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> { exportButton.setEnabled(true); exportButton.setAlpha(1f); statusLabel.setTextColor(Ui.RED); statusLabel.setText(message); Toast.makeText(ProfessionalEditorActivity.this, message, Toast.LENGTH_LONG).show(); });
            }
        }));
    }

    private void action(String text) {
        if (statusLabel != null) { statusLabel.setTextColor(Ui.CYAN); statusLabel.setText(text); }
    }

    private int nearestSpeed(float value) { int best = 0; float d = Float.MAX_VALUE; for (int i = 0; i < speeds.length; i++) { float x = Math.abs(speeds[i] - value); if (x < d) { d = x; best = i; } } return best; }
    private int indexOf(String[] a, String value) { if (a == null || a.length == 0) return 0; for (int i = 0; i < a.length; i++) if (a[i].equals(value)) return i; return 0; }
    private int closest(int[] values, int v) { int best = 0, d = Integer.MAX_VALUE; for (int i = 0; i < values.length; i++) { int x = Math.abs(values[i] - v); if (x < d) { d = x; best = i; } } return best; }
    private boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private String value(EditText e) { return e == null || e.getText() == null ? "" : e.getText().toString().trim(); }
    private String fmt(long ms) { if (ms < 0) ms = 0; long min = ms / 60000; double sec = (ms % 60000) / 1000.0; return String.format(Locale.US, "%02d:%04.1f", min, sec); }
}
