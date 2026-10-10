# Tasks

## 1. Annotation resolution in the scanner

- [x] 1.1 Add Jackson FQN constants (`com.fasterxml.jackson.annotation.JsonProperty`, `...JsonIgnore`) to `ControllerScanner`'s companion, alongside the existing validation constants.
- [x] 1.2 Add a helper that, given a `PsiField` and its owning `PsiClass`, finds the relevant `@JsonProperty`/`@JsonIgnore` annotation by checking (in order) the field, its getter (`getX`/`isX`), and the matching primary-constructor parameter. Verify with a unit test that an annotation on each of the three locations is found.
- [x] 1.3 Add a helper to resolve the effective JSON name: return the trimmed non-blank `value` of `@JsonProperty` when present, else the field name. Unit-test field-level, getter-level, and no-annotation cases.
- [x] 1.4 Add a helper to decide exclusion: true when `@JsonIgnore` is present on any accessor, or when `@JsonProperty` declares `access` containing `READ_ONLY`. Unit-test both exclusion triggers.

## 2. Apply resolution when building fields

- [x] 2.1 In `extractFields`, filter out fields whose exclusion helper returns true (in addition to the existing `static` filter), so ignored/read-only fields never become `RequestBodyField`s. Verify via `ControllerScannerTest` that an ignored field is absent from the result.
- [x] 2.2 In `toRequestBodyField`, set `RequestBodyField.name` to the resolved JSON name instead of the raw `field.name`. Verify the resolved name is carried into nested fields too (resolution runs recursively through `extractFields`).
- [x] 2.3 Confirm no writer change is needed: `BrunoWriter` already uses `field.name` for both key and placeholder. Verify with an existing `BrunoWriterTest`-style assertion that a renamed field produces the renamed key in the JSON block.

## 3. Tests and docs

- [x] 3.1 Extend `ControllerScannerTest` with cases: `@JsonProperty` on field, on getter, on record/Kotlin constructor parameter; `@JsonIgnore`; `access = READ_ONLY`; and a nested object combining a rename and an ignore. Run `bash ./gradlew test --no-daemon` and confirm green.
- [x] 3.2 Add an end-to-end `BrunoWriterTest` (or writer+scanner integration) asserting the generated `body:json {}` block uses renamed keys and omits ignored fields at top level and one level of nesting.
- [x] 3.3 Note the new behavior in `README.md` / `knowledge-base.md` (one line under request-body generation: Jackson `@JsonProperty`/`@JsonIgnore` are respected; `@JsonNaming`/`@JsonInclude` are not yet).
