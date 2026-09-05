package com.adam.downloadhub;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String PREFS = "downloadhub_settings";
    private static final String WIFI_ONLY = "wifi_only";
    private static final String AUTO_CLIPBOARD = "auto_clipboard";
    private static final String SAVE_HISTORY = "save_history";
    private static final String ORGANIZE_FOLDERS = "organize_folders";
    private static final String PRIVATE_MODE = "private_mode";
    private static final String AUTO_RETRY = "auto_retry";
    private static final String DEFAULT_MODE = "default_mode";
    private static final String DUPLICATE_SHIELD = "duplicate_shield";
    private static final String LINK_CLEANER = "link_cleaner";
    private static final String DOWNLOAD_PROFILE = "download_profile";
    private static final String EXPORT_QUALITY = "creator_export_quality";
    private static final String CREATOR_WATERMARK = "creator_watermark";
    private static final String AUTO_PUBLISH_TEXT = "auto_publish_text";

    private AppPrefs() {}

    private static SharedPreferences p(Context c) {return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);}

    public static boolean wifiOnly(Context c) { return p(c).getBoolean(WIFI_ONLY, false); }
    public static void setWifiOnly(Context c, boolean v) { p(c).edit().putBoolean(WIFI_ONLY, v).apply(); }
    public static boolean autoClipboard(Context c) { return p(c).getBoolean(AUTO_CLIPBOARD, true); }
    public static void setAutoClipboard(Context c, boolean v) { p(c).edit().putBoolean(AUTO_CLIPBOARD, v).apply(); }
    public static boolean saveHistory(Context c) { return p(c).getBoolean(SAVE_HISTORY, true) && !privateMode(c); }
    public static void setSaveHistory(Context c, boolean v) { p(c).edit().putBoolean(SAVE_HISTORY, v).apply(); }
    public static boolean organizeFolders(Context c) { return p(c).getBoolean(ORGANIZE_FOLDERS, true); }
    public static void setOrganizeFolders(Context c, boolean v) { p(c).edit().putBoolean(ORGANIZE_FOLDERS, v).apply(); }
    public static boolean privateMode(Context c) { return p(c).getBoolean(PRIVATE_MODE, false); }
    public static void setPrivateMode(Context c, boolean v) { p(c).edit().putBoolean(PRIVATE_MODE, v).apply(); }
    public static boolean autoRetry(Context c) { return p(c).getBoolean(AUTO_RETRY, true); }
    public static void setAutoRetry(Context c, boolean v) { p(c).edit().putBoolean(AUTO_RETRY, v).apply(); }
    public static boolean duplicateShield(Context c) { return p(c).getBoolean(DUPLICATE_SHIELD, true); }
    public static void setDuplicateShield(Context c, boolean v) { p(c).edit().putBoolean(DUPLICATE_SHIELD, v).apply(); }
    public static boolean linkCleaner(Context c) { return p(c).getBoolean(LINK_CLEANER, true); }
    public static void setLinkCleaner(Context c, boolean v) { p(c).edit().putBoolean(LINK_CLEANER, v).apply(); }

    public static String downloadProfile(Context c) { return p(c).getString(DOWNLOAD_PROFILE, "balanced"); }
    public static void setDownloadProfile(Context c, String v) {if (!"max".equals(v) && !"balanced".equals(v) && !"saver".equals(v)) v = "balanced";p(c).edit().putString(DOWNLOAD_PROFILE, v).apply();}
    public static String defaultMode(Context c) { return p(c).getString(DEFAULT_MODE, "ask"); }
    public static void setDefaultMode(Context c, String v) {if (!"ask".equals(v) && !"best".equals(v) && !"audio".equals(v)) v = "ask";p(c).edit().putString(DEFAULT_MODE, v).apply();}

    public static int exportQuality(Context c){return p(c).getInt(EXPORT_QUALITY,1080);}
    public static void setExportQuality(Context c,int v){p(c).edit().putInt(EXPORT_QUALITY,v==720?720:1080).apply();}
    public static boolean creatorWatermark(Context c){return p(c).getBoolean(CREATOR_WATERMARK,false);}
    public static void setCreatorWatermark(Context c,boolean v){p(c).edit().putBoolean(CREATOR_WATERMARK,v).apply();}
    public static boolean autoPublishText(Context c){return p(c).getBoolean(AUTO_PUBLISH_TEXT,true);}
    public static void setAutoPublishText(Context c,boolean v){p(c).edit().putBoolean(AUTO_PUBLISH_TEXT,v).apply();}
}
