package com.doubledee.ultrastar;

import com.doubledee.ultrastar.db.MsAccessDb;
import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.*;
import com.doubledee.ultrastar.utils.Normalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class UltrastarController {
    @Autowired
    ServletContext context;

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

    @GetMapping("")
    public String rootHandler(Model model) {
        List<Map.Entry<TagsEnum, Integer>> list = new ArrayList<>(getTagsList().entrySet());
        list.sort(new UltrastarTagComparator());
        Collections.reverse(list);
        model.addAttribute("tags", list.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        return "main";
    }

    @GetMapping("/reload")
    public String reload(Model model) {
        UltrastarApplication.init(new SongImporter());
        List<Map.Entry<TagsEnum, Integer>> list = new ArrayList<>(getTagsList().entrySet());
        list.sort(new UltrastarTagComparator());
        Collections.reverse(list);
        model.addAttribute("tags", list.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        return "main";
    }

    @GetMapping("/artists")
    public String artistList(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                             @RequestParam(name = "language", required = false, defaultValue = "") String language,
                             @RequestParam(name = "decade", required = false, defaultValue = "") String decade,
                             @RequestParam(name = "playlist", required = false, defaultValue = "") String playlist,
                             @RequestParam(name = "tags", required = false, defaultValue = "") String tags,
                             @RequestParam(name = "view", required = false, defaultValue = "") String view,
                             Model model) {
        List<Song> songs = retrieveSonglist(ListMode.ARTIST, playlist);
        Set<TagsEnum> filteredTags = TagsEnum.getTagsByString(tags);
        String tagsList = parseToString(filteredTags);
        songs = filterSongs(searchterm, songs, language, decade, filteredTags, true);
        return buildSonglistModel(model, songs, ListMode.ARTIST, searchterm, language, decade, playlist, tagsList, view);
    }

    @GetMapping("/title")
    public String titleList(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                            @RequestParam(name = "language", required = false, defaultValue = "") String language,
                            @RequestParam(name = "decade", required = false, defaultValue = "") String decade,
                            @RequestParam(name = "playlist", required = false, defaultValue = "") String playlist,
                            @RequestParam(name = "tags", required = false, defaultValue = "") String tags,
                            @RequestParam(name = "view", required = false, defaultValue = "") String view,
                            Model model) {
        List<Song> songs = retrieveSonglist(ListMode.TITLE, playlist);
        Set<TagsEnum> filteredTags = TagsEnum.getTagsByString(tags);
        String tagsList = parseToString(filteredTags);
        songs = filterSongs(searchterm, songs, language, decade, filteredTags, false);
        return buildSonglistModel(model, songs, ListMode.TITLE, searchterm, language, decade, playlist, tagsList, view);
    }
    
    @PostMapping("/favorites")
    public String handleFavorites(@RequestParam("favoritesJson") String favoritesJson,
                                  @RequestParam(name = "view", required = false, defaultValue = "") String view,
                                  Model model) {
        List<Song> songs = retrieveSonglist(ListMode.FAVORITES, favoritesJson);
        model.addAttribute("songs", songs);
        model.addAttribute("playlists", UltrastarApplication.PLAYLISTS);
        model.addAttribute("artistLink", "/artists");
        model.addAttribute("titleLink", "/title");
        model.addAttribute("view", view);
        model.addAttribute("favoritesActive", true);
        model.addAttribute("listLink", "submitFavorites('" + (view.isEmpty() ? "list" : "") + "');");

        return "index";
    }
    private List<Song> filterSongs(String searchterm, List<Song> songs, String language, String decade,
                                   Set<TagsEnum> filteredTags, boolean sortByArtist) {
        if (StringUtils.isEmpty(searchterm)) {
            return applyFilter(null, language, decade, filteredTags, songs);
        }
        List<Song> filteredCombined = filterCombined(searchterm, songs);
        List<Song> filteredByArtist = new ArrayList<>();
        List<Song> filteredByTitle = new ArrayList<>();
        List<String> searchterms = splitSearchTerms(searchterm);
        for (String term : searchterms) {
            filteredByArtist.addAll(applyFilter(filterByArtist(term), language, decade, filteredTags, songs));
            filteredByTitle.addAll(applyFilter(filterByTitle(term), language, decade, filteredTags, songs));
        }
        sortList(filteredByArtist, sortByArtist);
        sortList(filteredByTitle, sortByArtist);
        if (filteredByArtist.isEmpty() && filteredByTitle.isEmpty()) {
            songs = Collections.emptyList();
        } else {
            songs = Stream.concat(sortByArtist ? filteredByArtist.stream() : filteredByTitle.stream(),
                                  sortByArtist ? filteredByTitle.stream() : filteredByArtist.stream())
                          .distinct()
                          .collect(Collectors.toList());
        }
        if (filteredCombined != null) {
            if (!songs.isEmpty()) {
                songs = Stream.concat(filteredCombined.stream(), songs.stream())
                              .distinct()
                              .collect(Collectors.toList());
            } else {
                songs = filteredCombined;
            }
        }
        return songs;
    }

    private void sortList(List<Song> songs, boolean byArtistFirst) {
        if (byArtistFirst) {
            songs.sort(Comparator.comparing(Song::getArtistNormalized).thenComparing(Song::getTitleNormalized));
        } else {
            songs.sort(Comparator.comparing(Song::getTitleNormalized).thenComparing(Song::getArtistNormalized));
        }
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

    private List<Song> retrieveSonglist(ListMode listMode, String playlistOrFavorites) {
        if (MapUtils.isEmpty(UltrastarApplication.SONGS)) {
            System.out.println("Something odd happened. No Songs!");
            return Collections.emptyList();
        }
        List<Song> songs;
        if (StringUtils.isNotEmpty(playlistOrFavorites)) {
            if (!ListMode.FAVORITES.equals(listMode)) {
                songs = getSongs(UltrastarApplication.PLAYLISTS.get(playlistOrFavorites));
            } else {
                List<String> favorites;
                try {
                    favorites = new ObjectMapper().readValue(playlistOrFavorites, new TypeReference<>() {});
                } catch (JsonProcessingException e) {
                    favorites = new ArrayList<>();
                }
                songs = getSongs(favorites);
            }
        } else {
            songs = new ArrayList<>(UltrastarApplication.SONGS.values());
        }
        if (CollectionUtils.isEmpty(songs) && listMode != ListMode.FAVORITES) {
            System.out.println("No songs were initilized yet. Retrying...");
            songs = new SongImporter().getImportedSongs();
        }
        if (songs == null) {
            System.out.println("Something odd happened. No Songs!");
            songs = Collections.emptyList();
        }
        if (StringUtils.isEmpty(playlistOrFavorites) || listMode == ListMode.FAVORITES) {
            Comparator<Song> comparator;
            if (listMode == ListMode.ARTIST) {
                comparator = Comparator.comparing(Song::getArtistNormalized)
                                       .thenComparing(Song::getTitleNormalized);
            } else {
                comparator = Comparator.comparing(Song::getTitleNormalized)
                                       .thenComparing(Song::getArtistNormalized);
            }
            songs.sort(comparator);
        }
        return songs;
    }

    private List<Song> getSongs(UltrastarPlaylist ultrastarPlaylist) {
        if (ultrastarPlaylist == null || CollectionUtils.isEmpty(ultrastarPlaylist.getSongs())) {
            return new ArrayList<>(UltrastarApplication.SONGS.values());
        }
        List<Song> songs = new ArrayList<>();
        for (String artistAndTitle : ultrastarPlaylist.getSongs()) {
            Song song = UltrastarApplication.SONGS.values()
                                                  .stream()
                                                  .filter(it -> it.getArtistAndTitle().equals(artistAndTitle))
                                                  .findFirst().orElse(null);
            if (song == null) {
                System.out.println(artistAndTitle + " was not found. Skipped adding it to playlist");
            } else {
                songs.add(song);
            }
        }
        return songs;
    }

    private List<Song> getSongs(List<String> songIds) {
        if (CollectionUtils.isEmpty(songIds)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(UltrastarApplication.SONGS.values().stream()
                .filter(song -> songIds.contains(song.getUid()))
                .toList());
    }

    private List<Song> applyFilter(Predicate<Song> searchtermFilter,
                                   String language,
                                   String decade,
                                   Set<TagsEnum> tags,
                                   List<Song> songs) {
        if (searchtermFilter != null) {
            songs = songs.stream()
                         .filter(searchtermFilter)
                         .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(language)) {
            songs = songs.stream().filter(song -> song.containsLanguage(language.toLowerCase().substring(0, 2)))
                         .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(decade) && !decade.equals(Decade.ALL.getDisplayName())) {
            songs = songs.stream()
                         .filter(song -> song.isInDecade(decade)).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            songs = songs.stream()
                         .filter(song -> song.containsTags(tags)).collect(Collectors.toList());
        }
        return songs;
    }

    private String buildSonglistModel(Model model,
                                      List<Song> songs,
                                      ListMode listMode,
                                      String searchTerm,
                                      String language,
                                      String decade,
                                      String playlist,
                                      String filteredTags,
                                      String view) {
        String temp = "";
        List<String> availableTags = new ArrayList<>(
                Arrays.stream(TagsEnum.values()).map(TagsEnum::getTag).sorted().toList());
        model.addAttribute("songs", songs);
        model.addAttribute("search", "Search for " + listMode.getHeadline());
        model.addAttribute("artistsActive", listMode == ListMode.ARTIST);
        model.addAttribute("titlesActive", listMode == ListMode.TITLE);
        model.addAttribute("temp", temp);
        model.addAttribute("languages", UltrastarApplication.USED_LANGUAGES);
        if (StringUtils.isNotEmpty(searchTerm)) {
            model.addAttribute("searchterm", searchTerm);
        }
        model.addAttribute("selectedLanguage", UltrastarApplication.getLanguageByCode(language));
        model.addAttribute("decades", Decade.values());
        model.addAttribute("selectedDecade", Decade.getDecadeByString(decade));
        model.addAttribute("playlists", UltrastarApplication.PLAYLISTS);
        model.addAttribute("selectedPlaylist", playlist);
        model.addAttribute("tagsWhitelist", availableTags);
        model.addAttribute("filteredTags", filteredTags);
        model.addAttribute("view", view);
        model.addAttribute("artistLink", "/artists" + buildLink(searchTerm, language, decade, playlist, filteredTags, view));
        model.addAttribute("titleLink", "/title" + buildLink(searchTerm, language, decade, playlist, filteredTags, view));
        String listLink = (listMode == ListMode.ARTIST ? "/artists" : "/title") +
                buildLink(searchTerm, language, decade, playlist, filteredTags,
                          "list".equals(view) ? "" : "list");
        model.addAttribute("listLink", "window.location='" + listLink + "'");
        return "index";
    }

    private String buildLink(String searchTerm, String language, String decade, String playlist, 
                             String filteredTags, String view) {
        StringJoiner url = new StringJoiner("&");
        if (StringUtils.isNotEmpty(searchTerm)) {
            url.add("searchterm=" + searchTerm);
        }
        if (StringUtils.isNotEmpty(language)) {
            url.add("language=" + language);
        }
        if (StringUtils.isNotEmpty(decade) && !decade.equals("ALL")) {
            url.add("decade=" + decade);
        }
        if (StringUtils.isNotEmpty(playlist)) {
            url.add("playlist=" + playlist);
        }
        if (StringUtils.isNotEmpty(filteredTags) ) {
            url.add("tags=" + filteredTags);
        }
        if (StringUtils.isNotEmpty(view) && view.equals("list")) {
            url.add("view=list");
        }
        return (url.length() > 0 ? "?" + url : "");
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

    @RequestMapping(value = "/audio/{file}", method = RequestMethod.GET)
    public void getAudioAsByteArray(HttpServletResponse response,
                                    @PathVariable("file") String file) throws IOException {
        String uid = file.replace(".mp3", "");
        Song song = UltrastarApplication.SONGS.get(uid);
        if (song != null) {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(song.getPath() + File.separator + song.getMp3()));
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    @GetMapping("/songinfo/{songId}")
    public String songinfo(@PathVariable(name = "songId") String songId, Model model) {
        Song song = UltrastarApplication.SONGS.get(songId);
        List<Score> scores = UltrastarApplication.SCORES.get(songId);
        if (CollectionUtils.isNotEmpty(scores)) {
            scores.sort(Comparator.comparingInt(Score::getScore).reversed());
        }
        boolean lite = "true".equalsIgnoreCase(UltrastarApplication.CONFIGS.get("lite"));
        model.addAttribute("song", song);
        model.addAttribute("scores", scores != null ? scores : List.of());
        model.addAttribute("lite", lite);
        return "songinfo :: songinfo ";
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

    @PutMapping("/playlist/{playlistName}/{songId}")
    public String addSong2Playlist(@PathVariable String playlistName,
                                   @PathVariable String songId,
                                   Model model) {
        UltrastarPlaylist playlist = UltrastarApplication.PLAYLISTS.get(playlistName);
        if (playlist == null) {
            System.out.println("Playlist " + playlistName + " was not found");
        }
        Song song = UltrastarApplication.SONGS.get(songId);
        if (song == null) {
            System.out.println(
                    "Adding song with ID " + songId + " to playlist " + playlistName + " failed. Not found.");
            return "index";
        }
        System.out.println("Adding " + song.getArtistAndTitle() + " to playlist " + playlistName + "...");
        File plFile = new File(UltrastarApplication.CONFIGS.get(UltrastarApplication.PL_PATH) + "/" + playlistName);
        if (plFile.exists()) {
            UltrastarPlaylist newPlaylist = new UltrastarPlaylist(plFile);
            if (!newPlaylist.contains(song.getArtistAndTitle())) {
                try {
                    newPlaylist.getSongs().add(song.getArtistAndTitle());
                    Files.write(Paths.get(plFile.toURI()), newPlaylist.getContentsAsByteArray(),
                                StandardOpenOption.TRUNCATE_EXISTING);
                    UltrastarApplication.PLAYLISTS.put(playlistName, newPlaylist);
                } catch (IOException e) {
                    System.out.println(
                            "Adding song with ID " + songId + " to playlist " + playlistName + " failed." + e);
                    throw new RuntimeException(e);
                }
            }

        }
        buildSonglistModel(model, retrieveSonglist(ListMode.ARTIST, ""), ListMode.ARTIST, "", "", "", "", "", "");
        return "index";
    }
    
    private Map<TagsEnum, Integer> getTagsList() {
        List<Song> songs = getSongs((UltrastarPlaylist) null);
        Map<TagsEnum, Integer> usedTagsMap = new HashMap<>();
        for (Song song : songs) {
            for (TagsEnum usedTags : TagsEnum.getTagsByString(song.getTags(), true)) {
                if (!usedTagsMap.containsKey(usedTags)) {
                    usedTagsMap.put(usedTags, 0);
                }
                usedTagsMap.put(usedTags, usedTagsMap.get(usedTags) + 1);
            }
        }
        return usedTagsMap;
    }

    private String parseToString(Set<TagsEnum> filteredTags) {
        return filteredTags.stream().map(TagsEnum::getTag).collect(Collectors.joining(","));
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
