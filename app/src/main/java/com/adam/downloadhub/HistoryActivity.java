package com.adam.downloadhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends Activity {
    private LinearLayout listBox;
    private boolean favoritesOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(Ui.gradient(Ui.BG, Ui.SURFACE, 0, this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this,16), Ui.dp(this,22), Ui.dp(this,16), Ui.dp(this,28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.text(this, "السجل والمفضلة", 27, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        TextView sub = Ui.text(this, "كل الروابط التي تعامل معها Download Hub", 13, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams sp = Ui.matchWrap(); sp.setMargins(0, Ui.dp(this,5), 0, Ui.dp(this,18));
        root.addView(sub, sp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button all = Ui.primary(this, "الكل");
        all.setOnClickListener(v -> { favoritesOnly = false; refresh(); });
        actions.addView(all, new LinearLayout.LayoutParams(0, Ui.dp(this,48), 1f));
        Button fav = Ui.secondary(this, "★ المفضلة");
        fav.setOnClickListener(v -> { favoritesOnly = true; refresh(); });
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, Ui.dp(this,48), 1f); fp.setMargins(Ui.dp(this,8),0,0,0);
        actions.addView(fav, fp);
        Button clear = Ui.secondary(this, "مسح");
        clear.setOnClickListener(v -> { HistoryStore.clear(this); refresh(); });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, Ui.dp(this,48), .72f); cp.setMargins(Ui.dp(this,8),0,0,0);
        actions.addView(clear, cp);
        root.addView(actions);

        listBox = new LinearLayout(this); listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = Ui.matchWrap(); lp.setMargins(0,Ui.dp(this,16),0,0);
        root.addView(listBox, lp);
        return scroll;
    }

    private void refresh() {
        if (listBox == null) return;
        listBox.removeAllViews();
        List<HistoryStore.Item> items = HistoryStore.list(this);
        int shown = 0;
        for (HistoryStore.Item item : items) {
            if (favoritesOnly && !item.favorite) continue;
            listBox.addView(row(item)); shown++;
        }
        if (shown == 0) {
            TextView e = Ui.text(this, favoritesOnly ? "لا توجد عناصر مفضلة." : "السجل فارغ حتى الآن.", 15, Ui.MUTED, false);
            e.setGravity(Gravity.CENTER); e.setPadding(0,Ui.dp(this,36),0,Ui.dp(this,36)); listBox.addView(e);
        }
    }

    private View row(HistoryStore.Item item) {
        LinearLayout card = Ui.card(this);
        LinearLayout.LayoutParams mp = Ui.matchWrap(); mp.setMargins(0,0,0,Ui.dp(this,10)); card.setLayoutParams(mp);
        TextView kind = Ui.text(this, item.kind + (item.favorite ? "   ★" : ""), 13, item.favorite ? Ui.YELLOW : Ui.CYAN, true);
        card.addView(kind);
        TextView url = Ui.text(this, item.url, 13, Ui.TEXT, false); url.setTextDirection(View.TEXT_DIRECTION_LTR); url.setMaxLines(2);
        LinearLayout.LayoutParams up = Ui.matchWrap(); up.setMargins(0,Ui.dp(this,6),0,0); card.addView(url, up);
        String when = item.time > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(item.time)) : "";
        TextView time = Ui.text(this, when, 11, Ui.MUTED, false); LinearLayout.LayoutParams tp=Ui.matchWrap();tp.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,10));card.addView(time,tp);

        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button use = Ui.primary(this,"استخدام");
        use.setOnClickListener(v -> { Intent i = new Intent(this, MainActivity.class); i.putExtra(MainActivity.EXTRA_PREFILL_URL, item.url); i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(i); });
        buttons.addView(use,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f));
        Button fav=Ui.secondary(this,item.favorite?"إزالة ★":"إضافة ★");
        fav.setOnClickListener(v->{HistoryStore.toggleFavorite(this,item.url);refresh();});
        LinearLayout.LayoutParams f=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1f);f.setMargins(Ui.dp(this,7),0,0,0);buttons.addView(fav,f);
        Button copy=Ui.secondary(this,"نسخ");
        copy.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("url",item.url));Toast.makeText(this,"تم نسخ الرابط",Toast.LENGTH_SHORT).show();});
        LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(0,Ui.dp(this,44),.7f);c.setMargins(Ui.dp(this,7),0,0,0);buttons.addView(copy,c);
        card.addView(buttons);
        return card;
    }
}
