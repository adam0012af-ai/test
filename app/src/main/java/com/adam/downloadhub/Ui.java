package com.adam.downloadhub;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(6, 10, 18);
    public static final int SURFACE = Color.rgb(13, 20, 32);
    public static final int SURFACE_2 = Color.rgb(18, 28, 44);
    public static final int BORDER = Color.rgb(38, 58, 82);
    public static final int TEXT = Color.rgb(246, 249, 255);
    public static final int MUTED = Color.rgb(151, 166, 188);
    public static final int BLUE = Color.rgb(36, 119, 255);
    public static final int CYAN = Color.rgb(17, 214, 255);
    public static final int GREEN = Color.rgb(87, 220, 154);
    public static final int YELLOW = Color.rgb(255, 200, 88);
    public static final int RED = Color.rgb(245, 105, 120);

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

    public static TextView text(Context c, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
        b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.setBackground(gradient(BLUE, Color.rgb(35, 86, 226), 16, c));
        b.setStateListAnimator(null);
        return b;
    }

    public static Button secondary(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.setBackground(bordered(SURFACE_2, BORDER, 1, 14, c));
        b.setStateListAnimator(null);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        l.setBackground(bordered(SURFACE, BORDER, 1, 20, c));
        l.setElevation(dp(c, 2));
        return l;
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
