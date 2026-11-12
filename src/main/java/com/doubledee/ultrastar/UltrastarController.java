package com.doubledee.ultrastar;

import com.doubledee.ultrastar.db.MsAccessDb;
import com.doubledee.ultrastar.filters.FilterOptions;
import com.doubledee.ultrastar.i18n.Labels;
import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.*;
import com.doubledee.ultrastar.utils.Normalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class UltrastarController {
    @Autowired
    ServletContext context;

    @Autowired
    Labels labels;

    @Autowired
    FilterOptions filterOptions;

    public static final Map<Integer, List<Pair<List<Integer>, List<Integer>>>> PERMUTATION_MAP = initPermutationMap();

    private static Map<Integer, List<Pair<List<Integer>, List<Integer>>>> initPermutationMap() {
        Map<Integer, List<Pair<List<Integer>, List<Integer>>>> returnMap = new HashMap<>();
        List<Pair<List<Integer>, List<Integer>>> tempList = new ArrayList<>();
        tempList.add(new ImmutablePair<>(List.of(0, 1), Collections.emptyList()));
        tempList.add(new ImmutablePair<>(List.of(0), List.of(1)));
        tempList.add(new ImmutablePair<>(List.of(1), List.of(0)));
        tempList.add(new ImmutablePair<>(Collections.emptyList(), List.of(0, 1)));
        returnMap.put(2, tempList);
        tempList = new ArrayList<>();
        tempList.add(new ImmutablePair<>(List.of(0, 1, 2), Collections.emptyList()));
        tempList.add(new ImmutablePair<>(List.of(0, 1), List.of(2)));
        tempList.add(new ImmutablePair<>(List.of(0, 2), List.of(1)));
        tempList.add(new ImmutablePair<>(List.of(1, 2), List.of(0)));
        tempList.add(new ImmutablePair<>(List.of(0), List.of(1, 2)));
        tempList.add(new ImmutablePair<>(List.of(1), List.of(0, 2)));
        tempList.add(new ImmutablePair<>(List.of(2), List.of(0, 1)));
        tempList.add(new ImmutablePair<>(Collections.emptyList(), List.of(0, 1, 2)));
        returnMap.put(3, tempList);
        return returnMap;
    }

    /** Sort criteria of the catalog ("sort" URL parameter). */
    private static final List<String> SORTS = List.of("artist", "title", "year", "language");

    /** Icon of each sort criterion: singer, music note, calendar, globe (same globe as the Language filter). */
    private static final Map<String, String> SORT_ICONS = Map.of(
            "artist", "fa-solid fa-microphone",
            "title", "fa-solid fa-music",
            "year", "fa-solid fa-calendar-days",
            "language", "fa-solid fa-globe");

    /**
     * The catalog: search + multiple-choice filters + sort. Several values of one filter are combined with OR
     * (French OR English), different filters with AND (language AND decade AND tags AND playlists).
     * Favorites (fav=1) are kept in the browser (localStorage) and filtered there.
     */
    @GetMapping({"", "/"})
    public String catalog(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                          @RequestParam(name = "language", required = false) List<String> languages,
                          @RequestParam(name = "decade", required = false) List<String> decades,
                          @RequestParam(name = "tags", required = false) List<String> tags,
                          @RequestParam(name = "playlist", required = false) List<String> playlists,
                          @RequestParam(name = "sort", required = false, defaultValue = "artist") String sort,
                          @RequestParam(name = "dir", required = false, defaultValue = "asc") String dir,
                          @RequestParam(name = "view", required = false, defaultValue = "") String view,
                          @RequestParam(name = "fav", required = false, defaultValue = "") String fav,
                          Model model) {
        List<String> selLanguages = clean(languages);
        List<String> selDecades = clean(decades);
        selDecades.removeIf(d -> d.equalsIgnoreCase(Decade.ALL.getDisplayName()));
        Set<TagsEnum> tagSet = labels.parseTags(String.join(",", clean(tags)));
        List<String> selTags = tagSet.stream().map(TagsEnum::getTag).toList();
        List<String> selPlaylists = clean(playlists);
        sort = SORTS.contains(sort) ? sort : "artist";
        boolean descending = "desc".equalsIgnoreCase(dir);

        List<Song> songs = librarySongs();
        if (!selPlaylists.isEmpty()) {
            Map<String, Song> byArtistAndTitle = filterOptions.songsByArtistAndTitle();
            songs = selPlaylists.stream()
                                .map(p -> UltrastarApplication.PLAYLISTS == null ? null
                                        : UltrastarApplication.PLAYLISTS.get(p))
                                .flatMap(p -> filterOptions.songsOf(p, byArtistAndTitle).stream())
                                .distinct()
                                .collect(Collectors.toList());
        }
        songs = search(searchterm, songs);
        if (!selLanguages.isEmpty()) {
            songs = songs.stream().filter(song -> selLanguages.stream().anyMatch(song::containsLanguage))
                         .collect(Collectors.toList());
        }
        if (!selDecades.isEmpty()) {
            songs = songs.stream().filter(song -> selDecades.stream().anyMatch(song::isInDecade))
                         .collect(Collectors.toList());
        }
        if (!tagSet.isEmpty()) {
            songs = songs.stream().filter(song -> song.containsTags(tagSet)).collect(Collectors.toList());
        }
        songs = sortSongs(songs, sort, descending);

        boolean favoritesOnly = "1".equals(fav);
        model.addAttribute("songs", songs);
        model.addAttribute("searchterm", searchterm);
        model.addAttribute("selLanguages", selLanguages);
        model.addAttribute("selDecades", selDecades);
        model.addAttribute("selTags", selTags);
        model.addAttribute("selPlaylists", selPlaylists);
        model.addAttribute("sort", sort);
        model.addAttribute("sortIcons", SORT_ICONS);
        model.addAttribute("dir", descending ? "desc" : "asc");
        model.addAttribute("view", "list".equals(view) ? "list" : "");
        model.addAttribute("fav", favoritesOnly);
        model.addAttribute("hasFilters", StringUtils.isNotBlank(searchterm) || !selLanguages.isEmpty()
                || !selDecades.isEmpty() || !selTags.isEmpty() || !selPlaylists.isEmpty() || favoritesOnly);
        model.addAttribute("languageOptions", filterOptions.languages());
        model.addAttribute("decadeOptions", filterOptions.decades());
        model.addAttribute("tagOptions", filterOptions.tags());
        model.addAttribute("playlistOptions", filterOptions.playlists());
        model.addAttribute("playlists",
                           UltrastarApplication.PLAYLISTS == null ? Map.of() : UltrastarApplication.PLAYLISTS);
        model.addAttribute("lite", isLite());
        return "index";
    }

    /** Old addresses (version 0.4 and before): same page, sorted by artist or by title. */
    @GetMapping("/artists")
    public String artistsRedirect(HttpServletRequest request) {
        return "redirect:/" + withParam(request.getQueryString(), "sort=artist");
    }

    @GetMapping("/title")
    public String titleRedirect(HttpServletRequest request) {
        return "redirect:/" + withParam(request.getQueryString(), "sort=title");
    }

    @PostMapping("/favorites")
    public String favoritesRedirect(@RequestParam(name = "view", required = false, defaultValue = "") String view) {
        return "redirect:/?fav=1" + ("list".equals(view) ? "&view=list" : "");
    }

    @GetMapping("/reload")
    public String reload() {
        UltrastarApplication.init(new SongImporter());
        return "redirect:/";
    }

    private static String withParam(String query, String param) {
        return "?" + (StringUtils.isBlank(query) ? "" : query + "&") + param;
    }

    /** Removes empty values (e.g. "language=" sent by an empty field). */
    private static List<String> clean(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(StringUtils::isNotBlank).map(String::trim).distinct()
                     .collect(Collectors.toCollection(ArrayList::new));
    }

    /** All songs of the library (re-imported if the library is not loaded yet). */
    private List<Song> librarySongs() {
        if (MapUtils.isEmpty(UltrastarApplication.SONGS)) {
            System.out.println("No songs were initialized yet. Retrying...");
            List<Song> imported = new SongImporter().getImportedSongs();
            return imported == null ? new ArrayList<>() : new ArrayList<>(imported);
        }
        return new ArrayList<>(UltrastarApplication.SONGS.values());
    }

    /** Songs whose artist or title matches the search (same matching rules as before: words, "quotes"...). */
    private List<Song> search(String searchterm, List<Song> songs) {
        if (StringUtils.isBlank(searchterm)) {
            return songs;
        }
        Set<Song> found = new LinkedHashSet<>();
        List<Song> combined = filterCombined(searchterm, songs);
        if (combined != null) {
            found.addAll(combined);
        }
        for (String term : splitSearchTerms(searchterm)) {
            Predicate<Song> byArtist = filterByArtist(term);
            Predicate<Song> byTitle = filterByTitle(term);
            songs.stream().filter(song -> (byArtist != null && byArtist.test(song))
                    || (byTitle != null && byTitle.test(song))).forEach(found::add);
        }
        return new ArrayList<>(found);
    }

    /**
     * Sorts by artist, title, year or language, ascending or descending.
     * Songs without year (or without language) always stay at the end.
     */
    private List<Song> sortSongs(List<Song> songs, String sort, boolean descending) {
        Comparator<Song> byArtist = Comparator.comparing((Song s) -> StringUtils.defaultString(s.getArtistNormalized()))
                                              .thenComparing(s -> StringUtils.defaultString(s.getTitleNormalized()));
        Comparator<Song> byTitle = Comparator.comparing((Song s) -> StringUtils.defaultString(s.getTitleNormalized()))
                                             .thenComparing(s -> StringUtils.defaultString(s.getArtistNormalized()));
        List<Song> sorted = new ArrayList<>(songs);
        switch (sort) {
            case "title" -> sorted.sort(descending ? byTitle.reversed() : byTitle);
            case "year", "language" -> {
                Function<Song, String> key = "year".equals(sort)
                        ? s -> StringUtils.trimToEmpty(s.getYear())
                        : s -> labels.languages(s).toLowerCase();
                List<Song> withKey = new ArrayList<>(sorted.stream().filter(s -> !key.apply(s).isEmpty()).toList());
                List<Song> withoutKey = new ArrayList<>(sorted.stream().filter(s -> key.apply(s).isEmpty()).toList());
                Comparator<Song> byKey = Comparator.comparing(key);
                withKey.sort((descending ? byKey.reversed() : byKey).thenComparing(byArtist));
                withoutKey.sort(byArtist);
                sorted = new ArrayList<>(withKey);
                sorted.addAll(withoutKey);
            }
            default -> sorted.sort(descending ? byArtist.reversed() : byArtist);
        }
        return sorted;
    }

    private List<Song> filterCombined(String searchTerms, List<Song> songs) {
        if (StringUtils.isEmpty(searchTerms)) {
            return null;
        }
        String[] searchArray = searchTerms.split(" ");
        if (searchArray.length != 2 && searchArray.length != 3) {
            return null;
        }
        Map<Integer, List<Song>> prioFilterMap = new HashMap<>();
        List<Pair<List<Integer>, List<Integer>>> permutations = PERMUTATION_MAP.get(searchArray.length);
        int i = 0;
        for (Pair<List<Integer>, List<Integer>> permutation : permutations) {
            List<Song> tempList = songs;
            if (!permutation.getLeft().isEmpty()) {
                for (Integer idx : permutation.getLeft()) {
                    tempList = tempList.stream()
                                       .filter(Objects.requireNonNull(filterByArtist(searchArray[idx])))
                                       .collect(Collectors.toList());
                }
            }
            if (!permutation.getRight().isEmpty()) {
                for (Integer idx : permutation.getRight()) {
                    tempList = tempList.stream()
                                       .filter(Objects.requireNonNull(filterByTitle(searchArray[idx])))
                                       .collect(Collectors.toList());
                }
            }
            if (!tempList.isEmpty()) {
                tempList.sort(Comparator.comparing(Song::getArtistNormalized).thenComparing(Song::getTitleNormalized));
                tempList.sort(Comparator.comparing(Song::getArtistNormalized).thenComparing(Song::getTitleNormalized));
                prioFilterMap.put(++i, tempList);
            }
        }
        if (prioFilterMap.values().isEmpty()) {
            return null;
        } else {
            return prioFilterMap.entrySet()
                                .stream()
                                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                                .flatMap(it -> it.getValue()
                                                 .stream())
                                .collect(
                                        Collectors.toList());
        }
    }

    private Predicate<Song> filterByArtist(String artist) {
        if (StringUtils.isEmpty(artist)) {
            return null;
        }
        String normalizedSearch = Normalizer.normalize(artist);
        return song ->
                song.getArtistNormalized().contains(normalizedSearch) ||
                        song.getFeaturedArtist().contains(normalizedSearch);
    }

    private Predicate<Song> filterByTitle(String title) {
        if (StringUtils.isEmpty(title)) {
            return null;
        }
        String normalizedSearch = Normalizer.normalize(title);
        return song -> song.getTitleNormalized().contains(normalizedSearch);
    }

    @RequestMapping(value = "/images/{file}", method = RequestMethod.GET)
    public void getImageAsByteArray(HttpServletResponse response,
                                    @PathVariable("file") String file) throws IOException {
        String uid = file.replace(".jpg", "");
        Song song = UltrastarApplication.SONGS.get(uid);
        if (song != null) {
            String cover = song.getCover() == null ? song.getArtist() + " - " + song.getTitle() + " [co].jpg" : song.getCover();
            InputStream in = new BufferedInputStream(
                    new FileInputStream(song.getPath() + File.separator + cover));
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            IOUtils.copy(in, response.getOutputStream());
            in.close();
        }
    }

    /**
     * Streams the audio file of a song. Returning a Resource lets Spring answer "Range" requests
     * (206 Partial Content), which the browser needs to start playback at the preview position.
     */
    @GetMapping("/audio/{file}")
    public ResponseEntity<Resource> getAudio(@PathVariable("file") String file) {
        String uid = file.replace(".mp3", "");
        Song song = UltrastarApplication.SONGS.get(uid);
        if (song == null || !song.hasAudio()) {
            return ResponseEntity.notFound().build();
        }
        File audioFile = new File(song.getPath() + File.separator + song.getMp3());
        if (!audioFile.isFile()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(audioFile.getName())
                                              .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(audioFile));
    }

    @GetMapping("/songinfo/{songId}")
    public String songinfo(@PathVariable(name = "songId") String songId, Model model) {
        Song song = UltrastarApplication.SONGS.get(songId);
        if (song == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song " + songId + " not found");
        }
        List<Score> scores = UltrastarApplication.SCORES != null ? UltrastarApplication.SCORES.get(songId) : null;
        if (CollectionUtils.isNotEmpty(scores)) {
            scores.sort(Comparator.comparingInt(Score::getScore).reversed());
        }
        // Names of the playlists that already contain this song
        List<String> songPlaylists = UltrastarApplication.PLAYLISTS == null ? List.of()
                : UltrastarApplication.PLAYLISTS.values().stream()
                        .filter(playlist -> playlist.contains(song.getArtistAndTitle()))
                        .map(UltrastarPlaylist::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
        model.addAttribute("song", song);
        model.addAttribute("scores", scores != null ? scores : List.of());
        model.addAttribute("songPlaylists", songPlaylists);
        model.addAttribute("lite", isLite());
        return "songinfo :: songinfo ";
    }

    /** lite=true in config.properties disables the audio preview. */
    private boolean isLite() {
        return "true".equalsIgnoreCase(UltrastarApplication.CONFIGS.get("lite"));
    }

    @RequestMapping(value = "/dbsync", method = RequestMethod.GET)
    public String syncWithDb(@RequestParam(name = "file", required = false, defaultValue = "Ultrastar") String
                                     file,
                             Model model) {
        List<String> oddities = new ArrayList<>();
        System.out.println("Sync with " + file + " started.");
        File access = new File("C:\\Users\\User\\OneDrive\\Dokumente\\" + file + ".accdb");
        if (!access.exists()) {
            oddities.add(access.getName() + " does not exist.");
            model.addAttribute("oddities", oddities);
            return "sync";
        }
        MsAccessDb msAccess = new MsAccessDb("C:\\Users\\User\\OneDrive\\Dokumente\\" + file + ".accdb");
        System.out.println("Access DB " + file + " found. Adding new songs.");
        List<String> newSongs = new ArrayList<>();
        List<String> updatedSongs = new ArrayList<>();
        for (Song importedSong : UltrastarApplication.SONGS.values()) {
            if (importedSong == null || StringUtils.isEmpty(importedSong.getTextFile()) ||
                    importedSong.getTextFile().startsWith("_temp")) {
                continue;
            }
            Song dbSong = msAccess.findSongByPathAndFilename(importedSong.getPath(), importedSong.getTextFile());
            if (dbSong == null) {
                msAccess.addSong(importedSong);
                newSongs.add(importedSong.getPath() + File.separator + importedSong.getTextFile());
            } else {
                importedSong.setUid(dbSong.getUid());
                if (dbSong.getLastUpdate() == null || importedSong.getLastUpdate().after(dbSong.getLastUpdate()) ||
                        (!importedSong.getTitle().equalsIgnoreCase(dbSong.getTitle()))) {
                    msAccess.updateSong(importedSong);
                    updatedSongs.add(importedSong.getPath() + File.separator + importedSong.getTextFile());
                }
            }
        }
        System.out.println("Forward sync completed. Now starting reverse sync.");
        // Reverse-Check
        for (Song dbSong : msAccess.getSongs().values()) {
            String textFile = dbSong.getTextFile();
            boolean hasNoneMatch = UltrastarApplication.SONGS.values().stream()
                                                             .noneMatch(song -> song.getTextFile()
                                                                                    .equalsIgnoreCase(textFile));
            if (hasNoneMatch) {
                oddities.add(dbSong.getUid() + ": " + dbSong.getPath() + "/" + dbSong.getTextFile() + " not found");
            }
        }
        model.addAttribute("newSongs", newSongs);
        model.addAttribute("updatedSongs", updatedSongs);
        model.addAttribute("oddities", oddities);
        System.out.println("Sync Finished");
        return "sync";
    }

    /**
     * Playlists for the "+" menu of a song card: id (file name), name, and whether the song is already in it.
     */
    @GetMapping("/playlists/song/{songId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> playlistsForSong(@PathVariable String songId) {
        Song song = UltrastarApplication.SONGS.get(songId);
        if (song == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, UltrastarPlaylist> playlists = UltrastarApplication.PLAYLISTS;
        if (playlists == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Map<String, Object>> result = playlists.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String name = entry.getValue().getName();
                    item.put("id", entry.getKey());
                    item.put("name", StringUtils.isNotBlank(name) ? name.trim() : entry.getKey());
                    item.put("contains", entry.getValue().contains(song.getArtistAndTitle()));
                    return item;
                })
                .sorted(Comparator.comparing(item -> ((String) item.get("name")).toLowerCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Adds a song to a playlist (.upl file). Answers 200 when added, 409 when the song is already in the
     * playlist, 404 when the song or the playlist does not exist.
     * Synchronized: two guests adding songs at the same time must not overwrite each other's change.
     */
    @PutMapping("/playlist/{playlistName}/{songId}")
    public synchronized ResponseEntity<String> addSong2Playlist(@PathVariable String playlistName,
                                                                @PathVariable String songId) {
        Song song = UltrastarApplication.SONGS.get(songId);
        if (song == null) {
            System.out.println(
                    "Adding song with ID " + songId + " to playlist " + playlistName + " failed. Not found.");
            return ResponseEntity.notFound().build();
        }
        if (UltrastarApplication.PLAYLISTS == null || !UltrastarApplication.PLAYLISTS.containsKey(playlistName)) {
            System.out.println("Playlist " + playlistName + " was not found");
            return ResponseEntity.notFound().build();
        }
        File plFile = new File(UltrastarApplication.CONFIGS.get(UltrastarApplication.PL_PATH) + "/" + playlistName);
        if (!plFile.isFile()) {
            System.out.println("Playlist file " + plFile + " was not found");
            return ResponseEntity.notFound().build();
        }
        UltrastarPlaylist newPlaylist = new UltrastarPlaylist(plFile);
        if (newPlaylist.contains(song.getArtistAndTitle())) {
            UltrastarApplication.PLAYLISTS.put(playlistName, newPlaylist);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("already in playlist");
        }
        System.out.println("Adding " + song.getArtistAndTitle() + " to playlist " + playlistName + "...");
        try {
            newPlaylist.getSongs().add(song.getArtistAndTitle());
            Files.write(Paths.get(plFile.toURI()), newPlaylist.getContentsAsByteArray(),
                        StandardOpenOption.TRUNCATE_EXISTING);
            UltrastarApplication.PLAYLISTS.put(playlistName, newPlaylist);
        } catch (IOException e) {
            System.out.println("Adding song with ID " + songId + " to playlist " + playlistName + " failed." + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("could not write playlist");
        }
        return ResponseEntity.ok("added");
    }
    
    private List<String> splitSearchTerms(String searchTerm) {
        if (StringUtils.isEmpty(searchTerm)) {
            return Collections.emptyList();
        }
        if (!searchTerm.contains(" ")) {
            return Collections.singletonList(searchTerm);
        }
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(searchTerm);
        while (m.find()) {
            result.add(m.group(1).replace("\"", ""));
        }
        return result;
    }
}
