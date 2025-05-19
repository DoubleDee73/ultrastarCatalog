package com.doubledee.ultrastar;

import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.Score;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.UltrastarPlaylist;
import com.doubledee.ultrastar.utils.VersionUtils;
import net.davidashen.text.Hyphenator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class UltrastarApplication {

    public static Map<String, Song> SONGS;
    public static Map<String, UltrastarPlaylist> PLAYLISTS;
    public static Map<String, String> CONFIGS = new HashMap<>();
    public static List<Language> USED_LANGUAGES = new ArrayList<>();
    
    public static Map<String, List<Score>> SCORES;
    public static final String PL_PATH = "playlistPath";

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(UltrastarApplication.class, args);
        String version = VersionUtils.getVersionFromManifest();
        SongImporter songImporter = new SongImporter();
        init(songImporter);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("local.server.port");
        InetAddress localHost = null;
        String ip = "localhost";
        try {
            localHost = InetAddress.getLocalHost();
            ip = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        System.out.println("UltrastarCatalog v" + version + " started on: http://" + ip + ":" + port);
    }

    public static void init(SongImporter songImporter) {
        SONGS = songImporter.getImportedSongs()
                            .stream()
                            .collect(Collectors.toMap(Song::getUid, Function.identity()));
        initUsedLanguages();
        PLAYLISTS = songImporter.getPlaylists();
        CONFIGS.put(PL_PATH, songImporter.getPlaylistPath());
        if (songImporter.isLite()) {
            CONFIGS.put(SongImporter.LITE, "true");
        }
        SCORES = songImporter.getScores();
    }
    
    private static void initUsedLanguages() {
        if (SONGS == null || SONGS.isEmpty()) {
            return;
        }
        USED_LANGUAGES.add(Language.UNDEFINED);
        for (Song song : SONGS.values()) {
            for (Language language : song.getLanguages()) {
                language.rankUpPrio();
                if (!USED_LANGUAGES.contains(language)) {
                    USED_LANGUAGES.add(language);
                }
            }
        }
        USED_LANGUAGES.sort(Comparator.comparingInt(Language::getPrio));
    }

    public static void hyphenate(String[] args) throws IOException, UnsupportedFlavorException {
        String data = (String) Toolkit.getDefaultToolkit()
                                      .getSystemClipboard().getData(DataFlavor.stringFlavor);
        Hyphenator hyphenator = new Hyphenator();
        System.out.println(hyphenator.hyphenate(data));
    }

    public static Language getLanguageByCode(String languageCode) {
        if (StringUtils.isEmpty(languageCode)) {
            return Language.UNDEFINED;
        }
        return USED_LANGUAGES.stream()
                             .filter(lang -> lang.getLanguageCode().equals(languageCode))
                             .findFirst()
                             .orElse(Language.UNDEFINED);
    }
}
