# example-value-generation Specification

## Purpose

Define how the plugin picks the example value written for each request body field, multipart part, query, path, header and cookie parameter. Values come from Bruno dynamic variables, chosen by matching whole words in the field name (English or Portuguese), by the Java type, and by validation constraints.

## Requirements

### Requirement: Field names are matched by whole words

The generator SHALL split a field name into words at camelCase boundaries, underscores, hyphens and digits. It SHALL lowercase the words and strip diacritics. Name rules SHALL match whole words only, never a substring inside a word.

#### Scenario: camelCase name is split into words

- **WHEN** a String body field is named `firstName`
- **THEN** the generated value is `{{$randomFirstName}}`

#### Scenario: snake_case and accented names are normalized

- **WHEN** a String body field is named `descrição` or `nome_completo`
- **THEN** `descrição` is matched as the word `descricao` and gets `{{$randomLoremSentence}}`, and `nome_completo` gets `{{$randomFullName}}`

#### Scenario: Substring inside a word does not match

- **WHEN** body fields are named `paid` (boolean), `cancelReason` (String), `updatedBy` (String) and `timeout` (int)
- **THEN** `paid` gets `{{$randomBoolean}}`, `cancelReason` gets `{{$randomLoremWord}}`, `updatedBy` gets `{{$randomLoremWord}}` and `timeout` gets `{{$randomInt}}`; none of them is treated as an id, phone or date

### Requirement: Identifier fields

A field whose name is the word `id`, or whose last word is `id`, SHALL get `{{$randomInt}}` when its type is numeric and `{{$randomUUID}}` otherwise.

#### Scenario: Numeric and string ids

- **WHEN** a field `id` has type `Long` and a field `userId` has type `String`
- **THEN** `id` gets `{{$randomInt}}` and `userId` gets `{{$randomUUID}}`

### Requirement: Bilingual name dictionary

The generator SHALL recognize English and Portuguese words in field names and map them to Bruno dynamic variables, covering at least the words in the scenario "Dictionary coverage". When several rules match, the rule that requires the most words SHALL win.

#### Scenario: Dictionary coverage

- **WHEN** a field name contains the words in the first column
- **THEN** the generated value is the one in the second column:

| Words (EN / PT) | Value |
|---|---|
| first+name / primeiro+nome | `{{$randomFirstName}}` |
| last+name / sobrenome, ultimo+nome | `{{$randomLastName}}` |
| name alone, full+name / nome alone, nome+completo | `{{$randomFullName}}` |
| username, user+name, login / usuario, nome+usuario | `{{$randomUserName}}` |
| email, mail | `{{$randomEmail}}` |
| password, pwd / senha | `{{$randomPassword}}` |
| phone, mobile / telefone, celular, fone | `{{$randomPhoneNumber}}` |
| street, address / rua, logradouro, endereco | `{{$randomStreetAddress}}` |
| city / cidade, municipio | `{{$randomCity}}` |
| country+code / codigo+pais | `{{$randomCountryCode}}` |
| country / pais | `{{$randomCountry}}` |
| company / empresa, razao+social | `{{$randomCompanyName}}` |
| job+title, job, occupation / cargo, profissao | `{{$randomJobTitle}}` |
| url, website, site, link | `{{$randomUrl}}` |
| avatar, photo, image / foto, imagem | `{{$randomAvatarImage}}` |
| ip | `{{$randomIP}}` |
| latitude, lat | `{{$randomLatitude}}` |
| longitude, lng, lon | `{{$randomLongitude}}` |
| currency / moeda | `{{$randomCurrencyCode}}` |
| color / cor | `{{$randomHexColor}}` |
| file+name / nome+arquivo | `{{$randomFileName}}` |
| mime+type, content+type | `{{$randomMimeType}}` |
| locale, language / idioma | `{{$randomLocale}}` |
| slug | `{{$randomLoremSlug}}` |
| version / versao | `{{$randomSemver}}` |
| description, comment, notes / descricao, observacao, comentario | `{{$randomLoremSentence}}` |
| zip, postal, cep | `{{$randomInt}}` |
| price, amount, total / preco, valor | `{{$randomPrice}}` for decimal types, `{{$randomInt}}` for integer types |

#### Scenario: English and Portuguese words map to the same variable

