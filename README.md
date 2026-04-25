# Bruno IntelliJ Plugin

Plugin para IntelliJ IDEA que gera uma collection do Bruno (`.bru`) a partir de controllers Spring.

## Funcionalidades

- Descobre endpoints `GET`, `POST`, `PUT`, `PATCH` e `DELETE` em classes `@RestController`
- Gera estrutura de collection em `bruno/requests`
- Cria body de exemplo para métodos com payload (`POST`, `PUT`, `PATCH`)
- Preenche exemplos com variáveis dinâmicas do Bruno (ex.: `{{$randomUUID}}`, `{{$randomEmail}}`)
- Adiciona a ação `Generate Bruno Collection` no menu `Tools` com ícone `pluginIcon.svg`

## Como usar

1. Abra o projeto Spring no IntelliJ IDEA.
2. Execute `Tools > Generate Bruno Collection`.
3. Abra a pasta `bruno/` no Bruno.
4. Defina `baseUrl` em variáveis da collection (pre-request vars).

## Estrutura gerada

```text
bruno/
  bruno.json
  requests/
	...arquivos .bru por endpoint
```

## Desenvolvimento local

Use os comandos abaixo na raiz do projeto:

```bash
bash ./gradlew test --no-daemon
bash ./gradlew runIde --no-daemon
```

## Build para distribuição

```bash
bash ./gradlew clean buildPlugin --no-daemon
```

Artefato gerado em:

- `build/distributions/`

## Publicação no JetBrains Marketplace

1. Ajuste metadados em `src/main/resources/META-INF/plugin.xml` (`id`, `name`, `vendor`, `description`).
2. Garanta o ícone em `src/main/resources/META-INF/pluginIcon.svg`.
3. Execute validações:

```bash
bash ./gradlew test verifyPlugin --no-daemon
```

4. Gere o pacote com `buildPlugin`.
5. Publique manualmente no Marketplace ou use `publishPlugin` com token configurado.

## Arquivos principais

- `src/main/kotlin/GenerateBrunoCollectionAction.kt`
- `src/main/kotlin/scanner/ControllerScanner.kt`
- `src/main/kotlin/writer/BrunoWriter.kt`
- `src/main/resources/META-INF/plugin.xml`
# bruno
