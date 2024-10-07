package com.doubledee.ultrastar.models;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

public class Score {
    private int ultrastarId;
    private final String artist;
    private final String title;
    private final String player;
    private final int difficulty;
    private final int score;
    private final int timesPlayed;
    private final Date date;
    private String catalogId;

    public Score(ResultSet resultSet) throws SQLException {
        this.ultrastarId = resultSet.getInt(1);
        this.artist = reader2String(resultSet.getCharacterStream(2));
        this.title = reader2String(resultSet.getCharacterStream(3));
        this.player = reader2String(resultSet.getCharacterStream(4));
        this.difficulty = resultSet.getInt(5);
        this.score = resultSet.getInt(6);
        this.timesPlayed = resultSet.getInt(7);
        this.date = new Date(resultSet.getInt(8) * 1000L);
    }

    public void setUltrastarId(int ultrastarId) {
        this.ultrastarId = ultrastarId;
    }

    public int getUltrastarId() {
        return ultrastarId;
    }

    public String reader2String(Reader text) {
        int intValueOfChar;
        StringBuilder targetString = new StringBuilder();
        try {
            do {
                intValueOfChar = text.read();
                if (intValueOfChar > 0) {
                    targetString.append((char) intValueOfChar);
                }
            } while (intValueOfChar >= 0);
            text.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return targetString.toString();
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

    @Override
    public String toString() {
        return getArtist() + " - " + getTitle() + ": " + getScore() + " " + getDate();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Score that)) {
            return false;
        }
        return getScore() == that.getScore() && getPlayer().equals(
                that.getPlayer()) && getDate().getTime() == that.getDate().getTime();
    }
}
