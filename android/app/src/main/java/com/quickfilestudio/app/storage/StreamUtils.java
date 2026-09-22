package com.quickfilestudio.app.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class StreamUtils {

    private StreamUtils() {}

    public static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;

        while ((count = input.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void writeUtf8(OutputStream output, String text)
            throws IOException {

        if (text == null) {
            text = "";
        }

        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static String sha256(InputStream input)
            throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] buffer = new byte[1024 * 64];
        int count;

        while ((count = input.read(buffer)) != -1) {
            digest.update(buffer, 0, count);
        }

        byte[] hash = digest.digest();

        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    public static void closeQuietly(InputStream input) {
        if (input == null) return;

        try {
            input.close();
        } catch (Exception ignored) {
        }
    }

    public static void closeQuietly(OutputStream output) {
        if (output == null) return;

        try {
            output.close();
        } catch (Exception ignored) {
        }
    }
}
