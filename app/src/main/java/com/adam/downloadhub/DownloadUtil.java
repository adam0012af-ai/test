package com.adam.downloadhub;

import android.webkit.URLUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class DownloadUtil {
    private DownloadUtil() {}

    public static String guessFileName(String url, String fallback) {
        try {
            String guessed = URLUtil.guessFileName(url, null, null);
            if (guessed != null && !guessed.trim().isEmpty()) return sanitizeFileName(guessed);
        } catch (Exception ignored) {}
        return sanitizeFileName(fallback);
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "download.bin";
        String s = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        while (s.contains("  ")) s = s.replace("  ", " ");
        if (s.length() > 150) s = s.substring(0, 150);
        return s.isEmpty() ? "download.bin" : s;
    }
}
