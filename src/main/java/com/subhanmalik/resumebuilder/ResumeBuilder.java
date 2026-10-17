package com.subhanmalik.resumebuilder;

import com.subhanmalik.resumebuilder.model.Resume;
import com.subhanmalik.resumebuilder.parser.MarkdownResumeParser;
import com.subhanmalik.resumebuilder.parser.ResumeValidator;
import com.subhanmalik.resumebuilder.parser.ValidationResult;
import com.subhanmalik.resumebuilder.render.HtmlRenderer;
import com.subhanmalik.resumebuilder.render.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI entry point: turns a markdown resume into a self-contained HTML page.
 *
 * <p>Exit codes: 0 = success, 1 = validation errors, 2 = usage or I/O error.
 */
public final class ResumeBuilder {

    private ResumeBuilder() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Runs the CLI; package-visible so tests can assert on exit codes. */
    static int run(String[] args) {
        final Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.err.println();
            printUsage();
            return 2;
        }
        if (options.help()) {
            printUsage();
            return 0;
        }

        final Resume resume;
        try {
            resume = new MarkdownResumeParser().parse(options.input());
        } catch (IOException e) {
            System.err.println("error: cannot read " + options.input() + ": " + e.getMessage());
            return 2;
        }

        ValidationResult validation = new ResumeValidator().validate(resume);
        validation.warnings().forEach(w -> System.err.println("warning: " + w));
        if (validation.hasErrors()) {
            validation.errors().forEach(e -> System.err.println("error: " + e));
            return 1;
        }
        if (options.validateOnly()) {
            System.out.println("resume is valid (" + validation.warnings().size() + " warning(s))");
            return 0;
        }

        String html = new HtmlRenderer(options.theme()).render(resume);
        try {
            Files.writeString(options.output(), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("error: cannot write " + options.output() + ": " + e.getMessage());
            return 2;
        }
        System.out.println("wrote " + options.output());
        return 0;
    }

    private static void printUsage() {
        System.out.println("""
                Usage: resume-builder <resume.md> [options]

                Turns a Markdown resume into a self-contained HTML page.

                Options:
                  -o, --output <file>   output HTML file (default: <input>.html)
                  -t, --theme <name>    modern | minimal (default: modern)
                      --validate-only   check the resume and exit without rendering
                  -h, --help            show this help
                """);
    }

    /** Parsed command-line options. */
    record Options(Path input, Path output, Theme theme, boolean validateOnly, boolean help) {

        static Options parse(String[] args) {
            List<String> positionals = new ArrayList<>();
            Path output = null;
            Theme theme = Theme.MODERN;
            boolean validateOnly = false;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-h", "--help" -> help = true;
                    case "--validate-only" -> validateOnly = true;
                    case "-o", "--output" -> {
                        if (++i >= args.length) {
                            throw new IllegalArgumentException("--output needs a file argument");
                        }
                        output = Path.of(args[i]);
                    }
                    case "-t", "--theme" -> {
                        if (++i >= args.length) {
                            throw new IllegalArgumentException("--theme needs a name argument");
                        }
                        theme = Theme.fromName(args[i]);
                    }
                    default -> {
                        if (args[i].startsWith("-")) {
                            throw new IllegalArgumentException("unknown option: " + args[i]);
                        }
                        positionals.add(args[i]);
                    }
                }
            }
            if (help) {
                return new Options(null, null, theme, validateOnly, true);
            }
            if (positionals.size() != 1) {
                throw new IllegalArgumentException("expected exactly one input file");
            }
            Path input = Path.of(positionals.get(0));
            if (output == null) {
                String name = input.getFileName().toString();
                int dot = name.lastIndexOf('.');
                output = input.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".html");
            }
            return new Options(input, output, theme, validateOnly, false);
        }
    }
}
