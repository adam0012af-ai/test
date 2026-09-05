package com.adam.downloadhub;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MediaValidator {
    private static final Pattern TOTAL = Pattern.compile("/(\\d+)$");
    private MediaValidator() {}

    public static ProbeResult probe(String url, String referer, String cookie) {
        return probeMedia(url, referer, cookie, false);
    }

    public static ProbeResult probeMedia(String url, String referer, String cookie, boolean allowAudio) {
        String structural = structuralProblem(url);
        if (structural != null) return ProbeResult.bad(structural);

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(12000);
            c.setReadTimeout(12000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", NetUtil.USER_AGENT);
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("Range", "bytes=0-1");
            if (referer != null && !referer.isEmpty()) c.setRequestProperty("Referer", referer);
            if (cookie != null && !cookie.isEmpty()) c.setRequestProperty("Cookie", cookie);

            int code = c.getResponseCode();
            if (code == 401 || code == 403) return ProbeResult.bad("المصدر رفض طلب التحميل أو يحتاج جلسة صالحة");
            if (code == 404 || code == 410) return ProbeResult.bad("رابط الوسائط انتهى أو لم يعد موجودًا");
            if (code < 200 || code >= 400) return ProbeResult.bad("المصدر أعاد HTTP " + code);

            String finalUrl = c.getURL().toString();
            String again = structuralProblem(finalUrl);
            if (again != null) return ProbeResult.bad(again);

            String type = lower(c.getContentType());
            String disposition = lower(c.getHeaderField("Content-Disposition"));
            long size = fullSize(c);

            if (type.contains("mpegurl") || type.contains("dash+xml"))
                return ProbeResult.bad("المصدر قائمة بث مجزأ وليس ملف وسائط كامل");
            if (type.startsWith("text/") || type.contains("html") || type.contains("json") || type.contains("javascript"))
                return ProbeResult.bad("الرابط لا يعيد ملف وسائط صالحًا");

            boolean audioMime = type.startsWith("audio/");
            boolean videoMime = type.startsWith("video/");
            boolean audioName = looksAudioFile(finalUrl) || looksAudioFile(disposition);
            boolean videoName = looksVideoFile(finalUrl) || looksVideoFile(disposition);
            boolean genericBinary = type.isEmpty() || type.contains("octet-stream");

            if (audioMime || (genericBinary && audioName)) {
                if (!allowAudio) return ProbeResult.bad("المصدر صوت فقط وليس فيديو كاملًا");
                if (size > 0 && size < 8 * 1024L) return ProbeResult.bad("حجم ملف الصوت صغير جدًا ويبدو جزءًا غير مكتمل");
                return ProbeResult.good(finalUrl, type.isEmpty() ? "audio/*" : type, size, true);
            }

            if (!videoMime && !(genericBinary && videoName))
                return ProbeResult.bad("لم يتم التأكد أن المصدر ملف فيديو/صوت كامل");
            if (size > 0 && size < 64 * 1024L)
                return ProbeResult.bad("حجم المصدر صغير جدًا ويبدو جزءًا من الفيديو");

            return ProbeResult.good(finalUrl, type, size, false);
        } catch (Exception e) {
            return ProbeResult.bad("تعذر التحقق من الوسائط: " + safeMessage(e));
        } finally {
            if (c != null) c.disconnect();
        }
    }

    public static String structuralProblem(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://")))
            return "المصدر ليس رابط HTTP صالحًا";
        String l = url.toLowerCase(Locale.ROOT);
        if (l.contains(".m3u8") || l.contains(".mpd") || l.contains(".m4s")
                || l.contains("/segment/") || l.contains("/segments/")
                || l.contains("init.mp4") || l.contains("init.m4s")
                || hasParam(l, "range") || hasParam(l, "segment")
                || hasParam(l, "fragment") || hasParam(l, "frag")
                || hasParam(l, "part") || hasParam(l, "sq"))
            return "تم رصد جزء/Segment من البث وليس ملف وسائط كامل";
        return null;
    }

    public static boolean looksVideoFile(String value) {
        if (value == null) return false;
        String l = value.toLowerCase(Locale.ROOT);
        return l.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi)(?:[?&#;\"'].*)?$")
                || (l.contains("filename=") && (l.contains(".mp4") || l.contains(".webm") || l.contains(".mkv")));
    }

    public static boolean looksAudioFile(String value) {
        if (value == null) return false;
        String l = value.toLowerCase(Locale.ROOT);
        return l.matches(".*\\.(m4a|mp3|aac|ogg|opus|wav|flac)(?:[?&#;\"'].*)?$")
                || (l.contains("filename=") && (l.contains(".m4a") || l.contains(".mp3") || l.contains(".aac") || l.contains(".opus")));
    }

    private static boolean hasParam(String lowerUrl, String name) {
        return lowerUrl.matches(".*[?&]" + Pattern.quote(name) + "=[^&]*.*");
    }

    private static long fullSize(HttpURLConnection c) {
        try {
            String range = c.getHeaderField("Content-Range");
            if (range != null) {
                Matcher m = TOTAL.matcher(range.trim());
                if (m.find()) return Long.parseLong(m.group(1));
            }
        } catch (Exception ignored) {}
        try { return c.getContentLengthLong(); } catch (Exception ignored) { return -1L; }
    }

    private static String lower(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
    private static String safeMessage(Throwable t) {
        String m = t == null ? null : t.getMessage();
        return m == null || m.trim().isEmpty() ? "خطأ اتصال" : m.trim();
    }

    public static final class ProbeResult {
        public final boolean valid;
        public final String finalUrl;
        public final String contentType;
        public final long sizeBytes;
        public final String reason;
        public final boolean audioOnly;

        private ProbeResult(boolean valid, String finalUrl, String contentType, long sizeBytes, String reason, boolean audioOnly) {
            this.valid = valid;
            this.finalUrl = finalUrl == null ? "" : finalUrl;
            this.contentType = contentType == null ? "" : contentType;
            this.sizeBytes = sizeBytes;
            this.reason = reason == null ? "" : reason;
            this.audioOnly = audioOnly;
        }

        static ProbeResult good(String url, String type, long size, boolean audioOnly) {
            return new ProbeResult(true, url, type, size, "", audioOnly);
        }
        static ProbeResult bad(String reason) { return new ProbeResult(false, "", "", -1L, reason, false); }

        public String summary() {
            if (!valid) return reason;
            String kind = audioOnly ? "ملف الصوت" : "ملف الفيديو";
            if (sizeBytes <= 0) return "تم التحقق من " + kind + " ✅";
            double mb = sizeBytes / 1024.0 / 1024.0;
            return String.format(Locale.US, "تم التحقق من %s ✅ • %.1f MB", kind, mb);
        }
    }
}
