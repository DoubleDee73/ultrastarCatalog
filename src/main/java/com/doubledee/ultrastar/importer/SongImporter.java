package com.doubledee.ultrastar.importer;

import com.doubledee.ultrastar.models.*;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SongImporter {

    //    public static final String SONGS_PATH = "C:\\Program Files (x86)\\UltraStar Deluxe\\songs\\";
    public static final String SONGS_PATH = "SongDir";
    public static final String PLAYLISTS_PATH = "PlaylistsDir";

    private List<Song> importedSongs = new ArrayList<>();
    private Map<String, UltrastarPlaylist> playlists = new HashMap<>();

    private String playlistPath;

    public SongImporter() {
        File config = new File("config.properties");
        FileWriter myWriter;
        if (!config.exists()) {
            System.out.println("config.properties were not found in " + config.getParent() + ". Creating new file.");
            try {
                myWriter = new FileWriter("config.properties");
                myWriter.write(SONGS_PATH + "1=C:/Program Files (x86)/UltraStar Deluxe/songs/");
                myWriter.write(System.lineSeparator());
                myWriter.write(PLAYLISTS_PATH + "1=C:/Program Files (x86)/UltraStar Deluxe/playlists/");
                myWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Properties appProps = new Properties();
        try {
            appProps.load(new FileInputStream(config.getAbsolutePath()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        String songsPath;
        int i = 1;
        File ultraStarSongs = null;
        do {
            songsPath = (String) appProps.get(SONGS_PATH + (i++));
            if (songsPath != null) {
                ultraStarSongs = new File(songsPath);
                if (ultraStarSongs.exists()) {
                    init(songsPath);
                }
            }
        } while (songsPath != null);
        String playlistsPath = (String) appProps.get(PLAYLISTS_PATH);
        if (StringUtils.isNotEmpty(playlistsPath)) {
            setPlaylistPath(playlistsPath);
            initPlaylists(playlistsPath);
        }
    }

    public void init(String path) {
        try {
            System.out.println("Initializing songs in " + path);
            List<File> files = Files.walk(Paths.get(path))
                                    .filter(Files::isRegularFile)
                                    .map(Path::toFile)
                                    .filter(file -> !file.getParent().endsWith("\\_temp"))
                                    .filter(file -> file.getName().toLowerCase().endsWith(".txt"))
                                    .toList();
            if (files.isEmpty()) {
                System.out.println("No files were found in " + path);
            }
            Map<String, String> filesToUpdate = new HashMap<>();
            for (File file : files) {
//                System.out.println("Reading " + file.getName());
                String encoding = detectEncoding(file);
                UltrastarFile ultrastarFile = new UltrastarFile(file, encoding);
                Song song = new Song(ultrastarFile);
                if (song.isDirty()) {
                    //updateFile(song, file, filesToUpdate);
                }
                boolean hasFile = Files.exists(Paths.get(song.getPath() + "\\" + song.getMp3()));
                if (hasFile && StringUtils.isNotEmpty(ultrastarFile.getTitle())) {
                    if (StringUtils.isEmpty(song.getCover())) {
                        for (File cover : new File(song.getPath()).listFiles()) {
                            if (cover.isFile() && cover.getName().contains("[co]")) {
                                song.setCover(cover.getName());
                                break;
                            }
                        }
                    }

                    importedSongs.add(song);
                } else {
                    System.out.println("Not a valid Ultrastar song: " + file.getAbsolutePath());
                }
                hasFile = Files.exists(Paths.get(song.getPath() + "\\" + song.getCover()));
                if (!hasFile) {
                    System.out.println("Invalid cover art: " + file.getAbsolutePath());
                }
                if (song.getLanguage().equals(Language.UNDEFINED)) {
                    System.out.println("Missing language: " + file.getAbsolutePath());
                }
            }
            /*
            for (Map.Entry<String, String> keyValue : filesToUpdate.entrySet()) {
                renameAndWriteFiles(keyValue.getKey(), keyValue.getValue());
            }
             */
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished importing  " + importedSongs.size() + " songs.");
    }

    private void updateFile(Song song, File file, Map<String, String> fileNameToContent) {
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        StringJoiner content = new StringJoiner("\r\n");
        addToContent(content, UltrastarTag.TITLE, song.getTitle());
        addToContent(content, UltrastarTag.ARTIST, song.getArtist());
        addToContent(content, UltrastarTag.MP3, song.getMp3());
        addToContent(content, UltrastarTag.BPM, song.getBpm());
        addToContent(content, UltrastarTag.GAP, song.getGap());
        addToContent(content, UltrastarTag.START, song.getStart());
        addToContent(content, UltrastarTag.END, song.getEnd());
        addToContent(content, UltrastarTag.LANGUAGE, song.getLanguage());
        addToContent(content, UltrastarTag.YEAR, song.getYear());
        addToContent(content, UltrastarTag.GENRE, song.getGenre());
        addToContent(content, UltrastarTag.COVER, song.getCover());
        addToContent(content, UltrastarTag.BACKGROUND, song.getBackground());
        addToContent(content, UltrastarTag.VIDEO, song.getVideo());
        addToContent(content, UltrastarTag.VIDEOGAP, song.getVideogap());
        addToContent(content, UltrastarTag.PREVIEWSTART, song.getPreviewstart());
        addToContent(content, UltrastarTag.EDITION, song.getEdition());
        addToContent(content, UltrastarTag.CREATOR, song.getAuthor());
        addToContent(content, UltrastarTag.CREATOR, song.getCreator());
        addToContent(content, UltrastarTag.COMPOSER, song.getComposer());
        addToContent(content, UltrastarTag.COMMENT, song.getComment());
        addToContent(content, UltrastarTag.P1, song.getP1());
        addToContent(content, UltrastarTag.P2, song.getP2());

        boolean start = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!start && !line.startsWith("#")) {
                start = true;
            }
            if (start) {
                content.add(line);
            }
        }
        if (StringUtils.isNotEmpty(content.toString())) {
            String filename = file.getParent() + "\\" + file.getName();
            fileNameToContent.put(filename, content.toString());
        }
    }

    private void renameAndWriteFiles(String filename, String content) {
        if (StringUtils.isNotEmpty(content)) {
            File file = new File(filename);
            File oldFile = new File(filename.replace(".txt", ".old.txt"));
            if (file.renameTo(oldFile)) {
                FileWriter writer;
                try {
                    writer = new FileWriter(file);
                    writer.write(content.toString());
                    writer.close();
                    System.out.println("Updated " + filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addToContent(StringJoiner content, UltrastarTag tag, String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        content.add("#" + tag + ":" + text);
    }

    public List<Song> getImportedSongs() {
        return importedSongs;
    }

    private String detectEncoding(File file) {
        byte[] buf = new byte[4096];
        String encoding = null;
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    new FileInputStream(file));
            final UniversalDetector universalDetector = new UniversalDetector(null);
            int numberOfBytesRead;
            while ((numberOfBytesRead = bufferedInputStream.read(buf)) > 0
                    && !universalDetector.isDone()) {
                universalDetector.handleData(buf, 0, numberOfBytesRead);
            }
            universalDetector.dataEnd();
            encoding = universalDetector.getDetectedCharset();
            universalDetector.reset();
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encoding == null ? "Cp1252" : encoding;
    }

    private void initPlaylists(String playlistsPath) {
        System.out.println("Initializing playlists in " + playlistsPath);
        try {
            List<File> files = Files.walk(Paths.get(playlistsPath))
                                    .filter(Files::isRegularFile)
                                    .map(Path::toFile)
                                    .filter(file -> file.getName().toLowerCase().endsWith(".upl"))
                                    .toList();
            for (File file : files) {
                readPlaylist(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished initializing " + playlists.size() + " playlists");
    }

    private void readPlaylist(File file) {
        UltrastarPlaylist playlist = new UltrastarPlaylist(file);
        if (!playlist.getSongs().isEmpty()) {
            playlists.put(file.getName(), playlist);
        }
    }

    public Map<String, UltrastarPlaylist> getPlaylists() {
        return playlists;
    }

    public String getPlaylistPath() {
        return playlistPath;
    }

    public void setPlaylistPath(String playlistPath) {
        this.playlistPath = playlistPath;
    }
}