package com.adam.downloadhub;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class NetUtil {
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0 Mobile Safari/537.36";
    private static final int MAX_TEXT_BYTES = 16 * 1024 * 1024;

    private NetUtil() {}

    public static String fetchText(String url, String referer) throws Exception {
        HttpURLConnection c = open(url, referer);
        int code = c.getResponseCode();
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            return readText(in, MAX_TEXT_BYTES);
        } finally {
            c.disconnect();
        }
    }

    public static FetchResult fetchPage(String url) throws Exception {
        HttpURLConnection c = open(url, url);
        int code = c.getResponseCode();
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }
        String finalUrl = c.getURL().toString();
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            return new FetchResult(finalUrl, readText(in, MAX_TEXT_BYTES));
        } finally {
            c.disconnect();
        }
    }

    private static HttpURLConnection open(String url, String referer) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", USER_AGENT);
        c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,text/plain,*/*");
        c.setRequestProperty("Accept-Language", "ar,en-US;q=0.9,en;q=0.8");
        if (referer != null && !referer.isEmpty()) c.setRequestProperty("Referer", referer);
        return c;
    }

    public static String readUriText(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("تعذر فتح الملف");
            return readText(in, MAX_TEXT_BYTES);
        }
    }

    private static String readText(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) throw new IllegalStateException("الملف كبير جدًا للمعالجة النصية");
            out.write(buf, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    public static final class FetchResult {
        public final String finalUrl;
        public final String body;
        public FetchResult(String finalUrl, String body) {
            this.finalUrl = finalUrl;
            this.body = body;
        }
    }
}
