package com.adam.downloadhub;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(5, 12, 24);
    public static final int BG_2 = Color.rgb(7, 18, 35);
    public static final int SURFACE = Color.rgb(11, 24, 43);
    public static final int SURFACE_2 = Color.rgb(17, 33, 56);
    public static final int SURFACE_3 = Color.rgb(22, 43, 71);
    public static final int BORDER = Color.rgb(36, 67, 103);
    public static final int BORDER_SOFT = Color.rgb(27, 49, 78);
    public static final int TEXT = Color.rgb(246, 249, 255);
    public static final int MUTED = Color.rgb(153, 173, 199);
    public static final int MUTED_2 = Color.rgb(109, 135, 168);
    public static final int BLUE = Color.rgb(18, 102, 255);
    public static final int BLUE_2 = Color.rgb(48, 137, 255);
    public static final int CYAN = Color.rgb(24, 216, 255);
    public static final int GREEN = Color.rgb(72, 222, 153);
    public static final int YELLOW = Color.rgb(255, 199, 82);
    public static final int RED = Color.rgb(248, 102, 121);
    public static final int PURPLE = Color.rgb(128, 98, 255);

    private Ui() {}

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable bg(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, Math.round(radiusDp)));
        return d;
    }

    public static GradientDrawable bordered(int color, int borderColor, int borderDp, int radiusDp, Context c) {
        GradientDrawable d = bg(color, radiusDp, c);
        d.setStroke(dp(c, borderDp), borderColor);
        return d;
    }

    public static GradientDrawable gradient(int start, int end, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable gradient3(int a, int b, int c3, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{a, b, c3});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    private static Drawable ripple(Drawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    private static void finishButton(Button b) {
        b.setStateListAnimator(null);
        b.setSoundEffectsEnabled(true);
        b.setHapticFeedbackEnabled(true);
        b.setClickable(true);
        b.setFocusable(true);
        b.setMinHeight(0);
        b.setMinWidth(0);
    }

    public static TextView text(Context c, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static TextView sectionTitle(Context c, String value) {
        TextView t = text(c, value, 18, TEXT, true);
        t.setLetterSpacing(0.01f);
        return t;
    }

    public static Button primary(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.setBackground(ripple(gradient(BLUE_2, BLUE, 17, c), Color.argb(70, 255, 255, 255)));
        finishButton(b);
        return b;
    }

    public static Button accent(Context c, String label) {
        Button b = primary(c, label);
        b.setTextColor(Color.rgb(3, 25, 39));
        b.setBackground(ripple(gradient(Color.rgb(63, 235, 255), CYAN, 17, c), Color.argb(85, 255, 255, 255)));
        return b;
    }

    public static Button secondary(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 10), 0, dp(c, 10), 0);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.setBackground(ripple(bordered(SURFACE_2, BORDER, 1, 15, c), Color.argb(60, 24, 216, 255)));
        finishButton(b);
        return b;
    }

    public static Button ghost(Context c, String label) {
        Button b = secondary(c, label);
        b.setTextColor(CYAN);
        b.setBackground(ripple(bordered(Color.TRANSPARENT, BORDER_SOFT, 1, 14, c), Color.argb(75, 24, 216, 255)));
        return b;
    }

    public static TextView chip(Context c, String label, int accent) {
        TextView t = text(c, label, 11, accent, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 10), dp(c, 7), dp(c, 10), dp(c, 7));
        t.setBackground(bordered(Color.argb(35, Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb(100, Color.red(accent), Color.green(accent), Color.blue(accent)), 1, 40, c));
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        l.setBackground(bordered(SURFACE, BORDER_SOFT, 1, 21, c));
        l.setElevation(dp(c, 2));
        return l;
    }

    public static EditText input(Context c, String hint, boolean multiline) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED_2);
        e.setTextSize(14);
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        e.setBackground(bordered(Color.rgb(6, 16, 31), BORDER, 1, 16, c));
        e.setSingleLine(!multiline);
        if (multiline) {
            e.setMinLines(3);
            e.setMaxLines(6);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        return e;
    }

    public static LinearLayout topBar(Context c, String title, String sub, View.OnClickListener back) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (back != null) {
            Button b = ghost(c, "‹");
            b.setTextSize(26);
            b.setOnClickListener(back);
            row.addView(b, new LinearLayout.LayoutParams(dp(c, 48), dp(c, 46)));
        }
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView a = text(c, title, 22, TEXT, true);
        a.setGravity(Gravity.START);
        texts.addView(a);
        if (sub != null && !sub.isEmpty()) texts.addView(text(c, sub, 11, MUTED, false));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMargins(dp(c, 10), 0, 0, 0);
        row.addView(texts, tp);
        return row;
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchHeight(int px) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px);
    }

    public static LinearLayout spacer(Context c, int hDp) {
        LinearLayout l = new LinearLayout(c);
        l.setVisibility(View.INVISIBLE);
        l.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, hDp)));
        return l;
    }
}
