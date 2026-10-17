# Tasks

## 1. Constraints no scanner

- [x] 1.1 Adicionar `past`, `future` e `positive` em `ValidationConstraints` e extraí-los no `ControllerScanner` (jakarta e javax: `@Past`/`@PastOrPresent`, `@Future`/`@FutureOrPresent`, `@Positive`). Verificar com novos testes de fixture em `ControllerScannerTest`.

## 2. Tokenização e regras

- [x] 2.1 Criar `writer/ExampleValues.kt` com `tokenize(name)` (NFD sem acentos, camelCase incluindo siglas, `_`, `-`, dígitos, minúsculas). Verificar com testes unitários: `firstName`, `nome_completo`, `descrição`, `userID`, `URLPath`.
- [x] 2.2 Implementar a tabela de regras e `valueFor(name, type, constraints)` na ordem do design: id, dicionário PT/EN com regra mais específica vencendo, boolean/UUID por tipo e fallback atual. Verificar com testes para cada linha do dicionário (EN e PT), `countryCode` vs `country`, e os falsos positivos `paid`, `cancelReason`, `updatedBy`, `timeout`, `paisagem`.
- [x] 2.3 Adicionar as regras de data: `LocalDate`/`LocalDateTime`/`LocalTime` para as variáveis do script, variante passado/futuro por constraint e por palavra (a constraint vence), `Instant`/`OffsetDateTime`/`ZonedDateTime`/`Date` para `$isoTimestamp`. Verificar com testes dos cenários de data da spec.
- [x] 2.4 Adicionar paginação (`page`/`pagina` = 0; `size`/`limit`/`pageSize`/`tamanho`/`limite` = 20), `@Positive` (literal 1 sem `@Min`) e o corte para `$randomLoremWord` com `@Size(max <= 10)`. Verificar com testes unitários.

## 3. Integração no writer

- [x] 3.1 Fazer `BrunoWriter.fakeValue` e `dynamicFakeForField` delegarem para `ExampleValues`, mantendo a precedência de `@Schema(example)`, enum, `@Email` e `@Min`/`@Max`. Atualizar os testes de `BrunoWriterTest` que fixavam o comportamento antigo (`notes`, `flag: boolean`). Verificar com `./gradlew test`.
- [x] 3.2 Acrescentar o bloco `script:pre-request` com as cinco variáveis de data ao `collection.bru`, sempre presente. Verificar com teste em `BrunoWriterCollectionTest` (o bloco existe, define as cinco variáveis com `bru.setVar` e aparece também sem campos de data).
- [x] 3.3 Testes de ponta a ponta em `BrunoWriterCollectionTest`: body JSON com `ativo: boolean` sem aspas, `token: UUID` com aspas, `dataNascimento: LocalDate` como `{{randomPastDate}}`, e query `?page=0&size=20`. Verificar com `./gradlew test`.
- [x] 3.4 Validar o script num Bruno 2.x real: executado com `@usebruno/cli` 2.x contra um servidor echo local (6/6 testes: formatos `yyyy-MM-dd`, `yyyy-MM-ddTHH:mm:ss`, `HH:mm:ss`, passado < hoje < futuro, boolean sem aspas). Conferência visual no app desktop fica a cargo do usuário.

## 4. Documentação e release

- [x] 4.1 Atualizar o README (EN e PT) com a nova geração de valores, a lista de variáveis do script e o aviso de mudança de valores ao regenerar em Overwrite. Atualizar `knowledge-base.md`. Verificar por revisão.
- [x] 4.2 Subir a versão para 1.6.0 e adicionar a entrada nas `changeNotes`. Verificar com `./gradlew clean test verifyPlugin buildPlugin` (testes passando, verifier "Compatible" sem avisos, zip gerado).

## 5. Pares início/fim

- [x] 5.1 Gerar no script um dia D por request e definir `randomStart*`/`randomEnd*` para Date, DateTime, Time e Timestamp. Verificar com teste do `DATE_SCRIPT` e execução no `@usebruno/cli` 2.x (fim depois do início, mesmo dia, formatos corretos).
- [x] 5.2 Reconhecer palavras de início/fim (EN/PT) em campos de data de todos os tipos, com `@Past`/`@Future` vencendo. Verificar com testes unitários (`startDateTime`/`endDateTime`, `dataInicio`/`dataFim`, `startsAt: Instant`, `fromAccount: String`, `startDate @Future`) e teste de ponta a ponta no `BrunoWriterCollectionTest`.
- [x] 5.3 Atualizar README (EN/PT), `knowledge-base.md` e `changeNotes` da 1.6.0. Verificar com `./gradlew clean test verifyPlugin buildPlugin`.

## Workflow follow-up

- Commit em `feat/improve-example-values`, merge na `main` com merge commit (mediante autorização).
- Arquivar a mudança com `/opsx:archive`, sincronizando a spec `example-value-generation`.
