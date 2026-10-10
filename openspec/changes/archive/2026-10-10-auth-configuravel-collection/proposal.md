# Proposal

## Why

Todo request gerado sai com `auth: none` fixo. Quase toda API real exige autenticação, então hoje o usuário precisa configurar auth manualmente em cada request (ou na collection) depois de gerar — e essa edição é perdida na próxima regeneração. Falta uma forma de declarar, uma vez, o esquema de autenticação da API e tê-lo aplicado automaticamente.

## What Changes

- Novo ajuste **Autenticação** (Settings > Tools > Bruno Generator) com os modos: **None** (padrão, atual), **Bearer**, **Basic** e **API Key**.
- O `collection.bru` passa a emitir o bloco de auth correspondente ao modo, usando variáveis (ex.: `{{token}}`, `{{username}}`/`{{password}}`, `{{apiKey}}`) em vez de segredos literais.
- Cada request passa a usar `auth: inherit` (em vez de `auth: none`) quando um modo diferente de None está configurado, herdando a auth da collection; permanece `auth: none` quando o modo é None.
- As variáveis de credencial ficam disponíveis por ambiente: cada `environments/<env>.bru` ganha as chaves de credencial do modo ativo (valores em branco, para o usuário preencher), ao lado do `baseUrl` existente.

Non-goals: detecção automática de Spring Security para sugerir o modo (fica como extensão futura); OAuth2/flows com troca de token; exceção de auth por endpoint (ex.: `/login` sem auth) — tudo o que foge de um esquema único no nível da collection.

## Capabilities

### New Capabilities
- `collection-auth`: como o plugin configura a autenticação no nível da collection Bruno a partir das settings, emitindo o bloco de auth, fazendo os requests herdarem e provisionando as variáveis de credencial por ambiente.

### Modified Capabilities
<!-- Nenhuma: não existem specs anteriores neste projeto. -->

## Impact

- **Settings**: `src/main/kotlin/settings/BrunoGeneratorSettings.kt` — `BrunoGeneratorState` ganha `authMode` (enum) e, se necessário, nomes de variável; `BrunoGeneratorConfigurable` + possivelmente `EnvironmentsTable` para a UI.
- **Writer**: `src/main/kotlin/writer/BrunoWriter.kt` — `collectionVariables`/`collection.bru` (bloco auth), `requestContent` (`auth: inherit` vs `none`), `writeEnvironments` (chaves de credencial por ambiente).
- **Testes**: `BrunoWriterTest` (cada modo gera o bloco correto e requests herdam; None mantém comportamento atual), `BrunoGeneratorStateTest` (default = None).
- **Compatibilidade**: com o default None, a saída é idêntica à atual. Mudar de modo altera `collection.bru`, os requests e os arquivos de ambiente na próxima regeneração.
