# Proposal

## Why

O scanner deriva os nomes de campo do request body diretamente do PSI (`field.name`) e inclui todo campo não-estático. Quando o DTO usa anotações Jackson, o JSON gerado fica **errado**: campos renomeados por `@JsonProperty` saem com o nome Java, campos `@JsonIgnore` aparecem no body, e campos somente-leitura (`access = READ_ONLY`) são incluídos mesmo não fazendo parte da entrada. É um bug silencioso — a collection parece válida, mas o body não bate com o contrato real da API.

## What Changes

- O scanner passa a resolver o nome JSON efetivo de cada campo do request body respeitando `@JsonProperty("...")` (atributo `value`), inclusive quando a anotação está no getter ou no parâmetro de construtor (Java `record` / Kotlin `val` em construtor), não só no field.
- Campos anotados com `@JsonIgnore` são omitidos do body gerado.
- Campos com `@JsonProperty(access = READ_ONLY)` são omitidos do request body (são saída, não entrada).
- O nome JSON resolvido é usado de ponta a ponta: tanto na chave do objeto quanto no placeholder de valor (`{{nome}}`), mantendo consistência.
- Suporta ambos os pacotes de anotação: `com.fasterxml.jackson.annotation.*` (padrão).

Non-goals (desta change): estratégias de nomenclatura no nível da classe (`@JsonNaming`/`PropertyNamingStrategy`) e `@JsonInclude`. Ficam registrados como extensão futura.

## Capabilities

### New Capabilities
- `request-body-generation`: como o plugin deriva os campos, nomes e valores do corpo (JSON) de um request a partir do DTO anotado com `@RequestBody`, incluindo o respeito às anotações de serialização Jackson.

### Modified Capabilities
<!-- Nenhuma: não existem specs anteriores neste projeto. -->

## Impact

- **Código**: `src/main/kotlin/scanner/ControllerScanner.kt` (`extractFields`, `toRequestBodyField` e helpers de anotação); o modelo `RequestBodyField` pode ganhar o nome JSON resolvido se o Java name ainda for necessário em algum ponto.
- **Writer**: `src/main/kotlin/writer/BrunoWriter.kt` consome `field.name` em `renderScalarValue`/`renderObject`; nenhuma mudança de assinatura se o scanner já entregar o nome efetivo em `name`.
- **Testes**: novos casos em `src/test/kotlin/scanner/ControllerScannerTest.kt` cobrindo `@JsonProperty`, `@JsonIgnore`, `access = READ_ONLY` em field, getter e construtor.
- **Sem dependências novas**: as anotações Jackson já estão no classpath de qualquer projeto Spring Boot alvo; o scanner lê via PSI por FQN, sem precisar da lib no plugin.
