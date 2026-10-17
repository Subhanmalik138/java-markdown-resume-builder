# Markdown Resume Builder

Write your resume in Markdown, get a polished, ATS-friendly HTML page. One command,
zero dependencies, no account, no web service.

```bash
java -jar markdown-resume-builder-1.0.0.jar resume.md -o resume.html
```

## The problem

Resumes live in two bad places: Word documents that look different on every machine,
and web builders that lock your data behind an account. Markdown is the obvious
middle ground — plain text, version-controllable, diffable — but raw Markdown
renders as an ugly wall of text. This tool closes that gap: you keep the Markdown
source in git, and generate a styled, single-file HTML resume whenever you need it.

## Resume format

A resume is a Markdown file with YAML front matter for contact details and
`##` sections for content:

```markdown
---
name: Alex Rahman
headline: Backend Engineer · Java & Distributed Systems
email: alex.rahman@example.com
location: Lahore, Pakistan
github: github.com/alexrahman
---

## Summary
Backend engineer with 4 years of experience...

## Experience
### Senior Backend Engineer — Northwind Systems
*Mar 2023 – Present · Lahore*
- Rebuilt the order pipeline on **Kafka**, cutting latency to 180ms p99.
- Mentored 3 junior engineers.

## Skills
- Languages: Java, Python, SQL
- Ops: Docker, Kubernetes, AWS
```

Section names are flexible (`Work Experience`, `Technical Skills`, `Side Projects`
all work), sections the tool doesn't recognize — like `Certifications` — are
rendered as-is instead of being dropped. See [`examples/sample-resume.md`](examples/sample-resume.md)
for a complete resume.

## Usage

```
resume-builder <resume.md> [options]

  -o, --output <file>   output HTML file (default: <input>.html)
  -t, --theme <name>    modern | minimal (default: modern)
      --validate-only   check the resume and exit without rendering
  -h, --help            show this help
```

Exit codes: `0` success · `1` validation errors · `2` usage or I/O error.

Validation catches the mistakes that cost interviews before you send the file:

```bash
$ java -jar markdown-resume-builder-1.0.0.jar resume.md --validate-only
warning: experience 'Backend Engineer' has no date line — add '*Start – End*' under the heading
warning: summary is 742 characters; keep it under 600 so it reads in ~30 seconds
resume is valid (2 warning(s))
```

## Design decisions

1. **Zero runtime dependencies.** The front-matter parser and the Markdown subset
   parser are hand-rolled (a small line-based state machine). A resume tool should
   be a single small jar you can run anywhere, not a dependency tree — and the
   parsing is simple enough that a YAML library would be dead weight.
2. **Validation is separate from parsing.** `ResumeValidator` encodes publishing
   policy (required contact fields, summary length, missing dates) independently
   of structure, which is why `--validate-only` works without touching the renderer.
   Errors block the build; warnings don't.
3. **One self-contained HTML file.** All CSS is embedded and the markup uses
   semantic tags (`header`, `section`, `h1`–`h3`, lists) with no layout tables,
   so the output is portable *and* still parseable by applicant-tracking systems.
4. **Unknown sections are preserved.** A resume builder that silently drops your
   `Certifications` section is worse than useless — anything unrecognized renders
   as a generic section.

## Building

Requires Java 17+ and Maven:

```bash
mvn package        # produces target/markdown-resume-builder-1.0.0.jar
```

## Running tests

```bash
mvn test
```

19 tests across parser, validator, and renderer — section splitting, entry
parsing, validation rules, HTML escaping, and both themes.

## Tech stack

- Java 17 (records, sealed-by-design value types, text blocks)
- JUnit 5 for tests
- No runtime dependencies

## License

MIT — see [LICENSE](LICENSE).
