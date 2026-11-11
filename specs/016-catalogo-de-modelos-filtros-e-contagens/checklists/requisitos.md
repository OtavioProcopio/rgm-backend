# Checklist — Catálogo de modelos: filtros, busca e contagens / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md` e `plan.md`;
> o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-10) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO [LACUNA: RF-10 (acesso) não tem cenário próprio; é coberto por teste do controller]
- [ ] Todo requisito não funcional (RNF-01 a RNF-05) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito (situação, pendência, índice, formato da resposta, administração)
- [ ] A forma de publicação com o front está dita (aditivo, backend primeiro)
- [ ] O que acontece com `q` e `tipo` inexistentes está dito [LACUNA: `tipo` desconhecido devolve lista vazia, não 400; a spec não diz]

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] "Cada grupo ignora o próprio filtro" (RF-06) tem uma só definição e um cenário
- [ ] "Ordem alfabética do código" está declarada como texto, não natural ("A-10" antes de "A-2")
- [ ] Nenhum requisito descreve implementação em vez de comportamento

## Consistência

- [ ] RF-03 (igualdade) e a tela de administração (que segue "contém") não se contradizem
- [ ] RF-08 vale também com `ativo`, `codigo` e `descricao` (filtros de hoje) em qualquer combinação
- [ ] Nenhum requisito contradiz a constituição, em especial o Princípio 9

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro tem cenário próprio (`q` curto, `ordem` inválida)
- [ ] RNF-01 e RNF-02 são verificáveis por contagem de consultas
