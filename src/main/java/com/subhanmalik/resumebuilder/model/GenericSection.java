package com.subhanmalik.resumebuilder.model;

import java.util.List;

/**
 * A section the parser doesn't have a dedicated model for (e.g. "Certifications",
 * "Languages"). Its raw markdown lines are preserved so the renderer can show them
 * instead of silently dropping content.
 */
public record GenericSection(String title, List<String> lines) {

    public GenericSection {
        lines = List.copyOf(lines);
    }
}
