package com.codeevaluation.core.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtil {

    private static final Pattern SIMPLE_DURATION_PATTERN =
            Pattern.compile("^(\\d+)\\s*(ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);

    private DurationUtil() {}

    public static Duration parseFlexibleDuration(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }

        Matcher matcher = SIMPLE_DURATION_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            return switch (unit) {
                case "ms" -> Duration.ofMillis(amount);
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> throw new IllegalArgumentException(
                        "Unsupported duration unit: " + unit);
            };
        }

        try {
            return Duration.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid value: " + value
                            + ". Use values like 5m, 30s, 1h or ISO-8601 duration.",
                    e
            );
        }
    }
}
