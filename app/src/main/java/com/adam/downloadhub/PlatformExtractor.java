package com.adam.downloadhub;

import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformExtractor {
    private PlatformExtractor() {}

    public static VideoCandidate extract(String inputUrl) throws Exception {
        if (looksLikeDirectVideo(inputUrl)) {
            MediaValidator.ProbeResult probe = MediaValidator.probe(inputUrl, null, null);
            if (!probe.valid) throw new IllegalStateException(probe.reason);
            return new VideoCandidate(probe.finalUrl, DownloadUtil.guessFileName(probe.finalUrl, "video.mp4"), null);
        }

        String inputHost = safeHost(inputUrl);
        if (isTikTok(inputHost)) {
            try {
                return extractTikTokWithTikwm(inputUrl);
            } catch (Exception ignored) {
                // Fall back to page parsing, then BrowserCaptureActivity.
            }
        }

        NetUtil.FetchResult page = NetUtil.fetchPage(inputUrl);
        String html = page.body;
        String finalUrl = page.finalUrl;
        String host = safeHost(finalUrl);

        List<Pattern> patterns = new ArrayList<>();

        if (isTikTok(host)) {
            patterns.add(Pattern.compile("\\\"playAddr\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            patterns.add(Pattern.compile("\\\"playAddr\\\"\\s*:\\s*\\{.{0,2200}?\\\"src\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            patterns.add(Pattern.compile("\\\"PlayAddr\\\"\\s*:\\s*\\{.{0,4200}?\\\"UrlList\\\"\\s*:\\s*\\[\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            patterns.add(Pattern.compile("\\\"play_addr\\\"\\s*:\\s*\\{.{0,2200}?\\\"url_list\\\"\\s*:\\s*\\[\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        }

        patterns.add(Pattern.compile("\\\"browser_native_hd_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"browser_native_sd_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"video_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"contentUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"content_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("\\\"playbackUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));

        patterns.add(Pattern.compile("<meta[^>]+property=[\\\"']og:video(?::url|:secure_url)?[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+property=[\\\"']og:video(?::url|:secure_url)?[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<meta[^>]+name=[\\\"']twitter:player:stream[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<video[^>]+src=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("<source[^>]+src=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        patterns.add(Pattern.compile("[\\\"'](https?:\\\\?/\\\\?/[^\\\"']+?\\.(?:mp4|webm)(?:\\?[^\\\"']*)?)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));

        String lastReason = "";
        for (Pattern p : patterns) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                String candidate = normalizeUrl(m.group(1), finalUrl);
                if (!isDownloadableHttpVideo(candidate)) continue;
                if (isTikTok(host) && looksWatermarkedTikTokCandidate(candidate)) continue;

                MediaValidator.ProbeResult probe = MediaValidator.probe(candidate, finalUrl, null);
                if (!probe.valid) {
                    lastReason = probe.reason;
                    continue;
                }

                String title = extractTitle(html);
                String defaultName = platformDefaultName(host);
                String fileName = title.isEmpty() ? defaultName : DownloadUtil.sanitizeFileName(title) + ".mp4";
                return new VideoCandidate(probe.finalUrl, fileName, finalUrl);
            }
        }

        if (!lastReason.isEmpty()) {
            throw new IllegalStateException("تم العثور على وسائط لكن تم رفضها لأنها ليست ملف فيديو كامل: " + lastReason);
        }
        throw new IllegalStateException("لم أجد ملف فيديو كاملًا قابلًا للتحقق؛ استخدم المتصفح وشغّل الفيديو ثم اضغط تحقق ثم حمّل");
    }

    private static VideoCandidate extractTikTokWithTikwm(String inputUrl) throws Exception {
        String api = "https://www.tikwm.com/api/?url="
                + URLEncoder.encode(inputUrl, StandardCharsets.UTF_8.name()) + "&hd=1";
        String json = NetUtil.fetchText(api, "https://www.tikwm.com/");
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) throw new IllegalStateException("TikTok resolver returned no data");

        String video = firstNonEmpty(data.optString("hdplay"), data.optString("play"));
        if (video.isEmpty()) throw new IllegalStateException("TikTok resolver returned no video");
        if (video.startsWith("/")) video = "https://www.tikwm.com" + video;

        MediaValidator.ProbeResult probe = MediaValidator.probe(video, "https://www.tikwm.com/", null);
        if (!probe.valid) throw new IllegalStateException(probe.reason);

        String id = data.optString("id", "").trim();
        String title = data.optString("title", "").replaceAll("\\s+", " ").trim();
        if (title.length() > 80) title = title.substring(0, 80).trim();
        String name;
        if (!title.isEmpty()) name = "TikTok_clean_" + DownloadUtil.sanitizeFileName(title) + ".mp4";
        else if (!id.isEmpty()) name = "TikTok_clean_" + DownloadUtil.sanitizeFileName(id) + ".mp4";
        else name = "TikTok_clean_video.mp4";

        return new VideoCandidate(probe.finalUrl, name, "https://www.tikwm.com/");
    }

    public static boolean isKnownPlatform(String url) {
        String h = safeHost(url);
        return isTikTok(h)
                || h.contains("instagram.com")
                || h.contains("facebook.com") || h.equals("fb.watch")
                || h.equals("x.com") || h.contains("twitter.com")
                || h.contains("reddit.com") || h.contains("v.redd.it")
                || h.contains("pinterest.") || h.contains("pin.it")
                || h.contains("vimeo.com")
                || h.contains("dailymotion.com") || h.contains("dai.ly")
                || h.contains("twitch.tv")
                || h.contains("youtube.com") || h.equals("youtu.be");
    }

    private static String platformDefaultName(String host) {
        if (isTikTok(host)) return "TikTok_clean_video.mp4";
        if (host.contains("instagram")) return "Instagram_video.mp4";
        if (host.contains("facebook") || host.equals("fb.watch")) return "Facebook_video.mp4";
        if (host.equals("x.com") || host.contains("twitter")) return "X_video.mp4";
        if (host.contains("reddit") || host.contains("v.redd.it")) return "Reddit_video.mp4";
        if (host.contains("pinterest") || host.contains("pin.it")) return "Pinterest_video.mp4";
        if (host.contains("vimeo")) return "Vimeo_video.mp4";
        if (host.contains("dailymotion") || host.contains("dai.ly")) return "Dailymotion_video.mp4";
        if (host.contains("twitch")) return "Twitch_video.mp4";
        if (host.contains("youtube") || host.equals("youtu.be")) return "YouTube_video.mp4";
        return "video.mp4";
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
                .replace("\\u003D", "=")
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

    private static boolean looksWatermarkedTikTokCandidate(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("watermark=1") || lower.contains("watermark%3d1")
                || lower.contains("downloadaddr") || lower.contains("/download/");
    }

    private static boolean isDownloadableHttpVideo(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        if (MediaValidator.structuralProblem(url) != null) return false;
        return lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".mov")
                || lower.contains(".m4v") || lower.contains("video") || lower.contains("play");
    }

    private static boolean looksLikeDirectVideo(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi)(\\?.*)?$");
    }

    private static boolean isTikTok(String host) {
        return host.contains("tiktok.com") || host.contains("tiktokv.com") || host.contains("tiktokcdn.com");
    }

    private static String safeHost(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? "" : h.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
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
