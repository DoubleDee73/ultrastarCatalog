package com.doubledee.ultrastar;

import com.doubledee.ultrastar.models.TagsEnum;

import java.util.Comparator;
import java.util.Map;

public class UltrastarTagComparator implements Comparator<Map.Entry<TagsEnum, Integer>> {
    @Override
    public int compare(Map.Entry<TagsEnum, Integer> o1, Map.Entry<TagsEnum, Integer> o2) {
        if (o1.getKey() == TagsEnum.SONG_CHECKED) {
            return o2.getKey() == TagsEnum.UNTAGGED ? 1 : -1;
        } else if (o1.getKey() == TagsEnum.UNTAGGED) {
            return -1;
        } else if (o2.getKey() == TagsEnum.UNTAGGED) {
            return 1;
        } else {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
}
