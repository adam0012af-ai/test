package com.adam.downloadhub;

import android.content.Context;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class YtDlpResolver {
    private static volatile boolean initialized;
    private static final Object LOCK = new Object();

    private YtDlpResolver() {}

    private static void ensureInit(Context context) throws YoutubeDLException {
        if (initialized) return;
        synchronized (LOCK) {
            if (initialized) return;
            YoutubeDL.getInstance().init(context.getApplicationContext());
            initialized = true;
        }
    }

    public static PlatformExtractor.MediaBundle extractOptions(Context context, String inputUrl) throws Exception {
        ensureInit(context);

        YoutubeDLRequest request = new YoutubeDLRequest(inputUrl);
        request.addOption("--no-playlist");
        request.addOption("--no-warnings");
        request.addOption("--geo-bypass");

        VideoInfo info = YoutubeDL.getInstance().getInfo(request);
        if (info == null) throw new IllegalStateException("المحرك المتقدم لم يرجع بيانات");

        String title = clean(info.getTitle());
        if (title.isEmpty()) title = "media";
        String platform = platform(inputUrl, info.getExtractor());

        List<MediaOption> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<VideoFormat> formats = info.getFormats();
        if (formats != null) {
            for (VideoFormat f : formats) {
                if (f == null) continue;
                String url = clean(f.getUrl());
                if (!isUsableDirectUrl(url)) continue;

                String vcodec = lower(f.getVcodec());
                String acodec = lower(f.getAcodec());
                boolean hasVideo = !vcodec.isEmpty() && !"none".equals(vcodec);
                boolean hasAudio = !acodec.isEmpty() && !"none".equals(acodec);
                if (!hasVideo && !hasAudio) continue;

                // Only expose complete video+audio files, never video-only fragments.
                if (hasVideo && !hasAudio) continue;

                boolean audioOnly = !hasVideo && hasAudio;
                String ext = clean(f.getExt());
                if (ext.isEmpty()) ext = audioOnly ? "m4a" : "mp4";
                long size = f.getFileSize() > 0 ? f.getFileSize() : f.getFileSizeApproximate();
                String label = audioOnly ? audioLabel(f) : videoLabel(f);
                String key = audioOnly ? "a:" + label + ":" + ext : "v:" + label + ":" + ext;
                if (!seen.add(key)) continue;

                String fileName = DownloadUtil.sanitizeFileName(title) + "_" + sanitize(label) + "." + ext;
                String contentType = audioOnly ? audioType(ext) : videoType(ext);
                out.add(new MediaOption(url, label, fileName, inputUrl, platform, contentType, size, audioOnly));
            }
        }

        // Ask yt-dlp for a single compatible A/V stream if the format table had no complete video.
        if (!containsVideo(out)) {
            try {
                YoutubeDLRequest best = new YoutubeDLRequest(inputUrl);
                best.addOption("--no-playlist");
                best.addOption("-f", "best[ext=mp4]/best");
                VideoInfo one = YoutubeDL.getInstance().getInfo(best);
                addSingle(out, seen, one, title, platform, inputUrl, false);
            } catch (Exception ignored) {}
        }

        // Ensure at least one audio-only choice when yt-dlp can expose one.
        if (!containsAudio(out)) {
            try {
                YoutubeDLRequest audio = new YoutubeDLRequest(inputUrl);
                audio.addOption("--no-playlist");
                audio.addOption("-f", "bestaudio");
                VideoInfo one = YoutubeDL.getInstance().getInfo(audio);
                addSingle(out, seen, one, title, platform, inputUrl, true);
            } catch (Exception ignored) {}
        }

        if (out.isEmpty()) throw new IllegalStateException("المحرك المتقدم لم يجد ملف فيديو/صوت مباشر قابل للحفظ");
        sort(out);
        return new PlatformExtractor.MediaBundle(title, platform, inputUrl, out);
    }

    private static void addSingle(List<MediaOption> out, Set<String> seen, VideoInfo info, String title,
                                  String platform, String referer, boolean audioOnly) {
        if (info == null) return;
        String url = clean(info.getUrl());
        if (!isUsableDirectUrl(url)) return;
        String ext = clean(info.getExt());
        if (ext.isEmpty()) ext = audioOnly ? "m4a" : "mp4";
        String label = audioOnly ? "صوت فقط" : (info.getHeight() > 0 ? info.getHeight() + "p" : "أفضل فيديو متوافق");
        String key = (audioOnly ? "a:" : "v:") + label + ":" + ext;
        if (!seen.add(key)) return;
        long size = info.getFileSize() > 0 ? info.getFileSize() : info.getFileSizeApproximate();
        String fileName = DownloadUtil.sanitizeFileName(title) + "_" + sanitize(label) + "." + ext;
        out.add(new MediaOption(url, label, fileName, referer, platform,
                audioOnly ? audioType(ext) : videoType(ext), size, audioOnly));
    }

    private static String videoLabel(VideoFormat f) {
        if (f.getHeight() > 0) return f.getHeight() + "p";
        String note = clean(f.getFormatNote());
        return note.isEmpty() ? "فيديو" : note;
    }

    private static String audioLabel(VideoFormat f) {
        if (f.getAbr() > 0) return "صوت " + f.getAbr() + " kbps";
        String note = clean(f.getFormatNote());
        return note.isEmpty() ? "صوت فقط" : "صوت " + note;
    }

    private static boolean containsVideo(List<MediaOption> list) {
        for (MediaOption o : list) if (!o.audioOnly) return true;
        return false;
    }

    private static boolean containsAudio(List<MediaOption> list) {
        for (MediaOption o : list) if (o.audioOnly) return true;
        return false;
    }

    private static void sort(List<MediaOption> list) {
        Collections.sort(list, new Comparator<MediaOption>() {
            @Override public int compare(MediaOption a, MediaOption b) {
                if (a.audioOnly != b.audioOnly) return a.audioOnly ? 1 : -1;
                int qa = quality(a.label), qb = quality(b.label);
                if (qa != qb) return Integer.compare(qb, qa);
                return Long.compare(b.sizeBytes, a.sizeBytes);
            }
        });
    }

    private static int quality(String label) {
        String s = lower(label);
        String digits = s.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try { return Integer.parseInt(digits); } catch (Exception ignored) {}
        }
        if (s.contains("4k")) return 2160;
        if (s.contains("2k")) return 1440;
        if (s.contains("hd")) return 720;
        return 0;
    }

    private static boolean isUsableDirectUrl(String url) {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return false;
        String l = url.toLowerCase(Locale.ROOT);
        return !l.contains(".m3u8") && !l.contains("manifest.mpd") && !l.startsWith("blob:");
    }

    private static String platform(String inputUrl, String extractor) {
        String ex = clean(extractor);
        if (!ex.isEmpty()) return pretty(ex);
        try {
            String h = new URI(inputUrl).getHost();
            String p = PlatformExtractor.platformName(h);
            return "Web".equals(p) ? "Media" : p;
        } catch (Exception e) {
            return "Media";
        }
    }

    private static String pretty(String s) {
        if (s == null || s.isEmpty()) return "Media";
        return s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static String videoType(String ext) {
        String e = lower(ext);
        if ("webm".equals(e)) return "video/webm";
        if ("mov".equals(e)) return "video/quicktime";
        return "video/mp4";
    }

    private static String audioType(String ext) {
        String e = lower(ext);
        if ("mp3".equals(e)) return "audio/mpeg";
        if ("ogg".equals(e) || "opus".equals(e)) return "audio/ogg";
        if ("aac".equals(e)) return "audio/aac";
        return "audio/mp4";
    }

    private static String sanitize(String s) {
        String x = clean(s).replaceAll("[^A-Za-z0-9_-]+", "_");
        return x.isEmpty() ? "media" : x;
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String lower(String s) { return clean(s).toLowerCase(Locale.ROOT); }
}