- **WHEN** String fields are named `city` and `cidade`
- **THEN** both get `{{$randomCity}}`

#### Scenario: More specific rule wins

- **WHEN** a String field is named `countryCode`
- **THEN** it gets `{{$randomCountryCode}}`, not `{{$randomCountry}}`

#### Scenario: Description uses a sentence

- **WHEN** a String field is named `description`
- **THEN** it gets `{{$randomLoremSentence}}`

### Requirement: Java type identifies the field

When no name rule matches, the field's Java type SHALL decide the value: `boolean`/`Boolean` gets `{{$randomBoolean}}` and `UUID` gets `{{$randomUUID}}`. These values SHALL NOT be quoted unless the type is a string type (`UUID` values are quoted, booleans are not).

#### Scenario: Boolean field without a name hint

- **WHEN** a body field `ativo` has type `boolean`
- **THEN** the JSON contains `"ativo": {{$randomBoolean}}`

#### Scenario: UUID field

- **WHEN** a body field `token` has type `UUID`
- **THEN** the JSON contains `"token": "{{$randomUUID}}"`

### Requirement: Date script in the collection

The generated `collection.bru` SHALL always contain a `script:pre-request` block that sets date and time runtime variables before every request, in formats that `java.time` parses by default. The `collection.bru` SHALL keep being rewritten on every generation in both regeneration modes.

#### Scenario: Variable formats

- **WHEN** a request runs in Bruno
- **THEN** the script has set these variables:

| Variable | Format | Range |
|---|---|---|
| `randomDate` | `yyyy-MM-dd` | within one year before or after today |
| `randomPastDate` | `yyyy-MM-dd` | before today |
| `randomFutureDate` | `yyyy-MM-dd` | after today |
| `randomDateTime` | `yyyy-MM-ddTHH:mm:ss` (no offset) | within one year before or after today |
| `randomTime` | `HH:mm:ss` | any |
| `randomStartDate` / `randomEndDate` | `yyyy-MM-dd` | the same day D, within one year of today |
| `randomStartDateTime` / `randomEndDateTime` | `D T00:00:00` / `D T23:59:59` | same day D |
| `randomStartTime` / `randomEndTime` | `00:00:00` / `23:59:59` | fixed |
| `randomStartTimestamp` / `randomEndTimestamp` | `D T00:00:00Z` / `D T23:59:59Z` | same day D |

#### Scenario: Script is present

- **WHEN** a collection is generated
- **THEN** `collection.bru` contains a `script:pre-request` block that sets every variable listed in "Variable formats" with `bru.setVar`

#### Scenario: Script is present without date fields

- **WHEN** a collection is generated from endpoints that have no date fields
- **THEN** `collection.bru` still contains the same script block

### Requirement: Date fields use the type-appropriate value

Date and time fields SHALL get a value matching their Java type. `LocalDate` uses the past variant when annotated `@Past`/`@PastOrPresent` or named with `birth`, `birthday`, `nascimento`, `created` or `criado`. It uses the future variant when annotated `@Future`/`@FutureOrPresent` or named with `expiration`, `expires`, `due`, `vencimento` or `validade`. Annotations win over name words.

#### Scenario: Type mapping

- **WHEN** a field has one of these types and no past/future hint
- **THEN** it gets the value in the second column:

| Type | Value |
|---|---|
| `LocalDate` | `{{randomDate}}` |
| `LocalDateTime` | `{{randomDateTime}}` |
| `LocalTime` | `{{randomTime}}` |
| `Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date` | `{{$isoTimestamp}}` |

#### Scenario: LocalDate birth date

- **WHEN** a body field `dataNascimento` has type `LocalDate`
- **THEN** the JSON contains `"dataNascimento": "{{randomPastDate}}"`

#### Scenario: LocalDate with Future constraint

- **WHEN** a body field `startsOn` of type `LocalDate` is annotated `@Future`
- **THEN** the JSON contains `"startsOn": "{{randomFutureDate}}"`

#### Scenario: LocalDateTime and Instant

- **WHEN** a body field `scheduledAt` has type `LocalDateTime` and `createdAt` has type `Instant`
- **THEN** `scheduledAt` gets `"{{randomDateTime}}"` and `createdAt` gets `"{{$isoTimestamp}}"`

