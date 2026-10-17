package com.subhanmalik.resumebuilder.model;

import java.util.List;

/** One work-experience entry: a role at a company with dated bullet achievements. */
public record Experience(
        String title,
        String company,
        String dateRange,
        String location,
        List<String> bullets) {

    public Experience {
        bullets = List.copyOf(bullets);
    }

    public boolean hasCompany() {
        return company != null && !company.isBlank();
    }

    public boolean hasDates() {
        return dateRange != null && !dateRange.isBlank();
    }
}
