package com.subhanmalik.resumebuilder.model;

import java.util.List;

/** A named bucket of skills, e.g. "Languages" -> [Java, Python, Go]. */
public record SkillGroup(String category, List<String> items) {

    public SkillGroup {
        items = List.copyOf(items);
    }
}
