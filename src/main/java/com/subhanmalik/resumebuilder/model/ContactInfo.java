package com.subhanmalik.resumebuilder.model;

import java.util.Locale;
import java.util.Map;

/** Contact details for the resume header. Built from YAML front matter. */
public record ContactInfo(
        String name,
        String headline,
        String email,
        String phone,
        String location,
        String linkedin,
        String github,
        String website) {

    public static ContactInfo fromFrontMatter(Map<String, String> fm) {
        return new ContactInfo(
                fm.getOrDefault("name", ""),
                fm.getOrDefault("headline", fm.getOrDefault("title", "")),
                fm.getOrDefault("email", ""),
                fm.getOrDefault("phone", ""),
                fm.getOrDefault("location", ""),
                fm.getOrDefault("linkedin", ""),
                fm.getOrDefault("github", ""),
                fm.getOrDefault("website", ""));
    }

    /** True when at least one contact channel beyond the name is present. */
    public boolean hasAnyChannel() {
        return !isBlank(email) || !isBlank(phone) || !isBlank(linkedin)
                || !isBlank(github) || !isBlank(website);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Normalizes a front-matter key for lookup. */
    public static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
