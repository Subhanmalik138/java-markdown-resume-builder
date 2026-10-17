package com.subhanmalik.resumebuilder;

import com.subhanmalik.resumebuilder.model.ContactInfo;
import com.subhanmalik.resumebuilder.model.Experience;
import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.model.SkillGroup;
import com.subhanmalik.resumebuilder.render.HtmlRenderer;
import com.subhanmalik.resumebuilder.render.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HtmlRendererTest {

    private Resume sampleResume() {
        ContactInfo contact = ContactInfo.fromFrontMatter(Map.of(
                "name", "Jane Doe",
                "email", "jane@example.com",
                "github", "github.com/janedoe"));
        Experience experience = new Experience(
                "Senior Backend Engineer", "Acme Corp",
                "Jan 2023 – Present", "Berlin",
                List.of("Cut p99 latency by **40%**.", "See [the writeup](https://example.com/post)."));
        SkillGroup skills = new SkillGroup("Languages", List.of("Java", "Python"));
        return new Resume(contact, "Backend engineer.", List.of(experience),
                List.of(), List.of(skills), List.of(), List.of());
    }

    @Test
    void rendersNameEmailAndSections() {
        String html = new HtmlRenderer(Theme.MODERN).render(sampleResume());
        assertTrue(html.contains("<h1>Jane Doe</h1>"));
        assertTrue(html.contains("mailto:jane@example.com"));
        assertTrue(html.contains("<h2>Experience</h2>"));
        assertTrue(html.contains("<h2>Skills</h2>"));
        assertTrue(html.contains("Senior Backend Engineer"));
    }

    @Test
    void appliesInlineMarkdown() {
        String html = new HtmlRenderer(Theme.MODERN).render(sampleResume());
        assertTrue(html.contains("<strong>40%</strong>"));
        assertTrue(html.contains("<a href=\"https://example.com/post\">the writeup</a>"));
    }

    @Test
    void escapesUserSuppliedHtml() {
        ContactInfo contact = ContactInfo.fromFrontMatter(
                Map.of("name", "<script>alert(1)</script>", "email", "j@e.com"));
        Resume resume = new Resume(contact, "", List.of(), List.of(), List.of(), List.of(), List.of());
        String html = new HtmlRenderer(Theme.MODERN).render(resume);
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void minimalThemeUsesDifferentStyling() {
        String modern = new HtmlRenderer(Theme.MODERN).render(sampleResume());
        String minimal = new HtmlRenderer(Theme.MINIMAL).render(sampleResume());
        assertTrue(modern.contains("theme-modern"));
        assertTrue(minimal.contains("theme-minimal"));
        assertNotEquals(modern, minimal);
    }

    @Test
    void outputIsASingleSelfContainedDocument() {
        String html = new HtmlRenderer(Theme.MODERN).render(sampleResume());
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<style>"));
        assertFalse(html.contains("src=\"http")); // no external assets
    }
}
