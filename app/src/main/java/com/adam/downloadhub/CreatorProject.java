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
    public String voiceoverUri;
    public String overlayUri;
    public String layout;
    public String motion;
    public String captionStyle;
    public String textFont = "Sans";
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
    public int voiceoverVolume = 100;
    public long audioOffsetMs = 0;
    public long voiceoverOffsetMs = 0;
    public int musicFadeInMs = 0;
    public int musicFadeOutMs = 0;
    public int textScale = 100;
    public int textYPercent = 50;
    public int textColor = 0xFFFFFFFF;
    public boolean showText = true;
    public int overlayScale = 35;
    public int overlayXPercent = 50;
    public int overlayYPercent = 50;
    public int overlayOpacity = 100;
    public long overlayStartMs = 0;
    public long overlayEndMs = 0;
    public int fadeInMs = 250;
    public int fadeOutMs = 250;
    public int selectedClipIndex = 0;
    public ArrayList<EditorClip> clips = new ArrayList<>();
    public long updatedAt;

    public static CreatorProject fromTemplate(ReelTemplate t) {
        CreatorProject p = new CreatorProject();
        p.id = "project-" + System.currentTimeMillis();
        p.templateId = t == null ? "blank" : t.id;
        p.categoryKey = t == null ? "creator" : t.categoryKey;
        p.categoryName = t == null ? "مشروع جديد" : t.categoryName;
        p.name = t == null ? "مشروع جديد" : t.name;
        p.hook = t == null ? "" : t.hook;
        p.body = t == null ? "" : t.body;
        p.cta = t == null ? "" : t.cta;
        p.hashtags = t == null ? "" : t.hashtags;
        p.layout = t == null ? "Center Focus" : t.layout;
        p.motion = t == null ? "None" : t.motion;
        p.captionStyle = t == null ? "Bold Highlight" : t.captionStyle;
        p.visualSeed = t == null ? 1 : t.visualSeed;
        p.startColor = t == null ? 0xFF1357D5 : t.startColor;
        p.endColor = t == null ? 0xFF07152A : t.endColor;
        p.durationSec = t == null ? 15 : t.durationSec;
        p.trimStartMs = 0;
        p.trimEndMs = 0;
        p.sourceVolume = 100;
        p.audioVolume = 100;
        p.voiceoverVolume = 100;
        p.audioOffsetMs = 0;
        p.voiceoverOffsetMs = 0;
        p.musicFadeInMs = 0;
        p.musicFadeOutMs = 0;
        p.textScale = 100;
        p.textYPercent = 50;
        p.textColor = 0xFFFFFFFF;
        p.showText = true;
        p.textFont = "Sans";
        p.aspectRatio = "9:16";
        p.transitionStyle = "Fade";
        p.overlayScale = 35;
        p.overlayXPercent = 50;
        p.overlayYPercent = 50;
        p.overlayOpacity = 100;
        p.overlayStartMs = 0;
        p.overlayEndMs = 0;
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
            o.put("voiceoverUri", safe(voiceoverUri));
            o.put("overlayUri", safe(overlayUri));
            o.put("layout", safe(layout));
            o.put("motion", safe(motion));
            o.put("captionStyle", safe(captionStyle));
            o.put("textFont", safe(textFont));
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
            o.put("voiceoverVolume", voiceoverVolume);
            o.put("audioOffsetMs", audioOffsetMs);
            o.put("voiceoverOffsetMs", voiceoverOffsetMs);
            o.put("musicFadeInMs", musicFadeInMs);
            o.put("musicFadeOutMs", musicFadeOutMs);
            o.put("textScale", textScale);
            o.put("textYPercent", textYPercent);
            o.put("textColor", textColor);
            o.put("showText", showText);
            o.put("overlayScale", overlayScale);
            o.put("overlayXPercent", overlayXPercent);
            o.put("overlayYPercent", overlayYPercent);
            o.put("overlayOpacity", overlayOpacity);
            o.put("overlayStartMs", overlayStartMs);
            o.put("overlayEndMs", overlayEndMs);
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
        p.voiceoverUri = o.optString("voiceoverUri", "");
        p.overlayUri = o.optString("overlayUri", "");
        p.layout = o.optString("layout", "Center Focus");
        p.motion = o.optString("motion", "None");
        p.captionStyle = o.optString("captionStyle", "Bold Highlight");
        p.textFont = o.optString("textFont", "Sans");
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
        p.voiceoverVolume = clamp(o.optInt("voiceoverVolume", 100));
        p.audioOffsetMs = o.optLong("audioOffsetMs", 0);
        p.voiceoverOffsetMs = o.optLong("voiceoverOffsetMs", 0);
        p.musicFadeInMs = clampMs(o.optInt("musicFadeInMs", 0), 5000);
        p.musicFadeOutMs = clampMs(o.optInt("musicFadeOutMs", 0), 5000);
        p.textScale = Math.max(60, Math.min(180, o.optInt("textScale", 100)));
        p.textYPercent = Math.max(10, Math.min(90, o.optInt("textYPercent", 50)));
        p.textColor = o.optInt("textColor", 0xFFFFFFFF);
        p.showText = o.optBoolean("showText", true);
        p.overlayScale = Math.max(10, Math.min(100, o.optInt("overlayScale", 35)));
        p.overlayXPercent = Math.max(0, Math.min(100, o.optInt("overlayXPercent", 50)));
        p.overlayYPercent = Math.max(0, Math.min(100, o.optInt("overlayYPercent", 50)));
        p.overlayOpacity = clamp(o.optInt("overlayOpacity", 100));
        p.overlayStartMs = Math.max(0, o.optLong("overlayStartMs", 0));
        p.overlayEndMs = Math.max(0, o.optLong("overlayEndMs", 0));
        p.fadeInMs = clampMs(o.optInt("fadeInMs", 250), 3000);
        p.fadeOutMs = clampMs(o.optInt("fadeOutMs", 250), 3000);
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
    private static int clampMs(int v, int max) { return Math.max(0, Math.min(max, v)); }
    private static String safe(String s) { return s == null ? "" : s; }
}