### Requirement: Start and end fields share one day

A date or time field whose name has a start word (`start`, `starts`, `begin`, `begins`, `from`, `inicio`) or an end word (`end`, `ends`, `finish`, `until`, `fim`, `termino`) SHALL get the first or last second of one day drawn once per request. `@Past`/`@Future` SHALL win over these words, and these words SHALL win over past/future name words.

#### Scenario: Type mapping for start and end

- **WHEN** a field of the type in the first column has a start or end word
- **THEN** it gets the variable in the second or third column, set by the collection script:

| Type | Start | End |
|---|---|---|
| `LocalDateTime` | `{{randomStartDateTime}}` (`D T00:00:00`) | `{{randomEndDateTime}}` (`D T23:59:59`) |
| `LocalDate` | `{{randomStartDate}}` (`D`) | `{{randomEndDate}}` (`D`) |
| `LocalTime` | `{{randomStartTime}}` (`00:00:00`) | `{{randomEndTime}}` (`23:59:59`) |
| `Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date` | `{{randomStartTimestamp}}` (`D T00:00:00Z`) | `{{randomEndTimestamp}}` (`D T23:59:59Z`) |

#### Scenario: Period body

- **WHEN** a body has `startDateTime` and `endDateTime` of type `LocalDateTime`
- **THEN** the JSON contains `"startDateTime": "{{randomStartDateTime}}"` and `"endDateTime": "{{randomEndDateTime}}"`, and when sent both values fall on the same day with the start at `00:00:00` and the end at `23:59:59`

#### Scenario: Portuguese names

- **WHEN** a body has `dataInicio` and `dataFim` of type `LocalDate`
- **THEN** they get `{{randomStartDate}}` and `{{randomEndDate}}`

#### Scenario: Non-date field with a start word

- **WHEN** a String field is named `fromAccount`
- **THEN** it is not treated as a start date

#### Scenario: Constraint wins over start word

- **WHEN** a `LocalDate` field `startDate` is annotated `@Future`
- **THEN** it gets `{{randomFutureDate}}`

### Requirement: Pagination parameters get fixed values

Numeric fields and parameters named `page` or `pagina` SHALL get `0`. Numeric fields and parameters named `size`, `limit`, `pageSize`, `tamanho` or `limite` SHALL get `20`.

#### Scenario: Paged query parameters

- **WHEN** a GET endpoint has required query params `page` (int) and `size` (int)
- **THEN** the URL contains `?page=0&size=20`

### Requirement: Constraints refine generated values

A numeric field annotated `@Positive` with no `@Min` SHALL get a value of at least 1 rather than a value that may be zero. A String field with `@Size(max)` of 10 or less and no name match SHALL get `{{$randomLoremWord}}` even if a longer value would otherwise apply.

#### Scenario: Positive number without Min

- **WHEN** a body field `quantidade` of type `int` is annotated `@Positive`
- **THEN** the generated value is the literal `1`

#### Scenario: Short String with a sentence rule

- **WHEN** a body field `description` of type `String` is annotated `@Size(max = 10)`
- **THEN** it gets `{{$randomLoremWord}}` instead of `{{$randomLoremSentence}}`

### Requirement: Unrecognized fields keep the current fallback

When neither the name, the type nor the constraints identify a field, the generator SHALL keep the current behavior: String types get `{{$randomLoremWord}}`, numeric types get `{{$randomInt}}` (`{{$randomPrice}}` for decimal types) and any other type gets `{{<fieldName>}}`.

#### Scenario: Unrecognized String

- **WHEN** a String body field is named `misc`
- **THEN** it gets `{{$randomLoremWord}}`

#### Scenario: Unrecognized custom type

- **WHEN** a field `payload` has an unrecognized non-object type `Duration`
- **THEN** it gets `{{payload}}`

### Requirement: Precedence is unchanged

An explicit `@Schema(example)` SHALL still win over every rule in this capability. Enum fields SHALL still use their first constant, `@Email` SHALL still give `{{$randomEmail}}` and `@Min`/`@Max` SHALL still give the bounded literal, all before name and type rules apply.

#### Scenario: Example beats the dictionary

- **WHEN** a field `city` is annotated `@Schema(example = "Lisboa")`
- **THEN** the JSON contains `"city": "Lisboa"`
