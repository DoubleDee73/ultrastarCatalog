package com.doubledee.ultrastar.i18n;

import com.doubledee.ultrastar.models.Decade;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.TagsEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Translates display values (tags, languages, decades) into the language of the current request.
 * Texts live in messages.properties / messages_xx.properties. The language is taken from the
 * browser's Accept-Language header (Spring's default LocaleResolver).
 * <p>
 * Usable in Thymeleaf templates as {@code ${@labels.tag(tag)}}.
 * Internal values (tags in the Ultrastar txt files, URLs) always stay in English.
 */
@Component("labels")
public class Labels {

    private final MessageSource messageSource;

    public Labels(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    private String message(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, locale());
    }

    /** Translated name of a tag, or its English name if no translation exists. */
    public String tag(TagsEnum tag) {
        if (tag == null) {
            return "";
        }
        return message("tag." + tag.name(), tag.getTag());
    }

    /** Translated names of all tags, sorted alphabetically (for the Tagify whitelist). */
    public List<String> allTags() {
        return Arrays.stream(TagsEnum.values())
                     .map(this::tag)
                     .sorted(Collator.getInstance(locale())) // sorts "Été" with the E's, not at the end
                     .toList();
    }

    /**
     * Parses a comma-separated list of tags typed by the user. Each tag may be given by its
     * English name (links, txt files) or by its translated name (search form).
     */
    public Set<TagsEnum> parseTags(String tags) {
        if (StringUtils.isBlank(tags)) {
            return Collections.emptySet();
        }
        Set<TagsEnum> result = new LinkedHashSet<>();
        for (String tag : tags.split(",")) {
            if (StringUtils.isNotBlank(tag)) {
                result.add(fromLocalizedTag(tag.trim()));
            }
        }
        return result;
    }

    /** Finds a tag by its English or translated name. Unknown names give UNTAGGED (same as TagsEnum). */
    public TagsEnum fromLocalizedTag(String name) {
        for (TagsEnum tag : TagsEnum.values()) {
            if (tag.getTag().equalsIgnoreCase(name) || tag(tag).equalsIgnoreCase(name)) {
                return tag;
            }
        }
        return TagsEnum.UNTAGGED;
    }

    /** Comma-separated translated tag names, e.g. to put back into the search field. */
    public String joinTags(Set<TagsEnum> tags) {
        return tags.stream().map(this::tag).collect(Collectors.joining(","));
    }

    /** Translated name of a language, e.g. "Anglais" for "en" when the browser is in French. */
    public String language(Language language) {
        if (language == null || StringUtils.isEmpty(language.getLanguageCode())) {
            return message("language.all", "ALL");
        }
        if (Objects.equals(language.getLanguageCode(), Language.OTHER.getLanguageCode())) {
            return message("language.other", language.getDisplayLanguage());
        }
        Locale languageLocale = Locale.forLanguageTag(language.getLanguageCode());
        String name = languageLocale.getDisplayLanguage(locale());
        if (StringUtils.isEmpty(name) || name.equalsIgnoreCase(language.getLanguageCode())) {
            return language.getDisplayLanguage();
        }
        return StringUtils.capitalize(name);
    }

    /** Translated languages of a song, comma-separated. Songs without language give an empty text. */
    public String languages(Song song) {
        if (song == null || song.getLanguages() == null) {
            return "";
        }
        return song.getLanguages()
                   .stream()
                   .filter(lang -> StringUtils.isNotEmpty(lang.getLanguageCode()))
                   .map(this::language)
                   .collect(Collectors.joining(", "));
    }

    /** Translated name of a decade ("ALL" is translated, "80s" etc. stay as they are). */
    public String decade(Decade decade) {
        if (decade == null) {
            return "";
        }
        if (decade == Decade.ALL) {
            return message("decade.ALL", decade.getDisplayName());
        }
        return decade.getDisplayName();
    }
}
