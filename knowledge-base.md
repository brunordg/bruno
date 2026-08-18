# KNOWLEDGE BASE — Bruno Generator

> **Nota de escopo.** O template original desta KB foi desenhado para uma aplicação **Spring Boot** (controllers, entities, repositories, banco de dados, segurança de API etc.). Este repositório **não é uma aplicação Spring Boot** — é o **Bruno Generator**, um **plugin para IntelliJ IDEA** escrito em **Kotlin**, que analisa (via PSI) projetos Spring Boot de terceiros e gera, a partir deles, uma collection para o cliente HTTP [Bruno](https://www.usebruno.com/). As seções do template sem equivalente real no código (Entities JPA, Repositories, Segurança de API, Banco de dados, Mensageria, Cache de aplicação) foram mantidas na numeração original e marcadas explicitamente como **Não aplicável**, para preservar a estrutura solicitada sem inventar conteúdo.

---

## 1. Visão geral

O Bruno Generator é um plugin para IntelliJ IDEA que ajuda desenvolvedores Spring Boot a criar rapidamente uma collection do Bruno a partir de controllers já existentes no projeto aberto na IDE.

O plugin:
- Varre (scan) as classes anotadas com `@RestController` do projeto aberto na IDE, usando a API PSI (Program Structure Interface) do IntelliJ Platform.
- Extrai métodos HTTP, rotas, path variables, query params, headers, cookies, corpo de request (incluindo objetos aninhados, listas e multipart) e restrições de Bean Validation.
- Gera arquivos `.bru` organizados em pastas, com valores de exemplo "inteligentes" e ambientes (`environments`) configuráveis.
- Antes de gravar, mostra uma tela de seleção de endpoints e um resumo do que será adicionado/removido/mantido, para o usuário confirmar.

Fonte: `README.md`; `src/main/resources/META-INF/plugin.xml` (tag `<description>`).

Não é uma aplicação que roda em servidor: executa dentro do processo da IDE, disparado manualmente pelo usuário via **Tools > Generate Bruno Collection**.

Fonte: `src/main/resources/META-INF/plugin.xml`, elemento `<action id="GenerateBrunoCollectionAction">`.

---

## 2. Arquitetura

Arquitetura em camadas, específica de um plugin IntelliJ (não é MVC/REST):

```
AnAction (entrada, disparada pelo menu Tools)
    ↓
ControllerScanner  (lê PSI do projeto aberto → List<Endpoint>)
    ↓
UI: EndpointSelectionDialog  (tela única: seleção de endpoints + diff ao vivo, usuário filtra/confirma)
    ↓
BrunoWriter  (grava arquivos .bru em disco / calcula diff)
    ↓
Sistema de arquivos do projeto (pasta <nome-do-projeto>/ com bruno.json, collection.bru, requests/, environments/)
```

Persistência de configuração do plugin (não dos dados gerados) é feita à parte, via `PersistentStateComponent` do IntelliJ Platform:

```
BrunoGeneratorConfigurable (tela em Settings > Tools > Bruno Generator)
    ↓
BrunoGeneratorSettings (Service, PersistentStateComponent)
    ↓
bruno-generator.xml (armazenado no `.idea/` do projeto aberto na IDE)
```

Fonte:
`src/main/kotlin/GenerateBrunoCollectionAction.kt`,
`src/main/kotlin/scanner/ControllerScanner.kt`,
`src/main/kotlin/ui/EndpointSelectionDialog.kt`,
`src/main/kotlin/writer/BrunoWriter.kt`,
`src/main/kotlin/settings/BrunoGeneratorSettings.kt`,
`src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`.

---

## 3. Estrutura do projeto

```
src/main/kotlin/
  GenerateBrunoCollectionAction.kt   ação principal (entry point da IDE)
  MyMessageBundle.kt                 wrapper para mensagens i18n (DynamicBundle)
  model/
    Endpoint.kt                      modelos de domínio do plugin (não persistidos em banco)
  scanner/
    ControllerScanner.kt             leitura de PSI → List<Endpoint>
  writer/
    BrunoWriter.kt                   geração dos arquivos .bru + cálculo de diff
  settings/
    BrunoGeneratorSettings.kt        estado persistente (PersistentStateComponent)
    BrunoGeneratorConfigurable.kt    tela de Settings
    EnvironmentsTable.kt             tabela editável de ambientes (TableModelEditor)
  ui/
    EndpointSelectionDialog.kt       tela única: seleção de endpoints (CheckboxTree) + diff ao vivo (added/removed/unchanged)

src/main/resources/
  META-INF/plugin.xml                descritor do plugin (ações, extensões, dependências)
  META-INF/pluginIcon.svg            ícone do plugin
  icons/brunoAction.png              ícone da ação de menu
  messages/MyMessageBundle.properties

src/test/kotlin/
  scanner/ControllerScannerTest.kt
  writer/BrunoWriterTest.kt
  writer/BrunoWriterCollectionTest.kt
  settings/BrunoGeneratorStateTest.kt
  ui/EndpointSelectionDialogGroupingTest.kt

build.gradle.kts, gradle.properties, settings.gradle.kts, gradle/libs.versions.toml
```

Fonte: listagem de diretórios do repositório (`src/main`, `src/test`).

---

## 4. Tecnologias e dependências

| Item | Valor | Fonte |
|---|---|---|
| Linguagem | Kotlin 2.4.10 | `gradle/libs.versions.toml` |
| Build | Gradle 9.7.0 (wrapper) | `gradle.properties` (`gradleVersion`) |
| Plugin de build | `org.jetbrains.intellij.platform` 2.18.1 | `gradle/libs.versions.toml` |
| Plataforma alvo | IntelliJ IDEA `2025.3.1` (`intellijIdea`) | `gradle.properties` (`platformVersion`), `build.gradle.kts` |
| Compatibilidade mínima | build `253` (`pluginSinceBuild`) | `gradle.properties` |
| JDK de build | toolchain 26 | `build.gradle.kts` (`kotlin { jvmToolchain(26) }`) |
| Bytecode alvo | Java 21 (forçado) | `build.gradle.kts`, comentário: "IntelliJ Platform 2025.3.1's bundled JetBrains Runtime is JDK 21" |
| Dependência de plugin | `bundledPlugin("com.intellij.java")` | `build.gradle.kts` |
| Framework alvo analisado | Spring Boot / Spring MVC (via anotações lidas por PSI, não como dependência de runtime do plugin) | `src/main/kotlin/scanner/ControllerScanner.kt` (FQNs `org.springframework.web.bind.annotation.*`) |
| Testes | JUnit 4.13.2, `spring-web:6.2.1` (para PSI resolver anotações Spring em teste), `jakarta.validation-api:3.1.1` | `build.gradle.kts` (`dependencies { testImplementation(...) }`) |
| Test framework da plataforma | `TestFrameworkType.Platform` + `TestFrameworkType.Plugin.Java` | `build.gradle.kts` |

Não identificado no código analisado: uso de bibliotecas de terceiros para HTTP, banco de dados, mensageria ou serialização além do necessário para a integração com o IntelliJ Platform SDK.

---

## 5. Módulos

| Pacote | Responsabilidade |
|---|---|
| `com.codeteam` (raiz) | Ação de entrada (`GenerateBrunoCollectionAction`) e bundle de mensagens |
| `com.codeteam.model` | Modelos de domínio imutáveis (data classes) que representam o que foi escaneado |
| `com.codeteam.scanner` | Leitura do projeto-alvo via PSI e conversão para o modelo de domínio |
| `com.codeteam.writer` | Geração dos arquivos `.bru` e cálculo de diferenças (`CollectionDiff`) |
| `com.codeteam.settings` | Estado persistente do plugin e tela de configuração |
| `com.codeteam.ui` | Diálogo modal único de seleção de endpoints com diff ao vivo |

Fonte: declarações `package` de cada arquivo em `src/main/kotlin/`.

---

## 6. Controllers e APIs

**Não aplicável.** Este projeto não expõe endpoints REST — ele é quem **lê** endpoints REST de outro projeto.

O equivalente funcional a um "endpoint" aqui é a **ação da IDE** registrada em `plugin.xml`, documentada abaixo no formato adaptado.

### AÇÃO: Generate Bruno Collection

Descrição: dispara o fluxo completo de escaneamento, seleção/confirmação (em uma única tela) e geração da collection Bruno para o projeto atualmente aberto na IDE.

Como é disparada: menu **Tools > Generate Bruno Collection** (grupo `ToolsMenu`, IDE).

Pré-condições: `e.project?.basePath != null` (a ação só fica habilitada/visível se houver um projeto aberto com caminho base definido).

Parâmetros: nenhum parâmetro de usuário direto; o comportamento depende do estado salvo em `BrunoGeneratorSettings` (ambientes configurados, ambiente padrão, diretório de saída) e da seleção feita pelo usuário no `EndpointSelectionDialog` (única tela, com um só botão OK).

Fluxo: ver seção 12, "Fluxo: Gerar collection Bruno".

Saídas possíveis (notificações, grupo `"Bruno Generator"` declarado em `plugin.xml`):
- `INFORMATION` "Bruno collection generated" — sucesso, com contagem de requests gerados.
- `WARNING` "No endpoints found" — nenhum `@RestController` encontrado no projeto.
- `INFORMATION` "Nothing selected" — usuário desmarcou todos os endpoints no diálogo de seleção.
- `ERROR` "Failed to scan controllers" — exceção durante o scan (mensagem da exceção é exibida).
- `ERROR` "Failed to generate Bruno collection" — exceção durante a escrita em disco.

Fonte:
`src/main/kotlin/GenerateBrunoCollectionAction.kt`, classe `GenerateBrunoCollectionAction`, método `actionPerformed`/`continueAfterScan`/`notify`.

---

## 7. Services

Não há camada `@Service` do Spring aqui. As duas classes que concentram a lógica de negócio do plugin são:

### `ControllerScanner` (`com.codeteam.scanner.ControllerScanner`)

Responsabilidade: ler o projeto aberto na IDE via PSI e produzir uma `List<Endpoint>`.

Método público: `fun scan(project: Project): List<Endpoint>`.

Comportamento:
- Executa dentro de `ApplicationManager.getApplication().runReadAction { ... }` (obrigatório para leitura de PSI thread-safe).
- Localiza a classe `org.springframework.web.bind.annotation.RestController` no classpath do projeto (`libraryScope`); se não existir (projeto não depende de Spring Web), retorna lista vazia.
- Usa `AnnotatedElementsSearch.searchPsiClasses` para achar todas as classes do **projeto** (`projectScope`, não bibliotecas) anotadas com `@RestController`.
- Para cada classe, delega a `extractEndpoints`.
- Remove duplicatas por `"${httpMethod} ${path}"` (`distinctBy`) e ordena por `path`, depois por `httpMethod`.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:31-45`.

### `BrunoWriter` (`com.codeteam.writer.BrunoWriter`)

Responsabilidade: transformar `List<Endpoint>` em arquivos `.bru` em disco, e calcular o diff entre o que existe e o que seria gerado.

Métodos públicos:
- `fun computeDiff(projectRoot, endpoints, settings, outputRoot = projectRoot): CollectionDiff` — **somente leitura**, não escreve nem apaga nada.
- `fun writeCollection(projectRoot, endpoints, settings, outputRoot = projectRoot): Path` — escreve `bruno.json`, `collection.bru`, `environments/*.bru`, `requests/**/*.bru`, e remove (`prune`) arquivos `.bru` que não correspondem mais aos endpoints/ambientes atuais.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:16-89`.

---

## 8. Entities

**Não aplicável no sentido JPA/Hibernate** — não há banco de dados nem `@Entity`. Os "modelos de domínio" abaixo são `data class` Kotlin, existem apenas em memória durante a execução da ação, e não são persistidos.

### `Endpoint`

Descrição: representa um endpoint HTTP descoberto em um método anotado com `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/`@PatchMapping` (ou `@RequestMapping` com atributo `method`).

Campos:
- `httpMethod: String` — verbo HTTP em maiúsculas (GET/POST/PUT/DELETE/PATCH).
- `path: String` — caminho completo (classe + método), normalizado (ex.: `/api/widgets/{id}`).
- `handlerName: String` — nome do método Java/Kotlin que implementa o endpoint.
- `hasRequestBody: Boolean` — `true` se há `@RequestBody`, partes multipart, ou o verbo é POST/PUT/PATCH.
- `bodyKind: BodyKind` — `NONE` | `JSON` | `MULTIPART`, derivado pelo scanner (não configurável pelo usuário).
- `requestBodyFields: List<RequestBodyField>` — campos do corpo JSON (vazio se não houver `@RequestBody`).
- `multipartParts: List<MultipartPart>` — partes multipart (vazio se `bodyKind != MULTIPART`).
- `pathVariables`, `queryParams`, `headerParams`, `cookieParams: List<RequestParameter>`.

Fonte: `src/main/kotlin/model/Endpoint.kt:42-54`.

### `RequestBodyField`

Descrição: um campo do corpo de request, podendo se referenciar recursivamente (objeto aninhado) ou representar uma coleção.

Campos: `name`, `type` (nome do tipo Java/Kotlin como texto), `isEnum`, `enumConstants: List<String>`, `constraints: ValidationConstraints`, `isCollection: Boolean`, `nestedFields: List<RequestBodyField>`.

Semântica de `isCollection` + `nestedFields` (4 combinações):
- ambos vazios/false → campo escalar.
- `nestedFields` não vazio, `isCollection` false → objeto aninhado.
- `nestedFields` não vazio, `isCollection` true → array de objetos.
- `nestedFields` vazio, `isCollection` true → array de escalares.

Fonte: `src/main/kotlin/model/Endpoint.kt:23-31`.

### `ValidationConstraints`

Descrição: restrições de Bean Validation lidas por reflexão de anotações (Jakarta ou `javax`).

Campos: `required`, `minSize`, `maxSize`, `min`, `max`, `email`, `pattern`.

Fonte: `src/main/kotlin/model/Endpoint.kt:13-21`.

### `RequestParameter`

Descrição: um parâmetro de path/query/header/cookie.

Campos: `name`, `kind: ParamKind` (`QUERY`|`PATH`|`HEADER`|`COOKIE`), `type`, `required`, `defaultValue`.

Fonte: `src/main/kotlin/model/Endpoint.kt:3-11`.

### `MultipartPart`

Descrição: uma parte de um request `multipart/form-data`.

Campos: `name`, `isFile: Boolean`, `type`, `required` (default `true`).

Fonte: `src/main/kotlin/model/Endpoint.kt:35-40`.

---

## 9. DTOs

Não há DTOs de API (não há API). Os objetos abaixo funcionam como "carriers" de estado/configuração e de resultado:

### `BrunoGeneratorState`

Descrição: estado persistente configurável pelo usuário na tela de Settings.

Campos: `environments: MutableList<BrunoEnvironment>` (default: um único ambiente `dev` → `http://localhost:8080`), `defaultEnvironmentName: String` (default `"dev"`), `outputDirectory: String` (default `""`, vazio = raiz do projeto).

Fonte: `src/main/kotlin/settings/BrunoGeneratorSettings.kt:15-21`.

### `BrunoEnvironment`

Campos: `name: String`, `baseUrl: String`.

Fonte: `src/main/kotlin/settings/BrunoGeneratorSettings.kt:10-13`.

### `CollectionDiff`

Descrição: resultado de `BrunoWriter.computeDiff`, usado para popular o painel de diff dentro do `EndpointSelectionDialog`, recalculado a cada mudança de seleção.

Campos: `added: List<String>`, `removed: List<String>`, `unchanged: List<String>` — caminhos relativos à raiz da collection.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:16-20`.

---

## 10. Repositories

**Não aplicável** (não há Spring Data / JPA repositories). Duas formas de persistência existem no projeto, nenhuma delas é banco de dados:

1. **Configuração do plugin**: `BrunoGeneratorSettings` implementa `PersistentStateComponent<BrunoGeneratorState>` do IntelliJ Platform, com `@State(name = "BrunoGeneratorSettings", storages = [Storage("bruno-generator.xml")])`. É um `@Service(Service.Level.PROJECT)`, ou seja, uma instância por projeto aberto na IDE, serializada em XML pelo próprio framework da plataforma (`XmlSerializerUtil.copyBean`).
   Fonte: `src/main/kotlin/settings/BrunoGeneratorSettings.kt:28-43`.

2. **Dados gerados (a collection Bruno)**: gravados diretamente no sistema de arquivos pelo `BrunoWriter`, usando `java.nio.file.Path`/`Files` — não há camada de acesso a dados intermediária.
   Fonte: `src/main/kotlin/writer/BrunoWriter.kt`.

---

## 11. Regras de negócio

### REGRA: Descoberta de controllers restrita ao escopo do projeto

Descrição: apenas classes do próprio projeto aberto (não de bibliotecas) são consideradas como controllers.

Condição: `AnnotatedElementsSearch.searchPsiClasses(restControllerClass, projectScope)` — `projectScope`, não `allScope`.

Comportamento: controllers vindos de dependências (jars) nunca aparecem na collection gerada.

Exceção: nenhuma; se `org.springframework.web.bind.annotation.RestController` não existir nem nas bibliotecas do projeto, o scan retorna lista vazia (o projeto provavelmente não usa Spring Web).

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:31-37`.

Classe: `ControllerScanner`. Método: `scan`.

### REGRA: Mapeamento de verbo HTTP

Descrição: o verbo HTTP de um endpoint é determinado pela anotação de mapeamento do método.

Condição: `@GetMapping`→GET, `@PostMapping`→POST, `@PutMapping`→PUT, `@DeleteMapping`→DELETE, `@PatchMapping`→PATCH. Se nenhuma dessas existir, tenta `@RequestMapping(method = ...)`, inspecionando o texto do atributo `method` em busca das substrings `GET`/`POST`/`PUT`/`DELETE`/`PATCH` (nessa ordem de prioridade).

Comportamento: se `@RequestMapping` existir mas nenhum desses verbos for encontrado no atributo `method`, o padrão é `GET`.

Exceção: método sem nenhuma dessas anotações não é considerado um endpoint (`methodMapping` retorna `null`, e o método é descartado via `mapNotNull`).

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:223-250`. Classe: `ControllerScanner`. Método: `methodMapping`.

### REGRA: Composição de path (classe + método)

Descrição: o path final de um endpoint é a junção do `@RequestMapping` de nível de classe com o path do método.

Comportamento: ambos os lados têm `/` nas pontas removidas (`trim('/')`), são unidos com `/`, prefixados por `/`, e barras duplas resultantes são colapsadas (`replace("//", "/")`). Quando o valor é um array (`{"/a", "/b"}`), apenas o **primeiro** elemento é usado (`stripArraySyntax`).

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:201-206` (assinatura atual em `normalizePath`, linhas 276-281 no arquivo corrente), `stripArraySyntax` (linhas 266-272). Classe: `ControllerScanner`.

### REGRA: Deduplicação de endpoints

Descrição: se dois métodos resultarem no mesmo par (verbo, path), apenas um é mantido.

Condição: `distinctBy { "${it.httpMethod} ${it.path}" }`, aplicado após agregar todos os controllers.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:42`. Classe: `ControllerScanner`. Método: `scan`.

### REGRA: Corpo de request — origem

Descrição: um endpoint tem corpo (`hasRequestBody = true`) se: existe um parâmetro anotado com `@RequestBody`, OU existe ao menos uma parte multipart (`@RequestPart`/`MultipartFile`), OU o verbo HTTP é POST/PUT/PATCH (mesmo sem anotação explícita de body).

`bodyKind` é derivado assim: `MULTIPART` se houver `multipartParts` não vazio; senão `JSON` se `hasRequestBody`; senão `NONE`.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:52-60`. Classe: `ControllerScanner`. Método: `extractEndpoints`.

### REGRA: Recursão em campos de corpo aninhados, com limites

Descrição: campos de um `@RequestBody` que são objetos customizados (não tipos de JDK/Kotlin/Spring) têm seus próprios campos extraídos recursivamente.

Condição de recursão (`toRequestBodyField`): o tipo resolvido do campo não é `enum`, `interface` nem `@interface`; seu `qualifiedName` não começa com `java.`, `javax.`, `jakarta.`, `kotlin.` ou `org.springframework.` (`isRecursableType`); a profundidade atual é menor que `MAX_NESTING_DEPTH = 5`; e o `qualifiedName` do tipo ainda não está no conjunto `ancestry` da cadeia de recursão atual (guarda contra ciclos, ex.: `class Node { Node parent; }`).

Comportamento: a guarda de ciclo só entra em ação a partir da **segunda** ocorrência do mesmo tipo na cadeia — o tipo raiz do `@RequestBody` não está em `ancestry` até que a recursão realmente "entre" nele uma vez; logo, um campo auto-referenciado é expandido um nível antes de parar.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:145-192` (`toRequestBodyField`, `collectionElementType`, `isRecursableType`). Classe: `ControllerScanner`.

Confirmado por teste: `src/test/kotlin/scanner/ControllerScannerTest.kt`, teste `"test stops recursing into a self-referencing request body field instead of overflowing"`.

### REGRA: Detecção de coleções no corpo

Descrição: um campo do tipo array Java (`T[]`) ou de um tipo que herda de `java.util.Collection` (ex.: `List<T>`, `Set<T>`) é marcado com `isCollection = true`, e seus campos aninhados (se `T` for um objeto customizado) são extraídos a partir do **tipo do elemento**, não do tipo da coleção.

Comportamento: `List<String>` → `isCollection = true`, `nestedFields` vazio (elemento é escalar). `List<OrderItem>` → `isCollection = true`, `nestedFields` populado com os campos de `OrderItem`.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:180-186` (`collectionElementType`). Classe: `ControllerScanner`.

### REGRA: Detecção de multipart

Descrição: um parâmetro de método é considerado uma parte multipart se estiver anotado com `@RequestPart`, OU se seu tipo (ou tipo do elemento, se for coleção/array) for `org.springframework.web.multipart.MultipartFile`.

Comportamento: o nome da parte vem do atributo `value`/`name` de `@RequestPart`, se declarado; senão usa o nome do parâmetro Java/Kotlin. `required` vem do atributo `required` de `@RequestPart` quando presente; caso contrário, assume `true`.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:78-103` (`multipartParts`, `toMultipartPart`, `isMultipartFileType`). Classe: `ControllerScanner`.

### REGRA: Nome e obrigatoriedade de parâmetros (path/query/header/cookie)

Descrição: para `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue`.

Comportamento: o nome vem do atributo `value` ou `name` da anotação, se declarado (sem aspas); senão, o nome do parâmetro na assinatura do método. A obrigatoriedade (`required`) usa o atributo `required` da anotação, se declarado explicitamente; senão, é `true` apenas se **não** houver `defaultValue` declarado (ou seja, ter um valor padrão implica `required = false` quando `required` não foi explicitado).

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:105-124` (`extractParams`). Classe: `ControllerScanner`.

### REGRA: Restrições de Bean Validation lidas do campo

Descrição: para cada campo de `@RequestBody`, o scanner procura anotações de `jakarta.validation.constraints` **ou** `javax.validation.constraints` (compatibilidade com ambos os namespaces).

Comportamento: `required = true` se houver `@NotNull`, `@NotBlank` OU `@NotEmpty`. `minSize`/`maxSize` vêm de `@Size(min=, max=)`. `min`/`max` vêm de `@Min`/`@Max`. `email = true` se houver `@Email`. `pattern` vem do atributo `regexp` de `@Pattern`.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:194-215` (`extractConstraints`), constantes em `companion object` (linhas 297-328). Classe: `ControllerScanner`.

### REGRA: Geração de valores de exemplo ("fakes") — heurística por nome e tipo

Descrição: ao gerar o corpo JSON de exemplo, o valor de cada campo é escolhido por uma cascata de heurísticas sobre o **nome** (case-insensitive, com termos em inglês e português) e o **tipo** do campo.

Comportamento (ordem de prioridade, primeira que casar vence): nome termina em/é `id` → `{{$randomInt}}` (numérico) ou `{{$randomUUID}}` (string); contém `email` → `{{$randomEmail}}`; contém `first`+`name` → `{{$randomFirstName}}`; contém `last`+`name` → `{{$randomLastName}}`; contém `phone`/`mobile`/`cel`/`celular`/`telefone` → `{{$randomPhoneNumber}}`; contém `zip`/`postal`/`cep` → `{{$randomInt}}`; contém `date`/`time`/`createdAt`/`updatedAt` → `{{$isoTimestamp}}`; contém `amount`/`price`/`total` → `{{$randomInt}}`; contém `street`/`rua` → `{{$randomStreetAddress}}`; contém `city`/`cidade` → `{{$randomCity}}`; contém `country`/`pais` → `{{$randomCountry}}`; contém `description`/`descricao` → `{{$randomProduct}}`; senão, por tipo: `BigDecimal`/`double`/`float` → `{{$randomPrice}}`; tipo de data/hora → `{{$isoTimestamp}}`; `String` → `{{$randomLoremWord}}`; numérico → `{{$randomInt}}`; senão → `{{<nomeDoCampo>}}` (variável literal).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:323-360` (`fakeValue`). Classe: `BrunoWriter`.

### REGRA: Valores dinâmicos apenas em POST/PUT/PATCH

Descrição: os placeholders de runtime do Bruno (`{{$randomInt}}`, `{{$randomEmail}}` etc.) só são usados quando o verbo é POST, PUT ou PATCH.

Comportamento: para outros verbos (ex.: GET com corpo, caso raro), os valores gerados são referências de variável estáticas `{{nomeDoCampo}}`, não os placeholders dinâmicos do Bruno.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:286-288` (`usesDynamicFakes`), uso em `jsonObject`/`renderScalarValue`/`dynamicFakeForField`. Classe: `BrunoWriter`.

### REGRA: Campos com `@Min`/`@Max` recebem valor no meio do intervalo

Descrição: quando um campo numérico tem `min` e/ou `max` declarados via Bean Validation, o valor de exemplo é calculado a partir do intervalo, em vez de usar a heurística por nome.

Comportamento: se `min` e `max` estão presentes, o valor é `(min + max) / 2` (divisão inteira); se só um dos dois está presente, usa esse valor; se nenhum, usa `0`. Para tipos de ponto flutuante (`BigDecimal`/`double`/`float`), o literal recebe sufixo `.0`.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:290-311` (`dynamicFakeForField`, `boundedNumericLiteral`). Classe: `BrunoWriter`.

### REGRA: Enums usam o primeiro valor da constante

Descrição: um campo `enum` no corpo de request recebe, como valor de exemplo, a **primeira** constante declarada do enum (não é aleatório).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:290-293` (`dynamicFakeForField`). Classe: `BrunoWriter`.

### REGRA: Query params só entram na URL/params se forem obrigatórios

Descrição: ao montar a query string de exemplo (`url: {{baseUrl}}/caminho?...`) e o bloco `params:query { }`, apenas os parâmetros com `required = true` são incluídos habilitados por padrão na query string da URL; todos os query params (obrigatórios ou não) aparecem no bloco `params:query { }`, mas os opcionais ficam desabilitados (prefixo `~`).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:152-157` (bloco `params:query`), `211-215` (`buildQueryString`). Classe: `BrunoWriter`.

### REGRA: Parâmetro opcional é representado com prefixo `~` no `.bru`

Descrição: convenção do formato Bruno para "chave presente mas desabilitada".

Comportamento: aplicada a `params:path`, `params:query`, headers e ao bloco `body:multipart-form` — qualquer `RequestParameter`/`MultipartPart` com `required = false` tem seu nome prefixado por `~` na linha gerada.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:200-204` (`paramLine`), `271-278` (`multipartFormBlock`). Classe: `BrunoWriter`.

### REGRA: `auth: none` fixo em todo request gerado

Descrição: nenhum mecanismo de autenticação é configurado automaticamente nos requests gerados; o bloco do verbo sempre inclui `auth: none`.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:142` (dentro de `requestContent`). Classe: `BrunoWriter`.

[NECESSITA CONFIRMAÇÃO]: não há, no código analisado, nenhuma forma de o usuário configurar autenticação padrão para os requests gerados — permanece sempre `auth: none`.

### REGRA: `Content-Type` só é adicionado para corpo JSON

Descrição: o header `Content-Type: application/json` só é emitido quando `bodyKind == BodyKind.JSON`. Para `MULTIPART`, nenhum `Content-Type` explícito é emitido (o boundary do multipart é responsabilidade do cliente).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:160-164`. Classe: `BrunoWriter`.

### REGRA: Notas de padrão (`@Pattern`) viram bloco `docs`

Descrição: para cada campo (incluindo aninhados, com caminho tipo `endereco.rua`) que tenha uma `constraints.pattern` declarada, uma linha é adicionada a um bloco `docs { }` no `.bru` gerado.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:187-195` (uso de `flattenWithPath`), `265-269` (`flattenWithPath`). Classe: `BrunoWriter`.

### REGRA: Organização de pastas por primeiro segmento do path

Descrição: cada request gerado é colocado em `requests/<primeiroSegmentoDoPath>/`. Se o path não tiver segmento (ex.: `/`), usa a pasta `root`.

Comportamento: o nome do arquivo é `NN-slug-do-handler.bru`, onde `NN` é o índice sequencial (1-based, largura 2, ex.: `01-`) na ordem em que os endpoints aparecem na lista (já ordenada por `scan`).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:371-374` (`requestFilePath`). Classe: `BrunoWriter`.

### REGRA: Regeneração remove arquivos obsoletos (prune)

Descrição: toda chamada a `writeCollection` recalcula o conjunto esperado de arquivos de request e de ambiente e **apaga** (`Files.deleteIfExists`) qualquer `.bru` existente em `requests/` ou `environments/` que não esteja mais nesse conjunto.

Comportamento: isso mantém a collection sempre sincronizada com o código-fonte e com as configurações atuais, mas é uma operação destrutiva e silenciosa dentro de `writeCollection` — por isso a Feature de seleção/diff (`computeDiff`, exibido ao vivo dentro do `EndpointSelectionDialog`) foi criada para tornar essa remoção visível **antes** de acontecer.

Exceção/mitigação: `computeDiff` calcula o mesmo conjunto esperado/existente mas nunca escreve nem apaga nada — é somente leitura, propriedade coberta por teste.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:376-407` (`pruneStaleRequestFiles`, `pruneStaleEnvironmentFiles`), `24-49` (`computeDiff`). Classe: `BrunoWriter`.

Confirmado por teste: `src/test/kotlin/writer/BrunoWriterCollectionTest.kt`, testes `"regenerating prunes stale request files and removed environments"` e `"computeDiff reports added, removed and unchanged files without writing or deleting anything"`.

### REGRA: Seleção de endpoints é válida apenas para a execução atual

Descrição: desmarcar um endpoint no `EndpointSelectionDialog` faz com que ele seja excluído **apenas da geração atual** — não existe persistência de exclusões entre execuções.

Comportamento: se o usuário cancelar o diálogo (`showAndGet() == false`), nada é escrito em disco. Se confirmar com zero endpoints selecionados, a ação é abortada com uma notificação informativa, sem chamar `writeCollection`.

Fonte: `src/main/kotlin/GenerateBrunoCollectionAction.kt` (`continueAfterScan`). Classe: `GenerateBrunoCollectionAction`.

### REGRA: Diff é recalculado ao vivo a cada mudança de seleção

Descrição: o `EndpointSelectionDialog` é uma única tela (não dois diálogos em sequência) — a árvore de seleção e o painel de diff (added/removed/unchanged) ficam lado a lado, e o diff é recalculado automaticamente toda vez que o usuário marca/desmarca um endpoint na árvore.

Comportamento: um `CheckboxTreeListener` é registrado na árvore (`nodeStateChanged`); a cada disparo, `refreshDiff()` chama `BrunoWriter.computeDiff(...)` com os endpoints atualmente selecionados e substitui o conteúdo do painel de diff. O diff inicial é calculado uma vez no `init` do diálogo, com todos os endpoints pré-marcados. Um único botão OK confirma a seleção e dispara a escrita (`writeCollection`) com o que estiver selecionado naquele momento.

Fonte: `src/main/kotlin/ui/EndpointSelectionDialog.kt` (`init`, `refreshDiff`). Classe: `EndpointSelectionDialog`.

### REGRA: Ambiente padrão (`baseUrl`) resolvido com fallback em cascata

Descrição: a variável `baseUrl` gravada em `collection.bru` é resolvida assim: (1) o ambiente cujo `name` bate com `defaultEnvironmentName`; senão (2) o primeiro ambiente da lista; senão (3) o literal `"http://localhost:8080"`.

Fonte: `src/main/kotlin/settings/BrunoGeneratorSettings.kt:23-26` (`resolvedDefaultBaseUrl`). Confirmado por teste: `src/test/kotlin/settings/BrunoGeneratorStateTest.kt`.

### REGRA: Validação da tela de Settings

Descrição: ao salvar as configurações (`BrunoGeneratorConfigurable.apply()`), linhas da tabela de ambientes com `name` ou `baseUrl` em branco são descartadas silenciosamente antes de validar.

Condição de erro (`ConfigurationException`, impede salvar): lista de ambientes vazia após o filtro ("Add at least one environment with a name and base URL."); nomes de ambiente duplicados ("Environment names must be unique."); `defaultEnvironmentName` digitado não corresponde a nenhum ambiente configurado ("Default environment \"...\" does not match any configured environment name.").

Fonte: `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt:63-82` (`apply`). Classe: `BrunoGeneratorConfigurable`.

### REGRA: Diretório de saída — resolução de caminho

Descrição: se `outputDirectory` (Settings) estiver em branco, a collection é gerada na raiz do projeto. Se preenchido e for um caminho absoluto, é usado como está. Se for relativo, é resolvido a partir da raiz do projeto.

Fonte: `src/main/kotlin/GenerateBrunoCollectionAction.kt:103-108` (`resolveOutputRoot`). Classe: `GenerateBrunoCollectionAction`.

---

## 12. Fluxos de negócio

### Fluxo: Gerar collection Bruno

Objetivo: transformar os `@RestController`s de um projeto Spring Boot aberto na IDE em uma collection Bruno em disco, com confirmação do usuário.

Entrada: usuário aciona **Tools > Generate Bruno Collection**.

Passo 1 — Scan com progress bar (`Task.Backgroundable`, fora da EDT): `ControllerScanner.scan(project)` roda dentro de `runReadAction`, dentro de uma `Task.Backgroundable` com título "Scanning Spring controllers", que exibe uma barra de progresso indeterminada na status bar da IDE enquanto executa. `BrunoGeneratorSettings.getInstance(project).state` também é lido nesse passo. Se o scan lançar uma exceção, `onThrowable` (chamado na EDT) notifica "Failed to scan controllers" e encerra.

Passo 2 — `onSuccess` da task (EDT, automático): se `endpoints.isEmpty()`, notifica "No endpoints found" e encerra.

Passo 3 — Seleção + diff em uma única tela (EDT, modal): abre `EndpointSelectionDialog`, com os endpoints agrupados pelo primeiro segmento do path (`groupEndpointsByTopPath`), todos pré-marcados. Ao lado da árvore de seleção, um painel mostra o diff ("Added"/"Removed"/"Unchanged") calculado por `BrunoWriter.computeDiff(...)` a partir dos endpoints atualmente marcados; toda vez que o usuário marca/desmarca um endpoint, o diff é recalculado e o painel é atualizado (`CheckboxTreeListener.nodeStateChanged` → `refreshDiff()`). Um único botão OK confirma a seleção. Se o usuário cancelar, encerra sem escrever nada. Se confirmar com zero selecionados, notifica "Nothing selected" e encerra.

Passo 4 — Escrita com progress bar (`Task.Backgroundable`, título "Generating Bruno collection"): `BrunoWriter.writeCollection(...)` grava `bruno.json`, `collection.bru`, `environments/*.bru`, `requests/**/*.bru`, e remove arquivos obsoletos (prune). Em seguida, `LocalFileSystem.refreshAndFindFileByNioFile` atualiza a visão da IDE sobre os arquivos gravados.

Passo 5 — Notificação final (`onSuccess`/`onThrowable` da task, na EDT): sucesso ("Bruno collection generated", com contagem) ou erro ("Failed to generate Bruno collection", com a mensagem da exceção).

Resultado: pasta `<nome-do-projeto>/` (ou dentro do diretório de saída configurado) contendo a collection Bruno pronta para uso.

Erros possíveis: falha durante o scan (PSI); falha durante a escrita em disco (I/O); zero endpoints encontrados; zero endpoints selecionados; usuário cancela o diálogo.

Classes envolvidas: `GenerateBrunoCollectionAction`, `ControllerScanner`, `BrunoGeneratorSettings`, `EndpointSelectionDialog`, `BrunoWriter`.

Arquivos envolvidos: `src/main/kotlin/GenerateBrunoCollectionAction.kt`, `src/main/kotlin/scanner/ControllerScanner.kt`, `src/main/kotlin/ui/EndpointSelectionDialog.kt`, `src/main/kotlin/writer/BrunoWriter.kt`.

---

## 13. Autenticação e autorização

**Não aplicável ao plugin em si** (não há usuários, sessões ou perfis no plugin — ele roda com os privilégios do processo da IDE do usuário local).

Sobre a **saída gerada**: todo request `.bru` é gerado com `auth: none` fixo — o plugin não detecta nem configura nenhum mecanismo de autenticação (Bearer, Basic, OAuth) para os requests gerados.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:142`.

Não identificado no código analisado: qualquer leitura de anotações de segurança do Spring (`@PreAuthorize`, `@Secured`, Spring Security) pelo `ControllerScanner`.

---

## 14. Integrações externas

**Não aplicável em tempo de execução** — o plugin não faz chamadas de rede, não acessa bancos de dados, filas ou APIs de terceiros durante sua execução.

O que existe é integração **estrutural** com:

| Integração | Finalidade | Como é usada |
|---|---|---|
| IntelliJ Platform SDK (PSI, VFS, Actions, Settings, Notifications, UI DSL) | Ler o código do projeto aberto, registrar a ação de menu, persistir configurações, exibir diálogos/notificações | Usado em praticamente todas as classes `main`; ver `plugin.xml` para os extension points declarados |
| Bruno (formato `.bru`) | Formato de saída consumido por outro programa (o cliente Bruno) | O plugin apenas **escreve arquivos texto** no formato `.bru`; não há comunicação com o processo do Bruno |
| Spring Web / Bean Validation (anotações) | Fonte de metadados lidos via PSI no projeto-alvo | O plugin nunca importa ou executa código Spring; apenas compara `qualifiedName` de anotações com strings de FQN conhecidas (ver `companion object` de `ControllerScanner`) |

Fonte: `src/main/resources/META-INF/plugin.xml` (`<depends>com.intellij.modules.platform</depends>`, `<depends>com.intellij.modules.java</depends>`); `src/main/kotlin/scanner/ControllerScanner.kt` (constantes de FQN).

---

## 15. Banco de dados

**Não aplicável.** O plugin não usa nenhum banco de dados. A única persistência é a configuração do plugin, salva como XML pelo IntelliJ Platform (ver seção 10).

---

## 16. Configurações

### Tela de Settings (`Settings > Tools > Bruno Generator`)

Campos editáveis:
- **Tabela de ambientes** (`name`, `baseUrl`) — editor `TableModelEditor`, começa com um ambiente `dev` → `http://localhost:8080`.
- **Default environment name** — campo texto livre, deve bater com o `name` de algum ambiente configurado.
- **Output directory (optional)** — `TextFieldWithBrowseButton`, vazio = raiz do projeto.

Fonte: `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`, `src/main/kotlin/settings/EnvironmentsTable.kt`.

### Persistência

Arquivo: `bruno-generator.xml`, por projeto, via `@State(storages = [Storage("bruno-generator.xml")])`.

Fonte: `src/main/kotlin/settings/BrunoGeneratorSettings.kt:29`.

### Propriedades de build (`gradle.properties`)

| Propriedade | Valor |
|---|---|
| `pluginSinceBuild` | `253` |
| `platformVersion` | `2025.3.1` |
| `gradleVersion` | `9.7.0` |
| `kotlin.stdlib.default.dependency` | `false` |
| `org.gradle.configuration-cache` | `true` |
| `org.gradle.caching` | `true` |

Fonte: `gradle.properties`. Estas são configurações de **build/tooling**, não de aplicação em runtime.

### Variáveis de ambiente

Não identificado no código analisado: nenhuma leitura de variável de ambiente (`System.getenv`) foi encontrada no código de produção. `System.getProperty("java.home")` é usado apenas em código de **teste** (`ControllerScannerTest`, para localizar um JDK utilizável na sandbox de testes), não em código de produção.

### Segredos

Não identificado no código analisado: nenhuma credencial, token ou chave de API está presente no repositório. `[REDACTED]` não se aplica pois nada foi encontrado para redigir.

---

## 17. Tratamento de erros

Não há classes de exceção customizadas no projeto (`grep` por `class .*Exception` não retorna resultados fora de bibliotecas de terceiros).

Padrão usado: `runCatching { ... }.onSuccess { ... }.onFailure { ... }` em `GenerateBrunoCollectionAction`, em dois pontos:
1. Em torno do scan (`scanner.scan(project)`), na thread pooled.
2. Em torno da escrita (`writer.writeCollection(...)`), na thread pooled.

Em caso de falha, `t.message ?: "Unknown error"` é usado como conteúdo da notificação de erro (`NotificationType.ERROR`), sempre no grupo de notificação `"Bruno Generator"` declarado em `plugin.xml` (`<notificationGroup id="Bruno Generator" displayType="BALLOON" isLogByDefault="true"/>`).

Não há `@ControllerAdvice`/`@ExceptionHandler` (não é uma aplicação web). Não há mecanismo de retry ou fallback identificado no código.

Fonte: `src/main/kotlin/GenerateBrunoCollectionAction.kt:38-93`; `src/main/resources/META-INF/plugin.xml:19`.

---

## 18. Testes

| Arquivo | Framework | O que cobre |
|---|---|---|
| `src/test/kotlin/scanner/ControllerScannerTest.kt` | `BasePlatformTestCase` (testes de PSI, com sandbox de IDE em memória) | Extração de path/query/header/cookie params e corpo validado (`@NotBlank`, `@Email`, `@Min`/`@Max`, enum); objetos aninhados e listas (objeto e escalar) no corpo; guarda de recursão contra DTO auto-referenciado; detecção de `@RequestPart`/`MultipartFile` e `BodyKind.MULTIPART` |
| `src/test/kotlin/writer/BrunoWriterTest.kt` | JUnit 4 puro | Funções puras `internal`: `slug`, `toBrunoUrlPath`, `fakeValue` |
| `src/test/kotlin/writer/BrunoWriterCollectionTest.kt` | JUnit 4 puro (usa diretórios temporários reais) | `writeCollection` fim-a-fim (bruno.json, environments, request com params/headers/body/docs); prune de arquivos obsoletos na regeneração; diretório de saída customizado; renderização JSON aninhada/array real (não placeholder); bloco `multipart-form`; `computeDiff` (added/removed/unchanged, sem efeitos colaterais em disco) |
| `src/test/kotlin/settings/BrunoGeneratorStateTest.kt` | JUnit 4 puro | `resolvedDefaultBaseUrl` (ambiente padrão, fallback para o primeiro, fallback hardcoded); estado default (`dev`, `outputDirectory` vazio) |
| `src/test/kotlin/ui/EndpointSelectionDialogGroupingTest.kt` | JUnit 4 puro | Função pura `groupEndpointsByTopPath` (agrupamento por primeiro segmento do path, fallback `"root"`) — sem interação com Swing |

Observação registrada no próprio código/processo de desenvolvimento: não há testes diretos para `GenerateBrunoCollectionAction.actionPerformed` (fluxo de `AnAction`/EDT/diálogo modal não é testável com a infraestrutura de testes deste projeto); a cobertura desse fluxo vem das peças puras extraídas (`computeDiff`, `groupEndpointsByTopPath`).

Como rodar: `bash ./gradlew test --no-daemon` (documentado em `README.md`, seção "Desenvolvimento local").

Não identificado no código analisado: pipeline de CI (não há diretório `.github/workflows` nem outro arquivo de CI no repositório).

---

## 19. Jobs e processos assíncronos

Não há `@Scheduled`/jobs recorrentes (não é uma aplicação Spring). O plugin usa o **modelo de threading do IntelliJ Platform**, com feedback visual de progresso:

- `com.intellij.openapi.progress.Task.Backgroundable` — usado duas vezes em `GenerateBrunoCollectionAction`, cada uma como uma `object : Task.Backgroundable(project, title, canBeCancelled = false) { ... }.queue()`: uma para o scan de PSI ("Scanning Spring controllers"), outra para a escrita em disco ("Generating Bruno collection"). Ambas rodam `run(indicator: ProgressIndicator)` fora da EDT (mesmo mecanismo de thread pooled do `executeOnPooledThread`, mas com uma barra de progresso indeterminada visível na status bar da IDE enquanto a operação executa) — isso substitui o "travamento" perceptível sem feedback que existia antes.
- `onSuccess()`/`onThrowable(error)` de cada `Task.Backgroundable` — chamados automaticamente na EDT pelo próprio framework de `Task` ao final da execução, dispensando `invokeLater` manual. `onSuccess` segue o fluxo (abre o diálogo ou notifica sucesso); `onThrowable` notifica o erro.
- `ActionUpdateThread.BGT` — declarado em `GenerateBrunoCollectionAction.getActionUpdateThread()`, indicando que a atualização do estado da ação (habilitado/visível) roda em background thread, não na EDT.

Fonte: `src/main/kotlin/GenerateBrunoCollectionAction.kt`.

---

## 20. Cache

**Não aplicável em nível de aplicação** (o plugin não implementa nenhum cache de dados). As únicas menções a "cache" no repositório são de **tooling de build** (`org.gradle.caching=true`, `org.gradle.configuration-cache=true` em `gradle.properties`), não relacionadas ao comportamento do plugin em runtime.

---

## 21. Mensageria

**Não aplicável.** Não há filas, tópicos, Kafka, RabbitMQ ou qualquer mecanismo de mensageria no código.

---

## 22. Pontos críticos

- **Regeneração é destrutiva por padrão** (`pruneStaleRequestFiles`/`pruneStaleEnvironmentFiles` em `BrunoWriter.writeCollection`): qualquer `.bru` fora do conjunto esperado é apagado. Mitigado, desde a Feature de seleção/diff, pelo painel de diff ao vivo dentro do `EndpointSelectionDialog` (o usuário vê "Removed" antes de confirmar) — mas a exclusão em si continua acontecendo sem undo.
  Fonte: `src/main/kotlin/writer/BrunoWriter.kt:376-407`.

- **Limite de profundidade de recursão em corpos aninhados** (`MAX_NESTING_DEPTH = 5`): DTOs com grafos de objetos muito profundos (mas não cíclicos) são truncados na profundidade 5, sem aviso ao usuário no `.bru` gerado.
  Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:292`.

- **Auth sempre `none` nos requests gerados**: se a API-alvo exigir autenticação, o usuário precisa configurar manualmente no Bruno após a geração — o plugin não oferece essa configuração.
  Fonte: `src/main/kotlin/writer/BrunoWriter.kt:142`.

- **Compatibilidade de bytecode**: o projeto compila com toolchain JDK 26, mas força o bytecode para Java 21 (`options.release.set(21)`), porque o JetBrains Runtime empacotado no IntelliJ Platform 2025.3.1 é JDK 21 — se essa versão mudar, o ajuste em `build.gradle.kts` precisa ser revisado.
  Fonte: `build.gradle.kts`, comentário acima de `kotlin { jvmToolchain(26) }`.

- **Ambiente de testes de PSI sensível a JDK real**: os testes de `ControllerScannerTest` precisam de um JDK real e completo (via `JavaAwareProjectJdkTableImpl`/`ModuleRootModificationUtil.addModuleLibrary`, restrito ao módulo `java.base`) para resolver tipos como `java.util.List`/`java.lang.String` durante a recursão de corpo aninhado — sem isso, a resolução de tipos JDK falha silenciosamente (retorna `null`) e mascara bugs reais. Também foi observado que apontar o SDK de teste para a JBR completa do IDE (em vez de restringir a `java.base`) pode travar a indexação por causa de recursos de `jdk.javadoc` bundlados. [NECESSITA CONFIRMAÇÃO — comportamento observado durante o desenvolvimento desta sessão, não documentado oficialmente pela plataforma]
  Fonte: `src/test/kotlin/scanner/ControllerScannerTest.kt`.

- **Suporte a `Map<K,V>` no corpo**: campos do tipo `Map` resolvem para uma interface JDK e, portanto, nunca são expandidos recursivamente (ficam como campo escalar/placeholder) — comportamento consciente, não um bug, mas uma lacuna funcional.
  Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:188-192` (`isRecursableType` exclui interfaces).

---

## 23. Perguntas frequentes

### O que o Bruno Generator faz?

Resposta: gera uma collection do Bruno a partir dos controllers `@RestController` de um projeto Spring Boot aberto na IntelliJ IDEA, incluindo requests com parâmetros, corpo de exemplo e ambientes configuráveis.

Fonte: `README.md`; `src/main/resources/META-INF/plugin.xml`.

### Como eu gero a collection?

Resposta: abra o projeto Spring Boot na IDE, (opcionalmente) configure ambientes/diretório de saída em **Settings > Tools > Bruno Generator**, e execute **Tools > Generate Bruno Collection**. Uma única tela aparece com a seleção de endpoints e o diff (added/removed/unchanged) lado a lado, atualizado ao vivo conforme você marca/desmarca; ao confirmar com OK, a collection é gerada.

Fonte: `README.md`, seção "How to use"; `src/main/kotlin/GenerateBrunoCollectionAction.kt`.

### Por que meu controller não apareceu na collection?

Resposta possível, com base no código: (1) a classe não está anotada com `@RestController` (apenas essa anotação é procurada, não `@Controller`); (2) a classe está em uma dependência/biblioteca, não no código-fonte do projeto (o scan usa `projectScope`, não `allScope`); (3) o método não tem nenhuma das anotações de mapeamento reconhecidas (`@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/`@PatchMapping`/`@RequestMapping`); (4) você desmarcou o endpoint no diálogo de seleção antes de confirmar.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:31-45, 223-250`; `src/main/kotlin/ui/EndpointSelectionDialog.kt`.

### Os requests gerados já vêm com autenticação configurada?

Resposta: não. Todo request é gerado com `auth: none`; se a API exigir autenticação, é preciso configurar manualmente no Bruno.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:142`.

### Se eu rodar a geração de novo, meus requests manuais no Bruno são apagados?

Resposta: se um arquivo `.bru` existente dentro de `requests/` ou `environments/` não corresponder a nenhum endpoint/ambiente atual, ele é apagado na regeneração (comportamento de "prune"). Desde a introdução do painel de diff ao vivo, você vê essa lista de remoções antes de confirmar, mas a exclusão em si não tem undo.

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:376-407`; `src/main/kotlin/ui/EndpointSelectionDialog.kt`.

### O plugin suporta corpos de request com objetos aninhados e listas?

Resposta: sim. Objetos aninhados (até 5 níveis, com proteção contra referência cíclica) são renderizados como JSON real (não placeholder), e `List<T>`/arrays são renderizados como array JSON, com um elemento de exemplo. `Map<K,V>` não é expandido recursivamente.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:145-192`; `src/main/kotlin/writer/BrunoWriter.kt:220-263`.

### O plugin suporta upload de arquivo (`multipart/form-data`)?

Resposta: sim. Parâmetros anotados com `@RequestPart` ou do tipo `MultipartFile` (incluindo listas de `MultipartFile`) geram um bloco `body:multipart-form { }`, com `@file()` para partes de arquivo.

Fonte: `src/main/kotlin/scanner/ControllerScanner.kt:78-103`; `src/main/kotlin/writer/BrunoWriter.kt:271-278`.

### Onde a collection é gerada?

Resposta: em uma pasta com o nome do projeto, dentro da raiz do projeto por padrão, ou dentro do "Output directory" configurado em Settings (absoluto ou relativo à raiz do projeto).

Fonte: `src/main/kotlin/writer/BrunoWriter.kt:91-92` (`collectionName`); `src/main/kotlin/GenerateBrunoCollectionAction.kt:103-108` (`resolveOutputRoot`).

### Como rodar os testes do próprio plugin?

Resposta: `bash ./gradlew test --no-daemon`, a partir da raiz do repositório.

Fonte: `README.md`, seção "Desenvolvimento local".

---

## 24. Glossário

- **Bruno**: cliente de API open-source (usebruno.com), alternativa a Postman/Insomnia, cujos arquivos de collection são texto puro (`.bru`), pensados para versionamento em Git.
- **`.bru`**: formato de arquivo texto do Bruno, usado tanto para requests individuais quanto para ambientes e a própria collection.
- **Collection (Bruno)**: conjunto de requests, ambientes e configurações organizados em uma pasta, representando uma API.
- **PSI (Program Structure Interface)**: API do IntelliJ Platform para navegar/analisar a estrutura sintática do código-fonte de um projeto (classes, métodos, anotações etc.), usada pelo `ControllerScanner`.
- **`AnAction`**: classe base do IntelliJ Platform para uma ação de menu/atalho executável pelo usuário na IDE (aqui, `GenerateBrunoCollectionAction`).
- **`DialogWrapper`**: classe base do IntelliJ Platform para diálogos modais (aqui, `EndpointSelectionDialog`).
- **`PersistentStateComponent`**: interface do IntelliJ Platform para componentes cujo estado é automaticamente serializado/desserializado em XML (aqui, `BrunoGeneratorSettings`).
- **EDT (Event Dispatch Thread)**: thread de UI do Swing/IntelliJ; operações de UI (diálogos) precisam rodar nela; operações custosas (PSI, I/O) devem rodar fora dela.
- **`Endpoint`**: modelo de domínio interno do plugin representando um endpoint HTTP descoberto (não confundir com endpoint real de uma API).
- **`BodyKind`**: enum interno (`NONE`/`JSON`/`MULTIPART`) que classifica o tipo de corpo de um `Endpoint`.
- **Prune**: nome informal, usado neste documento e nos testes, para o processo de remoção de arquivos `.bru` obsoletos durante a regeneração.

---

## 25. Mapa de arquivos importantes

Arquivo: `src/main/kotlin/GenerateBrunoCollectionAction.kt`
Responsabilidade: ponto de entrada da funcionalidade (ação de menu); orquestra scan (com progress bar) → seleção+diff (tela única) → escrita (com progress bar) → notificação.
Principais classes: `GenerateBrunoCollectionAction`; duas `object : Task.Backgroundable` anônimas (scan e escrita).
Principais métodos: `actionPerformed`, `continueAfterScan`, `notify`, `resolveOutputRoot`.
Dependências: `ControllerScanner`, `BrunoGeneratorSettings`, `EndpointSelectionDialog`, `BrunoWriter`, `com.intellij.openapi.progress.Task`/`ProgressIndicator`.
Regras implementadas: seleção válida só para a execução atual; resolução do diretório de saída; feedback de progresso via `Task.Backgroundable` durante scan e escrita.

Arquivo: `src/main/kotlin/scanner/ControllerScanner.kt`
Responsabilidade: ler o projeto via PSI e produzir `List<Endpoint>`.
Principais classes: `ControllerScanner`.
Principais métodos: `scan`, `extractEndpoints`, `methodMapping`, `extractParams`, `extractFields`/`toRequestBodyField`, `collectionElementType`, `multipartParts`/`toMultipartPart`, `extractConstraints`, `normalizePath`.
Dependências: IntelliJ PSI API (`JavaPsiFacade`, `AnnotatedElementsSearch`, etc.); modelo `com.codeteam.model`.
Regras implementadas: quase todas as regras de "descoberta" da seção 11 (mapeamento de verbo, composição de path, recursão de corpo, coleções, multipart, Bean Validation).

Arquivo: `src/main/kotlin/writer/BrunoWriter.kt`
Responsabilidade: gerar os arquivos `.bru` a partir de `List<Endpoint>`; calcular diff sem efeitos colaterais.
Principais classes: `BrunoWriter`, `CollectionDiff` (top-level data class no mesmo arquivo).
Principais métodos: `writeCollection`, `computeDiff`, `requestContent`, `jsonObject`/`renderObject`/`renderFieldValue`/`renderArrayValue`, `multipartFormBlock`, `fakeValue`, `pruneStaleRequestFiles`/`pruneStaleEnvironmentFiles`.
Dependências: `com.codeteam.model`, `com.codeteam.settings`.
Regras implementadas: quase todas as regras de "geração" da seção 11 (fakes, indentação/estrutura JSON, multipart, prune, organização de pastas).

Arquivo: `src/main/kotlin/model/Endpoint.kt`
Responsabilidade: modelos de domínio imutáveis compartilhados entre scanner e writer.
Principais classes: `Endpoint`, `RequestBodyField`, `ValidationConstraints`, `RequestParameter`, `ParamKind`, `BodyKind`, `MultipartPart`.
Dependências: nenhuma (apenas Kotlin stdlib).

Arquivo: `src/main/kotlin/settings/BrunoGeneratorSettings.kt`
Responsabilidade: estado persistente do plugin (ambientes, ambiente padrão, diretório de saída).
Principais classes: `BrunoGeneratorState`, `BrunoEnvironment`, `BrunoGeneratorSettings`.
Principais métodos: `resolvedDefaultBaseUrl` (extension function).
Dependências: IntelliJ Platform (`PersistentStateComponent`, `@Service`, `@State`).

Arquivo: `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`
Responsabilidade: tela de Settings (Tools > Bruno Generator).
Principais classes: `BrunoGeneratorConfigurable`.
Principais métodos: `createComponent`, `isModified`, `apply`, `reset`.
Dependências: `BrunoGeneratorSettings`, `EnvironmentsTable`.
Regras implementadas: validação da tela de Settings (seção 11).

Arquivo: `src/main/kotlin/ui/EndpointSelectionDialog.kt`
Responsabilidade: diálogo modal único de seleção de endpoints com painel de diff (added/removed/unchanged) ao vivo, antes de gerar.
Principais classes: `EndpointSelectionDialog`.
Principais métodos: `init` (monta a árvore + registra `CheckboxTreeListener`), `refreshDiff` (recalcula `CollectionDiff` a cada mudança de seleção), `selectedEndpoints`, `createCenterPanel` (monta o `JSplitPane` com árvore + painel de diff).
Função top-level: `groupEndpointsByTopPath`.
Dependências: `com.codeteam.model.Endpoint`, `com.codeteam.settings.BrunoGeneratorState`, `com.codeteam.writer.BrunoWriter`/`CollectionDiff`, IntelliJ `CheckboxTree`/`DialogWrapper`/UI DSL (`panel { }`).
Regras implementadas: diff recalculado ao vivo a cada mudança de seleção (seção 11).

Arquivo: `src/main/resources/META-INF/plugin.xml`
Responsabilidade: descritor do plugin — id, dependências de plataforma, extension points (notificationGroup, projectConfigurable), registro da ação de menu.
Regras implementadas: escopo de compatibilidade (`sinceBuild`), grupo de notificação usado por `GenerateBrunoCollectionAction`.

---

## 26. Lacunas de conhecimento

- Não identificado no código analisado: pipeline de CI/CD (não há `.github/workflows` nem outro arquivo de CI no repositório fornecido).
- Não identificado no código analisado: processo real de publicação no JetBrains Marketplace (o `README.md` descreve os passos manuais, mas não há evidência de automação nem de credenciais/tokens de publicação no repositório).
- Não identificado no código analisado: métricas de uso, downloads ou adoção do plugin.
- Não identificado no código analisado: roadmap ou backlog além do que está descrito no `README.md` e no histórico de commits (`changeNotes` em `build.gradle.kts`).
- [NECESSITA CONFIRMAÇÃO] O comportamento de indexação de SDKs reais em testes de PSI (`ControllerScannerTest`), incluindo a falha observada ao apontar para a JBR completa do IDE (erro de indexação de `jdk.javadoc`), foi determinado empiricamente durante o desenvolvimento desta sessão e documentado como comentário no próprio teste — não há documentação oficial da JetBrains citada no repositório confirmando esse comportamento como esperado/estável entre versões da plataforma.
- Não identificado no código analisado: qualquer suporte a frameworks reativos (WebFlux) além do que já é coberto genericamente pelas mesmas anotações Spring MVC (o scanner não distingue `Mono`/`Flux` como tipos especiais de retorno, e não foi testado contra eles).
- Não identificado no código analisado: suporte a `@RequestMapping` combinando múltiplos verbos HTTP em uma única anotação (o código escolhe apenas um verbo, pela primeira substring encontrada).

---

# RESUMO EXECUTIVO

## Resumo da arquitetura

Plugin IntelliJ IDEA (Kotlin) com fluxo linear: `AnAction` (entrada) → `ControllerScanner` (leitura PSI do projeto-alvo → modelo de domínio em memória) → `EndpointSelectionDialog` (tela única de seleção de endpoints com painel de diff ao vivo, para filtragem/confirmação pelo usuário) → `BrunoWriter` (geração de arquivos `.bru` em disco). Configuração do plugin é persistida à parte via `PersistentStateComponent` (`BrunoGeneratorSettings`) em XML por projeto. Não há camada de rede, banco de dados ou API própria — o "backend" é o próprio processo da IDE, e o "frontend" é o diálogo Swing/IntelliJ UI DSL.

## Principais regras de negócio

1. Apenas classes `@RestController` do **código-fonte do projeto** (não de bibliotecas) são escaneadas.
2. Verbo HTTP vem de `@GetMapping`/`@PostMapping`/.../`@RequestMapping(method=...)`; sem nenhuma dessas, o método não vira endpoint.
3. Corpo de request é extraído recursivamente para objetos aninhados (limite de 5 níveis + guarda de ciclo) e coleções (`List`/array), com `Map` explicitamente fora de escopo.
4. Multipart (`@RequestPart`/`MultipartFile`) gera `body:multipart-form` em vez de `body:json`.
5. Valores de exemplo são escolhidos por heurística de nome+tipo do campo, com placeholders dinâmicos do Bruno apenas em POST/PUT/PATCH.
6. Regenerar a collection **apaga** arquivos `.bru` que não correspondem mais ao estado atual (prune) — mitigado por um diálogo de diff antes da escrita.
7. Todo request gerado tem `auth: none` fixo; nenhuma autenticação é configurada automaticamente.
8. Seleção de endpoints feita pelo usuário no diálogo vale só para a execução atual, sem persistência.

## Principais "APIs" (ação da IDE)

- **Tools > Generate Bruno Collection** (`GenerateBrunoCollectionAction`) — única ação exposta pelo plugin; não há REST APIs neste repositório.
- **Settings > Tools > Bruno Generator** (`BrunoGeneratorConfigurable`) — tela de configuração (ambientes, ambiente padrão, diretório de saída).

## Principais integrações

- IntelliJ Platform SDK (PSI, VFS, Actions, Notifications, Settings, UI DSL) — estrutural, não é chamada de rede.
- Leitura de anotações Spring Web / Bean Validation via comparação de FQN por PSI — não há dependência de runtime do Spring.
- Formato de saída `.bru`, consumido externamente pelo aplicativo Bruno — sem comunicação direta entre o plugin e o Bruno.

## Lacunas de conhecimento

- Ausência de CI/CD documentado no repositório.
- Processo de publicação no Marketplace descrito apenas manualmente no `README.md`.
- Comportamento de indexação de SDK em testes de PSI, registrado como observação empírica desta sessão, sem confirmação em documentação oficial.
- Suporte a WebFlux (`Mono`/`Flux`) e a `@RequestMapping` multi-verbo não foi testado/implementado explicitamente.

## Informações que não puderam ser determinadas pelo código

- Métricas de uso/adoção do plugin.
- Roadmap além do `README.md`/`changeNotes`.
- Credenciais ou processo automatizado de publicação.
- Qualquer variável de ambiente de runtime (nenhuma foi encontrada em código de produção).

## Documentos adicionais que melhorariam a KB

- Arquivo de CI/CD (se existir fora deste repositório) descrevendo como `test`/`verifyPlugin`/`buildPlugin`/`publishPlugin` são executados automaticamente.
- Documentação do processo de release/versionamento (hoje só inferível pelo histórico de commits e pelo `changeNotes` em `build.gradle.kts`).
- Um changelog dedicado (`CHANGELOG.md`), hoje embutido apenas como `changeNotes` no `build.gradle.kts`.
- Issues/roadmap público (GitHub Issues ou similar), se existir, para complementar a seção de pontos críticos e lacunas.
