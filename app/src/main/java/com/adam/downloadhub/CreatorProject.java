package com.adam.downloadhub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public final class CreatorProject implements Serializable {
    public String id;
    public String templateId;
    public String categoryKey;
    public String categoryName;
    public String name;
    public String hook;
    public String body;
    public String cta;
    public String hashtags;
    public String sourceUri;
    public String audioUri;
    public String layout;
    public String motion;
    public String captionStyle;
    public String aspectRatio = "9:16";
    public String transitionStyle = "Fade";
    public int visualSeed;
    public int startColor;
    public int endColor;
    public int durationSec;
    public long trimStartMs;
    public long trimEndMs;
    public int sourceVolume = 100;
    public int audioVolume = 100;
    public long audioOffsetMs = 0;
    public int textScale = 100;
    public int textYPercent = 50;
    public int textColor = 0xFFFFFFFF;
    public boolean showText = true;
    public int fadeInMs = 250;
    public int fadeOutMs = 250;
    public int selectedClipIndex = 0;
    public ArrayList<EditorClip> clips = new ArrayList<>();
    public long updatedAt;

    public static CreatorProject fromTemplate(ReelTemplate t) {
        CreatorProject p = new CreatorProject();
        p.id = "project-" + System.currentTimeMillis();
        p.templateId = t.id;
        p.categoryKey = t.categoryKey;
        p.categoryName = t.categoryName;
        p.name = t.name;
        p.hook = t.hook;
        p.body = t.body;
        p.cta = t.cta;
        p.hashtags = t.hashtags;
        p.layout = t.layout;
        p.motion = t.motion;
        p.captionStyle = t.captionStyle;
        p.visualSeed = t.visualSeed;
        p.startColor = t.startColor;
        p.endColor = t.endColor;
        p.durationSec = t.durationSec;
        p.trimStartMs = 0;
        p.trimEndMs = 0;
        p.sourceVolume = 100;
        p.audioVolume = 100;
        p.audioOffsetMs = 0;
        p.textScale = 100;
        p.textYPercent = 50;
        p.textColor = 0xFFFFFFFF;
        p.showText = true;
        p.aspectRatio = "9:16";
        p.transitionStyle = "Fade";
        p.fadeInMs = 250;
        p.fadeOutMs = 250;
        p.updatedAt = System.currentTimeMillis();
        return p;
    }

    public void ensureClips() {
        if (clips == null) clips = new ArrayList<>();
        if (clips.isEmpty() && sourceUri != null && !sourceUri.trim().isEmpty()) {
            EditorClip c = new EditorClip(sourceUri);
            c.trimStartMs = Math.max(0, trimStartMs);
            c.trimEndMs = Math.max(0, trimEndMs);
            c.volume = clamp(sourceVolume);
            clips.add(c);
        }
        if (selectedClipIndex < 0) selectedClipIndex = 0;
        if (!clips.isEmpty() && selectedClipIndex >= clips.size()) selectedClipIndex = clips.size() - 1;
        syncPrimarySource();
    }

    public void syncPrimarySource() {
        if (clips != null && !clips.isEmpty()) {
            EditorClip first = clips.get(0);
            sourceUri = first.uri;
            trimStartMs = first.trimStartMs;
            trimEndMs = first.trimEndMs;
            sourceVolume = first.volume;
        } else {
            sourceUri = "";
            trimStartMs = 0;
            trimEndMs = 0;
            sourceVolume = 100;
        }
    }

    public JSONObject toJson() {
        if (clips == null) clips = new ArrayList<>();
        syncPrimarySource();
        JSONObject o = new JSONObject();
        try {
            o.put("id", safe(id));
            o.put("templateId", safe(templateId));
            o.put("categoryKey", safe(categoryKey));
            o.put("categoryName", safe(categoryName));
            o.put("name", safe(name));
            o.put("hook", safe(hook));
            o.put("body", safe(body));
            o.put("cta", safe(cta));
            o.put("hashtags", safe(hashtags));
            o.put("sourceUri", safe(sourceUri));
            o.put("audioUri", safe(audioUri));
            o.put("layout", safe(layout));
            o.put("motion", safe(motion));
            o.put("captionStyle", safe(captionStyle));
            o.put("aspectRatio", safe(aspectRatio));
            o.put("transitionStyle", safe(transitionStyle));
            o.put("visualSeed", visualSeed);
            o.put("startColor", startColor);
            o.put("endColor", endColor);
            o.put("durationSec", durationSec);
            o.put("trimStartMs", trimStartMs);
            o.put("trimEndMs", trimEndMs);
            o.put("sourceVolume", sourceVolume);
            o.put("audioVolume", audioVolume);
            o.put("audioOffsetMs", audioOffsetMs);
            o.put("textScale", textScale);
            o.put("textYPercent", textYPercent);
            o.put("textColor", textColor);
            o.put("showText", showText);
            o.put("fadeInMs", fadeInMs);
            o.put("fadeOutMs", fadeOutMs);
            o.put("selectedClipIndex", selectedClipIndex);
            JSONArray a = new JSONArray();
            for (EditorClip c : clips) if (c != null && c.uri != null && !c.uri.trim().isEmpty()) a.put(c.toJson());
            o.put("clips", a);
            o.put("updatedAt", updatedAt);
        } catch (Exception ignored) {}
        return o;
    }

    public static CreatorProject fromJson(JSONObject o) {
        CreatorProject p = new CreatorProject();
        p.id = o.optString("id", "project-" + System.currentTimeMillis());
        p.templateId = o.optString("templateId", "");
        p.categoryKey = o.optString("categoryKey", "custom");
        p.categoryName = o.optString("categoryName", "مشروع مخصص");
        p.name = o.optString("name", "مشروع جديد");
        p.hook = o.optString("hook", "عنوان الريل");
        p.body = o.optString("body", "اكتب رسالتك هنا");
        p.cta = o.optString("cta", "تابع للمزيد");
        p.hashtags = o.optString("hashtags", "#Reels #DownloadHub");
        p.sourceUri = o.optString("sourceUri", "");
        p.audioUri = o.optString("audioUri", "");
        p.layout = o.optString("layout", "Center Focus");
        p.motion = o.optString("motion", "Smooth Zoom");
        p.captionStyle = o.optString("captionStyle", "Bold Highlight");
        p.aspectRatio = o.optString("aspectRatio", "9:16");
        p.transitionStyle = o.optString("transitionStyle", "Fade");
        p.visualSeed = o.optInt("visualSeed", Math.abs(p.templateId.hashCode()));
        p.startColor = o.optInt("startColor", 0xFF1357D5);
        p.endColor = o.optInt("endColor", 0xFF07152A);
        p.durationSec = Math.max(1, o.optInt("durationSec", 15));
        p.trimStartMs = Math.max(0, o.optLong("trimStartMs", 0));
        p.trimEndMs = Math.max(0, o.optLong("trimEndMs", 0));
        p.sourceVolume = clamp(o.optInt("sourceVolume", 100));
        p.audioVolume = clamp(o.optInt("audioVolume", 100));
        p.audioOffsetMs = o.optLong("audioOffsetMs", 0);
        p.textScale = Math.max(60, Math.min(180, o.optInt("textScale", 100)));
        p.textYPercent = Math.max(10, Math.min(90, o.optInt("textYPercent", 50)));
        p.textColor = o.optInt("textColor", 0xFFFFFFFF);
        p.showText = o.optBoolean("showText", true);
        p.fadeInMs = Math.max(0, Math.min(3000, o.optInt("fadeInMs", 250)));
        p.fadeOutMs = Math.max(0, Math.min(3000, o.optInt("fadeOutMs", 250)));
        p.selectedClipIndex = Math.max(0, o.optInt("selectedClipIndex", 0));
        p.clips = new ArrayList<>();
        JSONArray a = o.optJSONArray("clips");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject c = a.optJSONObject(i);
                if (c != null) p.clips.add(EditorClip.fromJson(c));
            }
        }
        p.updatedAt = o.optLong("updatedAt", System.currentTimeMillis());
        if (p.clips.isEmpty() && p.sourceUri != null && !p.sourceUri.trim().isEmpty()) {
            EditorClip c = new EditorClip(p.sourceUri);
            c.trimStartMs = p.trimStartMs;
            c.trimEndMs = p.trimEndMs;
            c.volume = p.sourceVolume;
            p.clips.add(c);
        }
        if (!p.clips.isEmpty() && p.selectedClipIndex >= p.clips.size()) p.selectedClipIndex = p.clips.size()-1;
        p.syncPrimarySource();
        return p;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
    private static String safe(String s) { return s == null ? "" : s; }
}
