package com.yihen.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UrlUtils {

    public static String extractFileExtension(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String filenameFromQuery = extractQueryFilename(url);
        if (filenameFromQuery != null) {
            return extractFileExtensionFromFilename(filenameFromQuery);
        }

        String clean = url;
        int q = clean.indexOf('?');
        if (q >= 0) clean = clean.substring(0, q);
        int f = clean.indexOf('#');
        if (f >= 0) clean = clean.substring(0, f);

        int slash = clean.lastIndexOf('/');
        String filename = (slash >= 0) ? clean.substring(slash + 1) : clean;
        return extractFileExtensionFromFilename(filename);
    }

    private static String extractQueryFilename(String url) {
        int q = url.indexOf('?');
        if (q < 0 || q == url.length() - 1) {
            return null;
        }

        String query = url.substring(q + 1);
        int fragment = query.indexOf('#');
        if (fragment >= 0) {
            query = query.substring(0, fragment);
        }

        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = pair.substring(0, eq);
            if (!"filename".equalsIgnoreCase(key) && !"name".equalsIgnoreCase(key)) {
                continue;
            }
            String value = pair.substring(eq + 1);
            if (value.isBlank()) {
                return null;
            }
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static String extractFileExtensionFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
