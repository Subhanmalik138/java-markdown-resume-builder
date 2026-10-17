package com.subhanmalik.resumebuilder.parser;

import com.subhanmalik.resumebuilder.model.Education;
import com.subhanmalik.resumebuilder.model.Experience;
import com.subhanmalik.resumebuilder.model.Resume;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks a parsed resume for problems before rendering.
 *
 * <p>Kept separate from the parser on purpose: validation is a policy decision
 * (what counts as "good enough to publish"), parsing is just structure.
 */
public final class ResumeValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_SUMMARY_CHARS = 600;
    private static final int MAX_BULLET_CHARS = 240;

    public ValidationResult validate(Resume resume) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String name = resume.contact().name();
        if (isBlank(name)) {
            errors.add("contact name is required — set 'name:' in the front matter");
        }
        String email = resume.contact().email();
        if (isBlank(email)) {
            errors.add("contact email is required — set 'email:' in the front matter");
        } else if (!EMAIL.matcher(email.trim()).matches()) {
            errors.add("contact email looks invalid: '" + email.trim() + "'");
        }
        if (!resume.contact().hasAnyChannel()) {
            warnings.add("no contact channels (phone, linkedin, github, website) — recruiters can't reach you");
        }
        if (!resume.hasContent()) {
            errors.add("resume has no content sections — add at least a summary, experience, education, skills or projects section");
        }

        if (resume.summary() != null && resume.summary().length() > MAX_SUMMARY_CHARS) {
            warnings.add("summary is " + resume.summary().length() + " characters; keep it under "
                    + MAX_SUMMARY_CHARS + " so it reads in ~30 seconds");
        }

        for (Experience e : resume.experience()) {
            String label = "'" + e.title() + "'";
            if (!e.hasDates()) {
                warnings.add("experience " + label + " has no date line — add '*Start – End*' under the heading");
            }
            if (e.bullets().isEmpty()) {
                warnings.add("experience " + label + " has no bullet points");
            }
            for (String bullet : e.bullets()) {
                if (bullet.length() > MAX_BULLET_CHARS) {
                    warnings.add("experience " + label + " has a bullet over " + MAX_BULLET_CHARS
                            + " characters — split it for readability");
                    break;
                }
            }
        }

        for (Education e : resume.education()) {
            if (!e.hasDates()) {
                warnings.add("education '" + e.degree() + "' has no date line");
            }
        }

        return new ValidationResult(errors, warnings);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
