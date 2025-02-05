package com.doubledee.ultrastar.utils;

import com.fasterxml.jackson.core.util.VersionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionUtils {
    private static String version;

    static {
        try (InputStream input = VersionUtil.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties properties = new Properties();
            properties.load(input);
            version = properties.getProperty("version");
        } catch (IOException ex) {
            ex.printStackTrace();
            version = "unknown";
        }
    }

    public static String getVersion() {
        return version;
    }
}
