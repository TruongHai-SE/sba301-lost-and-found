package com.sba301.lostandfound.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for sanitizing and normalizing text inputs across the application.
 * Strips HTML tags, removes dangerous control characters, cleans special symbols,
 * collapses redundant whitespace, and prevents malicious script injections.
 */
public final class StringSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern NON_ALPHANUMERIC_UNICODE_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    private StringSanitizer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sanitizes search and filter query text.
     * Strips HTML, removes non-alphanumeric characters (preserving Unicode letters, numbers, spaces),
     * collapses multi-spaces, and converts to lowercase.
     *
     * Example: "   v^Í!! " -> "ví"
     */
    public static String sanitizeSearchText(String input) {
        if (input == null) {
            return "";
        }
        String clean = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        clean = NON_ALPHANUMERIC_UNICODE_PATTERN.matcher(clean).replaceAll("");
        clean = MULTI_SPACE_PATTERN.matcher(clean).replaceAll(" ");
        return clean.trim().toLowerCase();
    }

    /**
     * Sanitizes short display titles (e.g. Post title, StockImage name, User name, Security question).
     * Strips HTML tags, removes non-alphanumeric symbols, collapses spaces, and trims.
     *
     * Example: "  TRường   HảI  !! " -> "TRường HảI"
     */
    public static String sanitizeTitle(String input) {
        if (input == null) {
            return "";
        }
        String clean = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        clean = NON_ALPHANUMERIC_UNICODE_PATTERN.matcher(clean).replaceAll("");
        clean = MULTI_SPACE_PATTERN.matcher(clean).replaceAll(" ");
        return clean.trim();
    }

    /**
     * Sanitizes long description text (e.g. Post description).
     * Strips dangerous HTML/script tags while preserving line breaks and standard text formatting.
     */
    public static String sanitizeDescription(String input) {
        if (input == null) {
            return "";
        }
        String clean = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        return clean.trim();
    }

    /**
     * Sanitizes a list of tag strings.
     * Sanitizes each tag string, removes empty/blank tags, and deduplicates the list.
     */
    public static List<String> sanitizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(StringSanitizer::sanitizeSearchText)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
