# Design

## Context

A UI usa Swing com componentes da plataforma IntelliJ: `CheckboxTree` no diálogo e Kotlin UI DSL nas Settings. O scan e a escrita já rodam em `Task.Backgroundable`. `Endpoint` é um data class puro, sem referência a PSI, e já carrega `controllerName` e `handlerName`. `BrunoWriter.computeDiff` devolve `CollectionDiff(added, removed, unchanged)` como listas de caminhos relativos. O nome da collection é `prefix + (collectionName | nome do projeto) + suffix`, calculado em `resolveCollectionName`.

## Goals / Non-Goals

**Goals:**
- Visual nativo, que funcione nos temas claro e escuro sem cores hardcoded fora de `JBColor`.
- Extrair a lógica de apresentação em funções puras testáveis com JUnit4: agrupamento, filtro, rótulo do botão, árvore de diff e preview do nome.

**Non-Goals:**
- Tema ou estilo próprio, além de componentes customizados pintados à mão.
- Alterar `BrunoWriter`, o formato `.bru` ou o estado persistido.

## Decisions

1. **Agrupamento por controller, fixo.** A árvore representa o código (como o dev pensa) e o diff representa o disco (a estrutura que a folder strategy produz). Seguir a strategy na árvore duplicaria o preview, e o modo Flat ficaria sem grupos. O grupo usa o nome simples do controller; o mesmo nome em pacotes diferentes resolve-se com o sufixo do pacote.

2. **Cores do método via `JBColor` com par claro/escuro**, aplicadas por `SimpleTextAttributes` no `CheckboxTreeCellRenderer`. Handler em `GRAYED_ATTRIBUTES`. Alternativa descartada: badges pintados, mais trabalho e destoam do IDE.

3. **Filtro como modelo reconstruído.** O filtro reconstrói os nós visíveis da `CheckboxTree` a partir de um mapa `Endpoint -> selecionado` mantido fora da árvore. Assim a seleção de itens ocultos sobrevive ao filtro (requisito da spec). `selectedEndpoints()` lê do mapa, não da árvore. Alternativa descartada: `FilteringTree` ou speed search. O speed search só destaca e não filtra; a `FilteringTree` complica o estado do checkbox.

4. **Segmented control com `segmentedButton` da Kotlin UI DSL** para a folder strategy no diálogo e radio (`buttonsGroup`) nas Settings. São controles diferentes de propósito: no diálogo a troca é frequente e compacta, nas Settings cabe uma descrição por opção.

5. **Diff em árvore com `SimpleTree` ou `Tree`**, construída por uma função pura `buildDiffTree(diff): Node` que divide os caminhos por `/`. Os inalterados entram só no resumo `=N`, porque listar dezenas de arquivos iguais é ruído.

6. **Botão "Generate N requests"** via `setOKButtonText` e `isOKActionEnabled`, atualizado no mesmo `refresh` que recalcula o diff. A notificação "Nothing selected" vira código morto e é removida.

7. **Navegação por resolução tardia.** `Endpoint` continua sem PSI. No duplo clique, resolve-se a classe por nome qualificado via `JavaPsiFacade` e o método por nome, e então se chama `navigate(true)`. Exige que o scanner exponha o nome qualificado do controller: adicionar o campo `controllerQualifiedName` em `Endpoint`, com default `""`, para não quebrar testes. Com overloads, navega para o primeiro método com o nome. Alternativa descartada: `SmartPsiElementPointer` no modelo, que acopla o modelo à PSI e complica os testes do writer.

8. **Notificação com ações** via `NotificationAction.createSimple`. "Open folder" usa `RevealFileAction.openDirectory`, "Settings" usa `ShowSettingsUtil.showSettingsDialog(project, BrunoGeneratorConfigurable::class.java)`. O diff para os contadores é calculado antes da escrita, com os mesmos argumentos.

9. **Settings.** Os grupos usam `group("Environments") { ... }`. O ambiente padrão vira `ComboBox`, repovoado por um listener do modelo da tabela (`TableModelEditor.modelListener`). Um valor persistido inexistente continua barrado pela validação atual. O preview do nome é um `label` atualizado por `DocumentListener` nos três campos, usando a mesma regra do writer, extraída para uma função `internal` compartilhada.

10. **Ícone SVG** em `icons/brunoAction.svg` com `brunoAction_dark.svg` (16x16). O `plugin.xml` aponta para o `.svg`.

## Risks / Trade-offs

- [Reconstruir a árvore a cada tecla perde a expansão e o scroll] -> expandir tudo após filtrar, já que o volume típico é pequeno.
- [`segmentedButton` muda de API entre versões da plataforma] -> compilar contra a 2025.3.1 fixada. Em caso de problema, voltar ao `ComboBox` sem afetar a spec de comportamento.
- [Cores próprias por método podem ter baixo contraste em algum tema] -> usar pares `JBColor` testados em Light e Darcula e conferir no `runIde`.
- [Resolver a navegação por nome falha em controllers Kotlin ou em classes geradas] -> se não resolver, não faz nada (sem erro).

## Migration Plan

Não há migração de dados. O estado persistido não muda. O release sobe a versão minor em `build.gradle.kts`.
