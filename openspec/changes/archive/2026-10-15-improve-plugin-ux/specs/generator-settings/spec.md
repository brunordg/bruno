# Spec Delta

## Purpose

Define a organização da página de configurações do Bruno Generator e as ajudas que evitam configurações inválidas.

## ADDED Requirements

### Requirement: Settings are organized in groups
A página de Settings SHALL agrupar os campos em seções rotuladas: Environments (tabela e ambiente padrão), Output (diretório, nome, prefixo e sufixo) e Generation (regeneração, folder strategy e autenticação).

#### Scenario: Grouped layout
- **WHEN** o usuário abre Settings > Tools > Bruno Generator
- **THEN** os campos aparecem sob os títulos Environments, Output e Generation

### Requirement: Default environment is picked from configured environments
O ambiente padrão SHALL ser escolhido numa lista com os nomes da tabela de ambientes, e a lista SHALL refletir as edições da tabela antes do Apply.

#### Scenario: New environment becomes selectable
- **WHEN** o usuário adiciona o ambiente `staging` na tabela
- **THEN** `staging` aparece como opção de ambiente padrão sem precisar aplicar antes

### Requirement: Output directory can be browsed
O campo de diretório de saída SHALL oferecer um seletor de pastas, mantendo a opção de deixar o campo vazio.

#### Scenario: Pick a folder
- **WHEN** o usuário escolhe uma pasta pelo seletor
- **THEN** o caminho escolhido preenche o campo

### Requirement: Collection name preview
A página SHALL exibir o nome final da pasta da collection, composto de prefixo, nome e sufixo, e SHALL atualizar a prévia enquanto o usuário edita esses campos.

#### Scenario: Prefix and suffix
- **WHEN** o prefixo é `my-`, o nome é `shop-api` e o sufixo é `-dev`
- **THEN** a prévia mostra `my-shop-api-dev`

### Requirement: Few-option settings are shown as radio buttons
O modo de regeneração e a folder strategy SHALL ser apresentados como radio buttons com todas as opções visíveis, cada uma com uma descrição curta.

#### Scenario: Regeneration options visible
- **WHEN** a página é aberta
- **THEN** "Overwrite" e "Add-only" aparecem lado a lado, com a opção persistida marcada
