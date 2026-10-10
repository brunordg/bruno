# Design

## Context

Ver `proposal.md - Why`. O repositório já possui `README.md` bilíngue (EN/PT) com seções de uso, saída e desenvolvimento. O plugin é o **Bruno Generator** para IntelliJ IDEA, distribuído como `.zip` (build via `./gradlew buildPlugin`) e publicável no JetBrains Marketplace. Este design cobre apenas a estrutura de um novo documento; não há mudança de código.

## Goals / Non-Goals

**Goals:**
- Definir a estrutura do `QUICKSTART.md` para que seja curto, escaneável e conduza ao "primeiro sucesso".
- Estabelecer o layout bilíngue (EN primeiro, PT depois) coerente com o `README.md`.
- Separar claramente os dois públicos (usuário final vs. desenvolvedor).

**Non-Goals:**
- Reescrever ou substituir o `README.md`.
- Documentar todas as funcionalidades/configurações (isso permanece no `README.md`, referenciado).
- Alterar comportamento, build ou metadados do plugin.

## Decisions

- **Formato Markdown, arquivo único na raiz (`QUICKSTART.md`)**: convenção reconhecida pelo GitHub e fácil de linkar a partir do `README.md`. Alternativa considerada: uma pasta `docs/` — descartada por ser overhead para um único arquivo.
- **Layout bilíngue com blocos EN e PT separados por `---`** (inglês primeiro), espelhando o padrão já usado no `README.md`. Alternativa: dois arquivos (`QUICKSTART.md` / `QUICKSTART.pt-BR.md`) — descartada para manter um único ponto de entrada e reduzir manutenção de sincronização.
- **Duas trilhas por idioma: "For Users" e "For Developers"**, cada uma como uma lista numerada de passos mínimos. O objetivo é que cada trilha caiba em poucos passos, do zero ao resultado (collection gerada / IDE de teste rodando).
- **Comandos de desenvolvimento reaproveitam os do `README.md`** (`./gradlew runIde`, `./gradlew buildPlugin`) para não divergir. O Quickstart não inventa novos comandos.
- **Fonte da verdade dos passos de uso**: os passos de "usuário final" derivam da seção "Como usar / How to use" do `README.md`, condensados ao caminho principal (sem as ramificações opcionais de configuração).

## Risks / Trade-offs

- [Documentação duplicada pode divergir do `README.md`] → Manter o Quickstart mínimo e apontar para o `README.md` nos detalhes, reduzindo a superfície que precisa ser mantida em sincronia.
- [Manter EN e PT em sincronia num único arquivo] → Estrutura idêntica nos dois blocos, separada por `---`, facilita revisão paralela; mesmo padrão já aceito no `README.md`.
