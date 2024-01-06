package com.doubledee.ultrastar;

import com.doubledee.ultrastar.db.MsAccessDb;
import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.Decade;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.Song;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UltrastarController {
    @Autowired
    ServletContext context;

    @GetMapping("/artists")
    public String artistList(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                             @RequestParam(name = "language", required = false, defaultValue = "") String language,
                             @RequestParam(name = "decade", required = false, defaultValue = "") String decade,
                             Model model) {
        List<Song> songs = new ArrayList<>(UltrastarApplication.SONGS.values());
        if (CollectionUtils.isEmpty(songs)) {
            System.out.println("No songs were initilized yet. Retrying...");
            songs = new SongImporter().getImportedSongs();
        }
        if (songs == null) {
            System.out.println("Something odd happened. No Songs!");
            songs = Collections.emptyList();
        }
        songs.sort(Comparator.comparing(Song::getArtistNormalized));
        if (StringUtils.isNotEmpty(searchterm)) {
            String searchTerm = Normalizer.normalize(searchterm, Normalizer.Form.NFD).toLowerCase();
            songs = songs.stream().filter(song -> song.getArtistNormalized().contains(searchTerm) ||
                    song.getFeaturedArtist().contains(searchTerm)).collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(language)) {
            songs = songs.stream().filter(song -> song.getLanguage()
                                                      .getLanguageCode()
                                                      .startsWith(language.toLowerCase().substring(0, 2)))
                         .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(decade) && !decade.equals(Decade.ALL.getDisplayName())) {
            songs = songs.stream().filter(song -> song.isInDecade(decade)).collect(Collectors.toList());
        }
        String temp = "";
        model.addAttribute("songs", songs);
        model.addAttribute("search", "Search for Artists");
        model.addAttribute("artistsActive", true);
        model.addAttribute("titlesActive", false);
        model.addAttribute("temp", temp);
        model.addAttribute("languages", Language.values());
        model.addAttribute("selectedLanguage", Language.getLanguageByCode(language));
        model.addAttribute("decades", Decade.values());
        model.addAttribute("selectedDecade", Decade.getDecadeByString(decade));
        return "index";
    }

    @GetMapping("/title")
    public String titleList(@RequestParam(name = "searchterm", required = false, defaultValue = "") String searchterm,
                            @RequestParam(name = "language", required = false, defaultValue = "") String language,
                            @RequestParam(name = "decade", required = false, defaultValue = "") String decade,
                            Model model) {
        List<Song> songs = new ArrayList<>(UltrastarApplication.SONGS.values());
        if (CollectionUtils.isEmpty(songs)) {
            System.out.println("No songs were initilized yet. Retrying...");
            songs = new SongImporter().getImportedSongs();
        }
        if (songs == null) {
            System.out.println("Something odd happened. No Songs!");
            songs = Collections.emptyList();
        }
        songs.sort(Comparator.comparing(Song::getTitleNormalized));
        if (StringUtils.isNotEmpty(searchterm)) {
            String normalizedSearch = Normalizer.normalize(searchterm, Normalizer.Form.NFD).toLowerCase();
            songs = songs.stream()
                         .filter(song -> song.getTitleNormalized().contains(normalizedSearch))
                         .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(language)) {
            songs = songs.stream().filter(song -> song.getLanguage()
                                                      .getLanguageCode()
                                                      .startsWith(language.toLowerCase().substring(0, 2)))
                         .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(decade) && !decade.equals(Decade.ALL.getDisplayName())) {
            songs = songs.stream().filter(song -> song.isInDecade(decade)).collect(Collectors.toList());
        }
        String temp = "";
        model.addAttribute("songs", songs);
        model.addAttribute("search", "Search for Song Titles");
        model.addAttribute("artistsActive", false);
        model.addAttribute("titlesActive", true);
        model.addAttribute("languages", Language.values());
        model.addAttribute("selectedLanguage", Language.getLanguageByCode(language));
        model.addAttribute("temp", temp);
        model.addAttribute("decades", Decade.values());
        model.addAttribute("selectedDecade", Decade.getDecadeByString(decade));
        return "index";
    }

    @RequestMapping(value = "/images/{file}", method = RequestMethod.GET)
    public void getImageAsByteArray(HttpServletResponse response,
                                    @PathVariable("file") String file) throws IOException {
        String uid = file.replace(".jpg", "");
        Song song = UltrastarApplication.SONGS.get(Long.parseLong(uid));
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
        Song song = UltrastarApplication.SONGS.get(Long.parseLong(uid));
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
}
