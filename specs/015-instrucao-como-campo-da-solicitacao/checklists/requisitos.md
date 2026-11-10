# Checklist — Instrução da triagem como campo da solicitação / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md` e `plan.md`;
> o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-15) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO
- [ ] Todo requisito não funcional (RNF-01 a RNF-06) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito (editar fora de `EM_ANDAMENTO`, edição pelo responsável, desfazer, preencher as antigas, remover comentários já gravados, front, editar/excluir comentário)
- [ ] A quebra do corpo de `/triar` e a ordem de publicação com o front estão ditas (Princípio 9)
- [ ] O que o cliente vê numa solicitação triada antes do campo está dito (`instrucao` nula) [LACUNA: a spec não diz se o PDF da ficha mostra algo no lugar; o plano decide por não mostrar linha]

- [ ] A edição diz o que o responsável vê quando a instrução muda enquanto ele executa [LACUNA: a spec cobre histórico e evento em tempo real, mas não notifica ninguém; o revisor decide se isso basta]
- [ ] A edição diz o que acontece com um pedido duplicado ou com o mesmo texto (RF-13: 200 sem efeito)

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] "Só em A_FAZER" (RF-04) lista os status recusados [LACUNA: os cenários citam `EM_ANDAMENTO`, `EM_VALIDACAO`, `CONCLUIDA` e `CANCELADA`; o texto do requisito diz só "qualquer outro status"]
- [ ] RNF-01 diz o que conta como "ponto" de construção em produção (fábricas e reidratação)
- [ ] Nenhum requisito funcional descreve implementação; o padrão de cópia está só no plano

## Consistência

- [ ] RF-05 (sem comentário) e a spec 014 (RF-03, com comentário) não se contradizem: esta substitui aquela, e o plano ajusta a linha do `openspec/README.md`
- [ ] RF-08 não contradiz a ação "editar solicitação": ela muda título e descrição, nunca a instrução; só a edição de instrução (RF-10) a muda
- [ ] RF-11 (409 em `A_FAZER`) e RF-04 (triar só em `A_FAZER`) não deixam um status em que a instrução não possa ser definida nem corrigida de forma alguma sem aviso: em `EM_VALIDACAO` ela só volta a ser editável depois de devolver
- [ ] Nenhum requisito contradiz a constituição, em especial os Princípios 7 e 9

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro tem cenário próprio (400, 409 por status, 403, corpo antigo, regras antigas, edição concorrente)
- [ ] RNF-01 e RNF-02 são verificáveis por busca de texto, registrada na convergência
- [ ] Nenhuma tarefa de teste existe só para cobrir linha: cada uma nomeia o comportamento que prova
