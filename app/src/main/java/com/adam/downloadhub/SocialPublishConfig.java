package com.adam.downloadhub;

/**
 * Build-time configuration for official social publishing integrations.
 *
 * Public client identifiers may be injected by CI. Client secrets, refresh tokens,
 * and platform app secrets must stay on the backend and never be embedded in the APK.
 */
public final class SocialPublishConfig {
    private SocialPublishConfig() {}

    public static final String YOUTUBE_CLIENT_ID = safe(BuildConfig.YOUTUBE_CLIENT_ID);
    public static final String TIKTOK_CLIENT_KEY = safe(BuildConfig.TIKTOK_CLIENT_KEY);
    public static final String META_APP_ID = safe(BuildConfig.META_APP_ID);
    public static final String BACKEND_BASE_URL = trimSlash(safe(BuildConfig.PUBLISH_BACKEND_BASE_URL));

    public static boolean youtubeReady() {
        return !YOUTUBE_CLIENT_ID.isEmpty();
    }

    public static boolean tiktokReady() {
        return !TIKTOK_CLIENT_KEY.isEmpty() && backendReady();
    }

    public static boolean metaReady() {
        return !META_APP_ID.isEmpty() && backendReady();
    }

    public static boolean backendReady() {
        return BACKEND_BASE_URL.startsWith("https://");
    }

    public static boolean directReady(String platform) {
        String p = platform == null ? "" : platform.toLowerCase();
        if (p.contains("youtube")) return youtubeReady();
        if (p.contains("tiktok")) return tiktokReady();
        if (p.contains("instagram") || p.contains("facebook")) return metaReady();
        return false;
    }

    public static String statusText(String platform) {
        if (directReady(platform)) return "النشر المباشر: جاهز للربط الرسمي";
        String p = platform == null ? "" : platform.toLowerCase();
        if (p.contains("youtube")) return "النشر المباشر يحتاج YouTube OAuth Client ID";
        if (p.contains("tiktok")) return "النشر المباشر يحتاج TikTok Client Key + Backend آمن";
        if (p.contains("instagram") || p.contains("facebook")) return "النشر المباشر يحتاج Meta App ID + Backend آمن";
        return "النشر المباشر غير متاح لهذه المنصة";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
