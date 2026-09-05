package com.adam.downloadhub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class DraftStore {
    private static final String PREFS = "creator_drafts";
    private static final String KEY = "projects";
    private static final int MAX = 100;

    private DraftStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void save(Context c, CreatorProject project) {
        if (project == null) return;
        project.updatedAt = System.currentTimeMillis();
        JSONArray old = array(c);
        JSONArray next = new JSONArray();
        next.put(project.toJson());
        for (int i = 0; i < old.length() && next.length() < MAX; i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null) continue;
            if (project.id != null && project.id.equals(o.optString("id"))) continue;
            next.put(o);
        }
        prefs(c).edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized CreatorProject get(Context c, String id) {
        if (id == null) return null;
        JSONArray a = array(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return CreatorProject.fromJson(o);
        }
        return null;
    }

    public static synchronized List<CreatorProject> list(Context c) {
        List<CreatorProject> out = new ArrayList<>();
        JSONArray a = array(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.add(CreatorProject.fromJson(o));
        }
        return out;
    }

    public static synchronized void remove(Context c, String id) {
        JSONArray old = array(c);
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null || id.equals(o.optString("id"))) continue;
            next.put(o);
        }
        prefs(c).edit().putString(KEY, next.toString()).apply();
    }

    private static JSONArray array(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
}
