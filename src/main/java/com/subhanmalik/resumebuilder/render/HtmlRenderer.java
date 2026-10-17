package com.subhanmalik.resumebuilder.render;

import com.subhanmalik.resumebuilder.model.ContactInfo;
import com.subhanmalik.resumebuilder.model.Education;
import com.subhanmalik.resumebuilder.model.Experience;
import com.subhanmalik.resumebuilder.model.GenericSection;
import com.subhanmalik.resumebuilder.model.Project;
import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.model.SkillGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a {@link Resume} as one self-contained HTML file (CSS embedded, no external
 * assets). Semantic tags only — header/section/h1-h3/ul — so applicant-tracking
 * systems can still parse the output.
 */
public final class HtmlRenderer {

    private static final Pattern CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)");

    private final Theme theme;

    public HtmlRenderer(Theme theme) {
        this.theme = theme;
    }

    public String render(Resume resume) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(displayName(resume))).append(" — Resume</title>\n")
                .append("<style>\n").append(css()).append("</style>\n")
                .append("</head>\n<body class=\"").append(theme.cssClass()).append("\">\n")
                .append("<div class=\"page\">\n");

        html.append(renderHeader(resume.contact()));

        if (!isBlank(resume.summary())) {
            html.append("<section class=\"summary\">\n<h2>Summary</h2>\n")
                    .append(renderBlocks(List.of(resume.summary().split("\n"))))
                    .append("</section>\n");
        }

        if (!resume.experience().isEmpty()) {
            html.append("<section class=\"experience\">\n<h2>Experience</h2>\n");
            for (Experience e : resume.experience()) {
                html.append("<article class=\"entry\">\n")
                        .append("<h3>").append(inline(e.title()));
                if (e.hasCompany()) {
                    html.append(" <span class=\"org\">— ").append(inline(e.company())).append("</span>");
                }
                html.append("</h3>\n");
                if (e.hasDates() || !isBlank(e.location())) {
                    html.append("<p class=\"meta\">").append(escape(e.dateRange()));
                    if (!isBlank(e.location())) {
                        html.append(" · ").append(escape(e.location()));
                    }
                    html.append("</p>\n");
                }
                if (!e.bullets().isEmpty()) {
                    html.append("<ul>\n");
                    for (String bullet : e.bullets()) {
                        html.append("<li>").append(inline(bullet)).append("</li>\n");
                    }
                    html.append("</ul>\n");
                }
                html.append("</article>\n");
            }
            html.append("</section>\n");
        }

        if (!resume.education().isEmpty()) {
            html.append("<section class=\"education\">\n<h2>Education</h2>\n");
            for (Education e : resume.education()) {
                html.append("<article class=\"entry\">\n")
                        .append("<h3>").append(inline(e.degree()));
                if (!isBlank(e.school())) {
                    html.append(" <span class=\"org\">— ").append(inline(e.school())).append("</span>");
                }
                html.append("</h3>\n");
                if (e.hasDates()) {
                    html.append("<p class=\"meta\">").append(escape(e.dateRange())).append("</p>\n");
                }
                if (!e.details().isEmpty()) {
                    html.append("<ul>\n");
                    for (String detail : e.details()) {
                        html.append("<li>").append(inline(detail)).append("</li>\n");
                    }
                    html.append("</ul>\n");
                }
                html.append("</article>\n");
            }
            html.append("</section>\n");
        }

        if (!resume.skills().isEmpty()) {
            html.append("<section class=\"skills\">\n<h2>Skills</h2>\n<ul class=\"skill-groups\">\n");
            for (SkillGroup group : resume.skills()) {
                html.append("<li>");
                if (!isBlank(group.category())) {
                    html.append("<strong>").append(escape(group.category())).append(":</strong> ");
                }
                html.append(escape(String.join(", ", group.items())));
                html.append("</li>\n");
            }
            html.append("</ul>\n</section>\n");
        }

        if (!resume.projects().isEmpty()) {
            html.append("<section class=\"projects\">\n<h2>Projects</h2>\n");
            for (Project p : resume.projects()) {
                html.append("<article class=\"entry\">\n<h3>");
                if (p.hasLink()) {
                    html.append("<a href=\"").append(escape(p.link())).append("\">")
                            .append(inline(p.name())).append("</a>");
                } else {
                    html.append(inline(p.name()));
                }
                html.append("</h3>\n");
                if (!isBlank(p.description())) {
                    html.append("<p>").append(inline(p.description())).append("</p>\n");
                }
                html.append("</article>\n");
            }
            html.append("</section>\n");
        }

        for (GenericSection section : resume.extraSections()) {
            html.append("<section class=\"extra\">\n<h2>").append(escape(section.title())).append("</h2>\n")
                    .append(renderBlocks(section.lines()))
                    .append("</section>\n");
        }

        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    // ---- header ------------------------------------------------------------

    private String renderHeader(ContactInfo contact) {
        StringBuilder header = new StringBuilder();
        header.append("<header>\n<h1>").append(escape(displayName(contact))).append("</h1>\n");
        if (!isBlank(contact.headline())) {
            header.append("<p class=\"headline\">").append(escape(contact.headline())).append("</p>\n");
        }
        List<String> channels = new ArrayList<>();
        if (!isBlank(contact.email())) {
            channels.add("<a href=\"mailto:" + escape(contact.email().trim()) + "\">"
                    + escape(contact.email().trim()) + "</a>");
        }
        if (!isBlank(contact.phone())) {
            channels.add(escape(contact.phone().trim()));
        }
        if (!isBlank(contact.location())) {
            channels.add(escape(contact.location().trim()));
        }
        if (!isBlank(contact.linkedin())) {
            channels.add(link(contact.linkedin().trim(), "LinkedIn"));
        }
        if (!isBlank(contact.github())) {
            channels.add(link(contact.github().trim(), "GitHub"));
        }
        if (!isBlank(contact.website())) {
            channels.add(link(contact.website().trim(), "Website"));
        }
        if (!channels.isEmpty()) {
            header.append("<p class=\"contact\">").append(String.join(" · ", channels)).append("</p>\n");
        }
        header.append("</header>\n");
        return header.toString();
    }

    private String link(String raw, String label) {
        String href = raw.startsWith("http") ? raw : "https://" + raw;
        return "<a href=\"" + escape(href) + "\">" + escape(label) + "</a>";
    }

    private String displayName(Resume resume) {
        return displayName(resume.contact());
    }

    private String displayName(ContactInfo contact) {
        return isBlank(contact.name()) ? "Resume" : contact.name().trim();
    }

    // ---- markdown ----------------------------------------------------------

    /** Renders raw markdown lines into paragraphs and bullet lists. */
    String renderBlocks(List<String> lines) {
        StringBuilder out = new StringBuilder();
        List<String> paragraph = new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph);
                flushList(out, list);
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                flushParagraph(out, paragraph);
                list.add(trimmed.substring(2).trim());
            } else {
                flushList(out, list);
                paragraph.add(trimmed);
            }
        }
        flushParagraph(out, paragraph);
        flushList(out, list);
        return out.toString();
    }

    private void flushParagraph(StringBuilder out, List<String> paragraph) {
        if (!paragraph.isEmpty()) {
            out.append("<p>").append(inline(String.join(" ", paragraph))).append("</p>\n");
            paragraph.clear();
        }
    }

    private void flushList(StringBuilder out, List<String> list) {
        if (!list.isEmpty()) {
            out.append("<ul>\n");
            for (String item : list) {
                out.append("<li>").append(inline(item)).append("</li>\n");
            }
            out.append("</ul>\n");
            list.clear();
        }
    }

    /** Escapes HTML, then applies inline markdown: code, links, bold, italic. */
    String inline(String text) {
        String escaped = escape(text);
        escaped = replace(CODE, escaped, m -> "<code>" + m.group(1) + "</code>");
        escaped = replace(LINK, escaped, m ->
                "<a href=\"" + m.group(2) + "\">" + m.group(1) + "</a>");
        escaped = replace(BOLD, escaped, m -> "<strong>" + m.group(1) + "</strong>");
        escaped = replace(ITALIC, escaped, m -> "<em>" + m.group(1) + "</em>");
        return escaped;
    }

    private String replace(Pattern pattern, String input, java.util.function.Function<Matcher, String> fn) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---- themes ------------------------------------------------------------

    private String css() {
        return theme == Theme.MINIMAL ? minimalCss() : modernCss();
    }

    private String modernCss() {
        return """
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                       color: #1f2937; background: #f3f4f6; line-height: 1.6; }
                .page { max-width: 820px; margin: 2rem auto; background: #fff;
                        padding: 3rem 3.5rem; border-radius: 8px;
                        box-shadow: 0 1px 3px rgba(0,0,0,.08); }
                header { border-bottom: 3px solid #1d4ed8; padding-bottom: 1rem; margin-bottom: 1.5rem; }
                h1 { font-size: 2rem; color: #111827; }
                .headline { color: #1d4ed8; font-weight: 600; margin-top: .25rem; }
                .contact { color: #4b5563; font-size: .9rem; margin-top: .5rem; }
                .contact a { color: #1d4ed8; }
                h2 { font-size: 1.15rem; text-transform: uppercase; letter-spacing: .06em;
                     color: #1d4ed8; border-bottom: 1px solid #e5e7eb;
                     padding-bottom: .25rem; margin: 1.75rem 0 .75rem; }
                h3 { font-size: 1.05rem; color: #111827; }
                .org { font-weight: 400; color: #4b5563; }
                .meta { color: #6b7280; font-size: .9rem; font-style: italic; }
                .entry { margin-bottom: 1.25rem; }
                ul { margin: .4rem 0 .4rem 1.25rem; }
                li { margin-bottom: .25rem; }
                .skill-groups { list-style: none; margin-left: 0; }
                a { color: #1d4ed8; text-decoration: none; }
                a:hover { text-decoration: underline; }
                code { background: #f3f4f6; padding: .1em .3em; border-radius: 3px; font-size: .9em; }
                @media print { body { background: #fff; } .page { box-shadow: none; margin: 0; } }
                """;
    }

    private String minimalCss() {
        return """
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: Georgia, "Times New Roman", serif; color: #222;
                       background: #fff; line-height: 1.55; }
                .page { max-width: 720px; margin: 0 auto; padding: 2.5rem 2rem; }
                header { text-align: center; margin-bottom: 1.5rem; }
                h1 { font-size: 1.8rem; letter-spacing: .02em; }
                .headline { font-style: italic; color: #555; margin-top: .25rem; }
                .contact { font-size: .88rem; color: #555; margin-top: .5rem; }
                h2 { font-size: 1.05rem; text-transform: uppercase; letter-spacing: .1em;
                     border-top: 1px solid #999; border-bottom: 1px solid #999;
                     padding: .3rem 0; margin: 1.5rem 0 .75rem; }
                h3 { font-size: 1rem; }
                .org { font-weight: 400; }
                .meta { font-size: .88rem; font-style: italic; color: #555; }
                .entry { margin-bottom: 1rem; }
                ul { margin: .35rem 0 .35rem 1.25rem; }
                li { margin-bottom: .2rem; }
                .skill-groups { list-style: none; margin-left: 0; }
                a { color: #222; }
                """;
    }
}
