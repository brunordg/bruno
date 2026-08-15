# Bruno Generator

**ENGLISH**

Bruno Generator helps Spring Boot developers quickly create a [Bruno](https://www.usebruno.com/) collection from existing controllers.

The plugin scans Spring MVC endpoints, detects HTTP methods, paths, and request body parameters, and generates a ready-to-use Bruno collection with organized folders, request files, and example payloads. It also creates collection-level `baseUrl` variables so you can easily point requests to your local or remote environments.

## Features

- Automatically discovers Spring controller endpoints
- Generates Bruno requests from your API structure
- Creates example JSON bodies with smart sample values
- Organizes requests by route path for easier navigation
- Configurable environments (Settings > Tools > Bruno Generator) — starts with a single `dev` environment, and you can add as many as you need
- Optional output directory setting, so the collection can be generated outside the project root
- Reduces repetitive manual setup when documenting or testing APIs

## How to use

1. Open your Spring Boot project in IntelliJ IDEA.
2. (Optional) Go to **Settings > Tools > Bruno Generator** to add/edit environments, pick the default one, and set a custom output directory. Leave the output directory empty to generate the collection in the project root.
3. Run **Tools > Generate Bruno Collection**.
4. Open the generated collection folder (named after your project) in Bruno.
5. Switch environments in Bruno, or edit the `baseUrl` variable directly, to point requests at your local or remote API.

Re-running the action regenerates the collection in place: requests and environments that no longer exist in your code or settings are removed automatically, so the collection always stays in sync.

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
- Organiza os requests por rota para facilitar a navegação
- Ambientes configuráveis (Settings > Tools > Bruno Generator) — começa com um único ambiente `dev`, e você pode adicionar quantos precisar
- Diretório de saída opcional, permitindo gerar a collection fora da raiz do projeto
- Reduz o trabalho manual repetitivo na documentação e no teste de APIs

## Como usar

1. Abra o seu projeto Spring Boot no IntelliJ IDEA.
2. (Opcional) Vá em **Settings > Tools > Bruno Generator** para adicionar/editar ambientes, escolher o ambiente padrão e definir um diretório de saída customizado. Deixe o diretório de saída vazio para gerar a collection na raiz do projeto.
3. Execute **Tools > Generate Bruno Collection**.
4. Abra a pasta da collection gerada (com o nome do seu projeto) no Bruno.
5. Troque de ambiente no Bruno, ou edite a variável `baseUrl` diretamente, para apontar os requests para a API local ou remota.

Executar a ação novamente regenera a collection no lugar: requests e ambientes que não existem mais no seu código ou nas configurações são removidos automaticamente, mantendo a collection sempre sincronizada.

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
