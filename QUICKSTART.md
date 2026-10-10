# Quickstart

**ENGLISH**

Get from zero to your first generated Bruno collection in a few steps. For the full feature list, settings, output details, and publishing, see the [README.md](README.md).

## For Users

Install the plugin and generate your first collection.

1. In IntelliJ IDEA, open **Settings > Plugins > Marketplace**, search for **Bruno Generator**, and install it (restart the IDE if prompted).
2. Open your Spring Boot project in IntelliJ IDEA.
3. Run **Tools > Generate Bruno Collection**.
4. Open the generated collection folder (named after your project) in [Bruno](https://www.usebruno.com/).
5. Switch environments in Bruno, or edit the `baseUrl` variable, to point requests at your API.

> Optional: configure environments and a custom output directory under **Settings > Tools > Bruno Generator**. See the [README.md](README.md) for details.

## For Developers

Clone the repository and run the plugin locally.

1. Clone the repository and open it in IntelliJ IDEA.
2. Launch a sandbox IDE with the plugin loaded:

   ```bash
   bash ./gradlew runIde --no-daemon
   ```

3. Build the distributable plugin artifact (output in `build/distributions/`):

   ```bash
   bash ./gradlew clean buildPlugin --no-daemon
   ```

For tests, publishing, and project layout, see the [README.md](README.md).

---

**PORTUGUÊS**

Vá do zero à sua primeira collection do Bruno gerada em poucos passos. Para a lista completa de funcionalidades, configurações, detalhes da saída e publicação, consulte o [README.md](README.md).

## Para Usuários

Instale o plugin e gere a sua primeira collection.

1. No IntelliJ IDEA, abra **Settings > Plugins > Marketplace**, procure por **Bruno Generator** e instale-o (reinicie a IDE se solicitado).
2. Abra o seu projeto Spring Boot no IntelliJ IDEA.
3. Execute **Tools > Generate Bruno Collection**.
4. Abra a pasta da collection gerada (com o nome do seu projeto) no [Bruno](https://www.usebruno.com/).
5. Troque de ambiente no Bruno, ou edite a variável `baseUrl`, para apontar os requests para a sua API.

> Opcional: configure ambientes e um diretório de saída customizado em **Settings > Tools > Bruno Generator**. Veja o [README.md](README.md) para detalhes.

## Para Desenvolvedores

Clone o repositório e rode o plugin localmente.

1. Clone o repositório e abra-o no IntelliJ IDEA.
2. Inicie uma IDE de teste (sandbox) com o plugin carregado:

   ```bash
   bash ./gradlew runIde --no-daemon
   ```

3. Gere o artefato de distribuição do plugin (saída em `build/distributions/`):

   ```bash
   bash ./gradlew clean buildPlugin --no-daemon
   ```

Para testes, publicação e estrutura do projeto, veja o [README.md](README.md).
