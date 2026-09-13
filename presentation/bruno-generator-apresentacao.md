---
marp: true
---

# Bruno Generator
### Da API ao teste, em um clique

Plugin para IntelliJ IDEA que gera coleções **Bruno** a partir dos seus controllers Spring

---

# 😩 O problema

Você acabou de criar um endpoint novo no Spring Boot.

Antes de testar, você precisa abrir o Bruno e montar o request **na mão**:
- método HTTP e URL
- path variables e query params
- headers e cookies
- e o corpo? Um JSON de exemplo... com objeto aninhado, lista, upload de arquivo?

E isso se repete a cada endpoint. Todo dia. Em todo projeto.

---

# O custo escondido

- **Tempo**: minutos por endpoint × dezenas de endpoints × todo o time
- **Divergência**: a collection vira uma "foto antiga" da API — diverge do código e engana quem testa
- **Erro humano**: valores inventados às pressas, header esquecido, validação ignorada

---

# 😤 A tensão: por que isso ainda é assim?

- Gerar a partir de OpenAPI/Swagger exige anotações extras e produz JSON genérico — e não fala a língua do Bruno
- Manter a documentação "na mão" não escala com a velocidade do time
- E se a geração automática **apagar sem avisar** o que alguém customizou na collection?

Regeneração automática sem controle é um risco real — ninguém confia numa ferramenta que possa te fazer perder trabalho.

---

# Uma jornada de tentativa e erro

- **v1.0** — gerava só o corpo básico: request "vazio demais" pra ser útil
- **v1.1** — passou a extrair path/query/header/cookie params e validações Bean Validation
- **Ambientes** — começou com 3 pré-configurados; o excesso virou fricção → reduzido para 1, com diretório de saída configurável
- Corpos aninhados, listas e multipart ainda caíam em placeholders "achatados"
- Regenerar a collection podia **apagar silenciosamente** requests customizados

---

# 💡 A virada: automatizar com segurança

**Bruno Generator**: um plugin IntelliJ que lê seus `@RestController` Spring
e gera a coleção Bruno completa — pronta pra testar.

Sem anotações extras. Sem JSON inventado. Sem perder o que você já customizou.

---

# Como funciona (1 clique)

1. `Tools > Generate Bruno Collection`
2. O plugin escaneia todos os `@RestController` do projeto (via PSI do IntelliJ)
3. Mostra uma tela de seleção com **diff ao vivo**: o que será Adicionado / Removido / Mantido
4. Você confirma → coleção gravada em disco, pronta pra abrir no Bruno

---

# O que ele entende do seu código

- Verbo HTTP e path completo (classe + método)
- Path variables, query params, headers, cookies
- Corpo do request: **objetos aninhados** (até 5 níveis, sem loop infinito), **listas**, **enums**
- **Multipart** / upload de arquivo
- **Bean Validation**: `@NotBlank`, `@Size`, `@Min/@Max`, `@Email`, `@Pattern`...

---

# Exemplos que fazem sentido, não "foo"/"bar"

- `email` → e-mail aleatório de verdade
- `id` → UUID ou inteiro aleatório
- Datas, preços, endereços, telefones → heurística por nome + tipo
- `enum` → primeira constante real
- Placeholders dinâmicos do Bruno: `{{$randomInt}}`, `{{$randomEmail}}`...

---

# Regenerar sem medo

- Tela de **diff ao vivo** antes de qualquer gravação em disco
- Prune inteligente: remove só o que não existe mais no código
- Nada é apagado sem você ver antes

---

# ✅ Evidência: não é promessa, é testado

Suíte de testes automatizados cobrindo:
- extração de params, headers, cookies e validações
- objetos aninhados e listas
- DTOs auto-referenciados **sem loop infinito**
- multipart e upload de arquivo
- geração de `.bru`, `bruno.json`, ambientes
- **prune** de arquivos obsoletos na regeneração
- diretório de saída customizado

`./gradlew test` — toda a suíte roda em segundos.

---

# Um plugin real, em evolução

- Publicado na **JetBrains Marketplace**
- De 1.0 a 1.2.0: mais de 15 commits de evolução real (features, correções, manutenção)
- Débito técnico tratado no caminho: APIs deprecated corrigidas, migração de Java, ícone ajustado às exigências do Marketplace

---

# 🚀 Ação: comece agora

1. Abra seu projeto Spring Boot no IntelliJ IDEA
2. *(Opcional)* Configure ambientes em `Settings > Tools > Bruno Generator`
3. Rode `Tools > Generate Bruno Collection`
4. Abra a pasta gerada no app **Bruno**
5. Teste sua API — sem digitar um JSON de exemplo na mão

---

# Bruno Generator
### Da API ao teste, em um clique

[link de instalação / JetBrains Marketplace]
