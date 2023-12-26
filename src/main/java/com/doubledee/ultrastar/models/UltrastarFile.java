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
    private String comment;
    private String gap;
    private String edition;
    private String genre;
    private String videogap;
    private String videostart;
    private String start;
    private String end;
    private String length;
    private String resolution;
    private String album;
    private String previewstart;
    private String composer;
    private String creator;
    private String author;
    private String encoding;
    private String p1;
    private String p2;
    private String notesgap;
    private String medleystartbeat;
    private String medleyendbeat;
    private String relative;
    private String id;

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getGap() {
        return gap;
    }

    public void setGap(String gap) {
        this.gap = gap;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getVideogap() {
        return videogap;
    }

    public void setVideogap(String videogap) {
        this.videogap = videogap;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPreviewstart() {
        return previewstart;
    }

    public void setPreviewstart(String previewstart) {
        this.previewstart = previewstart;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getP1() {
        return p1;
    }

    public void setP1(String p1) {
        this.p1 = p1;
    }

    public String getP2() {
        return p2;
    }

    public void setP2(String p2) {
        this.p2 = p2;
    }

    public String getNotesgap() {
        return notesgap;
    }

    public void setNotesgap(String notesgap) {
        this.notesgap = notesgap;
    }

    public String getMedleystartbeat() {
        return medleystartbeat;
    }

    public void setMedleystartbeat(String medleystartbeat) {
        this.medleystartbeat = medleystartbeat;
    }

    public String getMedleyendbeat() {
        return medleyendbeat;
    }

    public void setMedleyendbeat(String medleyendbeat) {
        this.medleyendbeat = medleyendbeat;
    }

    public String getRelative() {
        return relative;
    }

    public void setRelative(String relative) {
        this.relative = relative;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVideostart() {
        return videostart;
    }

    public void setVideostart(String videostart) {
        this.videostart = videostart;
    }
}
