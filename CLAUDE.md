# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Bruno Generator is an **IntelliJ IDEA plugin written in Kotlin**, not a Spring Boot app. It reads the PSI of the Spring Boot project open in the IDE and generates a [Bruno](https://www.usebruno.com/) collection from its `@RestController` endpoints. The user runs it from **Tools > Generate Bruno Collection**. `knowledge-base.md` (in Portuguese) is a detailed architecture reference.

## Commands

```bash
bash ./gradlew test --no-daemon                         # all tests
bash ./gradlew test --no-daemon --tests 'writer.BrunoWriterTest'            # one class
bash ./gradlew test --no-daemon --tests 'writer.BrunoWriterTest.slug*'      # one method (pattern)
bash ./gradlew runIde --no-daemon                       # sandbox IDE with the plugin loaded
bash ./gradlew clean buildPlugin --no-daemon            # distributable zip → build/distributions/
```

Build toolchain: the build runs on JDK 25, but the bytecode must stay at **Java 21**, because IntelliJ Platform 2025.3.1 (`platformVersion` in `gradle.properties`) runs on JBR 21. The Kotlin `jvmTarget` override sits in `afterEvaluate` on purpose, so don't move it out. Bump `version` in `build.gradle.kts` for each release.

## Architecture

Pipeline (`src/main/kotlin`, Kotlin packages are `com.codeteam.*` even though the directories are not nested that way):

1. `GenerateBrunoCollectionAction` is the entry point (`AnAction`, `DumbAware`). It loads the settings, runs the scan, shows the dialog and calls the writer.
2. `scanner/ControllerScanner` walks the PSI and returns a `List<Endpoint>` (`model/Endpoint.kt`). It extracts HTTP method/path, path/query/header/cookie params, JSON or multipart bodies (nested objects, lists), Bean Validation constraints, Jackson annotations (`@JsonProperty`, `@JsonIgnore`, `READ_ONLY`) and springdoc/OpenAPI 3 metadata (`@Operation`, `@Parameter`, `@Schema`).
3. `ui/EndpointSelectionDialog` is a single dialog for picking endpoints, with a live diff of added/removed/kept files that updates when the user changes the per-run folder strategy.
4. `writer/BrunoWriter` renders the `.bru` files (`bruno.json`, `collection.bru`, `environments/`, `requests/`) and computes the `CollectionDiff`. It owns stable file naming (method + path slug), folder strategy layout, example value generation, and the overwrite vs add-only regeneration logic.
5. `settings/` holds the persisted project-level state (`BrunoGeneratorState`): environments, default env, output dir, `RegenerationMode`, `AuthMode`, `FolderStrategy`. `BrunoGeneratorConfigurable` is the Settings > Tools page.

Registration (action, configurable, service) lives in `src/main/resources/META-INF/plugin.xml`. User-facing strings go in `messages/MyMessageBundle.properties`.

## Tests

- Writer and settings tests are plain JUnit4 and call `BrunoWriter` functions directly.
- `ControllerScannerTest` uses `BasePlatformTestCase` with real Spring, Jackson, Validation and Swagger annotation jars attached to the fixture. Scanner changes need fixture-based tests.

## Conventions

- Changes follow the **OpenSpec** workflow: specs live in `openspec/specs/`, active changes in `openspec/changes/`, and finished ones are archived to `openspec/changes/archive/`. Use the `opsx:*` / `openspec-*` skills.
- Commit messages and docs are in Portuguese. `README.md` is bilingual (EN + PT), so update both halves together.
- Feature work goes on a `feat/...` branch and is merged into `main` with a merge commit.
