# Spec Delta

## Purpose

Define how the plugin reads springdoc / OpenAPI 3 annotations on controllers and DTOs to enrich generated requests — their display name, documentation, and example values — while falling back to current behavior when the annotations are absent.

## ADDED Requirements

### Requirement: Operation summary names the request

When a handler method is annotated with `@Operation` declaring a non-blank `summary`, the generated request's `meta.name` SHALL be that summary. When no such summary is present, the request name SHALL remain `"<HTTP method> <path>"`.

#### Scenario: Handler has an Operation summary

- **WHEN** a handler is annotated `@Operation(summary = "Busca usuário por id")`
- **THEN** the generated request's `meta.name` is `Busca usuário por id`

#### Scenario: Handler has no Operation summary

- **WHEN** a handler has no `@Operation` or an `@Operation` with blank summary
- **THEN** the generated request's `meta.name` is `"<HTTP method> <path>"` (unchanged)

### Requirement: Descriptions enrich the docs block

The system SHALL include available OpenAPI descriptions in the request's `docs {}` block: `@Operation(description)` for the operation, `@Parameter(description)` for a path/query/header parameter, and `@Schema(description)` for a body field. These SHALL be additive to any existing `docs {}` content (such as pattern notes), not replace it.

#### Scenario: Operation and field descriptions present

- **WHEN** a handler has `@Operation(description = "...")` and a body field has `@Schema(description = "...")`
- **THEN** the generated request's `docs {}` block contains both descriptions

#### Scenario: No descriptions present

- **WHEN** no OpenAPI description annotations are present and there are no pattern notes
- **THEN** no empty `docs {}` block is emitted (unchanged from current behavior)

### Requirement: Schema example overrides generated values

When a request body field is annotated `@Schema` with a non-blank `example`, the generated body SHALL use that example value for the field. The precedence for a field's value SHALL be: `@Schema(example)` first, then validation-constraint-derived values (min/max/email), then the name/type heuristic fake.

#### Scenario: Field has a Schema example

- **WHEN** a body field `age` is annotated `@Schema(example = "30")` and also `@Min(18)`
- **THEN** the generated JSON uses `30` for `age` (the example wins over the constraint)

#### Scenario: Field has no example but has a constraint

- **WHEN** a body field has no `@Schema(example)` but has `@Min`/`@Max`/`@Email`
- **THEN** the generated value is derived from the constraint (unchanged from current behavior)

#### Scenario: Field has neither

- **WHEN** a body field has neither an example nor a relevant constraint
- **THEN** the generated value is the name/type heuristic fake (unchanged from current behavior)
