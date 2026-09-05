package com.adam.downloadhub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class HistoryStore {
    private static final String PREFS = "link_history";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 250;

    private HistoryStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void add(Context c, String url, String kind) {
        if (!AppPrefs.saveHistory(c) || url == null || url.trim().isEmpty()) return;
        try {
            JSONArray old = read(c);
            JSONArray next = new JSONArray();
            JSONObject n = new JSONObject();
            n.put("url", url);
            n.put("kind", kind == null ? "Link" : kind);
            n.put("time", System.currentTimeMillis());
            n.put("fav", false);
            next.put(n);
            for (int i = 0; i < old.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject o = old.optJSONObject(i);
                if (o == null || url.equals(o.optString("url"))) continue;
                next.put(o);
            }
            prefs(c).edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized void toggleFavorite(Context c, String url) {
        JSONArray a = read(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && url.equals(o.optString("url"))) {
                try { o.put("fav", !o.optBoolean("fav", false)); } catch (Exception ignored) {}
                break;
            }
        }
        prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static synchronized List<Item> list(Context c) {
        List<Item> out = new ArrayList<>();
        JSONArray a = read(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            out.add(new Item(o.optString("url", ""), o.optString("kind", "Link"),
                    o.optLong("time", 0L), o.optBoolean("fav", false)));
        }
        return out;
    }

    public static synchronized void clear(Context c) {
        prefs(c).edit().remove(KEY).apply();
    }

    private static JSONArray read(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    public static final class Item {
        public final String url;
        public final String kind;
        public final long time;
        public final boolean favorite;
        public Item(String url, String kind, long time, boolean favorite) {
            this.url = url;
            this.kind = kind;
            this.time = time;
            this.favorite = favorite;
        }
    }
}
