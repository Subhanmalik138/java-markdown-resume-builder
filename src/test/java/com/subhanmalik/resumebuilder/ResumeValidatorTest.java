package com.subhanmalik.resumebuilder;

import com.subhanmalik.resumebuilder.model.ContactInfo;
import com.subhanmalik.resumebuilder.model.Experience;
import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.parser.ResumeValidator;
import com.subhanmalik.resumebuilder.parser.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResumeValidatorTest {

    private final ResumeValidator validator = new ResumeValidator();

    private Resume resume(String name, String email, String summary) {
        ContactInfo contact = ContactInfo.fromFrontMatter(Map.of("name", name, "email", email));
        return new Resume(contact, summary, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void validResumePassesClean() {
        ValidationResult result = validator.validate(
                resume("Jane Doe", "jane@example.com", "Backend engineer."));
        assertFalse(result.hasErrors());
    }

    @Test
    void missingNameIsAnError() {
        ValidationResult result = validator.validate(
                resume("", "jane@example.com", "Backend engineer."));
        assertTrue(result.hasErrors());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("name")));
    }

    @Test
    void missingEmailIsAnError() {
        ValidationResult result = validator.validate(
                resume("Jane Doe", "", "Backend engineer."));
        assertTrue(result.hasErrors());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("email")));
    }

    @Test
    void malformedEmailIsAnError() {
        ValidationResult result = validator.validate(
                resume("Jane Doe", "not-an-email", "Backend engineer."));
        assertTrue(result.hasErrors());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("invalid")));
    }

    @Test
    void resumeWithoutContentIsAnError() {
        ValidationResult result = validator.validate(
                resume("Jane Doe", "jane@example.com", ""));
        assertTrue(result.hasErrors());
    }

    @Test
    void longSummaryIsAWarningNotAnError() {
        ValidationResult result = validator.validate(
                resume("Jane Doe", "jane@example.com", "x".repeat(700)));
        assertFalse(result.hasErrors());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("summary")));
    }

    @Test
    void experienceWithoutDatesWarns() {
        ContactInfo contact = ContactInfo.fromFrontMatter(Map.of("name", "Jane", "email", "j@e.com"));
        Experience undated = new Experience("Dev", "Acme", "", "", List.of("Did things."));
        Resume r = new Resume(contact, "", List.of(undated), List.of(), List.of(), List.of(), List.of());
        ValidationResult result = validator.validate(r);
        assertFalse(result.hasErrors());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("date")));
    }
}
