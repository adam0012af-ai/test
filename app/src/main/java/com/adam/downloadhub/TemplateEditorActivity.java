package com.adam.downloadhub;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Production entry point for the editor.
 *
 * ProfessionalEditorActivity owns the editing engine. This class gives that engine
 * a compact mobile-editor workspace and adds a proper export chooser without
 * duplicating the media/timeline implementation.
 */
public class TemplateEditorActivity extends ProfessionalEditorActivity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean destroyed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ui.post(this::polishWorkspace);
        ui.postDelayed(clockFixer, 120);
    }

    @Override protected void onResume() {
        super.onResume();
        ui.postDelayed(this::polishWorkspace, 80);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void polishWorkspace() {
        if (destroyed) return;
        try {
            ViewGroup content = findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return;
            View first = content.getChildAt(0);
            if (!(first instanceof LinearLayout)) return;
            LinearLayout page = (LinearLayout) first;
            page.setPadding(Ui.dp(this, 6), Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3));

            // Compact top bar: no wrapped title and no dead space.
            if (page.getChildCount() > 0 && page.getChildAt(0) instanceof LinearLayout) {
                LinearLayout top = (LinearLayout) page.getChildAt(0);
                top.setGravity(Gravity.CENTER_VERTICAL);
                if (top.getChildCount() > 1 && top.getChildAt(1) instanceof TextView) {
                    TextView title = (TextView) top.getChildAt(1);
                    title.setText("Studio Editor");
                    title.setTextSize(16);
                    title.setSingleLine(true);
                    title.setMaxLines(1);
                }
            }

            FrameLayout stage = field("previewStage", FrameLayout.class);
            FrameLayout frame = field("previewFrame", FrameLayout.class);
            if (stage != null) {
                int screenDp = Math.round(getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density);
                int h = Math.max(270, Math.min(355, Math.round(screenDp * .39f)));
                ViewGroup.LayoutParams lp = stage.getLayoutParams();
                lp.height = Ui.dp(this, h);
                stage.setLayoutParams(lp);
                stage.setPadding(0, 0, 0, 0);
                stage.setBackgroundColor(0xFF010307);
            }
            if (frame != null) {
                frame.setBackgroundColor(0xFF000000);
                frame.setElevation(Ui.dp(this, 2));
            }

            // Conventional left-to-right editing timeline even when labels are Arabic.
            LinearLayout strip = field("clipStrip", LinearLayout.class);
            if (strip != null) {
                strip.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                strip.setPadding(Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2));
                View parent = (View) strip.getParent();
                if (parent != null) parent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }
            LinearLayout layers = field("layersRow", LinearLayout.class);
            if (layers != null) layers.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            // Make the bottom tool rail feel like an editor toolbar, not large form buttons.
            if (page.getChildCount() > 0) {
                View last = page.getChildAt(page.getChildCount() - 1);
                if (last instanceof HorizontalScrollView) {
                    HorizontalScrollView scroll = (HorizontalScrollView) last;
                    scroll.setFillViewport(false);
                    scroll.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout tools = (LinearLayout) scroll.getChildAt(0);
                        tools.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                        for (int i = 0; i < tools.getChildCount(); i++) {
                            View tool = tools.getChildAt(i);
                            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(this, 66), Ui.dp(this, 56));
                            p.setMargins(0, 0, Ui.dp(this, 4), 0);
                            tool.setLayoutParams(p);
                            tool.setElevation(0);
                        }
                    }
                }
            }

            TextView status = field("statusLabel", TextView.class);
            if (status != null) {
                status.setTextSize(9);
                status.setGravity(Gravity.CENTER);
            }

            Button export = field("exportButton", Button.class);
            if (export != null) {
                export.setText("تصدير");
                export.setOnClickListener(v -> showExportSheet());
            }
        } catch (Throwable ignored) {}
    }

    private void showExportSheet() {
        int q = AppPrefs.exportQuality(this);
        String current = q == 720 ? "720p HD" : "1080p Full HD";
        EditorSheets.menu(this, "التصدير • " + current, new String[]{
                "1080p  •  Full HD  •  جودة عالية",
                "720p  •  HD  •  أسرع وأخف",
                "💾  حفظ المشروع كمسودة",
                AppPrefs.creatorWatermark(this) ? "✓  العلامة المائية مفعلة" : "○  العلامة المائية غير مفعلة"
        }, new Runnable[]{
                () -> { AppPrefs.setExportQuality(this, 1080); invokeParent("exportProject"); },
                () -> { AppPrefs.setExportQuality(this, 720); invokeParent("exportProject"); },
                () -> invokeParent("saveDraft", new Class[]{boolean.class}, new Object[]{true}),
                () -> {
                    AppPrefs.setCreatorWatermark(this, !AppPrefs.creatorWatermark(this));
                    toastStatus(AppPrefs.creatorWatermark(this) ? "تم تفعيل العلامة المائية" : "تم إلغاء العلامة المائية");
                }
        });
    }

    private final Runnable clockFixer = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            try {
                long total = ((Number) invokeParentForResult("totalProjectDurationMs")).longValue();
                long pos = ((Number) invokeParentForResult("projectPositionMs")).longValue();
                total = Math.max(0, total);
                pos = Math.max(0, Math.min(total, pos));
                TextView time = field("timeLabel", TextView.class);
                if (time != null) time.setText(fmt(pos) + "  /  " + fmt(total));

                TextView timeline = field("timelineTitle", TextView.class);
                Object p = rawField("project");
                if (timeline != null && p instanceof CreatorProject) {
                    CreatorProject cp = (CreatorProject) p;
                    int clips = cp.clips == null ? 0 : cp.clips.size();
                    timeline.setText("Timeline  •  " + clips + " clips  •  " + fmt(total));
                }
            } catch (Throwable ignored) {}
            ui.postDelayed(this, 180);
        }
    };

    private String fmt(long ms) {
        long min = ms / 60000;
        double sec = (ms % 60000) / 1000.0;
        return String.format(Locale.US, "%02d:%04.1f", min, sec);
    }

    private void toastStatus(String text) {
        TextView status = field("statusLabel", TextView.class);
        if (status != null) {
            status.setTextColor(Ui.CYAN);
            status.setText(text);
        }
    }

    private Object rawField(String name) {
        try {
            Field f = ProfessionalEditorActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(this);
        } catch (Throwable e) { return null; }
    }

    private <T> T field(String name, Class<T> type) {
        Object o = rawField(name);
        return type.isInstance(o) ? type.cast(o) : null;
    }

    private void invokeParent(String name) {
        invokeParent(name, new Class[0], new Object[0]);
    }

    private void invokeParent(String name, Class<?>[] types, Object[] args) {
        try {
            Method m = ProfessionalEditorActivity.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            m.invoke(this, args);
        } catch (Throwable e) { toastStatus("تعذر تنفيذ الأمر"); }
    }

    private Object invokeParentForResult(String name) throws Exception {
        Method m = ProfessionalEditorActivity.class.getDeclaredMethod(name);
        m.setAccessible(true);
        return m.invoke(this);
    }
}
