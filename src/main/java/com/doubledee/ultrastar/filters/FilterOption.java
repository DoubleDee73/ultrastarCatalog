package com.doubledee.ultrastar.filters;

import java.util.List;

/**
 * One tile of a filter selection screen (Language, Decade, Tags, Playlists).
 *
 * @param value  value sent in the URL (language code, "80s", English tag name, playlist file name)
 * @param label  text shown on the tile (translated)
 * @param count  number of songs of the whole library matching this choice
 * @param kind   how the tile picture is drawn: "flag", "decade", "tag" or "playlist"
 * @param visual flag country code (e.g. "fr"), decade css class, or tag image URL; may be null
 * @param covers song ids whose covers make the playlist mosaic (up to 4), empty otherwise
 */
public record FilterOption(String value, String label, long count, String kind, String visual, List<String> covers) {
}
