package com.doubledee.ultrastar.utils;

public class Normalizer {
    public static String normalize(String text) {
        String normalized;
        if (java.text.Normalizer.isNormalized(text, java.text.Normalizer.Form.NFKD)) {
            normalized = text.toLowerCase();
        } else {
            normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD).toLowerCase();
        }
        return normalized.replaceAll("\\p{M}", "");
    }
}
