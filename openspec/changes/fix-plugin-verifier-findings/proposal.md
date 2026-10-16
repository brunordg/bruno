# Proposal

## Why

O JetBrains Plugin Verifier, rodando o Bruno 1.5.0 contra o IU-263.7031.23, apontou três problemas de compatibilidade futura: uso de API marcada para remoção, chamada de método `@ApiStatus.OverrideOnly` e uso de API experimental. O plugin continua "Compatible", mas essas chamadas podem quebrar em versões futuras da IDE e aparecem como avisos no Marketplace.

## What Changes

- `SimpleListCellRenderer.create(String, Function)` (marcado para remoção) em `BrunoGeneratorConfigurable` é substituído por `textListCellRenderer` do Kotlin UI DSL.
- A chamada direta de `Navigatable.navigate(boolean)` (override-only) em `EndpointSelectionDialog.navigateToHandler` é substituída por `FileEditorManager.openEditor(OpenFileDescriptor, true)`.
- `SegmentedButton.whenItemSelected` (experimental) é substituído pela ligação do segmented button a uma `AtomicProperty` (`bind`) com `afterChange`.
- Build: a task `copyPluginToRoot` deixa de ser uma `Copy` com destino `rootDir` (que conflitava com `test`/`verifyPlugin` na validação do Gradle), e `pluginVerification` passa a declarar a IDE alvo para rodar `verifyPlugin` localmente.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. O comportamento visível não muda; é uma refatoração de compatibilidade e de tooling (`skip_specs: true`).

## Impact

- `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`
- `src/main/kotlin/ui/EndpointSelectionDialog.kt`
- `build.gradle.kts`
- Requisitos preservados: "Folder strategy uses a segmented control" e "Navigate to handler source" (endpoint-selection), "Few-option settings are shown as radio buttons" e o combo de autenticação (generator-settings).
