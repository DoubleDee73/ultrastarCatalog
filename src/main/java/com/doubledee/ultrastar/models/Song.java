package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Song {
    private long uid;
    private final String title;
    private final String artist;
    private final String mp3;
    private final String path;
    private final String textFile;
    private SongType songType;
    private Language language;
    private String bpm;
    private String gap;
    private String cover;
    private String background;
    private String video;
    private String videogap;
    private String year;
    private String length;
    private String start;
    private String end;
    private Date dateAdded;
    private Date lastUpdate;
    private String variant;
    private String comment;
    private String genre;
    private String edition;
    private String album;
    private String composer;
    private String creator;
    private String author;
    private String resolution;
    private String previewstart;
    private String p1;
    private String p2;
    private String notesgap;
    private String medleystartbeat;
    private String medleyendbeat;
    private String relative;
    private String id;
    private boolean dirty;

    private final static Pattern BRACKET_PATTERN = Pattern.compile("\\[.*?\\]");

    public Song(int uid, String title, String artist, String mp3, String path, String textFile) {
        this.uid = uid;
        this.title = title;
        this.artist = artist;
        this.mp3 = mp3;
        this.path = path;
        this.textFile = textFile;
    }


    public Song(ResultSet rs) throws SQLException {
        this(rs.getInt("ID"), rs.getString("strTitle"),
                rs.getString("strArtist"), rs.getString("strMp3"),
                rs.getString("strPath"), rs.getString("strTextfile"));
        setBackground(rs.getString("strBackground"));
        setCover(rs.getString("strCover"));
        setVideo(rs.getString("strVideo"));
        setBpm(rs.getString("dblBpm"));
        setYear(rs.getString("strYear"));
        setDateAdded(rs.getTimestamp("dtmDateAdded"));
        setSongType(SongType.getSongTypeByTitle(getTitle()));
        setLanguage(Language.getLanguage(rs.getString("strLanguage")));
        setLastUpdate(rs.getTimestamp("dtmUpdated"));
        setVariant(rs.getString("strVariant"));
    }

    public Song(UltrastarFile ultrastarFile) {
        this.uid = UUID.randomUUID().getMostSignificantBits();
        this.songType = SongType.getSongTypeByTitle(ultrastarFile.getTitle());
        this.comment = ultrastarFile.getComment();
        if (this.songType != SongType.ORIGINAL_KARAOKE) {
            if (StringUtils.isEmpty(ultrastarFile.getComment())) {
                setComment(songType.songTypeName);
                this.dirty = true;
            }
        }
        String tempTitle;
        if (StringUtils.isNotEmpty(songType.qualifier)) {
            tempTitle = StringUtils.replaceIgnoreCase(ultrastarFile.getTitle(), " (" + songType.qualifier + ")",
                    StringUtils.EMPTY).trim();
        } else {
            tempTitle = ultrastarFile.getTitle();
        }
        Matcher bracketResult = BRACKET_PATTERN.matcher(tempTitle);
        while (bracketResult.find()) {
            String variant = bracketResult.group();
            tempTitle = tempTitle.replace(variant, StringUtils.EMPTY).trim();
            this.variant = variant;
        }
        this.title = tempTitle;
        this.artist = ultrastarFile.getArtist();
        this.mp3 = ultrastarFile.getMp3();
        this.bpm = ultrastarFile.getBpm();
        this.background = ultrastarFile.getBackground();
        this.cover = ultrastarFile.getCover();
        this.video = ultrastarFile.getVideo();
        this.language = Language.getLanguage(ultrastarFile.getLanguage());
        this.year = ultrastarFile.getYear();
        this.path = ultrastarFile.getPath();
        this.textFile = ultrastarFile.getFilename();
        this.lastUpdate = ultrastarFile.getLastUpdate();

        this.gap = ultrastarFile.getGap();
        this.videogap = ultrastarFile.getVideogap();
        this.length = ultrastarFile.getLength();
        this.start = ultrastarFile.getStart();
        this.end = ultrastarFile.getEnd();
        this.genre = ultrastarFile.getGenre();
        this.edition = ultrastarFile.getEdition();
        this.album = ultrastarFile.getAlbum();
        this.composer = ultrastarFile.getComposer();
        this.creator = ultrastarFile.getCreator();
        this.author = ultrastarFile.getAuthor();
        this.resolution = ultrastarFile.getResolution();
        this.previewstart = ultrastarFile.getPreviewstart();
        this.p1 = ultrastarFile.getP1();
        this.p2 = ultrastarFile.getP2();
        this.notesgap = ultrastarFile.getNotesgap();
        this.medleystartbeat = ultrastarFile.getMedleystartbeat();
        this.medleyendbeat = ultrastarFile.getMedleyendbeat();
        this.relative = ultrastarFile.getRelative();
        this.id = ultrastarFile.getId();
    }

    public long getUid() {
        return uid;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getArtistNormalized() {
        return Normalizer.normalize(artist, Normalizer.Form.NFD).toLowerCase();
    }

    public String getTitleNormalized() {
        return Normalizer.normalize(title, Normalizer.Form.NFD).toLowerCase();
    }

    public String getFeaturedArtist() {
        int featIndex = getTitleNormalized().indexOf("feat.");
        if (featIndex > 0) {
            int lastIndex = getTitleNormalized().lastIndexOf(")");
            return getTitleNormalized().substring(featIndex + 5, lastIndex).trim();
        }
        return StringUtils.EMPTY;
    }

    public String getAudioType() {
        if (StringUtils.isEmpty(mp3) || !mp3.contains(".")) {
            return StringUtils.EMPTY;
        }
        String extension = mp3.substring(mp3.indexOf(".")).toLowerCase();
        return switch (extension) {
            case "m4a" -> "mp4";
            case "ogg", "oga" -> "ogg";
            default -> "mp3";
        };
    }

    public boolean hasAudio() {
        return StringUtils.isNotEmpty(mp3);
    }

    public String getMp3() {
        return mp3;
    }

    public String getPath() {
        return path;
    }

    public String getTextFile() {
        return textFile;
    }

    public SongType getSongType() {
        if (songType == null) {
            songType = SongType.getSongTypeByTitle(getTitle());
        }
        return songType;
    }

    public void setSongType(SongType songType) {
        this.songType = songType;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getLanguageCode() {
        return getLanguage().getLanguageCode();
    }

    public String getBpm() {
        return bpm;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getTitleAndVariant() {
        return (getTitle() + " " + StringUtils.defaultString(getVariant())).trim();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getImagePath() {
        return getTextFile().replace("[", "{")
                            .replace("]", "}")
                            .replace(".txt", ".jpg");
    }

    public boolean isInDecade(String decade) {
        if (StringUtils.isEmpty(decade) || StringUtils.isEmpty(getYear()) || !NumberUtils.isDigits(getYear())) {
            return false;
        }
        int year = NumberUtils.toInt(getYear());
        Decade decadeEnum = Decade.getDecadeByString(decade);
        return year >= decadeEnum.getStartYear() && year < (decadeEnum.getStartYear() + 10);
    }

    public String getGap() {
        return gap;
    }

    public void setGap(String gap) {
        this.gap = gap;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
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

    public String getVideogap() {
        return videogap;
    }

    public void setVideogap(String videogap) {
        this.videogap = videogap;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getPreviewstart() {
        return previewstart;
    }

    public void setPreviewstart(String previewstart) {
        this.previewstart = previewstart;
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

    public String getStartFragment() {
        if (StringUtils.isEmpty(getStart())) {
            return StringUtils.EMPTY;
        }
        return "#t=" + getStart();
    }
}
