package com.adam.downloadhub;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public final class MediaStoreSaver {
    private MediaStoreSaver() {}

    /** Save an exported MP4 into Movies/DownloadHub so it appears in Gallery. */
    public static Uri saveVideo(Context context, File source) throws Exception {
        if (source == null || !source.exists() || source.length() < 1024) {
            throw new IllegalArgumentException("ملف الفيديو غير موجود أو غير صالح");
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        String name = source.getName();
        if (!name.toLowerCase().endsWith(".mp4")) name += ".mp4";
        values.put(MediaStore.Video.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DownloadHub");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IllegalStateException("تعذر إنشاء ملف الفيديو في الاستوديو");
        boolean ok = false;
        try (FileInputStream in = new FileInputStream(source); OutputStream out = resolver.openOutputStream(uri, "w")) {
            if (out == null) throw new IllegalStateException("تعذر فتح ملف الحفظ");
            byte[] buf = new byte[1024 * 128];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            ok = true;
        } finally {
            if (!ok) {
                try { resolver.delete(uri, null, null); } catch (Exception ignored) {}
            }
        }
        values.clear();
        values.put(MediaStore.Video.Media.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }
}
