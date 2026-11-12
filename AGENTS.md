# AGENTS.md

Plugin do IntelliJ IDEA, escrito em Kotlin, para desenvolvedores de APIs Spring Boot. Lê o PSI do projeto aberto na IDE e gera uma coleção do [Bruno](https://www.usebruno.com/) a partir dos endpoints dos `@RestController`, evitando montar as requisições à mão. Não é uma aplicação Spring Boot: o usuário roda **Tools > Generate Bruno Collection** dentro da IDE. O módulo é único (`bruno`); `knowledge-base.md` (em português) é a referência detalhada de arquitetura.

---

## Antes de escrever ou revisar código

- Padrões de simplicidade e formatação (usados na revisão): `CODING_STANDARDS.md`.
- Como escrever testes (cenários, bordas, black box): `docs/agents/testing.md`.
- Arquitetura, estrutura de pastas e regras de negócio: `knowledge-base.md`.
- Ao descobrir uma armadilha que o próximo desenvolvedor também enfrentaria, registre-a em "Armadilhas".

---

## Visão geral do projeto

### Tecnologias e versões

- Kotlin + Gradle (Kotlin DSL), plugin `org.jetbrains.intellij.platform`; IntelliJ Platform **2025.3.1** (`platformVersion` em `gradle.properties`), `pluginSinceBuild=253`, Gradle 9.7.0.
- O build roda no **JDK 25**, mas o bytecode precisa ficar em **Java 21**, porque a JBR da IDE 2025.3.1 é JDK 21; senão dá `UnsupportedClassVersionError` ao carregar o plugin. O override do `jvmTarget` do Kotlin fica em `afterEvaluate` de propósito (o plugin do IntelliJ define o próprio alvo durante a avaliação e venceria um bloco no topo). Não mova.
- Plugin bundled `com.intellij.java`. Versões das libs de teste: `build.gradle.kts`.
- Configuration cache e build cache do Gradle ligados.

### Dados e contratos que NÃO podem mudar

- **Estado persistido** (arquivo `bruno-generator.xml` na pasta de configuração do projeto, normalmente `.idea/` — local exato não verificado; serviço `BrunoGeneratorSettings`, `@State(name = "BrunoGeneratorSettings")`): campos de `BrunoGeneratorState` (`environments`, `defaultEnvironmentName`, `outputDirectory`, `collectionName`, `collectionNamePrefix`, `collectionNameSuffix`, `regenerationMode`, `authMode`, `folderStrategy`) e nomes dos enums (`RegenerationMode`, `AuthMode`, `FolderStrategy`) são gravados em XML nos projetos dos usuários. Renomear perde a configuração deles.
- **IDs do `plugin.xml`**: `com.codeteam.bruno` (plugin), `GenerateBrunoCollectionAction`, `com.codeteam.bruno.settings`, grupo de notificação `Bruno Generator`.
- **Layout da coleção gerada**: `bruno.json` (`version "1"`, `name`, `type "collection"`), `collection.bru`, `environments/<slug>.bru`, `requests/**/<slug-metodo-caminho>.bru`. O nome do arquivo é estável (slug de método + caminho) para a regeneração e o diff reconhecerem os arquivos já existentes; mudar o `slug()` ou o layout renomeia/remove arquivos nos projetos dos usuários.
- **Variáveis da coleção**: `baseUrl` e as credenciais por modo de autenticação (`token`; `username` + `password`; `apiKey`, com header `X-API-Key`), mais as variáveis de data do script de pré-requisição (`randomDate`, `randomPastDate`, `randomFutureDate`, `randomDateTime`, `randomTime` e as variantes de início/fim) usadas pelos corpos gerados.
- **Pasta de saída**: `<outputDirectory ou raiz do projeto>/<prefixo><nome><sufixo>` (prefixo padrão `bruno-`).

---

## Como rodar

### Pré-requisitos

- JDK 25 instalado (o Gradle usa toolchain; o bytecode sai em Java 21).
- Acesso à internet na primeira execução, para baixar a IntelliJ Platform e o Gradle (o diretório `.intellijPlatform/` e o cache do Gradle guardam o que foi baixado).
- Nenhuma credencial ou variável de ambiente é necessária. Para publicar no Marketplace é preciso token da conta JetBrains (fora do repositório; publicação não verificada).

### Subir o projeto

Na raiz do projeto:

```bash
bash ./gradlew runIde --no-daemon     # IDE de teste (sandbox) com o plugin carregado — não verificado (abre uma IDE gráfica)
```

Na IDE do sandbox, abra um projeto Spring Boot e rode **Tools > Generate Bruno Collection**. Também existe a configuração de execução `.run/Run Plugin.run.xml` ("Run IDE with Plugin").

### Rodar os testes

```bash
bash ./gradlew test --no-daemon                                         # todos os testes (verificado, passou)
bash ./gradlew test --no-daemon --tests 'writer.BrunoWriterTest'        # uma classe
bash ./gradlew test --no-daemon --tests 'writer.BrunoWriterTest.slug*'  # um método (padrão)
```

- **Unidade (white box):** `writer/BrunoWriterTest`, `ExampleValuesTest`, `settings/*Test`, `ui/EndpointPresentationTest` são JUnit4 simples e chamam as funções diretamente.
- **Com a plataforma IntelliJ:** `scanner/ControllerScannerTest` (`BasePlatformTestCase`) usa jars reais de Spring, Jackson, Validation e Swagger anexados ao fixture. Mudança no scanner exige teste com fixture.
- **Mais próximos de black box:** `writer/BrunoWriterCollectionTest` gera a coleção em disco e confere os arquivos resultantes; `GenerateBrunoCollectionActionTest` e `ui/EndpointSelectionDialogTest` exercitam a ação e o diálogo.
- **Ponta a ponta (código Spring -> coleção):** `golden/ScannerToWriterGoldenTest` roda scanner + writer e compara a árvore gerada com `src/test/resources/golden/widgets`. Se a saída mudou de propósito, revise `build/golden-actual/bruno-widgets` (gravado a cada execução) e copie sobre a pasta golden.
- `ui/UiScreenshotsTest` só roda com `-Dbruno.screenshots=<pasta>` e precisa de display; serve para gerar as imagens do README.
- Não existe teste do plugin dentro de uma IDE completa (o golden usa a plataforma de teste, não a IDE). Não verificado se os cenários de valores de borda/inválidos estão cobertos em todas as regras de `ExampleValues`; revise ao mexer nelas.

### Gerar o build

```bash
bash ./gradlew clean buildPlugin --no-daemon
```

O zip fica em `build/distributions/` e a tarefa `copyPluginToRoot` também o copia para a raiz (`bruno-<versão>.zip`). Suba o `version` em `build.gradle.kts` a cada release e acrescente a entrada em `changeNotes`. A verificação de compatibilidade usa `verifyPlugin` (não verificado).

---

## Convenções do código

- Código, nomes e mensagens técnicas em inglês; **commits e documentação em português**. O `README.md` é bilíngue (EN + PT): atualize as duas metades juntas.
- Textos mostrados ao usuário ficam em `messages/MyMessageBundle.properties`.
- As mudanças seguem o fluxo **OpenSpec**: specs em `openspec/specs/`, mudanças ativas em `openspec/changes/` e as concluídas em `openspec/changes/archive/`. Use as skills `opsx:*` / `openspec-*`.
- Trabalho de feature vai em branch `feat/...` e entra na `main` com merge commit (sem squash/rebase).
- Issues no GitHub Issues (`gh`); rótulos e fluxo em `docs/agents/issue-tracker.md` e `docs/agents/triage-labels.md`.
- Testes: JUnit4; scanner com `BasePlatformTestCase`; ficam em `src/test/kotlin/<package>/`.

---

## Armadilhas já encontradas

- **Bytecode Java 21 com JDK 25:** compilar com alvo mais novo gera um plugin que compila e empacota, mas falha ao carregar na IDE. Veja "Tecnologias e versões".
- **Gradle sempre com `--no-daemon` e via `bash ./gradlew`**, como nos comandos acima. O motivo do `--no-daemon` não está registrado em lugar nenhum do repositório; cada execução paga a inicialização do Gradle.
- **`buildPlugin` usa `copyPluginToRoot` escrito à mão**, e não uma tarefa `Copy` para a raiz: uma `Copy` declararia o projeto inteiro como saída e conflitaria com `test` e `verifyPlugin`.
- **Regenerar em modo Overwrite muda valores de exemplo** já gerados (os valores são aleatórios/dinâmicos); no modo Add-only as edições manuais são preservadas.
- **Mudar a estratégia de pastas** reorganiza as pastas na primeira regeneração (o nome do arquivo é estável, a pasta não).
- O resultado do `test` pode vir de cache (`FROM-CACHE`); para forçar a execução use `--rerun-tasks` ou `cleanTest`.

---

## Limitações conhecidas e dívidas técnicas

- Anotações Jackson de classe (`@JsonNaming`, `@JsonInclude`) não são suportadas; Swagger v2 e agrupamento por `@Tag` também não.
- Autenticação é só no nível da coleção (Bearer, Basic, API Key); não há exceção por endpoint nem detecção do Spring Security.
- Não há comentários `shortcut:` em `src/` (verificado com `grep`).

---

## Antes de entregar uma alteração

1. O código está mais simples ou igualmente simples do que antes? Se ficou mais complexo, o motivo está explicado?
2. Os comandos de rodar, testar e buildar continuam funcionando? Rode-os.
3. Mexeu em algo coberto por teste? Rode os testes. Criou lógica nova relevante ou corrigiu um bug? Escreva os testes seguindo `docs/agents/testing.md`: todos os cenários, com valores diferentes (típicos, de borda e inválidos) e pelo menos um teste black box pelo contrato público.
4. Não renomeou nem alterou nenhum dado ou contrato listado na seção "Dados e contratos que NÃO podem mudar"?
5. Atualizou este arquivo se algo descrito nele mudou?
6. Contou ao usuário o que NÃO foi verificado e qual risco isso deixa?
