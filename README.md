# Bruno Generator

**ENGLISH**

Bruno Generator helps Spring Boot developers quickly create a [Bruno](https://www.usebruno.com/) collection from existing controllers.

The plugin scans Spring MVC endpoints, detects HTTP methods, paths, and request body parameters, and generates a ready-to-use Bruno collection with organized folders, request files, and example payloads. It also creates collection-level `baseUrl` variables so you can easily point requests to your local or remote environments.

## Features

- Automatically discovers Spring controller endpoints
- Generates Bruno requests from your API structure
- Creates example JSON bodies with smart sample values
- Respects Jackson annotations on request bodies: `@JsonProperty` renames keys and `@JsonIgnore` / `access = READ_ONLY` fields are omitted (class-level `@JsonNaming` and `@JsonInclude` are not yet supported)
- Enriches requests from springdoc / OpenAPI 3 annotations: `@Operation(summary)` names the request, `@Operation`/`@Parameter`/`@Schema` descriptions fill the `docs` block, and `@Schema(example)` sets the field value (taking precedence over validation and heuristic fakes). Swagger v2 and `@Tag`-based grouping are not covered.
- Configurable folder strategy for organizing requests — **By URL path** (default, nests folders mirroring the endpoint path), **By controller** (one folder per controller class), or **Flat** (all requests directly under `requests/`); pick it per generation in the dialog or set the persisted default in Settings
- Configurable environments (Settings > Tools > Bruno Generator) — starts with a single `dev` environment, and you can add as many as you need
- Configurable authentication (None, Bearer, Basic, API Key): sets collection-level auth that every request inherits, with credential variables (`token` / `username`+`password` / `apiKey`) provisioned per environment for you to fill in (per-endpoint auth exceptions and Spring Security detection are out of scope)
- Optional output directory setting, so the collection can be generated outside the project root
- Reduces repetitive manual setup when documenting or testing APIs

## How to use

1. Open your Spring Boot project in IntelliJ IDEA.
2. (Optional) Go to **Settings > Tools > Bruno Generator** to add/edit environments, pick the default one, and set a custom output directory. Leave the output directory empty to generate the collection in the project root.
3. Run **Tools > Generate Bruno Collection**.
4. Open the generated collection folder (named after your project) in Bruno.
5. Switch environments in Bruno, or edit the `baseUrl` variable directly, to point requests at your local or remote API.

Re-running the action regenerates the collection in place. Request files have **stable names** derived from the endpoint's method and path, so selecting a different subset no longer renumbers files (expect a one-time rename the first time you regenerate after upgrading to this version).

Two **regeneration modes** are available (Settings > Tools > Bruno Generator):

- **Overwrite** (default): rewrites all selected requests and removes request files and environments that no longer exist in your code or settings, keeping the collection fully in sync.
- **Add-only**: only creates request files that don't exist yet, never overwriting or deleting — so any manual edits to generated requests are preserved.

Three **folder strategies** control how request files are grouped. Set a persisted default in **Settings > Tools > Bruno Generator**, or override it per generation from the dialog (the diff preview updates as you switch):

- **By URL path** (default): nests folders mirroring the endpoint path (e.g. `GET /api/widgets/{id}` → `requests/api/widgets/id/`).
- **By controller**: one folder per declaring controller class (e.g. `WidgetController` → `requests/widget-controller/`).
- **Flat**: all request files directly under `requests/`, with no subfolders.

File names stay stable within a strategy; expect a one-time folder reshuffle the first time you regenerate after changing the strategy.

## Why use it?

Bruno Generator is ideal for teams that want to move faster when exploring, documenting, or testing Spring-based REST APIs with Bruno.

## Requirements

- IntelliJ IDEA
- Java / Kotlin project
- Spring Boot application with annotated controllers

## Output

The plugin generates a Bruno collection, in a folder named after your project, with:

- `bruno.json`
- `collection.bru`
- `environments/` — one `.bru` file per configured environment
- `requests/` — request files grouped by endpoint path, with example payloads for `POST`, `PUT`, and `PATCH` requests

---

**PORTUGUÊS**

O Bruno Generator ajuda desenvolvedores Spring Boot a criar rapidamente uma collection do [Bruno](https://www.usebruno.com/) a partir de controllers já existentes.

O plugin analisa endpoints Spring MVC, identifica métodos HTTP, rotas e parâmetros de corpo, e gera uma collection do Bruno pronta para uso, com pastas organizadas, arquivos de request e exemplos de payloads. Ele também cria variáveis `baseUrl` no nível da collection, facilitando o apontamento para ambientes local ou remoto.

## Funcionalidades

- Descobre automaticamente endpoints de controllers Spring
- Gera requests do Bruno a partir da estrutura da sua API
- Cria bodies JSON de exemplo com valores inteligentes
- Respeita anotações Jackson no request body: `@JsonProperty` renomeia as chaves e campos `@JsonIgnore` / `access = READ_ONLY` são omitidos (`@JsonNaming` no nível da classe e `@JsonInclude` ainda não são suportados)
- Enriquece os requests com anotações springdoc / OpenAPI 3: `@Operation(summary)` nomeia o request, descrições de `@Operation`/`@Parameter`/`@Schema` preenchem o bloco `docs`, e `@Schema(example)` define o valor do campo (com precedência sobre validação e fakes heurísticos). Swagger v2 e agrupamento por `@Tag` não são cobertos.
- Estratégia de agrupamento configurável para organizar os requests — **Por rota (URL)** (padrão, aninha pastas espelhando a rota do endpoint), **Por controller** (uma pasta por classe de controller) ou **Plano** (todos os requests direto em `requests/`); escolha por geração no diálogo ou defina o padrão persistido nas Configurações
- Ambientes configuráveis (Settings > Tools > Bruno Generator) — começa com um único ambiente `dev`, e você pode adicionar quantos precisar
- Autenticação configurável (None, Bearer, Basic, API Key): define a auth no nível da collection que todo request herda, com variáveis de credencial (`token` / `username`+`password` / `apiKey`) provisionadas por ambiente para você preencher (exceções de auth por endpoint e detecção de Spring Security estão fora do escopo)
- Diretório de saída opcional, permitindo gerar a collection fora da raiz do projeto
- Reduz o trabalho manual repetitivo na documentação e no teste de APIs

## Como usar

1. Abra o seu projeto Spring Boot no IntelliJ IDEA.
2. (Opcional) Vá em **Settings > Tools > Bruno Generator** para adicionar/editar ambientes, escolher o ambiente padrão e definir um diretório de saída customizado. Deixe o diretório de saída vazio para gerar a collection na raiz do projeto.
3. Execute **Tools > Generate Bruno Collection**.
4. Abra a pasta da collection gerada (com o nome do seu projeto) no Bruno.
5. Troque de ambiente no Bruno, ou edite a variável `baseUrl` diretamente, para apontar os requests para a API local ou remota.

Executar a ação novamente regenera a collection no lugar. Os arquivos de request têm **nomes estáveis** derivados do método e da rota do endpoint, então selecionar um subconjunto diferente não renumera mais os arquivos (espere uma renomeação única na primeira regeneração após atualizar para esta versão).

Há dois **modos de regeneração** (Settings > Tools > Bruno Generator):

- **Overwrite** (padrão): reescreve todos os requests selecionados e remove requests e ambientes que não existem mais no código ou nas configurações, mantendo a collection sincronizada.
- **Add-only**: apenas cria arquivos de request que ainda não existem, nunca sobrescrevendo nem removendo — preservando qualquer edição manual feita nos requests gerados.

Três **estratégias de agrupamento** controlam como os arquivos de request são organizados. Defina o padrão persistido em **Settings > Tools > Bruno Generator**, ou sobrescreva por geração no diálogo (o preview do diff atualiza ao trocar):

- **Por rota (URL)** (padrão): aninha pastas espelhando a rota do endpoint (ex.: `GET /api/widgets/{id}` → `requests/api/widgets/id/`).
- **Por controller**: uma pasta por classe de controller declarante (ex.: `WidgetController` → `requests/widget-controller/`).
- **Plano**: todos os arquivos de request direto em `requests/`, sem subpastas.

Os nomes dos arquivos permanecem estáveis dentro de cada estratégia; espere um rearranjo único de pastas na primeira regeneração após trocar a estratégia.

## Por que usar?

O Bruno Generator é ideal para equipes que querem ganhar velocidade ao explorar, documentar ou testar APIs REST baseadas em Spring com Bruno.

## Requisitos

- IntelliJ IDEA
- Projeto Java ou Kotlin
- Aplicação Spring Boot com controllers anotados

## Saída gerada

O plugin gera uma collection do Bruno, em uma pasta com o nome do seu projeto, contendo:

- `bruno.json`
- `collection.bru`
- `environments/` — um arquivo `.bru` por ambiente configurado
- `requests/` — arquivos de request agrupados por caminho da rota, com payloads de exemplo para requests `POST`, `PUT` e `PATCH`

---

## Desenvolvimento local

Use os comandos abaixo na raiz do projeto:

```bash
bash ./gradlew test --no-daemon
bash ./gradlew runIde --no-daemon
```

## Build para distribuição

```bash
bash ./gradlew clean buildPlugin --no-daemon
```

Artefato gerado em:

- `build/distributions/`

## Publicação no JetBrains Marketplace

1. Ajuste metadados em `src/main/resources/META-INF/plugin.xml` (`id`, `name`, `vendor`, `description`).
2. Garanta o ícone em `src/main/resources/META-INF/pluginIcon.svg`.
3. Execute validações:

```bash
bash ./gradlew test verifyPlugin --no-daemon
```

4. Gere o pacote com `buildPlugin`.
5. Publique manualmente no Marketplace ou use `publishPlugin` com token configurado.

## Arquivos principais

- `src/main/kotlin/GenerateBrunoCollectionAction.kt`
- `src/main/kotlin/scanner/ControllerScanner.kt`
- `src/main/kotlin/writer/BrunoWriter.kt`
- `src/main/kotlin/settings/BrunoGeneratorSettings.kt`
- `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`
- `src/main/resources/META-INF/plugin.xml`
