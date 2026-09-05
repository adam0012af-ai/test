package com.adam.downloadhub;

import java.util.Locale;

public final class MediaOption {
    public final String url;
    public final String label;
    public final String fileName;
    public final String referer;
    public final String platform;
    public final String contentType;
    public final long sizeBytes;
    public final boolean audioOnly;

    public MediaOption(String url, String label, String fileName, String referer,
                       String platform, String contentType, long sizeBytes, boolean audioOnly) {
        this.url = url == null ? "" : url;
        this.label = label == null ? "وسائط" : label;
        this.fileName = fileName == null ? "download" : fileName;
        this.referer = referer;
        this.platform = platform == null || platform.isEmpty() ? "Other" : platform;
        this.contentType = contentType == null ? "" : contentType;
        this.sizeBytes = sizeBytes;
        this.audioOnly = audioOnly;
    }

    public String displayLabel() {
        StringBuilder s = new StringBuilder();
        s.append(audioOnly ? "🎵 " : "🎬 ").append(label);
        if (sizeBytes > 0) s.append("  •  ").append(formatBytes(sizeBytes));
        if (!contentType.isEmpty()) {
            String t = contentType;
            int slash = t.indexOf('/');
            if (slash >= 0 && slash + 1 < t.length()) t = t.substring(slash + 1);
            int semi = t.indexOf(';');
            if (semi > 0) t = t.substring(0, semi);
            if (!t.isEmpty() && !"*".equals(t)) s.append("  •  ").append(t.toUpperCase(Locale.ROOT));
        }
        return s.toString();
    }

    public static String formatBytes(long b) {
        if (b <= 0) return "";
        double mb = b / 1024.0 / 1024.0;
        if (mb < 1.0) return String.format(Locale.US, "%.0f KB", b / 1024.0);
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }
}
