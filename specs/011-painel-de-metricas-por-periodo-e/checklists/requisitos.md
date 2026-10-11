# Checklist — Painel por período e ordenação / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md` e `plan.md`;
> o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-15) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO [LACUNA: RF-04 (paginação estável), RF-10 (série de abertas e atrasadas por dia), RF-11 (filtro de modelo no histórico) e RF-15 (formato do histórico preservado) não têm cenário próprio]
- [ ] Todo requisito não funcional (RNF-01 a RNF-06) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito e cita a issue que cuida de cada item deixado de fora (#119 a #121, #123, #111, #102, #103, #100, #106, #105, #104, #149)
- [ ] O formato da resposta do painel (quais indicadores, por dia ou semana, atual e anterior) está descrito [LACUNA: a spec diz "os mesmos indicadores" sem listá-los; o plano define a forma e o frontend (#149) é o consumidor]
- [ ] O que significam "abertas agora" e "em atraso agora" para o **período anterior** está dito [LACUNA: são valores de agora, sem equivalente no período anterior; só a série ao fim de cada dia existe nos dois]
- [ ] A fronteira de semana e das semanas incompletas nas pontas do período de 90 dias está dita [LACUNA: o plano usa semana de segunda a domingo, como o histórico atual; a spec só diz "por semana"]

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] "Em atraso" tem uma só definição em todo o documento: SLA vencido, em aberto, com a prioridade de hoje
- [ ] O sentido de `dir` para `prioridade` está dito [LACUNA: a spec não diz se "crescente" é mais urgente primeiro; o plano escolhe URGENTE, ALTA, MEDIA, BAIXA em crescente, pelas horas do prazo, com sem-prioridade sempre no fim]
- [ ] Nenhum requisito descreve implementação em vez de comportamento
- [ ] "Tempo de resolução" tem uma só definição: da criação à conclusão, só das concluídas na janela

## Consistência

- [ ] Nenhum requisito contradiz outro: RF-13 (nada removido) convive com RF-15 (histórico muda de fuso e teto)
- [ ] O histórico existente muda de comportamento em dia de borda (fuso): isso é compatível com o Princípio 9 porque o formato não muda; a diferença de contagem por dia é a correção pedida (#108)
- [ ] Nenhum requisito contradiz a constituição, em especial o Princípio 9 (frontend v1.5.0 em produção)
- [ ] A restrição de perfil do painel reaproveita a regra da spec 010 sem criar regra nova

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro relevante tem cenário próprio: `sort` inválido, `dir` inválido, `dias` fora da faixa do painel, `dias` acima do teto do histórico, operador, anônimo [LACUNA: `dir` inválido e anônimo não têm cenário próprio]
- [ ] Os critérios de tempo (RNF-01) dizem como medir: com quantas linhas, em que ambiente e com qual percentil
