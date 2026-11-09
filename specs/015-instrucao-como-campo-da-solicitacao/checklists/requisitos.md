# Checklist — Instrução da triagem como campo da solicitação / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md` e `plan.md`;
> o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-09) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO
- [ ] Todo requisito não funcional (RNF-01 a RNF-06) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito (editar a instrução, preencher as antigas, remover comentários já gravados, front, editar/excluir comentário)
- [ ] A quebra do corpo de `/triar` e a ordem de publicação com o front estão ditas (Princípio 9)
- [ ] O que o cliente vê numa solicitação triada antes do campo está dito (`instrucao` nula) [LACUNA: a spec não diz se o PDF da ficha mostra algo no lugar; o plano decide por não mostrar linha]

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] "Só em A_FAZER" (RF-04) lista os status recusados [LACUNA: os cenários citam `EM_ANDAMENTO`, `EM_VALIDACAO`, `CONCLUIDA` e `CANCELADA`; o texto do requisito diz só "qualquer outro status"]
- [ ] RNF-01 diz o que conta como "ponto" de construção em produção (fábricas e reidratação)
- [ ] Nenhum requisito funcional descreve implementação; o padrão de cópia está só no plano

## Consistência

- [ ] RF-05 (sem comentário) e a spec 014 (RF-03, com comentário) não se contradizem: esta substitui aquela, e o plano ajusta a linha do `openspec/README.md`
- [ ] RF-08 não contradiz a ação "editar solicitação": ela muda título e descrição, nunca a instrução
- [ ] Nenhum requisito contradiz a constituição, em especial os Princípios 7 e 9

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro tem cenário próprio (400, 409 por status, corpo antigo, regras antigas)
- [ ] RNF-01 e RNF-02 são verificáveis por busca de texto, registrada na convergência
- [ ] Nenhuma tarefa de teste existe só para cobrir linha: cada uma nomeia o comportamento que prova
