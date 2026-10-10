# Tasks

## 1. Model fields for OpenAPI data

- [x] 1.1 Add nullable `summary` and `description` to `Endpoint`; `example` and `description` to `RequestBodyField`; `description` to `RequestParameter` (all defaulting to null). Verify the project still compiles with `bash ./gradlew test --no-daemon`.

## 2. Scanner reads springdoc annotations

- [x] 2.1 Add FQN constants for `io.swagger.v3.oas.annotations.Operation`, `...media.Schema`, `...Parameter` in `ControllerScanner`'s companion.
- [x] 2.2 In `extractEndpoints`, read `@Operation` on the method into `Endpoint.summary`/`description`. Unit-test in `ControllerScannerTest` that summary and description are captured, and are null when absent.
- [x] 2.3 In `toRequestBodyField`, read `@Schema(example, description)` into the field (respecting the field/getter/constructor lookup consistent with the project). Unit-test example and description extraction.
- [x] 2.4 In `extractParams`, read `@Parameter(description)` into `RequestParameter.description`. Unit-test extraction.

## 3. Writer uses the enrichment

- [x] 3.1 In `requestContent`, use `endpoint.summary` for `meta.name` when non-blank (collapsing newlines to one line), else keep `"<method> <path>"`. Verify with `BrunoWriterTest`.
- [x] 3.2 Replace the pattern-only `docs {}` with a collected doc-line list (operation description, parameter descriptions, field descriptions, existing pattern notes); emit the block only when non-empty. Verify `BrunoWriterTest` for a populated block and for the no-block case.
- [x] 3.3 In the field-value path (`renderScalarValue`/`dynamicFakeForField`), apply precedence `@Schema(example)` > constraint > heuristic fake, reusing `quoteJsonValue` for quoting. Verify with `BrunoWriterTest` that an example overrides a `@Min` constraint and that fields without an example are unchanged.

## 4. Docs

- [x] 4.1 Document springdoc/OpenAPI 3 support in `README.md` (summary → request name, descriptions → docs, `@Schema(example)` → value), noting Swagger v2 and `@Tag` grouping are not covered. Run `bash ./gradlew test --no-daemon` and confirm green.
