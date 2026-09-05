package com.adam.downloadhub;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public final class EditorSheets {
    public interface ChoiceCallback { void onPick(int index); }
    public interface SliderCallback { void onChange(int value); }

    private EditorSheets() {}

    public static Dialog open(Activity a, String title, View body) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(a);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(Ui.dp(a, 18), Ui.dp(a, 14), Ui.dp(a, 18), Ui.dp(a, 18));
        shell.setBackground(Ui.bordered(Ui.SURFACE, Ui.BORDER, 1, 26, a));
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(a, title, 18, Ui.TEXT, true);
        head.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(a, 44), 1f));
        Button close = Ui.ghost(a, "×");
        close.setTextSize(22);
        close.setOnClickListener(v -> d.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 40)));
        shell.addView(head);

        View divider = new View(a);
        divider.setBackgroundColor(Ui.BORDER_SOFT);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 1));
        dp.setMargins(0, 0, 0, Ui.dp(a, 10));
        shell.addView(divider, dp);

        shell.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        d.setContentView(shell);

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(.58f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            w.setAttributes(lp);
        }
        d.setOnShowListener(x -> {
            Window ww = d.getWindow();
            if (ww != null) ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        });
        d.show();
        return d;
    }

    public static Dialog menu(Activity a, String title, String[] labels, Runnable[] actions) {
        ScrollView scroll = new ScrollView(a);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, Ui.dp(a, 6));
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Dialog[] ref = new Dialog[1];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView row = row(a, labels[i], false);
            row.setOnClickListener(v -> {
                Dialog d = ref[0];
                if (d != null) d.dismiss();
                v.postDelayed(() -> {
                    if (actions != null && index < actions.length && actions[index] != null) actions[index].run();
                }, 80);
            });
            list.addView(row, rowParams(a));
        }
        ref[0] = open(a, title, scroll);
        return ref[0];
    }

    public static Dialog choice(Activity a, String title, String[] labels, int selected, Typeface[] faces, ChoiceCallback cb) {
        ScrollView scroll = new ScrollView(a);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final Dialog[] ref = new Dialog[1];

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean on = i == selected;
            TextView row = row(a, (on ? "✓   " : "    ") + labels[i], on);
            if (faces != null && i < faces.length && faces[i] != null) row.setTypeface(faces[i], on ? Typeface.BOLD : Typeface.NORMAL);
            row.setOnClickListener(v -> {
                Dialog d = ref[0];
                if (d != null) d.dismiss();
                if (cb != null) cb.onPick(index);
            });
            list.addView(row, rowParams(a));
        }
        ref[0] = open(a, title, scroll);
        return ref[0];
    }

    public static Dialog slider(Activity a, String title, int min, int max, int value, String suffix, SliderCallback cb) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 2));

        TextView valueText = Ui.text(a, value + suffix, 24, Ui.CYAN, true);
        valueText.setGravity(Gravity.CENTER);
        box.addView(valueText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 48)));

        SeekBar bar = new SeekBar(a);
        bar.setMax(Math.max(1, max - min));
        bar.setProgress(Math.max(0, Math.min(max - min, value - min)));
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 48)));

        LinearLayout ends = new LinearLayout(a);
        ends.setOrientation(LinearLayout.HORIZONTAL);
        TextView lo = Ui.text(a, min + suffix, 10, Ui.MUTED, false);
        TextView hi = Ui.text(a, max + suffix, 10, Ui.MUTED, false);
        hi.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ends.addView(lo, new LinearLayout.LayoutParams(0, Ui.dp(a, 24), 1f));
        ends.addView(hi, new LinearLayout.LayoutParams(0, Ui.dp(a, 24), 1f));
        box.addView(ends);

        Button done = Ui.accent(a, "تم");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 46));
        bp.setMargins(0, Ui.dp(a, 10), 0, 0);
        box.addView(done, bp);

        Dialog d = open(a, title, box);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = min + progress;
                valueText.setText(v + suffix);
                if (fromUser && cb != null) cb.onChange(v);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        done.setOnClickListener(v -> d.dismiss());
        return d;
    }

    private static TextView row(Activity a, String text, boolean selected) {
        TextView r = Ui.text(a, text, 15, selected ? Ui.CYAN : Ui.TEXT, selected);
        r.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
        r.setBackground(selected
                ? Ui.bordered(Ui.SURFACE_3, Ui.CYAN, 1, 16, a)
                : Ui.bordered(Ui.SURFACE_2, Ui.BORDER_SOFT, 1, 16, a));
        return r;
    }

    private static LinearLayout.LayoutParams rowParams(Activity a) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 52));
        p.setMargins(0, 0, 0, Ui.dp(a, 7));
        return p;
    }
}
