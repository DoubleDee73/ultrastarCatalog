package com.doubledee.ultrastar;

import com.doubledee.ultrastar.db.MsAccessDb;
import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.*;
import com.doubledee.ultrastar.utils.Normalizer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Controller
public class UltrastarController {
    @Autowired
    ServletContext context;

    @GetMapping("")
    public String rootHandler(Model model) {
        List<Map.Entry<TagsEnum, Integer>> list = new ArrayList<>(getTagsList().entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        model.addAttribute("tags", list.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        return "main";
    }

    @GetMapping("/reload")
    public String reload(Model model) {
        UltrastarApplication.init(new SongImporter());
        List<Map.Entry<TagsEnum, Integer>> list = new ArrayList<>(getTagsList().entrySet());
        list.sort(Map.Entry.comparingByValue());
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
                             Model model) {
        List<Song> songs = retrieveSonglist(ListMode.ARTIST, playlist);
        Set<TagsEnum> filteredTags = TagsEnum.getTagsByString(tags);
        String tagsList = parseToString(filteredTags);
        songs = applyFilter(filterByArtist(searchterm), language, decade, filteredTags, songs);
        return buildSonglistModel(model, songs, ListMode.ARTIST, searchterm, language, decade, playlist, tagsList);
    }

    @GetMapping("/title")
    public String titleList(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                            @RequestParam(name = "language", required = false, defaultValue = "") String language,
                            @RequestParam(name = "decade", required = false, defaultValue = "") String decade,
                            @RequestParam(name = "playlist", required = false, defaultValue = "") String playlist,
                            @RequestParam(name = "tags", required = false, defaultValue = "") String tags,
                            Model model) {
        List<Song> songs = retrieveSonglist(ListMode.TITLE, playlist);
        Set<TagsEnum> filteredTags = TagsEnum.getTagsByString(tags);
        String tagsList = parseToString(filteredTags);
        songs = applyFilter(filterByTitle(searchterm), language, decade, filteredTags, songs);
        return buildSonglistModel(model, songs, ListMode.TITLE, searchterm, language, decade, playlist, tagsList);
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

    private List<Song> retrieveSonglist(ListMode listMode, String playlist) {
        UltrastarPlaylist ultrastarPlaylist;
        if (StringUtils.isNotEmpty(playlist)) {
            ultrastarPlaylist = UltrastarApplication.PLAYLISTS.get(playlist);
        } else {
            ultrastarPlaylist = null;
        }
        List<Song> songs = getSongs(ultrastarPlaylist);
        if (CollectionUtils.isEmpty(songs)) {
            System.out.println("No songs were initilized yet. Retrying...");
            songs = new SongImporter().getImportedSongs();
        }
        if (songs == null) {
            System.out.println("Something odd happened. No Songs!");
            songs = Collections.emptyList();
        }
        if (ultrastarPlaylist == null) {
            Comparator<Song> comparator;
            if (listMode == ListMode.ARTIST) {
                comparator = Comparator.comparing(Song::getArtistNormalized);
            } else {
                comparator = Comparator.comparing(Song::getTitleNormalized);
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
                                      String filteredTags) {
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

        return "index";
    }

    @RequestMapping(value = "/images/{file}", method = RequestMethod.GET)
    public void getImageAsByteArray(HttpServletResponse response,
                                    @PathVariable("file") String file) throws IOException {
        String uid = file.replace(".jpg", "");
        Song song = UltrastarApplication.SONGS.get(uid);
        if (song != null) {
            String cover = song.getCover() == null ? song.getArtist() + " - " + song.getTitle() + " [co].jpg" : song.getCover();
            InputStream in = new BufferedInputStream(
                    new FileInputStream(song.getPath() + "\\" + cover));
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    @RequestMapping(value = "/audio/{file}", method = RequestMethod.GET)
    public void getAudioAsByteArray(HttpServletResponse response,
                                    @PathVariable("file") String file) throws IOException {
        String uid = file.replace(".mp3", "");
        Song song = UltrastarApplication.SONGS.get(uid);
        if (song != null) {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(song.getPath() + "\\" + song.getMp3()));
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    @RequestMapping(value = "/dbsync", method = RequestMethod.GET)
    public String syncWithDb(@RequestParam(name = "file", required = false, defaultValue = "Ultrastar") String
                                     file,
                             Model model) {
        List<String> oddities = new ArrayList<>();
        System.out.println("Sync with " + file + " started.");
        File access = new File("C:\\Users\\Didi\\Documents\\" + file + ".accdb");
        if (!access.exists()) {
            oddities.add(access.getName() + " does not exist.");
            model.addAttribute("oddities", oddities);
            return "sync";
        }
        MsAccessDb msAccess = new MsAccessDb("C:\\Users\\Didi\\Documents\\" + file + ".accdb");
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
                newSongs.add(importedSong.getPath() + "\\" + importedSong.getTextFile());
            } else {
                importedSong.setUid(dbSong.getUid());
                if (dbSong.getLastUpdate() == null || importedSong.getLastUpdate().after(dbSong.getLastUpdate()) ||
                        (!importedSong.getTitle().equalsIgnoreCase(dbSong.getTitle()))) {
                    msAccess.updateSong(importedSong);
                    updatedSongs.add(importedSong.getPath() + "\\" + importedSong.getTextFile());
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
        buildSonglistModel(model, retrieveSonglist(ListMode.ARTIST, ""), ListMode.ARTIST, "", "", "", "", "");
        return "index";
    }

    private Map<TagsEnum, Integer> getTagsList() {
        List<Song> songs = getSongs(null);
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
}
