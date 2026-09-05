package com.adam.downloadhub;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.List;

/** Safety checks before enqueueing a download. */
public final class DownloadGuard {
    private DownloadGuard() {}

    public static void validate(Context context, MediaOption option) {
        if (context == null || option == null) throw new IllegalArgumentException("خيار التحميل غير صالح");
        checkStorage(option);
        checkDuplicate(context, option);
    }

    private static void checkStorage(MediaOption option) {
        if (option.sizeBytes <= 0) return;
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            StatFs stat = new StatFs(downloads.getAbsolutePath());
            long free = stat.getAvailableBytes();
            long required = option.sizeBytes + Math.max(32L * 1024L * 1024L, option.sizeBytes / 10L);
            if (free < required) {
                throw new IllegalStateException("المساحة غير كافية. المطلوب تقريبًا " + MediaOption.formatBytes(required) + " والمتاح " + MediaOption.formatBytes(free));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ignored) {}
    }

    private static void checkDuplicate(Context context, MediaOption option) {
        if (!AppPrefs.duplicateShield(context)) return;
        String name = DownloadUtil.sanitizeFileName(option.fileName);
        long now = System.currentTimeMillis();
        List<DownloadStore.Item> items = DownloadStore.list(context);
        for (DownloadStore.Item item : items) {
            if (item == null) continue;
            if (name.equalsIgnoreCase(DownloadUtil.sanitizeFileName(item.name)) && now - item.time < 7L * 24L * 60L * 60L * 1000L) {
                throw new IllegalStateException("Duplicate Shield: يبدو أن هذا الملف أُضيف للتحميل من قبل");
            }
        }
    }
}
