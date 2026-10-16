# Proposal

## Why

O plugin funciona, mas a experiência visual é crua. O diálogo de geração mostra texto sem estilo, agrupa quase tudo sob `api` (primeiro segmento da rota), não tem busca, exibe o diff como lista plana de caminhos longos e confirma com um "OK" genérico. A notificação final não leva a lugar nenhum, e a página de Settings é uma sequência de 9 campos sem hierarquia, com o ambiente padrão digitado à mão. Isso atrapalha a ferramenta justamente na tela que todo usuário vê a cada execução.

## What Changes

- **Diálogo de geração**
  - A árvore de endpoints passa a ser agrupada **por controller**, com contador de selecionados por grupo. O agrupamento independe da folder strategy, porque a estrutura de pastas já aparece no preview de diff.
  - Cada endpoint mostra o método HTTP em destaque e colorido, a rota e o nome do handler em texto secundário.
  - Um campo de busca filtra endpoints por método, rota, handler ou controller.
  - Ações "Selecionar todos" e "Limpar seleção".
  - A folder strategy passa a ser escolhida com botões segmentados, no lugar do combobox.
  - O preview de diff vira uma árvore de pastas, com resumo `+N / -N / =N` no topo e arquivos adicionados e removidos destacados.
  - O rodapé mostra o modo de regeneração ativo.
  - O botão de confirmação passa a se chamar "Generate N requests" e fica desabilitado quando nada está selecionado.
  - Duplo clique num endpoint navega até o método no editor.
- **Feedback pós-geração**: a notificação de sucesso informa a contagem do diff e oferece as ações "Open folder" e "Settings".
- **Settings**
  - Os campos são organizados em grupos: Environments, Output e Generation.
  - O ambiente padrão é escolhido em uma lista derivada da tabela de ambientes.
  - O diretório de saída ganha um botão de navegação.
  - Um preview mostra o nome final da collection (prefixo + nome + sufixo).
  - Regeneração e folder strategy passam a usar radio buttons.
- **Ícone**: o ícone da action passa de PNG para SVG, com variante escura.
- Sem mudanças no conteúdo dos arquivos `.bru`, nas regras de nomeação ou nos estados persistidos. Nada é **BREAKING**.

Non-goals: ação no gutter ou no menu de contexto do controller, abrir a collection diretamente no app Bruno, mudanças na página do Marketplace.

## Capabilities

### New Capabilities
- `endpoint-selection`: como o usuário encontra, entende e seleciona endpoints no diálogo de geração antes de confirmar.
- `generation-feedback`: o que o usuário recebe após gerar a collection e quais ações seguem dali.
- `generator-settings`: organização e validação da página de configurações do plugin.

### Modified Capabilities
- (nenhuma: o comportamento de `collection-regeneration` para a folder strategy com preview ao vivo se mantém. Só o controle visual muda.)

## Impact

- `src/main/kotlin/ui/EndpointSelectionDialog.kt`: reescrita principal da UI.
- `src/main/kotlin/GenerateBrunoCollectionAction.kt`: notificação com ações e uso do diff.
- `src/main/kotlin/settings/BrunoGeneratorConfigurable.kt`: layout em grupos, combo do ambiente padrão, browse e preview.
- `src/main/resources/icons/`: novo `brunoAction.svg` e `brunoAction_dark.svg`, além do `plugin.xml`.
- `README.md` (EN + PT): descrição do novo diálogo. Novos testes para a lógica pura (agrupamento, filtro, rótulo do botão, preview do nome).
