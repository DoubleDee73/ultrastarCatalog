package com.doubledee.ultrastar.models;

import com.doubledee.ultrastar.utils.VersionUtils;

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
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file),
                                                                        StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("﻿", ""); // UTF-8 BOM
                if (line.toLowerCase().startsWith("#name:")) {
                    this.name = line.substring(6).trim();
                }
                // blank lines are not songs (an empty playlist must stay empty)
                if (!line.startsWith("#") && !line.isBlank()) {
                    songs.add(line);
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(file.getName());
            e.printStackTrace();
        }
        if (name == null || name.isBlank()) {
            // no "#Name:" line: use the file name without ".upl"
            this.name = file.getName().replaceFirst("(?i)\\.upl$", "");
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
        contents.add("#Created with UltrastarCatalog v" + VersionUtils.getVersion());
        contents.add("######################################");
        contents.add("#Name: " + getName());
        contents.add("#Songs:");
        contents.add(getSongs().stream().collect(Collectors.joining(System.lineSeparator())));
        return contents.toString().getBytes(StandardCharsets.UTF_8);
    }
}
