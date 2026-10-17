package com.subhanmalik.resumebuilder.parser;

import java.util.List;

/**
 * The outcome of validating a resume. Errors block rendering; warnings are printed
 * but don't stop the build.
 */
public record ValidationResult(List<String> errors, List<String> warnings) {

    public ValidationResult {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isClean() {
        return errors.isEmpty() && warnings.isEmpty();
    }
}
