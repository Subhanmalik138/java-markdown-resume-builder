package com.subhanmalik.resumebuilder.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the YAML front matter block of a resume markdown file.
 *
 * <p>Deliberately a small hand-rolled subset (flat {@code key: value} pairs) rather
 * than a YAML dependency: resumes only need scalar contact fields, and keeping the
 * tool dependency-free makes the distributed jar trivially small.
 */
public final class FrontMatterParser {

    /** Parses front-matter lines into key/value pairs; keys are lower-cased. */
    public Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue; // not a key/value line — skip rather than fail
            }
            String key = trimmed.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
            String value = unquote(trimmed.substring(colon + 1).trim());
            values.put(key, value);
        }
        return values;
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
