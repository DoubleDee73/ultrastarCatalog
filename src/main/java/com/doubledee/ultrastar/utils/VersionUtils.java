package com.doubledee.ultrastar.utils;

import com.fasterxml.jackson.core.util.VersionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionUtils {
    private static String version;

    public static String getVersionFromManifest() {
        try (InputStream inputStream = VersionUtil.class.getClassLoader()
                                                        .getResourceAsStream("META-INF/MANIFEST.MF")) {
            if (inputStream == null) return "unknown";

            Manifest manifest = new Manifest(inputStream);
            Attributes attr = manifest.getMainAttributes();
            return attr.getValue("Implementation-Version");
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String getVersion() {
        if (version == null) {
            version = getVersionFromManifest();
        }
        return version;
    }
}
