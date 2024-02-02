package com.doubledee.ultrastar.models;

import com.doubledee.ultrastar.UltrastarApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class UltrastarPlaylist {
    private String name;
    private List<String> songs = new ArrayList<>();
    public UltrastarPlaylist(File file) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().startsWith("#name:")) {
                    this.name = line.substring(7);
                }
                if (!line.startsWith("#")) {
                    songs.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println(file.getName());
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSongs() {
        return songs;
    }

    public boolean contains(String song) {
        return songs != null && songs.contains(song);
    }

    public byte[] getContentsAsByteArray() {
        String size = songs.size() == 1 ? "1 Song." : songs.size() + " Songs.";
        StringJoiner contents = new StringJoiner(System.lineSeparator());
        contents.add("######################################");
        contents.add("#Ultrastar Deluxe Playlist Format v1.0");
        contents.add("#Playlist \"" + getName() + "\" with " + size);
        contents.add("#Created with UltrastarCatalog v0.1");
        contents.add("######################################");
        contents.add("#Name: temp");
        contents.add("#Songs:");
        contents.add(getSongs().stream().collect(Collectors.joining(System.lineSeparator())));
        return contents.toString().getBytes(StandardCharsets.UTF_8);
    }
}
