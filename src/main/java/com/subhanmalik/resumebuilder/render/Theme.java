package com.subhanmalik.resumebuilder.render;

/** The visual themes the HTML renderer supports. */
public enum Theme {
    MODERN("modern"),
    MINIMAL("minimal");

    private final String name;

    Theme(String name) {
        this.name = name;
    }

    public String cssClass() {
        return "theme-" + name;
    }

    /** Resolves a theme name from the CLI; throws IllegalArgumentException on unknown names. */
    public static Theme fromName(String name) {
        for (Theme theme : values()) {
            if (theme.name.equalsIgnoreCase(name.trim())) {
                return theme;
            }
        }
        throw new IllegalArgumentException("unknown theme '" + name + "' — expected 'modern' or 'minimal'");
    }
}
