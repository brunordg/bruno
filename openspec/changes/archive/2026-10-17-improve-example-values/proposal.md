# Proposal

## Why

Os valores de exemplo gerados nas requests vêm de uma heurística que procura trechos (substring) no nome do campo. Isso causa falsos positivos (`paid` vira UUID, `cancelReason` vira telefone, `updatedBy` e `timeout` viram timestamp). Também deixa variáveis indefinidas (`boolean ativo` vira `{{ativo}}`) e gera datas que o Jackson rejeita (`LocalDate` recebe `$isoTimestamp`). Além disso, o plugin usa só cerca de 13 das mais de 100 variáveis dinâmicas do Bruno 2.x e reconhece poucos nomes de campo em português.

## What Changes

- O nome do campo passa a ser tokenizado antes da comparação: camelCase, snake_case, minúsculas e acentos removidos. As regras comparam tokens inteiros, não trechos do nome.
- Um dicionário bilíngue (português e inglês) mapeia tokens para variáveis dinâmicas do Bruno (`$randomFirstName`, `$randomFullName`, `$randomUserName`, `$randomPassword`, `$randomStreetAddress`, `$randomCity`, `$randomCountryCode`, `$randomCompanyName`, `$randomJobTitle`, `$randomUrl`, `$randomAvatarImage`, `$randomIP`, `$randomLatitude`/`$randomLongitude`, `$randomCurrencyCode`, `$randomHexColor`, `$randomFileName`, `$randomMimeType`, `$randomLocale`, `$randomLoremSlug`, `$randomSemver`, `$randomPrice`, entre outras). Quando mais de uma regra se aplica, vence a mais específica.
- O tipo Java também identifica o campo: `boolean`/`Boolean` gera `{{$randomBoolean}}` e `UUID` gera `{{$randomUUID}}`.
- **BREAKING (valores gerados):** campos `LocalDate`, `LocalDateTime` e `LocalTime` passam a usar variáveis preenchidas por um `script:pre-request` gerado no `collection.bru`: `randomDate`, `randomPastDate`, `randomFutureDate`, `randomDateTime` e `randomTime`, nos formatos aceitos pelo `java.time`. O script entra sempre no `collection.bru`. `Instant`, `OffsetDateTime`, `ZonedDateTime` e `Date` continuam com `{{$isoTimestamp}}`.
- `@Past`/`@PastOrPresent` e tokens como `birth`, `nascimento`, `created` e `criado` usam a variante de passado. `@Future` e tokens como `expir`, `vencimento`, `due` e `validade` usam a variante de futuro.
- **BREAKING (valores gerados):** `description` e `descricao` passam de `$randomProduct` para `$randomLoremSentence`.
- Parâmetros de paginação recebem valores fixos: `page`/`pagina` = `0`, `size`/`tamanho`/`limit` = `20`.
- Constraints adicionais: `@Positive` evita um valor zero e `@Size(max)` pequeno em String gera uma palavra curta.
- Quando nem o nome nem o tipo identificam o campo, o comportamento atual é mantido: String gera `$randomLoremWord`, número gera `$randomInt` e os demais tipos geram `{{nomeDoCampo}}`.
- Fora de escopo: dados específicos do Brasil (CPF, CNPJ, formato de CEP, UF). O token `cep` é tratado apenas como código postal.

## Capabilities

### New Capabilities
- `example-value-generation`: como o plugin escolhe o valor de exemplo de cada campo de body, parâmetro, cookie e multipart. Cobre a tokenização do nome, o dicionário PT/EN de variáveis dinâmicas do Bruno, a identificação por tipo, o script de datas `java.time`, as variantes de passado e futuro, os valores de paginação, o uso de constraints e o fallback.

### Modified Capabilities

Nenhuma. A precedência definida em `openapi-enrichment` (`@Schema(example)`, depois constraints, depois heurística) continua igual; esta mudança define apenas a heurística.

## Impact

- `src/main/kotlin/writer/BrunoWriter.kt`: `fakeValue`, `dynamicFakeForField` e `collectionFileContent`. A heurística pode ser extraída para um componente próprio.
- `src/test/kotlin/writer/`: novos testes da tokenização, do dicionário, do fallback por tipo e do script no `collection.bru`. Testes existentes que fixam `$randomProduct` ou `$isoTimestamp` para `LocalDate` precisam ser atualizados.
- Coleções já geradas: no modo Overwrite, uma nova geração muda valores de exemplo, e o `collection.bru` passa a conter o script. Isso deve constar nas `changeNotes` e no README (EN e PT).
