# Padrões de código

Lido na revisão. O `AGENTS.md` aponta para cá.

## Simplicidade e legibilidade do código

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
- **Não reformate arquivos inteiros** fora do escopo da tarefa. O projeto não tem formatador automático configurado.
