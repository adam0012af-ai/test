package com.adam.downloadhub;

import org.json.JSONObject;

import java.io.Serializable;

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
    public int visualSeed;
    public int startColor;
    public int endColor;
    public int durationSec;
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
        p.updatedAt = System.currentTimeMillis();
        return p;
    }

    public JSONObject toJson() {
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
            o.put("visualSeed", visualSeed);
            o.put("startColor", startColor);
            o.put("endColor", endColor);
            o.put("durationSec", durationSec);
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
        p.visualSeed = o.optInt("visualSeed", Math.abs(p.templateId.hashCode()));
        p.startColor = o.optInt("startColor", 0xFF1357D5);
        p.endColor = o.optInt("endColor", 0xFF07152A);
        p.durationSec = Math.max(6, o.optInt("durationSec", 15));
        p.updatedAt = o.optLong("updatedAt", System.currentTimeMillis());
        return p;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
