package com.doubledee.ultrastar.models;

import org.apache.commons.lang.WordUtils;

public enum UltrastarTag {
    TITLE,
    ARTIST,
    MP3,
    LANGUAGE,
    YEAR,
    COVER,
    BACKGROUND,
    VIDEO,
    BPM;

    public static UltrastarTag getTagForLine(String line) {
        for (UltrastarTag tag : values()) {
            if (line.trim().startsWith("#" + tag.name() + ":")) {
                return tag;
            }
        }
        return null;
    }

    public String getSetterName() {
        return "set" + WordUtils.capitalizeFully(name());
    }
}
