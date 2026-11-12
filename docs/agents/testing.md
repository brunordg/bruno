# Como escrever testes

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
