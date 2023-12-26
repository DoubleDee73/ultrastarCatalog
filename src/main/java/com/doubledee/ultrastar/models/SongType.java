package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum SongType {
    ORIGINAL_KARAOKE("Original Karaoke", StringUtils.EMPTY),
    KARAOKE("Karaoke", "COVER"),
    WITH_VOCALS("With Vocals", "VOX"),
    FILTERED("Filtered", "FLT");

    String songTypeName;
    String qualifier;

    SongType(String songTypeName, String qualifier) {
        this.songTypeName = songTypeName;
        this.qualifier = qualifier;
    }

    public static SongType getSongTypeByTitle(String filename) {
        for (SongType songType : values()) {
            if (StringUtils.containsIgnoreCase(filename,"(" + songType.qualifier + ")")) {
                return songType;
            }
        }
        return ORIGINAL_KARAOKE;
    }

    public static SongType getSongType(String songTypeName) {
        return Arrays.stream(values())
                .filter(songType -> songType.songTypeName.equalsIgnoreCase(songTypeName))
                .findAny()
                .orElse(ORIGINAL_KARAOKE);
    }

    @Override
    public String toString() {
        return this.songTypeName;
    }
}
