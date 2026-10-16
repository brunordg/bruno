## 1. Build

- [x] 1.1 Configurar `pluginVerification` com as IDEs 2025.3.1 e IU-263.7031.23 e reproduzir os 3 achados com `verifyPlugin`
- [x] 1.2 Reescrever `copyPluginToRoot` sem declarar `rootDir` como output

## 2. Correções

- [x] 2.1 Trocar `SimpleListCellRenderer.create` por `textListCellRenderer` em `BrunoGeneratorConfigurable`
- [x] 2.2 Trocar `method.navigate(true)` por `PsiNavigateUtil.navigate(method)` em `navigateToHandler`
- [x] 2.3 Trocar `whenItemSelected` por `bind(AtomicProperty)` + `afterChange` no segmented button

## 3. Validação

- [x] 3.1 `./gradlew test` passando (incluindo `EndpointSelectionDialogTest`: troca de estratégia e navegação)
- [x] 3.2 `./gradlew verifyPlugin` sem usos de API deprecated/removida, experimental ou override-only
- [x] 3.3 `./gradlew clean buildPlugin` gerando o zip
