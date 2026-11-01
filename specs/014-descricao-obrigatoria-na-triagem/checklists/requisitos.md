# Checklist — Descrição do que fazer na triagem / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md` e `plan.md`;
> o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-04) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO
- [ ] Todo requisito não funcional (RNF-01 a RNF-03) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito (campo próprio, tipo de atividade novo, edição posterior, outras ações, triagens antigas)
- [ ] A ordem de publicação com o front está dita e a quebra está declarada (Princípio 9)

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] "Fica registrada no histórico" (RF-03) diz o que o leitor do histórico vê [LACUNA: a spec diz "histórico", o plano diz atividade de comentário; o revisor decide se a spec deve exigir que seja distinguível de um comentário comum]
- [ ] Nenhum requisito descreve implementação em vez de comportamento

## Consistência

- [ ] RF-04 não contradiz RF-01: perfil sem permissão recebe a recusa de antes, mesmo com descrição válida
- [ ] Nenhum requisito contradiz a constituição, em especial o Princípio 9
- [ ] O registro de que o código veio antes da spec está no topo e será repetido na convergência

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro tem cenário próprio (ausente, em branco, 2001, regra antiga)
