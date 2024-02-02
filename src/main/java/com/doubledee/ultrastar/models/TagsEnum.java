package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum TagsEnum {
    UNTAGGED("untagged"),
    CHARTS("Charts"),
    CHRISTMAS("Christmas"),
    CLUB("Club"),
    COVER("Cover"),
    DISNEY("Disney"),
    DUET("Duet"),
    EUROVISION_SONG_CONTEST("Eurovision Song Contest"),
    EXPLICIT("Explicit"),
    FAN_SONG("Fan Song"),
    FEEL_GOOD("Feel-Good"),
    FUNNY("Funny"),
    GUILTY_PLEASURE("Guilty Pleasure"),
    HALLOWEEN("Halloween"),
    HEARTBREAK("Heartbreak"),
    LIVE("Live"),
    LOVE_SONG("Love Song"),
    MAINSTREAM("Mainstream"),
    MOVIES("Movies"),
    MUSICAL("Musical"),
    PARTY("Party"),
    PRIDE_LGBT("Pride/LGBTQ"),
    RELAXED("Relaxed"),
    SLOW("Slow"),
    SONG_CHECKED("Song-checked"),
    SPECIAL_INTEREST("Special interest"),
    SUMMER("Summer"),
    TV_SHOW("TV Show"),
    UNDERGROUND("Underground"),
    UNDERRATED("Underrated"),
    VIDEO_GAME("Video Game"),
    VIRAL_HIT("Viral Hit");
    final String tag;

    TagsEnum(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public String getFilename() {
        return toString().toLowerCase();
    }

    public static TagsEnum getTagFromString(String tagAsString) {
        for (TagsEnum tag : values()) {
            if (tag.getTag().equalsIgnoreCase(tagAsString.trim())) {
                return tag;
            }
        }
        return TagsEnum.UNTAGGED;
    }

    public static Set<TagsEnum> getTagsByString(String tags) {
        return getTagsByString(tags, false);
    }

    public static Set<TagsEnum> getTagsByString(String tags, boolean assignUntagged) {
        if (StringUtils.isEmpty(tags)) {
            if (assignUntagged) {
                return Collections.singleton(TagsEnum.UNTAGGED);
            } else {
                return Collections.emptySet();
            }
        }
        String[] tagArray = tags.split(",");
        Set<TagsEnum> usedTags = new HashSet<>();
        for (String tag : tagArray) {
            usedTags.add(TagsEnum.getTagFromString(tag));
        }
        return usedTags;
    }
}
