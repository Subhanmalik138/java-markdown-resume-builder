package com.subhanmalik.resumebuilder.model;

/** One project entry with an optional link and a short description. */
public record Project(String name, String link, String description) {

    public boolean hasLink() {
        return link != null && !link.isBlank();
    }
}
