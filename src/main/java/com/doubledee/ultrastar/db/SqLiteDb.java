package com.doubledee.ultrastar.db;

import com.doubledee.ultrastar.models.Score;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqLiteDb {
    public List<Score> fetchSongScores(String dbFile) {
        Connection connection;
        List<Score> scores = new ArrayList<>();
        // create a database connection
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            String sql = "select s.id, s.artist, s.title, sc.player, " +
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
        return scores.stream().distinct().collect(Collectors.toList());
    }

    public List<Score> migrateScores(List<Score> ultrastarScores, String legacyDb, String currentDb) {
        List<Score> legacyScores = fetchSongScores(legacyDb);
        List<Score> migratedScores = new ArrayList<>();
        for (Score score : legacyScores) {
            if (ultrastarScores.stream().anyMatch(it -> scoresAreEqual(it, score))) {
                System.out.println(score + " was already found in the target DB. Skipping the score");
                continue;
            }
            migratedScores.add(persistLegacyScore(score, currentDb));
        }
        return migratedScores;
    }

    private Score persistLegacyScore(Score score, String currentDb) {
        try {
            Connection currentConnection = DriverManager.getConnection("jdbc:sqlite:" + currentDb);
            String sql = "select * from us_songs where Artist=? and Title=?";
            PreparedStatement statement = currentConnection.prepareStatement(sql);
            Reader artistReader = createCharStream(score.getArtist());
            Reader titleReader = createCharStream(score.getTitle());
            statement.setCharacterStream(1, artistReader, score.getArtist().length() + 1);
            statement.setCharacterStream(2, titleReader, score.getTitle().length() + 1);
            ResultSet resultSet = statement.executeQuery();
            int songId;
            if (resultSet.next()) {
                songId = resultSet.getInt(1);
            } else {
                artistReader.reset();
                titleReader.reset();
                statement = currentConnection.prepareStatement("INSERT INTO us_songs (Artist, Title, TimesPlayed) " +
                                                                       "VALUES (?, ?, ?)");
                statement.setCharacterStream(1, artistReader, score.getArtist().length() + 1);
                statement.setCharacterStream(2, titleReader, score.getTitle().length() + 1);
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
            statement.setCharacterStream(3, createCharStream(score.getPlayer()), score.getPlayer().length() + 1);
            statement.setInt(4, score.getScore());
            statement.setLong(5, score.getDate().getTime() / 1000);
            statement.execute();
            score.setUltrastarId(songId);
            currentConnection.close();
            System.out.println(score + " was successfully imported into the new score database.");
        } catch (SQLException | IOException e) {
            System.out.println("Something went wrong while trying to persist score " + score);
            throw new RuntimeException(e);
        }
        return score;
    }
    private boolean scoresAreEqual(Score newScore, Score oldScore) {
        return oldScore.toString().equalsIgnoreCase(newScore.toString());
    }

    private Reader createCharStream(String text) {
        return new StringReader(text + "\0");
    }
}
