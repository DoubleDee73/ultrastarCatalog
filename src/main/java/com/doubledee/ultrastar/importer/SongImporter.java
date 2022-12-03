package com.doubledee.ultrastar.importer;

import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.UltrastarFile;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SongImporter {

    public static final String SONGS_PATH = "C:\\Program Files (x86)\\UltraStar Deluxe\\songs\\";

    private List<Song> importedSongs = new ArrayList<>();

    public SongImporter() {
        this(SONGS_PATH);
    }

    public SongImporter(String path) {
        try {
            List<File> files = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> !file.getParent().endsWith("\\_temp"))
                    .filter(file -> file.getName().toLowerCase().endsWith(".txt"))
                    .collect(Collectors.toList());
            for (File file : files) {
//                System.out.println("Reading " + file.getName());
                UltrastarFile ultrastarFile = new UltrastarFile(file, detectEncoding(file));
                Song song = new Song(ultrastarFile);
                boolean hasFile = Files.exists(Paths.get(SONGS_PATH + song.getPath() + "\\" + song.getMp3()));
                if (hasFile && StringUtils.isNotEmpty(ultrastarFile.getTitle())) {
                    if (StringUtils.isEmpty(song.getCover())) {
                        for (File cover : new File(SONGS_PATH + song.getPath()).listFiles()) {
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
                hasFile = Files.exists(Paths.get(SONGS_PATH + song.getPath() + "\\" + song.getCover()));
                if (!hasFile) {
                    System.out.println("Invalid cover art: " + file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished importing  " + importedSongs.size() + " songs.");
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
}
