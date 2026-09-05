package com.adam.downloadhub;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformExtractor {
    private PlatformExtractor() {}

    public static VideoCandidate extract(String inputUrl) throws Exception {
        if (looksLikeDirectVideo(inputUrl)) {
            return new VideoCandidate(inputUrl, DownloadUtil.guessFileName(inputUrl, "video.mp4"), null);
        }

        NetUtil.FetchResult page = NetUtil.fetchPage(inputUrl);
        String html = page.body;
        String finalUrl = page.finalUrl;
        String host = safeHost(finalUrl);

        List<Pattern> patterns = new ArrayList<>();

        // TikTok public pages often expose a clean playback address in embedded JSON.
        if (host.contains("tiktok.com")) {
            patterns.add(Pattern.compile("\\\"playAddr\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            patterns.add(Pattern.compile("\\\"playAddr\\\"\\s*:\\s*\\{.{0,1600}?\\\"src\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            patterns.add(Pattern.compile("\\\"play_addr\\\"\\s*:\\s*\\{.{0,2000}?\\\"url_list\\\"\\s*:\\s*\\[\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        }

        // Generic public-page video hints.
        patterns.add(Pattern.compile("<meta[^>]+property=[\\\"']og:video(?::url)?[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+property=[\\\"']og:video(?::url)?[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<video[^>]+src=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<source[^>]+src=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"contentUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"content_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));

        for (Pattern p : patterns) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                String candidate = normalizeUrl(m.group(1), finalUrl);
                if (candidate == null) continue;
                if (candidate.toLowerCase(Locale.ROOT).contains(".m3u8")) continue;
                if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) continue;

                String title = extractTitle(html);
                String defaultName = host.contains("tiktok.com") ? "TikTok_video.mp4" : "video.mp4";
                String fileName = title.isEmpty() ? defaultName : DownloadUtil.sanitizeFileName(title) + ".mp4";
                return new VideoCandidate(candidate, fileName, finalUrl);
            }
        }

        throw new IllegalStateException("لم أجد رابط فيديو عام مباشر. قد تحتاج المنصة تسجيل دخول أو تستخدم HLS/DRM أو غيّرت طريقة عرض الصفحة");
    }

    private static String extractTitle(String html) {
        Pattern[] titlePatterns = new Pattern[] {
                Pattern.compile("<meta[^>]+property=[\\\"']og:title[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
                Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
        };
        for (Pattern p : titlePatterns) {
            Matcher m = p.matcher(html);
            if (m.find()) {
                String s = htmlDecode(m.group(1)).replaceAll("\\s+", " ").trim();
                if (s.length() > 90) s = s.substring(0, 90).trim();
                return s;
            }
        }
        return "";
    }

    private static String normalizeUrl(String raw, String base) {
        if (raw == null) return null;
        String s = raw.trim();
        s = s.replace("\\u002F", "/")
                .replace("\\u002f", "/")
                .replace("\\u0026", "&")
                .replace("\\/", "/");
        s = htmlDecode(s);
        try {
            if (s.startsWith("//")) {
                URI b = URI.create(base);
                s = b.getScheme() + ":" + s;
            } else if (s.startsWith("/")) {
                s = new URL(new URL(base), s).toString();
            }
        } catch (Exception ignored) {}
        return s;
    }

    private static String htmlDecode(String s) {
        return s.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private static boolean looksLikeDirectVideo(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi|ts)(\\?.*)?$");
    }

    private static String safeHost(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? "" : h.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    public static final class VideoCandidate {
        public final String url;
        public final String fileName;
        public final String referer;

        public VideoCandidate(String url, String fileName, String referer) {
            this.url = url;
            this.fileName = fileName;
            this.referer = referer;
        }
    }
}
