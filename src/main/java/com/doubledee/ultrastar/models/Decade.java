package com.doubledee.ultrastar.models;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Decade {
    ALL("ALL", 1800),
    FIFTIES("50s", 1950),
    SIXTIES("60s", 1960),
    SEVENTIES("70s", 1970),
    EIGHTIES("80s", 1980),
    NINETIES("90s", 1990),
    TWO_THOUSANDS("2000s", 2000),
    TWENTY_TENS("2010s", 2010),
    TWENTY_TWENTIES("2020s", 2020);

    private String displayName;
    private int startYear;

    Decade(String displayName, int startYear) {
        this.displayName = displayName;
        this.startYear = startYear;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
                     .map(Decade::getDisplayName)
                     .collect(Collectors.toList());
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStartYear() {
        return startYear;
    }

    public static Decade getDecadeByString(String decade) {
        return Arrays.stream(values())
                     .filter(value -> value.getDisplayName().equals(decade))
                     .findAny()
                     .orElse(ALL);
    }
}
