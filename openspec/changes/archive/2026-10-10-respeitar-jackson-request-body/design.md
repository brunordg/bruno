# Design

## Context

See proposal.md — Why. Today `ControllerScanner.extractFields` iterates `psiClass.allFields` (filtering only `static`) and `toRequestBodyField` builds each `RequestBodyField` with `name = field.name`. The writer (`BrunoWriter.renderObject`/`renderScalarValue`) uses that `name` both as the JSON key and inside the `{{name}}` placeholder. There is no awareness of Jackson annotations, which Spring's default Jackson `ObjectMapper` uses to map Java properties to JSON.

## Goals / Non-Goals

Goals:
- Resolve the effective JSON name per field, and omit fields Jackson would not deserialize, at every nesting level.

Non-Goals (deferred, not addressed here):
- Class-level `@JsonNaming` / `PropertyNamingStrategy` (snake_case, etc.).
- `@JsonInclude` (NON_NULL/NON_EMPTY) affecting which example values appear.
- `@JsonAlias`, `@JsonUnwrapped`, `@JsonTypeInfo`/polymorphism.

## Decisions

**D1 — Annotation lookup spans field, getter, and constructor parameter.**
Jackson resolves a property from any of its accessors, so the annotation may sit on the field, the getter (`getUserName`/`isActive`), or the constructor parameter (Java `record` components, Kotlin `val` in the primary constructor). Resolution order for a field named `x`: (1) the `PsiField` itself; (2) its getter method (`getX`/`isX`) found on the class; (3) a constructor parameter named `x`. First non-blank `@JsonProperty("value")` wins; first `@JsonIgnore` / read-only found on any accessor excludes the field.
- *Alternative considered*: field-only lookup. Rejected — misses records and Kotlin data classes, which are common for DTOs and are exactly where the current behavior is most often wrong.

**D2 — Keep `RequestBodyField.name` as the single source of truth.**
Store the resolved JSON name in the existing `name` property so the writer needs no change. The raw Java field name is not needed downstream once the JSON name is resolved. This keeps the writer and its tests untouched.
- *Alternative considered*: add a separate `jsonName` field and change the writer to prefer it. Rejected as unnecessary surface area; nothing else consumes the Java name.

**D3 — Match annotations by fully-qualified name via PSI, no new dependency.**
Reuse the existing pattern (`hasAnnotation(FQN)` / `findAnnotation(FQN)` / `findDeclaredAttributeValue`) already used for Bean Validation. Constant: `com.fasterxml.jackson.annotation.JsonProperty`, `...JsonIgnore`. Read `access` by inspecting the `access` attribute text for `READ_ONLY`.
- *Alternative considered*: depend on Jackson at plugin build time. Rejected — the plugin inspects the target project's PSI; the annotations live in the analyzed project, not the plugin.

## Risks / Trade-offs

- [Getter/parameter resolution can be ambiguous for non-standard accessor names] → Limit to conventional `getX`/`isX` and exact constructor-parameter name match; fall back to field name when nothing resolves (current behavior), so the worst case is unchanged, never worse.
- [Reading the `access` attribute as text is brittle vs. `JsonProperty.Access.READ_ONLY` spelled different ways] → Match on the `READ_ONLY` token substring, mirroring how `methodMapping` already matches `method = {GET}` text.

## Migration Plan

Pure behavior improvement in generation; no persisted state or file-format change. Rollback is reverting the scanner change. Existing collections regenerate correctly on next run.
