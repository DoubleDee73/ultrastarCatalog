package com.doubledee.ultrastar.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Song {
    private int id;
    private final String title;
    private final String artist;
    private final String mp3;
    private final String path;
    private final String textFile;
    private SongType songType;
    private Language language;
    private String bpm;
    private String cover;
    private String background;
    private String video;
    private String year;
    private Date dateAdded;
    private Date lastUpdate;
    private String variant;

    private final static Pattern BRACKET_PATTERN = Pattern.compile("\\[.*?\\]");

    public Song(int id, String title, String artist, String mp3, String path, String textFile) {
        this.id = id;
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
        this.id = 0;
        this.songType = SongType.getSongTypeByTitle(ultrastarFile.getTitle());
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
    }

    public int getId() {
        return id;
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

    public void setId(int id) {
        this.id = id;
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
}
