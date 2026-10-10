# Design

## Context

See proposal.md — Why. The scanner builds `Endpoint`/`RequestBodyField`/`RequestParameter` from PSI and already reads annotations by FQN (Bean Validation). The writer names a request `"${httpMethod} ${path}"` (`requestContent`), emits a `docs {}` block only for `@Pattern` notes, and chooses field values via `dynamicFakeForField` → constraint → `fakeValue`. This change adds springdoc/OpenAPI 3 as an input source.

## Goals / Non-Goals

Goals:
- Use `@Operation`, `@Parameter`, `@Schema` to enrich name, docs, and example values, with clean fallback to today's behavior.

Non-Goals:
- `@Tag`-based folder grouping (owned by the regeneration/organization capability; would change file layout).
- Swagger v2 (`io.swagger.annotations.*`, `@ApiModelProperty`).
- Generating response examples or schemas.

## Decisions

**D1 — Carry OpenAPI data as optional model fields.**
Add nullable `summary: String?` and `description: String?` to `Endpoint`; `example: String?` and `description: String?` to `RequestBodyField`; `description: String?` to `RequestParameter`. Null means "absent", so the writer falls back to current behavior. Keeps the enrichment decoupled from extraction.

**D2 — Read annotations by FQN via PSI, no new dependency.**
Constants: `io.swagger.v3.oas.annotations.Operation` (`summary`, `description`), `io.swagger.v3.oas.annotations.media.Schema` (`example`, `description`) on fields, `io.swagger.v3.oas.annotations.Parameter` (`description`) on parameters. Reuse `findDeclaredAttributeValue` + `trimQuotes`, exactly as validation reading does.

**D3 — Value precedence resolved in the writer.**
Introduce the order `@Schema(example)` > constraint > heuristic fake inside the field-value path (`renderScalarValue`/`dynamicFakeForField`). The example is a literal string from the annotation; apply existing `quoteJsonValue` logic so numeric/boolean fields stay unquoted and strings/enums get quoted. This composes with P1 (`respeitar-jackson-request-body`): P1 fixes the key name, this change fixes the value source; they touch different parts of the same field render.

**D4 — Docs block becomes additive.**
Generalize the current pattern-notes block into a collected list of doc lines: operation description, per-parameter descriptions, per-field descriptions, plus the existing pattern notes. Emit `docs {}` only when the collected list is non-empty, preserving the "no empty block" behavior.

## Risks / Trade-offs

- [`@Schema(example)` is a raw string; a user may put a value whose type mismatches the field] → Apply the field's existing quoting rule and emit as-is; do not attempt type coercion. The example reflects the author's intent.
- [Both P1 and P3 edit the field-render path] → Whichever lands first, the other rebases on the same `renderScalarValue`; the two concerns (key vs. value) are orthogonal, so no logic conflict, only textual.
- [`@Operation` summary with characters that break `.bru` `meta.name`] → `meta.name` already holds free text (`"GET /users"`); keep the summary on one line (collapse newlines) to stay within the single-line value format.

## Open Questions

None that affect the specs or task breakdown.
