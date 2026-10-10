# Proposal

## Why

O request gerado usa `"<MÉTODO> <rota>"` como nome, valores de exemplo puramente heurísticos (fakes por nome/tipo) e só documenta regras de `@Pattern`. Muitos projetos Spring já anotam seus controllers com springdoc/OpenAPI (`@Operation`, `@Schema`, `@Parameter`), descrevendo propósito, exemplos reais e descrições de campo. Hoje essa informação é ignorada, e a collection sai menos legível e com exemplos menos fiéis do que poderia.

## What Changes

- Quando um handler tem `@Operation(summary = "...")`, o nome do request (`meta.name`) passa a usar o summary; sem ele, mantém-se o atual `"<MÉTODO> <rota>"`.
- `@Operation(description = ...)` e `@Parameter(description = ...)` passam a alimentar o bloco `docs {}` do request (que hoje só recebe notas de `@Pattern`).
- `@Schema(example = ...)` num campo do request body define o valor de exemplo, com precedência sobre qualquer fake heurístico. `@Schema(description = ...)` entra em `docs {}`.
- Precedência de valor de campo passa a ser: **`@Schema(example)` > constraint (min/max/email) > fake por nome/tipo** (hoje começa em constraint).
- Suporte a springdoc / OpenAPI 3 (`io.swagger.v3.oas.annotations.*`).

Non-goals: agrupar pastas por `@Tag` (fica como extensão futura, pois muda a organização de arquivos já tratada em outra capability); Swagger v2 (`io.swagger.annotations.*` / `@ApiModelProperty`).

## Capabilities

### New Capabilities
- `openapi-enrichment`: como o plugin lê anotações springdoc/OpenAPI 3 nos controllers e DTOs para enriquecer o nome, a documentação e os valores de exemplo dos requests gerados.

### Modified Capabilities
<!-- Nenhuma: não existem specs anteriores neste projeto. A precedência de valor interage com request-body-generation (P1), mas aqui apenas adiciona a fonte @Schema(example) no topo; a ordem é descrita nesta capability. -->

## Impact

- **Código**: `src/main/kotlin/scanner/ControllerScanner.kt` — ler `@Operation` no método, `@Schema` nos fields, `@Parameter` nos params; `src/main/kotlin/model/Endpoint.kt` — campos opcionais `summary`/`description` em `Endpoint`, `example`/`description` em `RequestBodyField` e `RequestParameter`.
- **Writer**: `src/main/kotlin/writer/BrunoWriter.kt` — `requestContent` (nome via summary, docs ampliado), `renderScalarValue`/`fakeValue` (respeitar `example`).
- **Testes**: `ControllerScannerTest` (extração das anotações) e `BrunoWriterTest` (nome via summary, example tem precedência, docs populado).
- **Sem dependências novas**: anotações lidas por FQN via PSI do projeto alvo, como já é feito para Bean Validation.
