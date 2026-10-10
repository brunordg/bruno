# Proposal

## Why

O repositório tem um `README.md` bilíngue e completo, mas ele mistura visão geral, uso, saída gerada e instruções de desenvolvimento/publicação. Quem chega ao repositório no GitHub — seja um usuário final querendo gerar sua primeira collection do Bruno, seja um desenvolvedor querendo rodar o plugin localmente — não tem um caminho único e curto para o "primeiro sucesso". Um arquivo de Quickstart dedicado reduz esse atrito e melhora a apresentação do projeto no GitHub.

## What Changes

- Adiciona um novo arquivo `QUICKSTART.md` na raiz do repositório.
- O arquivo é **bilíngue** (inglês e português), seguindo o padrão do `README.md`.
- Cobre dois públicos, em seções separadas:
  - **Usuário final**: instalar o plugin no IntelliJ IDEA e gerar a primeira collection do Bruno no menor número de passos.
  - **Desenvolvedor/contribuidor**: clonar o repositório, rodar o plugin em uma IDE de teste (`runIde`) e gerar o build de distribuição.
- Referencia o `README.md` para detalhes aprofundados (funcionalidades, configurações, publicação), mantendo o Quickstart enxuto.
- Não altera código do plugin nem seu comportamento.

## Capabilities

### New Capabilities
<!-- Nenhuma. Mudança puramente de documentação; sem comportamento de spec. -->

### Modified Capabilities
<!-- Nenhuma. -->

Esta change não altera comportamento observável do plugin — é documentação apenas. Por isso `skip_specs: true` está definido em `.openspec.yaml`.

## Impact

- **Novo arquivo**: `QUICKSTART.md` (raiz do repositório).
- **Sem impacto em código**: nenhum arquivo em `src/`, `build.gradle.kts` ou configurações do plugin é alterado.
- **Documentação**: complementa o `README.md`; pode ser referenciado a partir dele futuramente (fora do escopo desta change).
- **Distribuição**: arquivo versionado e enviado ao GitHub; não afeta o artefato publicado no JetBrains Marketplace.
