package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Language {
    private int prio;
    private final String languageCode;
    private final String displayLanguage;
    public static final Language UNDEFINED = new Language(Integer.MIN_VALUE, "", "ALL");
    public static final Language OTHER = new Language(Integer.MAX_VALUE, "other", "Other");

    public Language(LanguageEnum languageEnum) {
        this.languageCode = languageEnum.getLanguageCode();
        this.displayLanguage = languageEnum.getDisplayLanguage();
    }

    public Language(int prio, String languageCode, String displayLanguage) {
        this.prio = prio;
        this.languageCode = languageCode;
        this.displayLanguage = displayLanguage;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getDisplayLanguage() {
        return displayLanguage;
    }

    public int getPrio() {
        return prio;
    }

    /**
     * Ranks up the importance of a language by decreasing the prio value.
     */
    public void rankUpPrio() {
        if (prio == Integer.MIN_VALUE || prio == Integer.MAX_VALUE) {
            return;
        }
        prio--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Language language = (Language) o;

        return new EqualsBuilder().append(languageCode, language.languageCode)
                                  .append(displayLanguage, language.displayLanguage)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(languageCode).append(displayLanguage).toHashCode();
    }
}
