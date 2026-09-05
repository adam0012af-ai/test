package com.adam.downloadhub;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class DownloadsActivity extends Activity {
    private LinearLayout listBox;
    private DownloadManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 13, 18));
        getWindow().setNavigationBarColor(Color.rgb(10, 13, 18));
        dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(10, 13, 18));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(30));
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("التحميلات", 26, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView sub = text("متابعة حالة الملفات التي بدأها Download Hub", 13,
                Color.rgb(160, 170, 185), false);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams slp = lp();
        slp.setMargins(0, dp(5), 0, dp(14));
        root.addView(sub, slp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        Button refresh = button("تحديث");
        refresh.setOnClickListener(v -> refresh());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button clear = button("مسح السجل");
        clear.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(55, 63, 78)));
        clear.setOnClickListener(v -> {
            DownloadStore.clear(this);
            refresh();
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        clp.setMargins(dp(8), 0, 0, 0);
        actions.addView(clear, clp);
        root.addView(actions);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llp = lp();
        llp.setMargins(0, dp(14), 0, 0);
        root.addView(listBox, llp);

        return scroll;
    }

    private void refresh() {
        if (listBox == null) return;
        listBox.removeAllViews();
        List<DownloadStore.Item> items = DownloadStore.list(this);
        if (items.isEmpty()) {
            TextView empty = text("لا توجد تحميلات مسجلة حتى الآن.", 15,
                    Color.rgb(160, 170, 185), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            listBox.addView(empty);
            return;
        }

        for (DownloadStore.Item item : items) {
            listBox.addView(buildRow(item));
        }
    }

    private LinearLayout buildRow(DownloadStore.Item item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundColor(Color.rgb(20, 25, 34));
        LinearLayout.LayoutParams rlp = lp();
        rlp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rlp);

        TextView name = text(item.name, 15, Color.WHITE, true);
        row.addView(name);

        StatusInfo info = query(item.id);
        TextView state = text(info.label, 13, info.color, false);
        LinearLayout.LayoutParams stlp = lp();
        stlp.setMargins(0, dp(5), 0, dp(7));
        row.addView(state, stlp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.START);

        if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
            Button open = miniButton("فتح");
            open.setOnClickListener(v -> openDownload(item.id));
            buttons.addView(open);
        } else if (info.status == DownloadManager.STATUS_RUNNING
                || info.status == DownloadManager.STATUS_PENDING
                || info.status == DownloadManager.STATUS_PAUSED) {
            Button cancel = miniButton("إلغاء");
            cancel.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(160, 65, 65)));
            cancel.setOnClickListener(v -> {
                dm.remove(item.id);
                Toast.makeText(this, "تم إلغاء التحميل", Toast.LENGTH_SHORT).show();
                refresh();
            });
            buttons.addView(cancel);
        }

        Button remove = miniButton("حذف من السجل");
        remove.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(55, 63, 78)));
        remove.setOnClickListener(v -> {
            DownloadStore.remove(this, item.id);
            refresh();
        });
        LinearLayout.LayoutParams rmp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        rmp.setMargins(dp(8), 0, 0, 0);
        buttons.addView(remove, rmp);

        row.addView(buttons);
        return row;
    }

    private StatusInfo query(long id) {
        DownloadManager.Query q = new DownloadManager.Query().setFilterById(id);
        try (Cursor c = dm.query(q)) {
            if (c == null || !c.moveToFirst()) {
                return new StatusInfo(-1, "غير موجود في مدير التحميلات", Color.rgb(200, 140, 95));
            }
            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            int progress = total > 0 ? (int) Math.min(100L, (done * 100L) / total) : -1;

            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    return new StatusInfo(status, "مكتمل ✅", Color.rgb(115, 220, 160));
                case DownloadManager.STATUS_RUNNING:
                    return new StatusInfo(status,
                            progress >= 0 ? "جاري التحميل… " + progress + "%" : "جاري التحميل…",
                            Color.rgb(105, 175, 255));
                case DownloadManager.STATUS_PENDING:
                    return new StatusInfo(status, "في الانتظار…", Color.rgb(220, 190, 100));
                case DownloadManager.STATUS_PAUSED:
                    return new StatusInfo(status,
                            progress >= 0 ? "متوقف مؤقتًا — " + progress + "%" : "متوقف مؤقتًا",
                            Color.rgb(220, 190, 100));
                case DownloadManager.STATUS_FAILED:
                    int reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                    return new StatusInfo(status, "فشل التحميل — كود " + reason,
                            Color.rgb(235, 105, 105));
                default:
                    return new StatusInfo(status, String.format(Locale.US, "حالة %d", status),
                            Color.LTGRAY);
            }
        } catch (Exception e) {
            return new StatusInfo(-1, "تعذر قراءة الحالة", Color.rgb(235, 105, 105));
        }
    }

    private void openDownload(long id) {
        try {
            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) throw new IllegalStateException("الملف غير متاح");
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "*/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(34, 116, 232)));
        return b;
    }

    private Button miniButton(String label) {
        Button b = button(label);
        b.setTextSize(12);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(13), 0, dp(13), 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class StatusInfo {
        final int status;
        final String label;
        final int color;

        StatusInfo(int status, String label, int color) {
            this.status = status;
            this.label = label;
            this.color = color;
        }
    }
}
