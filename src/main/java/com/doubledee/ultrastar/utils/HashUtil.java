package com.doubledee.ultrastar.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class HashUtil {
    public static String md5Hash(String text) {
        byte[] textAsBytes = text.getBytes(StandardCharsets.UTF_8);
        String hash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(textAsBytes);
            hash = bytesToHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            hash = UUID.nameUUIDFromBytes(textAsBytes).toString();
        }
        return hash;
    }
    public static String sha1(String text) {
        byte[] textAsBytes = text.getBytes(StandardCharsets.UTF_8);
        String hash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(textAsBytes);
            hash = bytesToHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            hash = UUID.nameUUIDFromBytes(textAsBytes).toString();
        }
        return hash.substring(0,23);
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
