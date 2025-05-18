package com.doubledee.ultrastar.models;

public enum ListMode {
    ARTIST("Artist"),
    TITLE("Song Title"),
    FAVORITES("Favorites");

    final String headline;

    ListMode(String headline) {
        this.headline = headline;
    }

    public String getHeadline() {
        return headline;
    }
}
