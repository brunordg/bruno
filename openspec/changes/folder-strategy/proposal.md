# Proposal

## Why

A pasta de cada request é derivada do **primeiro segmento da rota**. Como quase toda API Spring vive sob um prefixo comum (`/api`, `/v1`, ...), o primeiro segmento é o mesmo para todos os endpoints e tudo cai numa única pasta (`requests/api/`). O usuário não tem como organizar a collection por controller nem por recurso, e não há escolha no momento de gerar.

## What Changes

- Introduz uma **estratégia de organização de pastas** configurável com três valores: **By URL path** (pastas aninhadas espelhando a rota — padrão), **By controller** (uma pasta por classe de controller) e **Flat** (tudo direto em `requests/`).
- A estratégia padrão é configurável em **Settings > Tools > Bruno Generator** (ao lado dos modos de regeneração e autenticação), com padrão **By URL path**.
- O **diálogo de geração** passa a exibir um combobox de estratégia no topo, pré-selecionado com o default das Settings. Trocar o combobox **atualiza o preview de diff ao vivo**, mostrando o novo layout de pastas antes de gerar. A escolha do diálogo vale para aquela geração (override per-run) e **não** altera o default salvo.
- O nome de arquivo permanece `slug("<método> <rota>")` em todas as estratégias (preserva a estabilidade introduzida na capability `collection-regeneration`); apenas a **pasta** varia conforme a estratégia.
- **BREAKING (layout)**: o agrupamento por primeiro-segmento deixa de existir; a primeira regeneração após a atualização reorganiza os arquivos (renomeação única, como a mudança de nomes estáveis já exigiu).

Non-goals: mover os modos de regeneração/autenticação para o diálogo (seguem só nas Settings); árvore de seleção aninhada refletindo a hierarquia completa (o preview de diff já reflete a estratégia; o agrupamento da árvore é best-effort).

## Capabilities

### New Capabilities
<!-- Nenhuma capability nova. -->

### Modified Capabilities
- `collection-regeneration`: o requisito de nome/caminho estável deixa de fixar "pasta = primeiro segmento da rota"; passa a definir a pasta pela estratégia escolhida, mantendo a estabilidade dentro de cada estratégia. Novos requisitos descrevem as três estratégias e a escolha da estratégia no momento de gerar com preview ao vivo.

## Impact

- **Settings**: `BrunoGeneratorSettings.kt` (`BrunoGeneratorState` ganha `folderStrategy: FolderStrategy = BY_URL_PATH`) e `BrunoGeneratorConfigurable.kt` (combo do default).
- **Scanner/Model**: `Endpoint` ganha o nome do controller; `ControllerScanner.extractEndpoints(psiClass)` propaga `psiClass.name` (usado só pelo By controller).
- **Writer**: `BrunoWriter.requestFilePath` vira strategy-aware (só a pasta); `computeDiff` e `writeCollection` passam a receber a estratégia como **parâmetro explícito** (não lida do settings), para o preview não mutar o estado e nada vazar ao cancelar.
- **UI**: `EndpointSelectionDialog` ganha o combobox no topo e expõe `selectedFolderStrategy()`, disparando `refreshDiff` na troca; `GenerateBrunoCollectionAction` repassa a escolha ao `writeCollection`.
- **Testes**: `BrunoWriterTest`/`BrunoWriterCollectionTest` (layout por estratégia, preview por estratégia), `ControllerScannerTest` (nome do controller), `BrunoGeneratorStateTest` (default `BY_URL_PATH`).
- **Compatibilidade**: renomeação única dos arquivos na primeira regeneração; sem mudança no conteúdo dos `.bru`.
