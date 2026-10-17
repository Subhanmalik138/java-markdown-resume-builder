package com.subhanmalik.resumebuilder.parser;

import com.subhanmalik.resumebuilder.model.ContactInfo;
import com.subhanmalik.resumebuilder.model.Education;
import com.subhanmalik.resumebuilder.model.Experience;
import com.subhanmalik.resumebuilder.model.GenericSection;
import com.subhanmalik.resumebuilder.model.Project;
import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.model.SkillGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses a resume written in a small markdown dialect into a {@link Resume}.
 *
 * <pre>
 * ---
 * name: Jane Doe
 * email: jane@example.com
 * ---
 *
 * ## Summary
 * Backend engineer who ...
 *
 * ## Experience
 * ### Senior Backend Engineer — Acme Corp
 * *Jan 2023 – Present · Lahore*
 * - Shipped ...
 * </pre>
 */
public final class MarkdownResumeParser {

    private static final Set<String> SUMMARY_ALIASES = Set.of("summary", "profile", "objective");
    private static final Set<String> EXPERIENCE_ALIASES = Set.of("experience", "work experience", "employment", "employment history");
    private static final Set<String> EDUCATION_ALIASES = Set.of("education");
    private static final Set<String> SKILLS_ALIASES = Set.of("skills", "technical skills");
    private static final Set<String> PROJECTS_ALIASES = Set.of("projects", "side projects", "selected projects");

    /** Separators tried, in order, to split an entry heading into role/school and org. */
    private static final String[] TITLE_SEPARATORS = {" — ", " – ", " - ", " @ ", " | ", " at ", " / "};

    /** Separators tried, in order, to split a date line into dates and location. */
    private static final String[] DATE_LOCATION_SEPARATORS = {" · ", " • ", " | "};

    public Resume parse(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return parseLines(lines);
    }

    Resume parseLines(List<String> lines) {
        List<String> body = new ArrayList<>(lines);
        Map<String, String> frontMatter = Map.of();
        if (!body.isEmpty() && body.get(0).trim().equals("---")) {
            int end = -1;
            for (int i = 1; i < body.size(); i++) {
                if (body.get(i).trim().equals("---")) {
                    end = i;
                    break;
                }
            }
            if (end > 0) {
                frontMatter = new FrontMatterParser().parse(body.subList(1, end));
                body = new ArrayList<>(body.subList(end + 1, body.size()));
            }
        }

        ContactInfo contact = ContactInfo.fromFrontMatter(frontMatter);
        Map<String, List<String>> sections = splitSections(body);

        String summary = frontMatter.getOrDefault("summary", "");
        List<Experience> experience = new ArrayList<>();
        List<Education> education = new ArrayList<>();
        List<SkillGroup> skills = new ArrayList<>();
        List<Project> projects = new ArrayList<>();
        List<GenericSection> extras = new ArrayList<>();

        for (Map.Entry<String, List<String>> section : sections.entrySet()) {
            String name = section.getKey();
            List<String> sectionLines = section.getValue();
            if (SUMMARY_ALIASES.contains(name)) {
                if (summary.isBlank()) {
                    summary = joinParagraphs(sectionLines);
                }
            } else if (EXPERIENCE_ALIASES.contains(name)) {
                for (Entry entry : splitEntries(sectionLines)) {
                    experience.add(parseExperience(entry));
                }
            } else if (EDUCATION_ALIASES.contains(name)) {
                for (Entry entry : splitEntries(sectionLines)) {
                    education.add(parseEducation(entry));
                }
            } else if (SKILLS_ALIASES.contains(name)) {
                skills.addAll(parseSkills(sectionLines));
            } else if (PROJECTS_ALIASES.contains(name)) {
                for (Entry entry : splitEntries(sectionLines)) {
                    projects.add(parseProject(entry));
                }
            } else if (!name.isEmpty()) {
                extras.add(new GenericSection(capitalize(name), sectionLines));
            }
        }

        return new Resume(contact, summary.trim(), experience, education, skills, projects, extras);
    }

    // ---- section splitting -------------------------------------------------

