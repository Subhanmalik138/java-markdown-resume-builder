package com.subhanmalik.resumebuilder;

import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.parser.MarkdownResumeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownResumeParserTest {

    private final MarkdownResumeParser parser = new MarkdownResumeParser();

    @TempDir
    Path tempDir;

    private Resume parse(String markdown) throws IOException {
        Path file = tempDir.resolve("resume.md");
        Files.writeString(file, markdown, StandardCharsets.UTF_8);
        return parser.parse(file);
    }

    @Test
    void parsesFrontMatterIntoContactInfo() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                headline: Backend Engineer
                email: jane@example.com
                phone: +1 555 0100
                location: Berlin, Germany
                github: github.com/janedoe
                ---

                ## Summary
                Backend engineer.
                """);

        assertEquals("Jane Doe", resume.contact().name());
        assertEquals("Backend Engineer", resume.contact().headline());
        assertEquals("jane@example.com", resume.contact().email());
        assertEquals("Berlin, Germany", resume.contact().location());
        assertEquals("github.com/janedoe", resume.contact().github());
        assertEquals("Backend engineer.", resume.summary());
    }

    @Test
    void parsesExperienceEntriesWithDatesAndBullets() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                email: jane@example.com
                ---

                ## Experience
                ### Senior Backend Engineer — Acme Corp
                *Jan 2023 – Present · Berlin*
                - Cut p99 latency by 40%.
                - Led a team of four.

                ### Backend Engineer — Startup XYZ
                *2020 – 2022*
                - Shipped the billing service.
                """);

        assertEquals(2, resume.experience().size());
        var first = resume.experience().get(0);
        assertEquals("Senior Backend Engineer", first.title());
        assertEquals("Acme Corp", first.company());
        assertEquals("Jan 2023 – Present", first.dateRange());
        assertEquals("Berlin", first.location());
        assertEquals(List.of("Cut p99 latency by 40%.", "Led a team of four."), first.bullets());

        var second = resume.experience().get(1);
        assertEquals("Backend Engineer", second.title());
        assertEquals("Startup XYZ", second.company());
        assertEquals("2020 – 2022", second.dateRange());
    }

    @Test
    void parsesSkillsIntoCategories() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                email: jane@example.com
                ---

                ## Skills
                - Languages: Java, Python, Go
                - Tools: Docker, Kubernetes
                """);

        assertEquals(2, resume.skills().size());
        assertEquals("Languages", resume.skills().get(0).category());
        assertEquals(List.of("Java", "Python", "Go"), resume.skills().get(0).items());
        assertEquals("Tools", resume.skills().get(1).category());
    }

    @Test
    void parsesProjectsWithLinks() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                email: jane@example.com
                ---

                ## Projects
                ### Rate Limiter
                https://github.com/janedoe/rate-limiter
                A token-bucket rate limiter with per-key quotas.
                """);

        assertEquals(1, resume.projects().size());
        assertEquals("Rate Limiter", resume.projects().get(0).name());
        assertEquals("https://github.com/janedoe/rate-limiter", resume.projects().get(0).link());
        assertTrue(resume.projects().get(0).description().contains("token-bucket"));
    }

    @Test
    void parsesEducationAndUnknownSections() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                email: jane@example.com
                ---

                ## Education
                ### BSc Computer Science — TU Berlin
                *2016 – 2020*
                - Graduated with honors.

                ## Certifications
                - AWS Solutions Architect Associate
                """);

        assertEquals(1, resume.education().size());
        assertEquals("BSc Computer Science", resume.education().get(0).degree());
        assertEquals("TU Berlin", resume.education().get(0).school());

        assertEquals(1, resume.extraSections().size());
        assertEquals("Certifications", resume.extraSections().get(0).title());
    }

    @Test
    void worksWithoutFrontMatter() throws IOException {
        Resume resume = parse("""
                ## Summary
                Self-taught developer.
                """);

        assertEquals("", resume.contact().name());
        assertEquals("Self-taught developer.", resume.summary());
    }

    @Test
    void headingWithoutSeparatorKeepsWholeTitle() throws IOException {
        Resume resume = parse("""
                ---
                name: Jane Doe
                email: jane@example.com
                ---

                ## Experience
                ### Freelance Developer
                *2021 – Present*
                - Built client sites.
                """);

        assertEquals("Freelance Developer", resume.experience().get(0).title());
        assertEquals("", resume.experience().get(0).company());
    }
}
