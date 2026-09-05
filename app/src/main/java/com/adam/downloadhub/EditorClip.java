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
    public int rotation = 0;
    public int zoom = 100;
    public boolean mirror = false;
    public String filter = "None";

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
        c.rotation = rotation;
        c.zoom = zoom;
        c.mirror = mirror;
        c.filter = filter;
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
            o.put("rotation", normalizeRotation(rotation));
            o.put("zoom", Math.max(100, Math.min(180, zoom)));
            o.put("mirror", mirror);
            o.put("filter", filter == null ? "None" : filter);
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
        c.rotation = normalizeRotation(o.optInt("rotation", 0));
        c.zoom = Math.max(100, Math.min(180, o.optInt("zoom", 100)));
        c.mirror = o.optBoolean("mirror", false);
        c.filter = o.optString("filter", "None");
        return c;
    }

    public static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
    public static float clampSpeed(float v) {
        if (v < .5f) return .5f;
        if (v > 2f) return 2f;
        return v;
    }
    public static int normalizeRotation(int v) {
        int r = ((v % 360) + 360) % 360;
        if (r < 45) return 0;
        if (r < 135) return 90;
        if (r < 225) return 180;
        if (r < 315) return 270;
        return 0;
    }
}
