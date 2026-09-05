package com.adam.downloadhub;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class TextExporter {
    private TextExporter() {}

    public static ExportResult export(Context context, M3uParser.Result result) throws Exception {
        Uri channels = write(context, "Channels.txt", "القنوات", result.channels);
        Uri movies = write(context, "Movies.txt", "الأفلام", result.movies);
        Uri series = write(context, "Series.txt", "المسلسلات", result.series);
        return new ExportResult(channels, movies, series);
    }

    private static Uri write(Context context, String fileName, String title, List<M3uParser.Entry> items) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DownloadHub");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IllegalStateException("تعذر إنشاء " + fileName);

        try {
            try (OutputStream out = resolver.openOutputStream(uri, "w")) {
                if (out == null) throw new IllegalStateException("تعذر فتح " + fileName);
                out.write(render(title, items).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return uri;
        } catch (Exception e) {
            resolver.delete(uri, null, null);
            throw e;
        }
    }

    private static String render(String title, List<M3uParser.Entry> items) {
        StringBuilder b = new StringBuilder(Math.max(1024, items.size() * 140));
        b.append("Download Hub - ").append(title).append('\n');
        b.append("العدد: ").append(items.size()).append("\n\n");

        int i = 1;
        for (M3uParser.Entry e : items) {
            b.append('[').append(i++).append("]\n");
            b.append("الاسم: ").append(e.name).append('\n');
            if (!e.group.isEmpty()) b.append("المجموعة: ").append(e.group).append('\n');
            b.append("الرابط: ").append(e.url).append('\n');
            b.append("EXTINF: ").append(e.extInf).append("\n\n");
        }
        return b.toString();
    }

    public static final class ExportResult {
        public final Uri channels;
        public final Uri movies;
        public final Uri series;

        public ExportResult(Uri channels, Uri movies, Uri series) {
            this.channels = channels;
            this.movies = movies;
            this.series = series;
        }
    }
}
