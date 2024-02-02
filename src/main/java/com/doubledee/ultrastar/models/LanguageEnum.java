package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.core.Local;

import java.util.*;

public enum LanguageEnum {
    ENGLISH("English", "en", Arrays.asList("Englisch", "Anglais", "Inglese", "Inglés", "Angielski")),
    GERMAN("German", "de", Arrays.asList("Deutsch", "Allemand", "Tedesco", "Alemán", "Niemiecki")),
    FRENCH("French", "fr", Arrays.asList("Français", "Französisch", "Francese", "Francés", "Francuski")),
    SPANISH("Spanish", "es", Arrays.asList("Español", "Spanisch", "Espagnol", "Spagnolo", "Hiszpański")),
    ITALIAN("Italian", "it", Arrays.asList("Italiano", "Italienisch", "Italien", "włoski")),
    KOREAN("Korean", "ko", Arrays.asList("Korean", "Koreanisch", "Coréen", "Coreano", "Koreański")),
    CHINESE("Chinese", "zh",
            Arrays.asList("Mandarin", "Chinese", "Chinesisch", "Chinois", "Cinese", "Chino", "chiński")),
    JAPANESE("Japanese", "ja", Arrays.asList("Japonais", "Japanisch", "Giapponese", "Japonés", "japoński")),
    PORTUGUESE("Portuguese", "pt", Arrays.asList("Portugiesisch", "Português", "Portugais", "Portoghese", "Portugués", "portugalski"
    ));

    String displayLanguage;
    String languageCode;
    List<String> alternatives;
    Language language;

    LanguageEnum(String displayLanguage, String languageCode, List<String> alternatives) {
        this.displayLanguage = displayLanguage;
        this.languageCode = languageCode;
        this.alternatives = alternatives;
        this.language = new Language(0, languageCode, displayLanguage);
    }

    public String getDisplayLanguage() {
        return displayLanguage;
    }

    public static Set<Language> getLanguages(String languages) {
        if (StringUtils.isEmpty(languages)) {
            return Collections.singleton(Language.UNDEFINED);
        }
        Set<Language> usedLanguages = new HashSet<>();
        String[] languageArray = languages.split(",");
        for (String language : languageArray) {
            usedLanguages.add(getLanguage(language.trim()));
        }
        if (usedLanguages.size() > 1) {
            usedLanguages.remove(Language.UNDEFINED);
        }
        return usedLanguages;
    }

    public static Language getLanguage(String language) {
        if (language == null) {
            return Language.UNDEFINED;
        }
        for (LanguageEnum languageEnum : values()) {
            if (languageEnum.getDisplayLanguage().equalsIgnoreCase(language) || languageEnum.alternatives.stream()
                                                                                                         .anyMatch(
                                                                                                          language::equalsIgnoreCase)) {
                languageEnum.language.rankUpPrio();
                return languageEnum.language;
            }
        }
        return LanguageEnum.getDisplayLanguage(language);
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public static Set<Language> getDisplayLanguages(String languages) {
        if (StringUtils.isEmpty(languages)) {
            return Collections.singleton(Language.UNDEFINED);
        }
        Set<Language> results = new HashSet<>();
        String[] languageArray = languages.split(",");
        for (String language : languageArray) {
            results.add(getDisplayLanguage(language.trim()));
        }
        if (results.size() > 1) {
            results.remove(Language.UNDEFINED);
        }
        return results;
    }
    public static Language getDisplayLanguage(String language) {
        List<Locale> mainLocales = Arrays.asList(Locale.US, Locale.GERMANY, Locale.FRANCE, Locale.ITALIAN,
                                                 Locale.forLanguageTag("es"), Locale.forLanguageTag("pl"));
        for (Locale locale : Locale.getAvailableLocales()) {
            for (Locale displayLocale : mainLocales) {
                if (language.toLowerCase().equalsIgnoreCase(locale.getDisplayLanguage(displayLocale))) {
                    return new Language(0, locale.getLanguage(), locale.getDisplayLanguage(Locale.ENGLISH));
                }
            }
        }
        return Language.OTHER;
    }
}
