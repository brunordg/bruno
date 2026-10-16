# endpoint-selection Specification

## Purpose
Define como o usuário encontra, entende e seleciona os endpoints descobertos no diálogo de geração antes de confirmar a escrita da collection.

## Requirements

### Requirement: Endpoints are grouped by controller
O diálogo SHALL agrupar os endpoints pela classe de controller que os declara, independentemente da folder strategy selecionada. Cada grupo SHALL exibir quantos de seus endpoints estão selecionados em relação ao total.

#### Scenario: Two controllers under a common prefix
- **WHEN** `OrderController` e `ProductController` declaram endpoints sob `/api`
- **THEN** a árvore mostra dois grupos, `OrderController` e `ProductController`, e não um único grupo `api`

#### Scenario: Group counter
- **WHEN** um dos 4 endpoints de `OrderController` está desmarcado
- **THEN** o grupo exibe `3/4`

### Requirement: Endpoint rows highlight the HTTP method
Cada endpoint SHALL ser exibido com o método HTTP em destaque visual distinto por método, seguido da rota e do nome do handler em estilo secundário.

#### Scenario: Row rendering
- **WHEN** o endpoint `DELETE /api/orders/{id}` com handler `cancel` é listado
- **THEN** a linha mostra `DELETE` destacado, `/api/orders/{id}` em texto normal e `cancel` em texto secundário

### Requirement: Endpoint list can be filtered
O diálogo SHALL oferecer um campo de busca que filtra, sem diferenciar maiúsculas de minúsculas, os endpoints exibidos por método, rota, handler ou nome do controller. Filtrar MUST NOT alterar a seleção dos endpoints ocultos.

#### Scenario: Filter by path fragment
- **WHEN** o usuário digita `orders`
- **THEN** só os endpoints cuja rota, handler ou controller contém `orders` permanecem visíveis

#### Scenario: Hidden endpoints keep their selection
- **WHEN** um endpoint selecionado fica oculto pelo filtro e o usuário confirma
- **THEN** esse endpoint é gerado mesmo assim

### Requirement: Bulk selection actions
O diálogo SHALL oferecer ações para selecionar todos os endpoints e para limpar a seleção.

#### Scenario: Clear selection
- **WHEN** o usuário aciona "Limpar seleção"
- **THEN** nenhum endpoint fica selecionado e o preview de diff é atualizado

### Requirement: Diff preview is shown as a folder tree with summary
O preview de diff SHALL exibir um resumo com as contagens de arquivos adicionados, removidos e inalterados, e SHALL apresentar os arquivos adicionados e removidos numa árvore de pastas relativa à raiz da collection, com marcação visual distinta para adição e remoção.

#### Scenario: Summary and tree
- **WHEN** a seleção resulta em 3 arquivos adicionados, 1 removido e 7 inalterados
- **THEN** o topo do preview mostra `+3`, `-1` e `=7`, e o arquivo removido aparece sob sua pasta, marcado como remoção

### Requirement: Footer shows the active regeneration mode
O diálogo SHALL exibir o modo de regeneração ativo (Overwrite ou Add-only).

#### Scenario: Add-only mode visible
- **WHEN** as Settings estão em Add-only
- **THEN** o diálogo indica "Add-only" no rodapé

### Requirement: Confirm action states what will be generated
O botão de confirmação SHALL indicar a quantidade de requests selecionadas (por exemplo, "Generate 8 requests") e SHALL ficar desabilitado quando nenhum endpoint está selecionado.

#### Scenario: Nothing selected
- **WHEN** o usuário limpa toda a seleção
- **THEN** o botão de confirmação fica desabilitado

#### Scenario: Label follows the selection
- **WHEN** 8 endpoints estão selecionados
- **THEN** o botão exibe "Generate 8 requests"

### Requirement: Folder strategy uses a segmented control
O diálogo SHALL apresentar as três folder strategies como opções visíveis lado a lado, pré-selecionadas com o padrão das Settings, mantendo a atualização ao vivo do preview definida em `collection-regeneration`.

#### Scenario: Switch strategy
- **WHEN** o usuário seleciona "Flat"
- **THEN** o preview de diff passa a mostrar todos os arquivos diretamente em `requests/`

### Requirement: Navigate to handler source
Um duplo clique num endpoint SHALL abrir o método handler correspondente no editor.

#### Scenario: Double click
- **WHEN** o usuário dá duplo clique em `POST /api/orders`
- **THEN** o editor abre `OrderController` posicionado no método `create`
