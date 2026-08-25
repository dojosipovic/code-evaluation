package com.codeevaluation.core.util;

import jakarta.ws.rs.BadRequestException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    private TotpUtil() {
    }

    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public static boolean verify(String secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }

        long counter = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (long candidate = counter - 1; candidate <= counter + 1; candidate++) {
            if (generateCode(secret, candidate).equals(code)) {
                return true;
            }
        }

        return false;
    }

    public static String otpauthUrl(String issuer, String account, String secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(account)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    private static String generateCode(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception e) {
            throw new BadRequestException("Invalid TOTP secret");
        }
    }

    private static String encodeBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer buffer = ByteBuffer.allocate(normalized.length() * 5 / 8);
        int bits = 0;
        int bitsLeft = 0;

        for (char c : normalized.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                throw new BadRequestException("Invalid TOTP secret");
            }
            bits = (bits << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                buffer.put((byte) ((bits >> (bitsLeft - 8)) & 0xff));
                bitsLeft -= 8;
            }
        }

        byte[] decoded = new byte[buffer.position()];
        buffer.flip();
        buffer.get(decoded);
        return decoded;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
