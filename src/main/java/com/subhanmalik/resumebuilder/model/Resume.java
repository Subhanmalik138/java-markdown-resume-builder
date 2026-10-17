package com.subhanmalik.resumebuilder.model;

import java.util.List;

/** The fully parsed resume: contact info plus every content section. */
public record Resume(
        ContactInfo contact,
        String summary,
        List<Experience> experience,
        List<Education> education,
        List<SkillGroup> skills,
        List<Project> projects,
        List<GenericSection> extraSections) {

    public Resume {
        experience = List.copyOf(experience);
        education = List.copyOf(education);
        skills = List.copyOf(skills);
        projects = List.copyOf(projects);
        extraSections = List.copyOf(extraSections);
    }

    /** True when the resume carries at least one renderable content section. */
    public boolean hasContent() {
        return (summary != null && !summary.isBlank())
                || !experience.isEmpty()
                || !education.isEmpty()
                || !skills.isEmpty()
                || !projects.isEmpty()
                || !extraSections.isEmpty();
    }
}
