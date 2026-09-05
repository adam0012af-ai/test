package com.adam.downloadhub;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String PREFS = "downloadhub_settings";
    private static final String WIFI_ONLY = "wifi_only";
    private static final String AUTO_BROWSER = "auto_browser";
    private static final String AUTO_CLIPBOARD = "auto_clipboard";
    private static final String SAVE_HISTORY = "save_history";

    private AppPrefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean wifiOnly(Context c) { return p(c).getBoolean(WIFI_ONLY, false); }
    public static void setWifiOnly(Context c, boolean v) { p(c).edit().putBoolean(WIFI_ONLY, v).apply(); }

    public static boolean autoBrowser(Context c) { return p(c).getBoolean(AUTO_BROWSER, true); }
    public static void setAutoBrowser(Context c, boolean v) { p(c).edit().putBoolean(AUTO_BROWSER, v).apply(); }

    public static boolean autoClipboard(Context c) { return p(c).getBoolean(AUTO_CLIPBOARD, true); }
    public static void setAutoClipboard(Context c, boolean v) { p(c).edit().putBoolean(AUTO_CLIPBOARD, v).apply(); }

    public static boolean saveHistory(Context c) { return p(c).getBoolean(SAVE_HISTORY, true); }
    public static void setSaveHistory(Context c, boolean v) { p(c).edit().putBoolean(SAVE_HISTORY, v).apply(); }
}