    private Map<String, List<String>> splitSections(List<String> lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String current = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ") && !trimmed.startsWith("###")) {
                current = trimmed.substring(3).trim().toLowerCase(Locale.ROOT);
                sections.computeIfAbsent(current, k -> new ArrayList<>());
            } else if (current != null) {
                sections.get(current).add(line);
            }
        }
        return sections;
    }

    /** One `###` entry: its heading plus the body lines beneath it. */
    private record Entry(String heading, List<String> lines) {}

    private List<Entry> splitEntries(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        String heading = null;
        List<String> entryLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("### ")) {
                if (heading != null) {
                    entries.add(new Entry(heading, List.copyOf(entryLines)));
                    entryLines.clear();
                }
                heading = trimmed.substring(4).trim();
            } else if (heading != null) {
                entryLines.add(line);
            }
        }
        if (heading != null) {
            entries.add(new Entry(heading, List.copyOf(entryLines)));
        }
        return entries;
    }

    // ---- entry parsing -----------------------------------------------------

    private Experience parseExperience(Entry entry) {
        String[] title = splitTitle(entry.heading());
        String dateRange = "";
        String location = "";
        List<String> bullets = new ArrayList<>();
        for (String line : entry.lines()) {
            String trimmed = line.trim();
            if (isDateLine(trimmed) && dateRange.isEmpty()) {
                String[] parts = splitDateLocation(trimmed.substring(1, trimmed.length() - 1).trim());
                dateRange = parts[0];
                location = parts[1];
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                bullets.add(trimmed.substring(2).trim());
            }
        }
        return new Experience(title[0], title[1], dateRange, location, bullets);
    }

    private Education parseEducation(Entry entry) {
        String[] title = splitTitle(entry.heading());
        String dateRange = "";
        List<String> details = new ArrayList<>();
        for (String line : entry.lines()) {
            String trimmed = line.trim();
            if (isDateLine(trimmed) && dateRange.isEmpty()) {
                dateRange = trimmed.substring(1, trimmed.length() - 1).trim();
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                details.add(trimmed.substring(2).trim());
            } else if (!trimmed.isEmpty()) {
                details.add(trimmed);
            }
        }
        return new Education(title[0], title[1], dateRange, details);
    }

    private List<SkillGroup> parseSkills(List<String> lines) {
        List<SkillGroup> groups = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!(trimmed.startsWith("- ") || trimmed.startsWith("* "))) {
                continue;
            }
            String content = trimmed.substring(2).trim();
            int colon = content.indexOf(':');
            if (colon <= 0) {
                groups.add(new SkillGroup("", splitItems(content)));
            } else {
                groups.add(new SkillGroup(
                        content.substring(0, colon).trim(),
                        splitItems(content.substring(colon + 1))));
            }
        }
        return groups;
    }

    private Project parseProject(Entry entry) {
        String link = "";
        List<String> description = new ArrayList<>();
        for (String line : entry.lines()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (link.isEmpty() && looksLikeUrl(trimmed)) {
                link = stripLinkMarkup(trimmed);
            } else {
                description.add(trimmed);
            }
        }
        return new Project(entry.heading(), link, String.join(" ", description));
    }

    // ---- small helpers -----------------------------------------------------

    private String[] splitTitle(String heading) {
        for (String sep : TITLE_SEPARATORS) {
            int i = heading.indexOf(sep);
            if (i > 0) {
                return new String[]{heading.substring(0, i).trim(), heading.substring(i + sep.length()).trim()};
            }
        }
        return new String[]{heading.trim(), ""};
    }

    private boolean isDateLine(String trimmed) {
        return trimmed.startsWith("*") && trimmed.endsWith("*") && trimmed.length() > 2
                && !trimmed.startsWith("* ");
    }

    private String[] splitDateLocation(String inner) {
        for (String sep : DATE_LOCATION_SEPARATORS) {
            int i = inner.indexOf(sep);
            if (i > 0) {
                return new String[]{inner.substring(0, i).trim(), inner.substring(i + sep.length()).trim()};
            }
        }
        return new String[]{inner.trim(), ""};
    }

    private List<String> splitItems(String text) {
        List<String> items = new ArrayList<>();
        for (String item : text.split("[,;]")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private boolean looksLikeUrl(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("www.") || lower.matches("[\\w-]+(\\.[\\w-]+)+(/\\S*)?");
    }

    private String stripLinkMarkup(String text) {
        // support [label](url) as well as bare urls
        int open = text.indexOf("](");
        int close = text.lastIndexOf(')');
        if (text.startsWith("[") && open > 0 && close > open) {
            return text.substring(open + 2, close).trim();
        }
        return text;
    }

    private String joinParagraphs(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                sb.append("\n\n");
            } else {
                if (sb.length() > 0 && !sb.toString().endsWith("\n\n") && !sb.toString().endsWith("\n")) {
                    sb.append(' ');
                }
                sb.append(line.trim());
            }
        }
        return sb.toString().replaceAll("\n{3,}", "\n\n").trim();
    }

    private String capitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
