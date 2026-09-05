package com.adam.downloadhub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class DownloadStore {
    private static final String PREFS = "download_history";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 100;

    private DownloadStore() {}

    public static synchronized void add(Context context, long id, String name, String url) {
        try {
            JSONArray old = readArray(context);
            JSONArray next = new JSONArray();

            JSONObject item = new JSONObject();
            item.put("id", id);
            item.put("name", name == null ? "download" : name);
            item.put("url", url == null ? "" : url);
            item.put("time", System.currentTimeMillis());
            next.put(item);

            for (int i = 0; i < old.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject o = old.optJSONObject(i);
                if (o == null || o.optLong("id", -1L) == id) continue;
                next.put(o);
            }
            prefs(context).edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized List<Item> list(Context context) {
        List<Item> out = new ArrayList<>();
        JSONArray a = readArray(context);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            out.add(new Item(
                    o.optLong("id", -1L),
                    o.optString("name", "download"),
                    o.optString("url", ""),
                    o.optLong("time", 0L)));
        }
        return out;
    }

    public static synchronized void remove(Context context, long id) {
        JSONArray old = readArray(context);
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null || o.optLong("id", -1L) == id) continue;
            next.put(o);
        }
        prefs(context).edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized void clear(Context context) {
        prefs(context).edit().remove(KEY).apply();
    }

    private static JSONArray readArray(Context context) {
        try {
            return new JSONArray(prefs(context).getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class Item {
        public final long id;
        public final String name;
        public final String url;
        public final long time;

        public Item(long id, String name, String url, long time) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.time = time;
        }
    }
}
