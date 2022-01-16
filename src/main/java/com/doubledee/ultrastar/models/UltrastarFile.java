package com.doubledee.ultrastar.models;

import com.doubledee.ultrastar.importer.SongImporter;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

public class UltrastarFile {
    private String title;
    private String artist;
    private String mp3;
    private String bpm;
    private String year;
    private String cover;
    private String background;
    private String video;
    private String language;
    private String path;
    private String filename;
    private Date lastUpdate;

    public static final String UTF8_BOM = "\uFEFF";

    public UltrastarFile(String title) {
        this.title = title;
    }

    public UltrastarFile(File file, String encoding) {
        this.filename = file.getName();
        this.path = file.getParent().replace(SongImporter.SONGS_PATH, StringUtils.EMPTY);
        this.lastUpdate = new Date(file.lastModified());
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), encoding);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace(UTF8_BOM, StringUtils.EMPTY);
                UltrastarTag ultrastarTag = UltrastarTag.getTagForLine(line);
                if (ultrastarTag != null) {
                    Method setter = this.getClass().getMethod(ultrastarTag.getSetterName(), String.class);
                    if (setter != null) {
                        setter.invoke(this, line.substring(ultrastarTag.name().length() + 2));
                    }
                }
                if (line.startsWith(":")) {
                    break;
                }
            }
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.out.println(file.getName());
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getMp3() {
        return mp3;
    }

    public void setMp3(String mp3) {
        this.mp3 = mp3;
    }

    public String getBpm() {
        return bpm;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
