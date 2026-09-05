package com.adam.downloadhub;

import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformExtractor {
    private PlatformExtractor() {}

    public static VideoCandidate extract(String inputUrl) throws Exception {
        MediaBundle bundle = extractOptions(inputUrl);
        for (MediaOption o : bundle.options) {
            if (!o.audioOnly) return new VideoCandidate(o.url, o.fileName, o.referer);
        }
        if (!bundle.options.isEmpty()) {
            MediaOption o = bundle.options.get(0);
            return new VideoCandidate(o.url, o.fileName, o.referer);
        }
        throw new IllegalStateException("لم يتم العثور على وسائط صالحة");
    }

    public static MediaBundle extractOptions(String inputUrl) throws Exception {
        if (inputUrl == null || (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("الرابط غير صالح");
        }

        String inputHost = safeHost(inputUrl);
        String platform = platformName(inputHost);

        if (looksLikeDirectMedia(inputUrl)) {
            MediaValidator.ProbeResult p = MediaValidator.probeMedia(inputUrl, null, null, true);
            if (!p.valid) throw new IllegalStateException(p.reason);
            List<MediaOption> one = new ArrayList<>();
            one.add(optionFromProbe(p, p.audioOnly ? "صوت فقط" : inferQuality(inputUrl, "Direct"),
                    "download", null, platform));
            return new MediaBundle("Direct media", platform, inputUrl, one);
        }

        if (isTikTok(inputHost)) {
            try {
                MediaBundle t = extractTikTokOptions(inputUrl);
                if (!t.options.isEmpty()) return t;
            } catch (Exception ignored) {}
        }

        NetUtil.FetchResult page = NetUtil.fetchPage(inputUrl);
        String html = page.body;
        String finalUrl = page.finalUrl;
        String host = safeHost(finalUrl);
        platform = platformName(host.isEmpty() ? inputHost : host);
        String title = extractTitle(html);
        if (title.isEmpty()) title = platform + " media";

        List<CandidatePattern> patterns = new ArrayList<>();
        if (isTikTok(host)) {
            patterns.add(cp("TikTok Playback", "\\\"playAddr\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
            patterns.add(cp("TikTok Playback", "\\\"play_addr\\\"\\s*:\\s*\\{.{0,2600}?\\\"url_list\\\"\\s*:\\s*\\[\\s*\\\"([^\\\"]+)\\\""));
        }
        patterns.add(cp("HD", "\\\"browser_native_hd_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("SD", "\\\"browser_native_sd_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("Video", "\\\"video_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("Video", "\\\"contentUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("Video", "\\\"content_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("Video", "\\\"playbackUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("صوت فقط", "\\\"audio_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("صوت فقط", "\\\"audioUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("صوت فقط", "\\\"music_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        patterns.add(cp("Video", "<meta[^>]+property=[\\\"']og:video(?::url|:secure_url)?[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']"));
        patterns.add(cp("Video", "<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+property=[\\\"']og:video(?::url|:secure_url)?[\\\"']"));
        patterns.add(cp("Video", "<meta[^>]+name=[\\\"']twitter:player:stream[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']"));
        patterns.add(cp("Video", "<video[^>]+src=[\\\"']([^\\\"']+)[\\\"']"));
        patterns.add(cp("Video", "<source[^>]+src=[\\\"']([^\\\"']+)[\\\"']"));
        patterns.add(cp("Video", "[\\\"'](https?:\\\\?/\\\\?/[^\\\"']+?\\.(?:mp4|webm|mov)(?:\\?[^\\\"']*)?)[\\\"']"));
        patterns.add(cp("صوت فقط", "[\\\"'](https?:\\\\?/\\\\?/[^\\\"']+?\\.(?:m4a|mp3|aac|opus|ogg)(?:\\?[^\\\"']*)?)[\\\"']"));

        Map<String, MediaOption> found = new LinkedHashMap<>();
        String lastReason = "";
        outer:
        for (CandidatePattern item : patterns) {
            Matcher m = item.pattern.matcher(html);
            while (m.find()) {
                String candidate = normalizeUrl(m.group(1), finalUrl);
                if (!isHttp(candidate)) continue;
                if (isTikTok(host) && looksWatermarkedTikTokCandidate(candidate)) continue;
                if (found.containsKey(candidate)) continue;

                MediaValidator.ProbeResult probe = MediaValidator.probeMedia(candidate, finalUrl, null, true);
                if (!probe.valid) { lastReason = probe.reason; continue; }
                if (found.containsKey(probe.finalUrl)) continue;

                String label = probe.audioOnly ? "صوت فقط" : inferQuality(probe.finalUrl, item.hint);
                MediaOption option = optionFromProbe(probe, label, title, finalUrl, platform);
                found.put(probe.finalUrl, option);
                if (found.size() >= 12) break outer;
            }
        }

        List<MediaOption> options = new ArrayList<>(found.values());
        sortOptions(options);
        if (options.isEmpty()) {
            if (!lastReason.isEmpty()) throw new IllegalStateException("تم رصد وسائط لكنها ليست ملفًا كاملًا: " + lastReason);
            throw new IllegalStateException("لم يتم العثور على ملف فيديو/صوت كامل؛ جرّب متصفح الالتقاط وشغّل الفيديو");
        }
        return new MediaBundle(title, platform, finalUrl, options);
    }

    private static MediaBundle extractTikTokOptions(String inputUrl) throws Exception {
        String api = "https://www.tikwm.com/api/?url=" + URLEncoder.encode(inputUrl, StandardCharsets.UTF_8.name()) + "&hd=1";
        String json = NetUtil.fetchText(api, "https://www.tikwm.com/");
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) throw new IllegalStateException("TikTok resolver returned no data");

        String title = cleanTitle(data.optString("title", ""));
        String id = data.optString("id", "").trim();
        if (title.isEmpty()) title = id.isEmpty() ? "TikTok video" : "TikTok " + id;

        Map<String, MediaOption> found = new LinkedHashMap<>();
        addTikTokOption(found, data.optString("hdplay"), "HD • بدون علامة", title, false);
        addTikTokOption(found, data.optString("play"), "Standard • بدون علامة", title, false);
        String audio = firstNonEmpty(data.optString("music"), data.optString("music_info"));
        if (!audio.isEmpty()) addTikTokOption(found, audio, "صوت فقط", title, true);

        List<MediaOption> options = new ArrayList<>(found.values());
        sortOptions(options);
        if (options.isEmpty()) throw new IllegalStateException("لم يتم العثور على نسخة TikTok صالحة");
        return new MediaBundle(title, "TikTok", inputUrl, options);
    }

    private static void addTikTokOption(Map<String, MediaOption> out, String raw, String label, String title, boolean audio) {
        try {
            if (raw == null || raw.trim().isEmpty()) return;
            String u = raw.trim();
            if (u.startsWith("/")) u = "https://www.tikwm.com" + u;
            if (looksWatermarkedTikTokCandidate(u)) return;
            MediaValidator.ProbeResult p = MediaValidator.probeMedia(u, "https://www.tikwm.com/", null, true);
            if (!p.valid || (audio && !p.audioOnly)) return;
            out.put(p.finalUrl, optionFromProbe(p, p.audioOnly ? "صوت فقط" : label, title, "https://www.tikwm.com/", "TikTok"));
        } catch (Exception ignored) {}
    }

    private static MediaOption optionFromProbe(MediaValidator.ProbeResult p, String label, String title, String referer, String platform) {
        String ext = extensionFor(p);
        String base = DownloadUtil.sanitizeFileName(cleanTitle(title));
        if (base.isEmpty()) base = DownloadUtil.sanitizeFileName(platform + "_media");
        String suffix = p.audioOnly ? "_audio" : "_" + sanitizeLabel(label);
        String name = base + suffix + "." + ext;
        return new MediaOption(p.finalUrl, label, name, referer, platform, p.contentType, p.sizeBytes, p.audioOnly);
    }

    private static String extensionFor(MediaValidator.ProbeResult p) {
        String type = p.contentType == null ? "" : p.contentType.toLowerCase(Locale.ROOT);
        String u = p.finalUrl == null ? "" : p.finalUrl.toLowerCase(Locale.ROOT);
        if (p.audioOnly) {
            if (type.contains("mpeg") || u.contains(".mp3")) return "mp3";
            if (type.contains("ogg") || u.contains(".ogg")) return "ogg";
            if (type.contains("opus") || u.contains(".opus")) return "opus";
            if (type.contains("aac") || u.contains(".aac")) return "aac";
            return "m4a";
        }
        if (type.contains("webm") || u.contains(".webm")) return "webm";
        if (type.contains("quicktime") || u.contains(".mov")) return "mov";
        return "mp4";
    }

    private static String inferQuality(String url, String hint) {
        String all = ((hint == null ? "" : hint) + " " + (url == null ? "" : url)).toLowerCase(Locale.ROOT);
        Matcher q = Pattern.compile("(?:^|[^0-9])(2160|1440|1080|720|540|480|360|240)(?:p|[^0-9]|$)").matcher(all);
        if (q.find()) return q.group(1) + "p";
        if (all.contains("4k")) return "4K";
        if (all.contains("2k")) return "2K";
        if (all.contains("fullhd") || all.contains("full_hd")) return "1080p";
        if (all.contains("hd")) return "HD";
        if (all.contains("sd")) return "SD";
        return hint == null || hint.isEmpty() ? "Video" : hint;
    }

    private static int qualityRank(MediaOption o) {
        if (o.audioOnly) return -1;
        String l = o.label.toLowerCase(Locale.ROOT);
        if (l.contains("4k") || l.contains("2160")) return 2160;
        if (l.contains("1440") || l.contains("2k")) return 1440;
        if (l.contains("1080")) return 1080;
        if (l.contains("720") || l.contains("hd")) return 720;
        if (l.contains("540")) return 540;
        if (l.contains("480") || l.contains("sd")) return 480;
        if (l.contains("360")) return 360;
        return 500;
    }

    private static void sortOptions(List<MediaOption> options) {
        Collections.sort(options, new Comparator<MediaOption>() {
            @Override public int compare(MediaOption a, MediaOption b) {
                if (a.audioOnly != b.audioOnly) return a.audioOnly ? 1 : -1;
                int qa = qualityRank(a), qb = qualityRank(b);
                if (qa != qb) return Integer.compare(qb, qa);
                return Long.compare(b.sizeBytes, a.sizeBytes);
            }
        });
    }

    public static boolean isKnownPlatform(String url) { return !"Web".equals(platformName(safeHost(url))); }

    public static String platformName(String host) {
        String h = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (h.contains("tiktok")) return "TikTok";
        if (h.contains("youtube") || h.equals("youtu.be")) return "YouTube";
        if (h.contains("instagram")) return "Instagram";
        if (h.contains("facebook") || h.equals("fb.watch")) return "Facebook";
        if (h.equals("x.com") || h.contains("twitter")) return "X";
        if (h.contains("reddit") || h.contains("v.redd.it")) return "Reddit";
        if (h.contains("pinterest") || h.contains("pin.it")) return "Pinterest";
        if (h.contains("vimeo")) return "Vimeo";
        if (h.contains("dailymotion") || h.contains("dai.ly")) return "Dailymotion";
        if (h.contains("twitch")) return "Twitch";
        if (h.contains("threads.net")) return "Threads";
        if (h.contains("snapchat")) return "Snapchat";
        if (h.contains("likee")) return "Likee";
        if (h.contains("kwai")) return "Kwai";
        if (h.contains("vk.com") || h.contains("vkvideo")) return "VK";
        if (h.contains("tumblr")) return "Tumblr";
        if (h.contains("streamable")) return "Streamable";
        if (h.contains("rumble")) return "Rumble";
        return "Web";
    }

    private static String extractTitle(String html) {
        Pattern[] ps = new Pattern[] {
                Pattern.compile("<meta[^>]+property=[\\\"']og:title[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
                Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
        };
        for (Pattern p : ps) {
            Matcher m = p.matcher(html);
            if (m.find()) return cleanTitle(htmlDecode(m.group(1)));
        }
        return "";
    }

    private static String cleanTitle(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > 90) s = s.substring(0, 90).trim();
        return s;
    }

    private static String normalizeUrl(String raw, String base) {
        if (raw == null) return null;
        String s = raw.trim().replace("\\u002F", "/").replace("\\u002f", "/")
                .replace("\\u0026", "&").replace("\\u003D", "=").replace("\\/", "/");
        s = htmlDecode(s);
        try {
            if (s.startsWith("//")) s = URI.create(base).getScheme() + ":" + s;
            else if (s.startsWith("/")) s = new URL(new URL(base), s).toString();
        } catch (Exception ignored) {}
        return s;
    }

    private static String htmlDecode(String s) {
        return s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">");
    }

    private static boolean looksWatermarkedTikTokCandidate(String url) {
        String l = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return l.contains("watermark=1") || l.contains("watermark%3d1") || l.contains("downloadaddr") || l.contains("/download/");
    }

    private static boolean looksLikeDirectMedia(String url) {
        String l = url.toLowerCase(Locale.ROOT);
        return l.matches(".*\\.(mp4|m4v|webm|mov|mkv|avi|m4a|mp3|aac|ogg|opus|wav|flac)(\\?.*)?$");
    }

    private static boolean isTikTok(String host) { return host.contains("tiktok.com") || host.contains("tiktokv.com") || host.contains("tiktokcdn.com"); }
    private static boolean isHttp(String s) { return s != null && (s.startsWith("http://") || s.startsWith("https://")); }
    private static String safeHost(String url) {
        try { String h = URI.create(url).getHost(); return h == null ? "" : h.toLowerCase(Locale.ROOT); }
        catch (Exception e) { return ""; }
    }
    private static String sanitizeLabel(String s) { return DownloadUtil.sanitizeFileName((s == null ? "media" : s).replace("•", " ").replaceAll("\\s+", "_")); }
    private static String firstNonEmpty(String... values) { for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim(); return ""; }
    private static CandidatePattern cp(String hint, String regex) { return new CandidatePattern(hint, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)); }

    private static final class CandidatePattern {
        final String hint; final Pattern pattern;
        CandidatePattern(String hint, Pattern pattern) { this.hint = hint; this.pattern = pattern; }
    }

    public static final class MediaBundle {
        public final String title;
        public final String platform;
        public final String pageUrl;
        public final List<MediaOption> options;
        MediaBundle(String title, String platform, String pageUrl, List<MediaOption> options) {
            this.title = title; this.platform = platform; this.pageUrl = pageUrl; this.options = options;
        }
    }

    public static final class VideoCandidate {
        public final String url; public final String fileName; public final String referer;
        public VideoCandidate(String url, String fileName, String referer) { this.url = url; this.fileName = fileName; this.referer = referer; }
    }
}
