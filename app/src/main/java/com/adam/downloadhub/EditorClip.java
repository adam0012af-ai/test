package com.adam.downloadhub;

import org.json.JSONObject;

import java.io.Serializable;

public final class EditorClip implements Serializable {
    public String id = "clip-" + System.currentTimeMillis();
    public String uri = "";
    public long trimStartMs = 0;
    public long trimEndMs = 0;
    public long stillDurationMs = 3000;
    public int volume = 100;
    public float speed = 1f;

    public EditorClip() {}

    public EditorClip(String value) {
        uri = value == null ? "" : value;
    }

    public EditorClip copy() {
        EditorClip c = new EditorClip();
        c.id = "clip-" + System.nanoTime();
        c.uri = uri;
        c.trimStartMs = trimStartMs;
        c.trimEndMs = trimEndMs;
        c.stillDurationMs = stillDurationMs;
        c.volume = volume;
        c.speed = speed;
        return c;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id == null ? "" : id);
            o.put("uri", uri == null ? "" : uri);
            o.put("trimStartMs", Math.max(0, trimStartMs));
            o.put("trimEndMs", Math.max(0, trimEndMs));
            o.put("stillDurationMs", Math.max(500, stillDurationMs));
            o.put("volume", clamp(volume));
            o.put("speed", clampSpeed(speed));
        } catch (Exception ignored) {}
        return o;
    }

    public static EditorClip fromJson(JSONObject o) {
        EditorClip c = new EditorClip();
        c.id = o.optString("id", "clip-" + System.nanoTime());
        c.uri = o.optString("uri", "");
        c.trimStartMs = Math.max(0, o.optLong("trimStartMs", 0));
        c.trimEndMs = Math.max(0, o.optLong("trimEndMs", 0));
        c.stillDurationMs = Math.max(500, o.optLong("stillDurationMs", 3000));
        c.volume = clamp(o.optInt("volume", 100));
        c.speed = clampSpeed((float) o.optDouble("speed", 1.0));
        return c;
    }

    public static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
    public static float clampSpeed(float v) {
        if (v < .5f) return .5f;
        if (v > 2f) return 2f;
        return v;
    }
}
