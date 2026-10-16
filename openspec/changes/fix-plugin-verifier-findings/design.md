# Design

## Context

Achados do Plugin Verifier (IU-263.7031.23), reproduzidos localmente com `verifyPlugin`:

| Tipo | API | Onde |
|---|---|---|
| Marcada para remoção | `SimpleListCellRenderer.create(String, Function)` | `BrunoGeneratorConfigurable.createComponent` |
| Override-only | `Navigatable.navigate(boolean)` | `EndpointSelectionDialog.navigateToHandler` |
| Experimental | `SegmentedButton.whenItemSelected` | `EndpointSelectionDialog.createNorthPanel` |

## Decisions

- **Renderer do combo de auth:** `textListCellRenderer { it?.label }` (`com.intellij.ui.dsl.listCellRenderer`). `SimpleListCellRenderer.create(Customizer)` também está depreciado no 263, então não serve como alternativa.
- **Navegação:** `PsiNavigateUtil.navigate(method)`, mantendo o `canNavigate()` antes. Alternativas descartadas: no 263, `FileEditorManager.openTextEditor`, `openEditor(OpenFileDescriptor, …)` e `openFileEditor` estão depreciados, e os substitutos (`requestOpenEditor`/`requestOpenTextEditor`) não existem na 2025.3 (since-build).
- **Segmented button:** o estado passa para `AtomicProperty<FolderStrategy>` ligada via `segmentedButton(...).bind(property)`, e `property.afterChange(disposable) { refresh() }` atualiza o diff. `selectStrategy` (usado em testes) passa a escrever na propriedade.
- **Build:** `copyPluginToRoot` vira uma task com `doLast` copiando o `archiveFile` do `buildPlugin`, sem declarar `rootDir` como output.

## Risks

- O `verifyPlugin` roda contra a 2025.3.1 (`platformVersion`) e a IU-263.7031.23 (a do relatório do Marketplace).
