package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Language {
    UNDEFINED("ALL", StringUtils.EMPTY, Collections.emptyList()),
    ENGLISH("English", "en", Arrays.asList("Englisch")),
    GERMAN("Deutsch", "de", Arrays.asList("German")),
    FRENCH("Français", "fr", Arrays.asList("French", "Französisch")),
    SPANISH("Español", "es", Arrays.asList("Spanish", "Spanisch")),
    ITALIAN("Italiano", "it", Arrays.asList("Italian", "Italienisch")),
    KOREAN("Korean", "ko", Arrays.asList("Korean", "Koreanisch")),
    CHINESE("Chinese", "zh", Arrays.asList("Mandarin", "Chinese", "Chinesisch")),
    JAPANESE("Japanese", "jp", Arrays.asList("Japanese", "Japanisch")),
    PORTUGUESE( "Português", "pt", Arrays.asList("Portuguese"));

    String language;
    String languageCode;
    List<String> alternatives;

    Language(String language, String languageCode, List<String> alternatives) {
        this.language = language;
        this.languageCode = languageCode;
        this.alternatives = alternatives;
    }

    public String getLanguage() {
        return language;
    }

    public static Language getLanguage(String language) {
        if (language == null) {
            return UNDEFINED;
        }
        for (Language languageEnum : values()) {
            if (languageEnum.getLanguage().equalsIgnoreCase(language) || languageEnum.alternatives.stream().anyMatch(language::equalsIgnoreCase)) {
                return languageEnum;
            }
        }
        return UNDEFINED;
    }

    public static Language getLanguageByCode(String code) {
        if (code == null) {
            return UNDEFINED;
        }
        for (Language languageEnum : values()) {
            if (languageEnum.getLanguageCode().equalsIgnoreCase(code)) {
                return languageEnum;
            }
        }
        return UNDEFINED;
    }

    public String getLanguageCode() {
        return languageCode;
    }
}
