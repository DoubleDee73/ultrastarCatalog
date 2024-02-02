package com.doubledee.ultrastar.models;

import org.apache.commons.lang.WordUtils;

public enum UltrastarTag {
    TITLE,
    ARTIST,
    MP3,
    GAP,
    EDITION,
    GENRE,
    LANGUAGE,
    YEAR,
    COVER,
    BACKGROUND,
    VIDEO,
    VIDEOGAP,
    VIDEOSTART,
    BPM,
    START,
    END,
    LENGTH,
    RESOLUTION,
    COMMENT,
    ALBUM,
    PREVIEWSTART,
    COMPOSER,
    CREATOR,
    AUTHOR,
    ENCODING,
    NOTESGAP,
    MEDLEYSTARTBEAT,
    MEDLEYENDBEAT,
    ID,
    RELATIVE,
    P1,
    P2,
    VERSION,
    DUETSINGERP1,
    DUETSINGERP2,
    FIXER,
    AUDIO,
    INSTRUMENTAL,
    VOCALS,
    TAGS,
    PROVIDEDBY;

    public static UltrastarTag getTagForLine(String line) {
        if (!line.startsWith("#")) {
            return null;
        }
        for (UltrastarTag tag : values()) {
            if (line.trim().toUpperCase().startsWith("#" + tag.name() + ":")) {
                return tag;
            }
        }
        System.out.println("Tag for " + line + " was not found");
        return null;
    }

    public String getSetterName() {
        return "set" + WordUtils.capitalizeFully(name());
    }
}
