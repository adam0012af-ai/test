package com.adam.downloadhub;

import java.io.Serializable;

public final class ReelTemplate implements Serializable {
    public final String id;
    public final String categoryKey;
    public final String categoryName;
    public final String name;
    public final String hook;
    public final String body;
    public final String cta;
    public final String hashtags;
    public final String layout;
    public final String motion;
    public final String captionStyle;
    public final int startColor;
    public final int endColor;
    public final int durationSec;
    public final boolean islamic;
    public final int visualSeed;

    public ReelTemplate(String id, String categoryKey, String categoryName, String name,
                        String hook, String body, String cta, String hashtags,
                        String layout, String motion, String captionStyle,
                        int startColor, int endColor, int durationSec, boolean islamic) {
        this(id, categoryKey, categoryName, name, hook, body, cta, hashtags,
                layout, motion, captionStyle, startColor, endColor, durationSec,
                islamic, Math.abs((id == null ? "template" : id).hashCode()));
    }

    public ReelTemplate(String id, String categoryKey, String categoryName, String name,
                        String hook, String body, String cta, String hashtags,
                        String layout, String motion, String captionStyle,
                        int startColor, int endColor, int durationSec, boolean islamic,
                        int visualSeed) {
        this.id = id;
        this.categoryKey = categoryKey;
        this.categoryName = categoryName;
        this.name = name;
        this.hook = hook;
        this.body = body;
        this.cta = cta;
        this.hashtags = hashtags;
        this.layout = layout;
        this.motion = motion;
        this.captionStyle = captionStyle;
        this.startColor = startColor;
        this.endColor = endColor;
        this.durationSec = durationSec;
        this.islamic = islamic;
        this.visualSeed = visualSeed;
    }
}
