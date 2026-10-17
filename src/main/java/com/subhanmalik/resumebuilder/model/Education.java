package com.subhanmalik.resumebuilder.model;

import java.util.List;

/** One education entry: a degree or certification with optional detail lines. */
public record Education(
        String degree,
        String school,
        String dateRange,
        List<String> details) {

    public Education {
        details = List.copyOf(details);
    }

    public boolean hasDates() {
        return dateRange != null && !dateRange.isBlank();
    }
}
