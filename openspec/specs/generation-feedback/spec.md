# generation-feedback Specification

## Purpose
Define o retorno que o usuário recebe após gerar a collection e as ações disponíveis a partir dele.

## Requirements

### Requirement: Success notification summarizes the result
A notificação de sucesso SHALL informar a quantidade de requests geradas, o nome da pasta da collection e as contagens de arquivos adicionados e removidos.

#### Scenario: Summary text
- **WHEN** 8 requests são geradas em `shop-api/` com 3 arquivos adicionados e 1 removido
- **THEN** a notificação mostra `8 requests in shop-api/ (+3, -1)`

### Requirement: Success notification offers follow-up actions
A notificação de sucesso SHALL oferecer uma ação que revela a pasta da collection no gerenciador de arquivos do sistema e uma ação que abre a página de Settings do plugin.

#### Scenario: Open folder
- **WHEN** o usuário aciona "Open folder"
- **THEN** a pasta da collection é exibida no gerenciador de arquivos do sistema

#### Scenario: Open settings
- **WHEN** o usuário aciona "Settings"
- **THEN** a página Settings > Tools > Bruno Generator é aberta
