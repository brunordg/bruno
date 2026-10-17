# Design

## Context

All example values come from `BrunoWriter`. `dynamicFakeForField` handles enums, `@Email` and `@Min`/`@Max`, then falls through to `fakeValue(name, type)`, a chain of substring checks. `fakeValue` is also used directly for query, path, header and cookie params and for non-file multipart parts. Types reach the writer as PSI `presentableText` (e.g. `LocalDate`, `Boolean`, `UUID`, `List<String>`). `ControllerScanner` extracts `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern` and `@NotNull` into `ValidationConstraints`, but not `@Past`, `@Future` or `@Positive`. `collection.bru` is rewritten on every generation, in both regeneration modes.

## Goals / Non-Goals

**Goals:**
- Keep the decision logic pure and table-driven, so each dictionary entry is easy to test and add.
- Make the same rules apply to body fields, params, cookies and multipart parts.

**Non-Goals:**
- Locale-specific generated data (Bruno's faker is English-only), and Brazil-specific formats.
- A settings toggle to disable dynamic values.

## Decisions

**1. Extract an `ExampleValues` object in `writer/`.** It holds `tokenize(name)`, the rule table and `valueFor(name, type, constraints)`. `BrunoWriter.fakeValue` and `dynamicFakeForField` delegate to it, and `fakeValue` stays as a thin `internal` wrapper so existing call sites and tests keep working. Alternative: grow `BrunoWriter` further. Rejected, because the writer already owns rendering, layout and diffing.

**2. Tokenizer.** Steps: NFD-normalize, drop combining marks, split on `(?<=[a-z0-9])(?=[A-Z])`, `(?<=[A-Z])(?=[A-Z][a-z])`, `_`, `-` and digit boundaries, then lowercase. So `userID` becomes `[user, id]`, `URLPath` becomes `[url, path]` and `descrição` becomes `[descricao]`.

**3. Rules are data.** Each rule is a list of alternative word sets (all words in a set must be present) plus a value function of the type. Matching picks the rule with the largest matched set; ties go to table order. Single-word exceptions where position matters (`id` as the last word, `name`/`nome` alone meaning full name) are written as explicit predicates. Alternative: regex per rule. Rejected, because word sets are easier to read and to state in tests.

**4. Evaluation order** in `valueFor`:

```
schema example (caller) -> enum -> @Email -> @Min/@Max literal
 -> pagination names -> date/time types (with past/future)
 -> id rule -> dictionary -> boolean/UUID types
 -> @Positive -> @Size(max<=10) clamp -> current fallback
```

Date types come before the dictionary so that a field like `createdAt: LocalDate` is a date, not a string. `@Size` clamping is applied to the dictionary result only when that result is `$randomLoremSentence`.

**5. Scanner adds three flags** to `ValidationConstraints`: `past` (`@Past`/`@PastOrPresent`), `future` (`@Future`/`@FutureOrPresent`) and `positive` (`@Positive`). They support the jakarta and javax packages, like the existing constraints. Covered by fixture tests in `ControllerScannerTest`.

**6. Date script** is a constant string appended to `collectionFileContent`, after `vars:pre-request` and the auth block. It uses only `Date`, `Math.random` and `bru.setVar`, so it runs in Bruno's safe and developer script modes without npm modules. Runtime variables are used (not `$`-prefixed), because Bruno reserves `$` for its built-in dynamic variables.

**7. Start/end pairs share one drawn day.** The script draws a day D once per request and derives `randomStart*`/`randomEnd*` from it: first and last second of D, with a `Z` suffix for the offset-based types. Start/end words are checked inside the date branch of `valueFor`, after `@Past`/`@Future` constraints and before past/future name words, so `startDate @Future` stays future. Alternative: offset the end by a random number of hours after a random start. Rejected, because the user asked for whole-day bounds, which are also easier to read.

**8. Quoting** stays with `quoteJsonValue`, which keys on the type: `boolean` is unquoted, `UUID` and dates are quoted, and numbers are unquoted. Pagination and `@Positive` produce numeric literals.

## Risks / Trade-offs

- [Regenerating in Overwrite mode changes many values, which shows up as noise in the diff preview and in git] → Note it in `changeNotes` and the README. Add-only mode keeps existing request files untouched.
- [The variable names `randomDate` and friends may clash with a user's environment variable] → Runtime variables set by the script take precedence for that request. Document the names in the README. The user chose plain names knowingly.
- [A dictionary word may still mislabel a field (`title` in a non-job context)] → Leave `title` out of the job rule. Keep the table conservative, and keep `@Schema(example)` as the escape hatch.
- [A user who edits `collection.bru` loses the script edits on regeneration] → This was already true for the file. Unchanged behavior.

## Migration Plan

No data migration is needed. Release as a minor version (1.6.0), with `changeNotes` describing the new values and the script.
