package com.adam.downloadhub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;

public final class MediaOptionsDialog {
    private MediaOptionsDialog() {}

    public interface Callback {
        void onSelected(MediaOption option);
    }

    public static void show(Activity activity, String title, List<MediaOption> options, Callback callback) {
        if (activity == null || activity.isFinishing()) return;
        if (options == null || options.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("لا توجد خيارات صالحة")
                    .setMessage("لم يتم العثور على ملف فيديو أو صوت كامل قابل للتحميل.")
                    .setPositiveButton("حسنًا", null)
                    .show();
            return;
        }

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) labels[i] = options.get(i).displayLabel();

        new AlertDialog.Builder(activity)
                .setTitle(title == null || title.isEmpty() ? "اختر الجودة أو الصوت" : title)
                .setItems(labels, (DialogInterface dialog, int which) -> {
                    if (which >= 0 && which < options.size() && callback != null) callback.onSelected(options.get(which));
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
