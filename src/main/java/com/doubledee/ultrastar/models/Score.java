package com.doubledee.ultrastar.models;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

public class Score {
    private int ultrastarId;
    private final String artist;
    private final byte[] artistBytes;
    private final String title;
    private final byte[] titleBytes;
    private final String player;
    private final byte[] playerBytes;
    private final int difficulty;
    private final int score;
    private final int timesPlayed;
    private final Date date;
    private String catalogId;

    public Score(ResultSet resultSet) throws SQLException {
        this.ultrastarId = resultSet.getInt(1);
        this.artist = dehex(resultSet.getString(2));
        this.artistBytes = resultSet.getBytes(3);
        this.title = dehex(resultSet.getString(4));
        this.titleBytes = resultSet.getBytes(5);
        this.player = dehex(resultSet.getString(6));
        this.playerBytes = resultSet.getBytes(7);
        this.difficulty = resultSet.getInt(8);
        this.score = resultSet.getInt(9);
        this.timesPlayed = resultSet.getInt(10);
        this.date = new Date(resultSet.getInt(11) * 1000L);
    }

    public void setUltrastarId(int ultrastarId) {
        this.ultrastarId = ultrastarId;
    }

    public int getUltrastarId() {
        return ultrastarId;
    }

    public String dehex(String text) {
        try {
            byte[] bytes = Hex.decodeHex(text.toCharArray());
            return new String(trim(bytes), StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            return null;
        }
    }

    private static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getPlayer() {
        return player;
    }

    public int getScore() {
        return score;
    }

    public Date getDate() {
        return date;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    public byte[] getArtistBytes() {
        return artistBytes;
    }

    public byte[] getTitleBytes() {
        return titleBytes;
    }

    public byte[] getPlayerBytes() {
        return playerBytes;
    }

    @Override
    public String toString() {
        return getArtist() + " - " + getTitle() + ": " + getScore() + " " + getDate();
    }
}
