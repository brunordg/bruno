# AGENTS.md

Plugin do IntelliJ IDEA, escrito em Kotlin, para desenvolvedores de APIs Spring Boot. Lê o PSI do projeto aberto na IDE e gera uma coleção do [Bruno](https://www.usebruno.com/) a partir dos endpoints dos `@RestController`, evitando montar as requisições à mão. Não é uma aplicação Spring Boot: o usuário roda **Tools > Generate Bruno Collection** dentro da IDE. O módulo é único (`bruno`); `knowledge-base.md` (em português) é a referência detalhada de arquitetura.

---

## Regras para a IA ao preencher e ao manter este arquivo

- **Não invente.** Só escreva o que foi verificado no código, nos arquivos de configuração ou ao rodar o comando. Se não tiver certeza, escreva "não verificado" em vez de supor.
- **Teste os comandos** antes de documentá-los. Um comando de execução que não funciona é pior do que nenhum.
- **Não repita o que o código já mostra.** Documente o que não é óbvio: decisões, regras de negócio, limites e armadilhas.
- **Mantenha o arquivo curto e atual.** Ao mudar algo que está descrito aqui (comando, estrutura, convenção), atualize este arquivo na mesma alteração. Apague o que ficou obsoleto.
- **Registre cada armadilha nova** na seção "Armadilhas" quando você gastar tempo com um problema que o próximo desenvolvedor também enfrentaria.

---

## Diretriz obrigatória: simplicidade e legibilidade do código

Ao desenvolver ou modificar qualquer parte deste projeto, tenha sempre em mente que o código será mantido e evoluído por desenvolvedores humanos.

Não escreva código pensando apenas em fazer a IA executar a tarefa. Escreva código para que um desenvolvedor consiga entender facilmente o que foi feito, por que foi feito e como dar manutenção no futuro.

Siga estas regras:

- **Simplicidade acima de sofisticação:** prefira soluções simples, diretas e convencionais. Evite arquiteturas desnecessariamente complexas.
- **Legibilidade:** escreva códigos claros, com nomes de variáveis, funções e classes que expressem sua finalidade.
- **Evite abstrações desnecessárias:** não crie camadas, interfaces, padrões de projeto, componentes ou funções genéricas sem uma necessidade real.
- **Facilidade de manutenção:** organize o código para que outro desenvolvedor consiga localizar, compreender e alterar funcionalidades sem precisar entender todo o sistema.
- **Evite código excessivamente inteligente:** não utilize encadeamentos complexos, expressões difíceis de interpretar ou soluções compactas que prejudiquem a compreensão.
- **Comentários úteis:** explique regras de negócio, decisões técnicas e comportamentos que não sejam óbvios. Não comente o que o próprio código já deixa claro.
- **Consistência:** respeite os padrões e a arquitetura já existentes no projeto. Não introduza novas tecnologias ou bibliotecas sem necessidade.
- **Complexidade controlada:** antes de implementar uma solução, avalie se existe uma alternativa mais simples que entregue o mesmo resultado.
- **Refatoração responsável:** não complique uma parte do sistema que já funciona apenas para aplicar um padrão mais sofisticado.
- **Transparência:** quando uma solução realmente exigir complexidade, explique o motivo, o funcionamento e como um desenvolvedor poderá mantê-la.

### Regra principal

O código não deve ser otimizado apenas para a IA produzir mais rápido ou com menos linhas. Deve ser otimizado para que seres humanos consigam compreender, testar, depurar e manter o sistema ao longo dos anos.

Pense como um desenvolvedor sênior que precisa entregar um sistema para uma equipe manter por muitos anos, e não como uma IA tentando resolver uma tarefa da maneira mais elaborada possível.

---

## Formatação para leitura

Código denso esconde onde as coisas acontecem. Escreva para quem vai ler, não para quem vai digitar.

- **Uma etapa por bloco.** Separe com linha em branco as fases de um método (ler, validar, decidir, gravar).
- **Métodos pequenos, com nome que diz o que fazem.** Se um método faz três coisas, divida em três (`buscarUsuario`, `validarPermissao`, `gravarAuditoria`).
- **Requisições e consultas montadas em variáveis com nome**, uma configuração por linha, e só depois executadas. Evite encadear tudo numa única linha ou dentro dos parênteses da chamada.
  ```
  // Evite
  resultado = banco.consultar(c -> c.tabela("pedidos").filtro("cliente = :c").limite(1));

  // Prefira
  consulta = Consulta.nova()
      .tabela("pedidos")
      .filtro("cliente = :c")
      .limite(1)            // só o mais recente
      .montar();

  resultado = banco.consultar(consulta);
  ```
- **Nomes do negócio, não abreviações.** `numeroDoPedido`, não `n`; `ehClienteVip`, não uma condição inline repetida; `ultimoEventoDoSnapshot`, não `base`.
- **Condições com nome.** Guarde expressões booleanas em variáveis (`boolean saldoInsuficiente = ...`) antes do `if`.
- **Um nível de abstração por método.** Um método de alto nível lê como uma receita; os detalhes ficam nos métodos que ele chama.
- **Classes e tipos aninhados viram arquivos próprios**, organizados por pasta ou package conforme o papel (controlador, serviço, modelo, exceção).
- **Comentários à direita só para o que não é óbvio**, e blocos de comentário curtos para explicar o porquê de uma decisão.
- **Respeite o formatador do projeto** (Prettier, Black, gofmt, Spotless etc.). Não reformate arquivos inteiros fora do escopo da tarefa.

---

## Testes

Teste existe para provar que o sistema se comporta como esperado em todos os cenários, não só no caminho feliz.

- **Liste os cenários antes de escrever os testes.** Faça uma tabela simples (entrada, resultado esperado) com o caminho feliz, cada erro possível, cada regra de negócio e cada código de resposta. Depois confira se todas as linhas viraram teste.
- **Valide a mesma regra com valores diferentes**, não com um valor só:
  - valor típico (o caso comum);
  - valores de borda (0, 1, o limite exato, um acima e um abaixo do limite);
  - valores inválidos (negativo, vazio, nulo, texto no lugar de número, formato errado, muito grande);
  - estados diferentes (registro novo, registro existente, repetido, já processado).
  Use testes parametrizados quando a linguagem permitir (`@ParameterizedTest`, `pytest.mark.parametrize`, `test.each`, tabelas em Go), para ver todos os valores lado a lado.
- **Inclua testes black box.** Teste o sistema pelo contrato público, como um usuário ou outro sistema o usaria (requisição HTTP, comando de linha, função ou mensagem pública), olhando só o resultado observável: resposta, código de status e efeito gravado (banco, fila, arquivo). O teste não deve depender de como o código está organizado por dentro, para continuar válido depois de uma refatoração.
- **Combine com testes de unidade (white box)** para regras de negócio e cálculos isolados, que são rápidos e apontam exatamente onde quebrou.
- **Fluxos assíncronos** (filas, streams, jobs): o teste aguarda o efeito com um tempo limite explícito, nunca com uma pausa fixa e arbitrária.
- **Um teste, um comportamento.** O nome diz o cenário e o resultado esperado (`saque_maior_que_saldo_retorna_422`). Os testes não dependem da ordem de execução nem de dados deixados por outro teste.
- **Todo bug corrigido ganha um teste** que falha sem a correção.
- **Nunca altere um teste só para ele passar.** Se o teste e o código discordam, descubra qual está certo. Se algum cenário não puder ser coberto, diga qual e por quê.

---

## Visão geral do projeto

### Arquitetura e fluxo principal

1. `GenerateBrunoCollectionAction` (`AnAction`, `DumbAware`) é o ponto de entrada: carrega as configurações, roda o scan, abre o diálogo e chama o writer.
2. `scanner/ControllerScanner` percorre o PSI e devolve `List<Endpoint>` (`model/Endpoint.kt`): método/caminho HTTP, parâmetros (path, query, header, cookie), corpos JSON ou multipart (objetos aninhados, listas), restrições do Bean Validation, anotações Jackson (`@JsonProperty`, `@JsonIgnore`, `READ_ONLY`) e metadados springdoc/OpenAPI 3 (`@Operation`, `@Parameter`, `@Schema`).
3. `ui/EndpointSelectionDialog` é um diálogo único para escolher endpoints, com diff ao vivo (adicionados/removidos/mantidos) que muda quando o usuário troca a estratégia de pastas daquela execução.
4. `writer/BrunoWriter` gera os `.bru` e calcula o `CollectionDiff`. É dele a nomeação estável dos arquivos (método + caminho), o layout por estratégia de pastas e a lógica de regeneração (sobrescrever ou só adicionar). Os valores de exemplo vêm de `writer/ExampleValues`.
5. `settings/` guarda o estado persistido por projeto (`BrunoGeneratorState`); `BrunoGeneratorConfigurable` é a página Settings > Tools > Bruno Generator.

Falhas: o scan e a escrita rodam com indicador de progresso; os erros são mostrados por notificação (grupo "Bruno Generator"), e exceções não tratadas podem ser reportadas pelo JetBrains Marketplace (`errorHandler` no `plugin.xml`).

### Estrutura de pastas

```
src/main/kotlin/         (packages com.codeteam.*; as pastas não são aninhadas assim)
  GenerateBrunoCollectionAction.kt   ação do menu Tools
  MyMessageBundle.kt                 acesso às mensagens de tela
  model/      Endpoint e tipos de parâmetro/corpo
  scanner/    ControllerScanner (PSI -> Endpoint)
  ui/         EndpointSelectionDialog, EndpointPresentation
  writer/     BrunoWriter, ExampleValues
  settings/   estado persistido e página de configurações
src/main/resources/
  META-INF/plugin.xml                registro de ação, configurable e serviço
  messages/MyMessageBundle.properties  textos para o usuário
src/test/kotlin/         testes espelhando os packages acima
openspec/                specs (specs/) e mudanças (changes/, archive/)
docs/agents/             configuração das skills de agentes; docs/images/ imagens do README
knowledge-base.md        arquitetura detalhada (pt)
```

### Tecnologias e versões

- Kotlin + Gradle (Kotlin DSL), plugin `org.jetbrains.intellij.platform`; IntelliJ Platform **2025.3.1** (`platformVersion` em `gradle.properties`), `pluginSinceBuild=253`, Gradle 9.7.0.
- O build roda no **JDK 25**, mas o bytecode precisa ficar em **Java 21**, porque a JBR da IDE 2025.3.1 é JDK 21; senão dá `UnsupportedClassVersionError` ao carregar o plugin. O override do `jvmTarget` do Kotlin fica em `afterEvaluate` de propósito (o plugin do IntelliJ define o próprio alvo durante a avaliação e venceria um bloco no topo). Não mova.
- Dependências da plataforma: plugin bundled `com.intellij.java`. Nos testes: Spring Web 6.2.1, Jakarta Validation 3.1.1, Jackson annotations 2.18.2, Swagger annotations 2.2.28, JUnit 4.13.2.
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
- `ui/UiScreenshotsTest` só roda com `-Dbruno.screenshots=<pasta>` e precisa de display; serve para gerar as imagens do README.
- Não existe teste automatizado do plugin dentro de uma IDE completa com um projeto Spring real (ponta a ponta). Não verificado se os cenários de valores de borda/inválidos estão cobertos em todas as regras de `ExampleValues`; revise ao mexer nelas.

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
- Vocabulário de domínio: `GLOSSARY.md` e `docs/adr/` na raiz, quando existirem (hoje não existem). Veja `docs/agents/domain.md`.
- Testes: JUnit4; scanner com `BasePlatformTestCase`; ficam em `src/test/kotlin/<package>/`.

---

## Armadilhas já encontradas

- **Bytecode Java 21 com JDK 25:** compilar com alvo mais novo gera um plugin que compila e empacota, mas falha ao carregar na IDE. Veja "Tecnologias e versões".
- **Gradle sempre com `--no-daemon` e via `bash ./gradlew`**, como nos comandos acima.
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
3. Mexeu em algo coberto por teste? Rode os testes. Criou lógica nova relevante ou corrigiu um bug? Escreva os testes seguindo a seção "Testes": todos os cenários, com valores diferentes (típicos, de borda e inválidos) e pelo menos um teste black box pelo contrato público.
4. Não renomeou nem alterou nenhum dado ou contrato listado na seção "Dados e contratos que NÃO podem mudar"?
5. Atualizou este arquivo se algo descrito nele mudou?
6. Contou ao usuário o que NÃO foi verificado e qual risco isso deixa?
