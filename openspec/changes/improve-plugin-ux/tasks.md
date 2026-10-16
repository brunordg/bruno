# Tasks

## 1. Lógica de apresentação pura

- [x] 1.1 Criar `groupEndpointsByController` (substitui `groupEndpointsByTopPath`), com desambiguação por pacote em nomes repetidos; verificar com teste JUnit cobrindo dois controllers sob `/api` e nomes duplicados
- [x] 1.2 Criar `matchesFilter(endpoint, query)` case-insensitive sobre método, rota, handler e controller; verificar com teste JUnit
- [x] 1.3 Criar `confirmLabel(count)` e `buildDiffTree(diff)` (árvore de pastas para added/removed com resumo +N/-N/=N); verificar com testes JUnit
- [x] 1.4 Extrair a composição do nome da collection do `BrunoWriter` para função `internal` reutilizável; verificar que `BrunoWriterTest` continua passando e adicionar teste do preview `my-` + `shop-api` + `-dev`

## 2. Diálogo de geração

- [x] 2.1 Manter o mapa de seleção fora da árvore e reconstruir os nós por grupo de controller, com contador `x/y`; verificar no `runIde` que a seleção de itens ocultos sobrevive ao filtro
- [x] 2.2 Renderer com método colorido (`JBColor`), rota normal e handler em cinza; verificar visualmente em Light e Darcula
- [x] 2.3 Adicionar o `SearchTextField` e as ações "Select all"/"Clear"; verificar que o diff atualiza ao limpar
- [x] 2.4 Trocar o combo de folder strategy por `segmentedButton`, mantendo o preview ao vivo; verificar que "Flat" mostra arquivos direto em `requests/`
- [x] 2.5 Preview de diff em árvore com resumo e marcação de adição e remoção; verificar com o cenário 3/1/7 no `runIde`
- [x] 2.6 Rodapé com o modo de regeneração ativo e botão "Generate N requests", desabilitado com 0; remover a notificação "Nothing selected"; verificar os dois estados no `runIde`
- [x] 2.7 Adicionar `controllerQualifiedName` ao `Endpoint`, preenchido pelo `ControllerScanner`, e implementar a navegação por duplo clique; verificar com teste em `ControllerScannerTest` para o campo e manualmente para a navegação
- [x] 2.8 Atualizar `README.md` (EN e PT) na seção "How to use" / "Como usar" descrevendo busca, agrupamento e botão de geração

## 3. Feedback pós-geração

- [x] 3.1 Calcular o diff antes da escrita e incluir `(+N, -N)` no texto da notificação; verificar no `runIde`
- [x] 3.2 Adicionar as ações "Open folder" e "Settings" na notificação; verificar que cada uma abre o destino correto

## 4. Settings

- [x] 4.1 Reorganizar a página em grupos Environments, Output e Generation; verificar visualmente
- [x] 4.2 Trocar o campo de ambiente padrão por um combo sincronizado com a tabela; verificar que um ambiente recém-adicionado aparece antes do Apply e que `BrunoGeneratorSettingsTest` passa
- [x] 4.3 Usar `textFieldWithBrowseButton` para o diretório de saída e adicionar o preview do nome da collection; verificar o preview ao digitar
- [x] 4.4 Usar radio buttons para regeneração e folder strategy, com descrições curtas no lugar dos rowComments longos; verificar que `isModified`/`apply`/`reset` persistem corretamente
- [x] 4.5 Atualizar `README.md` (EN e PT) descrevendo a nova página de Settings

## 5. Ícone e release

- [x] 5.1 Criar `brunoAction.svg` e `brunoAction_dark.svg` e apontar o `plugin.xml` para eles; verificar o ícone no menu Tools nos dois temas
- [x] 5.2 Subir a versão minor em `build.gradle.kts` e rodar `bash ./gradlew clean test buildPlugin --no-daemon` com sucesso

## Workflow follow-up

- Arquivar a change após o merge de `feat/improve-plugin-ux` em `main`.
