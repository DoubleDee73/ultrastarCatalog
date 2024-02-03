package com.doubledee.ultrastar.db;

import com.doubledee.ultrastar.models.Score;
import com.doubledee.ultrastar.models.Song;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqLiteDb {
    public List<Score> fetchSongScores(String dbFile) {
        Connection connection;
        List<Score> scores = new ArrayList<>();
        // create a database connection
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            String sql = "select s.id, hex(s.artist), s.artist, hex(s.title), s.title, hex(sc.player), sc.player, " +
                    "sc.difficulty, sc.score, s.TimesPlayed, sc.date from us_songs s " +
                    "inner join us_scores sc on s.id = sc.SongId";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                scores.add(new Score(resultSet));
            }
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return scores;
    }

    public List<Score> migrateScores(List<Score> ultrastarScores, String legacyDb, String currentDb) {
        List<Score> legacyScores = fetchSongScores(legacyDb);
        List<Score> migratedScores = new ArrayList<>();
        for (Score score : legacyScores) {
            if (ultrastarScores.stream().anyMatch(it -> scoresAreEqual(it, score))) {
                continue;
            }
            migratedScores.add(persistLegacyScore(score, currentDb));
        }
        return migratedScores;
    }

    private Score persistLegacyScore(Score score, String currentDb) {
        try {
            Connection currentConnection = DriverManager.getConnection("jdbc:sqlite:" + currentDb);
            String sql = "select * from us_songs where Hex(Artist)=? and Hex(Title)=?";
            PreparedStatement statement = currentConnection.prepareStatement(sql);
            statement.setString(1, enhex(score.getArtist()));
            statement.setString(2, enhex(score.getTitle()));
            ResultSet resultSet = statement.executeQuery();
            int songId;
            if (resultSet.next()) {
                songId = resultSet.getInt(1);
            } else {
                statement = currentConnection.prepareStatement("INSERT INTO us_songs (Artist, Title, TimesPlayed) " +
                                                                       "VALUES (?, ?, ?)");
                statement.setBytes(1, score.getArtistBytes());
                statement.setBytes(2, score.getTitleBytes());
                statement.setInt(3, score.getTimesPlayed());
                statement.execute();
                resultSet = currentConnection.createStatement().executeQuery("SELECT Max(id) from us_songs");
                resultSet.next();
                songId = resultSet.getInt(1);
            }
            statement = currentConnection.prepareStatement("INSERT INTO us_scores " +
                                                                   "(SongId, Difficulty, Player, Score, Date) VALUES " +
                                                                   "(?, ?, ?, ?, ?)");
            statement.setInt(1, songId);
            statement.setInt(2, score.getDifficulty());
            statement.setBytes(3, score.getPlayerBytes());
            statement.setInt(4, score.getScore());
            statement.setLong(5, score.getDate().getTime() / 1000);
            statement.execute();
            score.setUltrastarId(songId);
            currentConnection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return score;
    }

    private String enhex(String text) {
        return Hex.encodeHexString(text.getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    private boolean scoresAreEqual(Score newScore, Score oldScore) {
        return oldScore.toString().equalsIgnoreCase(newScore.toString());
    }

    private byte[] getBytesFromText(String text) {
        byte[] withoutNullByte = text.getBytes();
        byte[] returnArray = new byte[withoutNullByte.length + 1];
        System.arraycopy(withoutNullByte, 0, returnArray, 0, withoutNullByte.length);
        return withoutNullByte;
    }
}
