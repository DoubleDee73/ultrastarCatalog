package com.doubledee.ultrastar.db;

import com.doubledee.ultrastar.models.Song;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MsAccessDb {

    private Map<Long, Song> songs;

    private final String path;

    public MsAccessDb(String path) {
        this.path = path;
        this.songs = new HashMap<>();
        initSongsDb();
    }

    private void initSongsDb() {
        Connection connection = null;
        Properties props = new Properties();
        props.put("charSet", "UTF-8");
        try {
            connection = DriverManager.getConnection("jdbc:ucanaccess://" + path, props);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("Select * from tblSongs");
            while (rs.next()) {
                Song song = new Song(rs);
                songs.put((long)song.getId(), song);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(connection);
        }
    }

    public Song findSongByPathAndFilename(String path, String filename) {
        for (Song song : getSongs().values()) {
            if (song.getPath().equalsIgnoreCase(path) && song.getTextFile().equalsIgnoreCase(filename)) {
                return song;
            }
        }
        return null;
    }

    public Map<Long, Song> getSongs() {
        return songs;
    }

    public void addSong(Song song) {
        String sql = "INSERT INTO tblSongs (" +
                "strTitle, strArtist, strLanguage, strMp3, dblBpm, " +
                "strCover, strBackground, strVideo, strPath, strTextfile, " +
                "strType, strYear, dtmDateAdded, dtmUpdated, strVariant, ID) VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        persistSong(song, sql);
    }

    public void updateSong(Song song) {
        String sql = "UPDATE tblSongs SET " +
                "strTitle = ?, strArtist = ?, strLanguage = ?, strMp3 = ?, dblBpm = ?, " +
                "strCover = ?, strBackground = ?, strVideo = ?, strPath = ?, strTextfile = ?, " +
                "strType = ?, strYear = ?, dtmUpdated = ?, strVariant = ? WHERE ID = ?";
        persistSong(song, sql);
    }

    private void persistSong(Song song, String sql) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:ucanaccess://" + path);
            int i = 1;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(i++, song.getTitle());
            preparedStatement.setString(i++, song.getArtist());
            preparedStatement.setString(i++, song.getLanguage() != null ? song.getLanguage() : null);
            preparedStatement.setString(i++, song.getMp3());
            preparedStatement.setString(i++, song.getBpm());
            preparedStatement.setString(i++, song.getCover());
            preparedStatement.setString(i++, song.getBackground());
            preparedStatement.setString(i++, song.getVideo());
            preparedStatement.setString(i++, song.getPath());
            preparedStatement.setString(i++, song.getTextFile());
            preparedStatement.setString(i++, song.getSongType().toString());
            preparedStatement.setString(i++, song.getYear());
            if (sql.startsWith("INSERT INTO")) {
                preparedStatement.setDate(i++, new Date(System.currentTimeMillis()));
            }
            preparedStatement.setDate(i++, new Date(System.currentTimeMillis()));
            preparedStatement.setString(i++, song.getVariant());
            preparedStatement.setLong(i++, song.getId());

            if (preparedStatement.executeUpdate() > 0) {
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    song.setId(generatedKeys.getInt(1));
                }
                getSongs().put((long)song.getId(), song);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(connection);
        }
    }

    public static void close(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
