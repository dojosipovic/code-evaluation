package com.codeevaluation.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FileUtil {

    private FileUtil() {}

    public static String toBase64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
