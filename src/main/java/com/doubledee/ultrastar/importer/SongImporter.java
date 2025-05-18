package com.doubledee.ultrastar.importer;

import com.doubledee.ultrastar.db.SqLiteDb;
import com.doubledee.ultrastar.models.*;
import com.doubledee.ultrastar.utils.HashUtil;
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
    public static final String LITE = "lite";
    public static final String ULTRASTAR_DB = "UltrastarDb";
    public static final String ULTRASTAR_DB_LEGACY = "UltrastarDbLegacy";

    private List<Song> importedSongs = new ArrayList<>();
    private Map<String, UltrastarPlaylist> playlists = new HashMap<>();
    private Map<String, List<Score>> scores = new HashMap<>();

    private String playlistPath;
    private boolean lite;

    private static final Set<String> IGNORE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(".git",
                                                                                                      ".gitignore",
                                                                                                      ".idea",
                                                                                                      "_temp")));

    public SongImporter() {
        File config = new File("config.properties");
        FileWriter myWriter;
        if (!config.exists()) {
            System.out.println("config.properties were not found in " + config.getParent() + ". Creating new file.");
            try {
                myWriter = new FileWriter("config.properties");
                myWriter.write(SONGS_PATH + "1=C:/Program Files (x86)/UltraStar Deluxe/songs/");
                myWriter.write(System.lineSeparator());
                myWriter.write(PLAYLISTS_PATH + "=C:/Program Files (x86)/UltraStar Deluxe/playlists/");
                myWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Properties appProps = new Properties();
        try {
            System.out.println("Loading config from " + config.getAbsolutePath());
            appProps.load(new FileInputStream(config.getAbsolutePath()));
            for (Map.Entry<Object, Object> propEntry : appProps.entrySet()) {
                System.out.println("Key: " + propEntry.getKey() + " = " + propEntry.getValue());
            }
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
        boolean lite = "true".equalsIgnoreCase((String)appProps.get(LITE));
        setLite(lite);
        String dbPath = (String) appProps.get(ULTRASTAR_DB);
        if (!lite && (StringUtils.isEmpty(playlistsPath) || StringUtils.isEmpty(dbPath))) {
            System.out.println(
                    PLAYLISTS_PATH + " and/or " + ULTRASTAR_DB + " property not found. Trying to guess them.");
            guessMissingPaths(config, appProps, StringUtils.isEmpty(playlistsPath), StringUtils.isEmpty(dbPath));
            playlistsPath = (String) appProps.get(PLAYLISTS_PATH);
            dbPath = (String) appProps.get(ULTRASTAR_DB);

        }
        if (!lite && StringUtils.isNotEmpty(playlistsPath)) {
            setPlaylistPath(playlistsPath);
            initPlaylists(playlistsPath);
        } else {
            System.out.println("No playlist found in " + PLAYLISTS_PATH);
        }
        if (StringUtils.isNotEmpty(dbPath)) {
            Path databasePath = Paths.get(dbPath);
            if (!Files.exists(databasePath)) {
                System.out.println("Something is wrong with the songs path " + databasePath);
            }
            SqLiteDb sqLiteDb = new SqLiteDb();
            List<Score> ultrastarScores = sqLiteDb.fetchSongScores(dbPath);
            /*
            This could work, but storing from the legacy to the actual db doesn't work properly, as the artist
            and title are blobs in SQlite, and the bytestreams that end up in the database seem to break it
            */
            //String legacyDb = (String)appProps.get(ULTRASTAR_DB_LEGACY);
            //if (legacyDb != null) {
            //    ultrastarScores.addAll(sqLiteDb.migrateScores(ultrastarScores, legacyDb, dbPath));
            //}
            scores = attachCatalogIds(ultrastarScores);
        }
    }

    private void guessMissingPaths(File config, Properties appProps, boolean playlistMissing, boolean dbMissing) {
        File playlist;
        File dbPath;
        String songdir = (String) appProps.get(SONGS_PATH + 1);
        if (songdir == null) {
            System.out.println("The songdir was not configured. Can't deduce playlist path from that");
            return;
        }
        File songPath = new File(songdir);
        if (!songPath.exists()) {
            System.out.println("The songdir was not found. Can't deduce playlist path from that");
            return;
        }
        if (playlistMissing) {
            playlist = new File(songPath.getParent() + "/playlists");
        } else {
            playlist = null;
        }
        if (dbMissing) {
            dbPath = new File(songPath.getParent() + "/Ultrastar.db");
        } else {
            dbPath = null;
        }
        if (playlist != null && playlist.exists() || (dbPath != null && dbPath.exists())) {
            FileWriter fw;
            try {
                fw = new FileWriter(config.getAbsolutePath(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                if (playlist != null) {
                    bw.newLine();
                    bw.write(PLAYLISTS_PATH + "=" + playlist.getAbsolutePath().replace("\\", "\\\\"));
                    appProps.put(PLAYLISTS_PATH, playlist.getAbsolutePath());
                }
                if (dbPath != null) {
                    bw.newLine();
                    bw.write(ULTRASTAR_DB + "=" + dbPath.getAbsolutePath().replace("\\", "\\\\"));
                    appProps.put(ULTRASTAR_DB, dbPath.getAbsolutePath());
                }
                bw.newLine();
                bw.close();
            } catch (IOException e) {
                System.out.println("Writing to config failed");
            }
        }
    }

    private List<File> retrieveFilesFromPath(String path) throws IOException {
        if (isIgnored(path)) {
            System.out.println("Skipping " + path);
            return Collections.emptyList();
        }
        Path songPath = Paths.get(path);
        File pathAsFile = songPath.toFile();
        boolean hasTxt = Arrays.stream(Objects.requireNonNull(pathAsFile.listFiles()))
                               .map(File::getName)
                               .anyMatch(filename -> filename.endsWith(".txt") && !filename.startsWith("._"));
        int size = -1;
        if (pathAsFile.isDirectory()) {
            if (!hasTxt) {
                if (Arrays.stream(Objects.requireNonNull(pathAsFile.listFiles())).anyMatch(File::isDirectory)) {
                    System.out.println("Initializing songs in " + path);
                    size = Objects.requireNonNull(pathAsFile.listFiles()).length;
                } else {
                    System.out.println("TXT file is missing in " + path);
                }
            }
        }

        int lastStep = 0;
        int counter = 0;
        List<File> files = new ArrayList<>();
        for (File folder : Objects.requireNonNull(pathAsFile.listFiles())) {
            counter++;

            if (isIgnored(folder.getName())) {
                continue;
            }
            if (folder.isDirectory()) {
                double percentDone = ((double) counter) / size;
                int percentRounded = ((int) (percentDone * 100));
                if (percentRounded % 10 == 0 && percentRounded > lastStep) {
                    System.out.println("Preparing folder " + pathAsFile.getPath() + ": " + percentRounded + "% done");
                    lastStep = percentRounded;
                }
                files.addAll(retrieveFilesFromPath(folder.getAbsolutePath()));
            } else {
                String txtName = folder.getName().toLowerCase();
                if (txtName.endsWith(".txt") && !txtName.startsWith("._")) {
                    files.add(folder);
                }
            }
        }
        return files;
    }

    private boolean isIgnored(String name) {
        return IGNORE.stream().anyMatch(name::equalsIgnoreCase);
    }

    public void init(String path) {
        try {
            List<File> files = retrieveFilesFromPath(path);
            if (files.isEmpty()) {
                System.out.println("No files were found in " + path);
            }
            Map<String, String> filesToUpdate = new HashMap<>();
            int counter = 0;
            int size = files.size();
            int lastStep = 0;
            System.out.println("Reading songs");
            for (File file : files) {
                counter++;
//                System.out.println("Reading " + file.getName());
                double percentDone = ((double) counter) / size;
                int percentRounded = ((int) (percentDone * 100));
                if (percentRounded % 10 == 0 && percentRounded > lastStep) {
                    System.out.println("Reading songs: " + percentRounded + "% done");
                    lastStep = percentRounded;
                }

                String encoding = detectEncoding(file);
                UltrastarFile ultrastarFile = new UltrastarFile(file, encoding);
                if (StringUtils.isEmpty(ultrastarFile.getTitle())) {
                    System.out.println("File: " + file.getAbsolutePath() + " seems to be wrong. This shouldn't happen");
                    continue;
                }
                Song song = new Song(ultrastarFile);
                song.setUid(HashUtil.sha1(file.getAbsolutePath()));
                if (song.isDirty()) {
                    //updateFile(song, file, filesToUpdate);
                }
                String mp3Path = song.getPath() + File.separator + song.getMp3();
                boolean hasFile = Files.exists(Paths.get(mp3Path));
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
                    System.out.println("Audio Missing: " + mp3Path);
                }
                String cover = song.getCover();
                if (StringUtils.isEmpty(cover)) {
                    System.out.println("Cover-Tag missing: " + file.getAbsolutePath());
                    hasFile = Files.walk(Paths.get(song.getPath()))
                                   .filter(Files::isRegularFile)
                                   .map(Path::toFile)
                                   .anyMatch(filename -> filename.getName().toLowerCase().contains("[co]"));
                } else {
                    hasFile = Files.exists(Paths.get(song.getPath() + File.separator + cover));
                }
                if (!hasFile) {
                    System.out.println("Cover missing/invalid: " + file.getAbsolutePath());
                }
//                if (song.getLanguage().equals(Language.UNDEFINED)) {
//                    System.out.println("Missing language: " + file.getAbsolutePath());
//                }
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

    private void checkMissingTxt(Path songPath) throws IOException {
        List<File> folders = Arrays.stream(Objects.requireNonNull(songPath.toFile().listFiles()))
                                   .filter(File::isDirectory)
                                   .filter(it -> !it.getName().equals(".git"))
                                   .filter(it -> !it.getName().equals(".gitignore"))
                                   .filter(it -> !it.getName().equals(".idea"))
                                   .filter(it -> !it.getName().equals("_temp"))
                                   .toList();
        int size = folders.size();
        System.out.println("Checking for missing txt files in " + size + " folders");
        int lastStep = 0;
        int counter = 0;
        for (File subfolder : folders) {
            counter++;
            if (subfolder.listFiles() == null) {
                System.out.println(subfolder.getAbsolutePath() + " has no files");
                continue;
            }
            double percentDone = ((double) counter) / size;
            int percentRounded = ((int) (percentDone * 100));
            if (percentRounded % 10 == 0 && percentRounded > lastStep) {
                System.out.println("checkMissingTxt: " + percentRounded + "% done");
                lastStep = percentRounded;
            }
            if (Arrays.stream(Objects.requireNonNull(subfolder.listFiles())).filter(File::isFile)
                      .noneMatch(file -> file.getName().toLowerCase().endsWith(".txt"))) {
                System.out.println(subfolder.getAbsolutePath() + " has no txt");
            }
        }
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
        Path playlist = Paths.get(playlistsPath);
        if (Files.exists(playlist)) {
            System.out.println("Initializing playlists in " + playlistsPath);
        } else {
            System.out.println("Could not find playlist path " + playlistsPath);
            return;
        }
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


    private Map<String, List<Score>> attachCatalogIds(List<Score> ultrastarScores) {
        Map<String, List<Score>> usdx2cat = new HashMap<>();
        for (Score score : ultrastarScores) {
            if (score.getCatalogId() == null) {
                String scoreArtistTitle = score.getArtist() + " : " + score.getTitle();
                Song catSong = getImportedSongs().stream()
                                                 .filter(song -> song.getArtistAndTitle()
                                                                     .equalsIgnoreCase(scoreArtistTitle))
                                                 .findFirst()
                                                 .orElse(null);
                if (catSong != null) {
                    usdx2cat.put(catSong.getUid(), new ArrayList<>());
                    ultrastarScores.stream()
                                   .filter(sc -> sc.getUltrastarId() == score.getUltrastarId())
                                   .forEach(it -> it.setCatalogId(
                                           catSong.getUid()));
                }
            }
            List<Score> scoresPerSong = usdx2cat.get(score.getCatalogId());
            if (scoresPerSong != null) {
                scoresPerSong.add(score);
            } else {
                System.out.println("Could not attach score of " + score);
            }
        }
        usdx2cat.values().forEach(col -> col.sort(Comparator.comparingLong(Score::getScore).reversed()));
        return usdx2cat;
    }

    public Map<String, List<Score>> getScores() {
        return scores;
    }

    public boolean isLite() {
        return lite;
    }

    public void setLite(boolean lite) {
        this.lite = lite;
    }
}