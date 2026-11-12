package com.doubledee.ultrastar.filters;

import com.doubledee.ultrastar.UltrastarApplication;
import com.doubledee.ultrastar.i18n.Labels;
import com.doubledee.ultrastar.models.Decade;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.TagsEnum;
import com.doubledee.ultrastar.models.UltrastarPlaylist;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the tiles of the filter selection screens, with the number of songs of each choice.
 * Counts are computed on the whole library (not on the current selection).
 */
@Component
public class FilterOptions {

    /** Language code -> country code of the flag (flag-icons CSS library). */
    private static final Map<String, String> FLAGS = Map.ofEntries(
            Map.entry("en", "gb"), Map.entry("de", "de"), Map.entry("fr", "fr"), Map.entry("es", "es"),
            Map.entry("it", "it"), Map.entry("pt", "pt"), Map.entry("nl", "nl"), Map.entry("ko", "kr"),
            Map.entry("zh", "cn"), Map.entry("ja", "jp"), Map.entry("pl", "pl"), Map.entry("ru", "ru"),
            Map.entry("sv", "se"), Map.entry("da", "dk"), Map.entry("fi", "fi"), Map.entry("no", "no"),
            Map.entry("nb", "no"), Map.entry("el", "gr"), Map.entry("tr", "tr"), Map.entry("cs", "cz"),
            Map.entry("hu", "hu"), Map.entry("ro", "ro"), Map.entry("uk", "ua"), Map.entry("hr", "hr"),
            Map.entry("sr", "rs"), Map.entry("ar", "sa"), Map.entry("he", "il"), Map.entry("hi", "in"),
            Map.entry("ga", "ie"), Map.entry("is", "is"), Map.entry("ca", "ad"), Map.entry("eu", "es"),
            Map.entry("la", "va"), Map.entry("id", "id"), Map.entry("th", "th"), Map.entry("vi", "vn"));

    private final Labels labels;

    public FilterOptions(Labels labels) {
        this.labels = labels;
    }

    private static Collection<Song> library() {
        return UltrastarApplication.SONGS == null ? List.of() : UltrastarApplication.SONGS.values();
    }

    public List<FilterOption> languages() {
        List<FilterOption> options = new ArrayList<>();
        for (Language language : UltrastarApplication.USED_LANGUAGES) {
            String code = language.getLanguageCode();
            if (StringUtils.isEmpty(code)) {
                continue; // "All"
            }
            long count = library().stream().filter(song -> song.containsLanguage(code)).count();
            if (count > 0) {
                options.add(new FilterOption(code, labels.language(language), count, "flag",
                                             FLAGS.get(code.toLowerCase()), List.of()));
            }
        }
        options.sort(Comparator.comparingLong(FilterOption::count).reversed()); // most sung languages first
        return options;
    }

    public List<FilterOption> decades() {
        List<FilterOption> options = new ArrayList<>();
        for (Decade decade : Decade.values()) {
            if (decade == Decade.ALL) {
                continue;
            }
            long count = library().stream().filter(song -> song.isInDecade(decade.getDisplayName())).count();
            if (count > 0) {
                options.add(new FilterOption(decade.getDisplayName(), decade.getDisplayName(), count, "decade",
                                             "decade-" + decade.getStartYear(), List.of()));
            }
        }
        return options; // chronological order
    }

    public List<FilterOption> tags() {
        Map<TagsEnum, Long> counts = new EnumMap<>(TagsEnum.class);
        for (Song song : library()) {
            for (TagsEnum tag : TagsEnum.getTagsByString(song.getTags(), true)) {
                counts.merge(tag, 1L, Long::sum);
            }
        }
        Collator collator = Collator.getInstance(LocaleContextHolder.getLocale());
        return counts.entrySet().stream()
                     .map(e -> new FilterOption(e.getKey().getTag(), labels.tag(e.getKey()), e.getValue(), "tag",
                                                "/img/tag/" + e.getKey().getFilename() + ".jpg", List.of()))
                     .sorted(Comparator.comparing(FilterOption::label, collator))
                     .toList();
    }

    public List<FilterOption> playlists() {
        if (UltrastarApplication.PLAYLISTS == null) {
            return List.of();
        }
        Map<String, Song> byArtistAndTitle = songsByArtistAndTitle();
        Collator collator = Collator.getInstance(LocaleContextHolder.getLocale());
        return UltrastarApplication.PLAYLISTS.entrySet().stream()
                .map(e -> {
                    List<Song> songs = songsOf(e.getValue(), byArtistAndTitle);
                    List<String> covers = songs.stream().limit(4).map(Song::getUid).toList();
                    String name = StringUtils.defaultIfBlank(e.getValue().getName(), e.getKey());
                    return new FilterOption(e.getKey(), name, songs.size(), "playlist", null, covers);
                })
                .sorted(Comparator.comparing(FilterOption::label, collator))
                .toList();
    }

    /** Library songs indexed by "Artist : Title" (the format used in .upl playlist files). */
    public Map<String, Song> songsByArtistAndTitle() {
        return library().stream()
                        .collect(Collectors.toMap(Song::getArtistAndTitle, Function.identity(), (a, b) -> a));
    }

    /** Songs of a playlist that exist in the library, in the playlist order. */
    public List<Song> songsOf(UltrastarPlaylist playlist, Map<String, Song> byArtistAndTitle) {
        if (playlist == null || playlist.getSongs() == null) {
            return List.of();
        }
        return playlist.getSongs().stream()
                       .map(byArtistAndTitle::get)
                       .filter(Objects::nonNull)
                       .toList();
    }
}
