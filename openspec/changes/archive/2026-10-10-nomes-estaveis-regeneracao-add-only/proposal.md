# Proposal

## Why

O nome de arquivo de cada request embute um número de sequência (`%02d`) derivado do **índice do endpoint na seleção**. Gerar um subconjunto diferente renumera todos os arquivos, produzindo diffs enormes no git mesmo quando nada mudou de verdade. Pior: a regeneração reescreve todo arquivo `.bru` e remove os órfãos, então qualquer ajuste manual feito num request (ex.: um body de exemplo real, um header de auth) é perdido. Hoje não há como manter a collection sincronizada **e** preservar edições.

## What Changes

- O nome de arquivo de cada request passa a ser **estável e determinístico**, derivado do método HTTP + rota do endpoint, independente de quais/quantos endpoints estão selecionados. Dois endpoints distintos nunca colidem no mesmo arquivo.
- O campo `seq` no bloco `meta {}` continua existindo para ordenação no Bruno, calculado de forma determinística (ordenação estável), não pelo índice de seleção.
- Novo ajuste de configuração **Modo de regeneração** com dois valores:
  - **Overwrite** (padrão, comportamento atual): reescreve todos os requests e remove os órfãos.
  - **Add-only**: escreve apenas os arquivos de request ausentes, nunca sobrescreve um arquivo existente e não remove arquivos — preservando edições manuais e requests adicionados à mão.
- O preview de diff no diálogo reflete o modo selecionado.

Non-goals: merge por marcadores/seções gerenciadas dentro de um request editado (preservação parcial). Add-only é tudo-ou-nada por arquivo; o merge fino fica como extensão futura.

## Capabilities

### New Capabilities
- `collection-regeneration`: como o plugin nomeia os arquivos de request e como reconcilia uma collection existente em regenerações sucessivas (criar, sobrescrever, preservar e remover), incluindo os modos de regeneração.

### Modified Capabilities
<!-- Nenhuma: não existem specs anteriores neste projeto. -->

## Impact

- **Código**: `src/main/kotlin/writer/BrunoWriter.kt` — `requestFilePath` (esquema de nome), `writeCollection` e `pruneStaleRequestFiles` (respeitar o modo), `computeDiff` (refletir o modo); `requestContent` para o cálculo determinístico de `seq`.
- **Settings**: `src/main/kotlin/settings/BrunoGeneratorSettings.kt` (`BrunoGeneratorState` ganha o modo) e `BrunoGeneratorConfigurable` (UI).
- **UI**: `src/main/kotlin/ui/EndpointSelectionDialog.kt` consome `computeDiff`.
- **Testes**: `BrunoWriterTest` (estabilidade de nomes entre seleções diferentes, colisão, add-only preserva/ não remove).
- **Compatibilidade**: nomes de arquivo mudam uma vez na primeira regeneração após a atualização (migração única, documentada); sem mudança de formato dentro dos `.bru`.
