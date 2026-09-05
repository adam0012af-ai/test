package com.adam.downloadhub;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;

public final class DownloadEngine {
    private DownloadEngine() {}

    public static long enqueue(Context context, MediaOption option) throws Exception {
        if (context == null || option == null || option.url.isEmpty()) throw new IllegalArgumentException("مصدر التحميل غير صالح");

        DownloadGuard.validate(context, option);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(option.url));
        request.setTitle(option.fileName);
        request.setDescription("Download Hub v5 • " + option.platform + " • " + option.label);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.addRequestHeader("User-Agent", NetUtil.USER_AGENT);
        if (option.referer != null && !option.referer.isEmpty()) request.addRequestHeader("Referer", option.referer);
        try {
            String cookie = CookieManager.getInstance().getCookie(option.referer == null ? option.url : option.referer);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
        } catch (Exception ignored) {}

        if (AppPrefs.wifiOnly(context)) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        } else {
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
        }

        String folder = "DownloadHub";
        if (AppPrefs.organizeFolders(context)) {
            folder += "/" + DownloadUtil.sanitizeFileName(option.platform);
            folder += option.audioOnly ? "/Audio" : "/Video";
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                folder + "/" + DownloadUtil.sanitizeFileName(option.fileName));

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) throw new IllegalStateException("مدير التحميل غير متاح");
        long id = dm.enqueue(request);
        DownloadStore.add(context, id, option.fileName, option.url);
        return id;
    }
}
