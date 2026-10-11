# Checklist — Correção dos bugs abertos e melhorias para o frontend / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

> Itens marcados `[LACUNA]` são pontos que o agente achou ao cruzar `spec.md`, `plan.md` e
> `tasks.md`; o revisor decide se viram requisito, ajuste de texto ou são aceitos como estão.

## Completude

- [x] Todo requisito funcional (RF-01 a RF-31) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO [LACUNA: RF-02 (senha fora da política em produção), RF-06 (limite de tentativas), RF-13 (máquina aparada), RF-24 (`size` acima de 100) e RF-28 (mensagem lista `concluidaEm`, coberta pelo cenário "Ordenação inválida") não têm cenário próprio; o plano os cobre por teste de unidade]
- [x] Todo requisito não funcional (RNF-01 a RNF-08) tem critério mensurável, com número e unidade
- [x] O que está fora de escopo está escrito e cita a issue de cada item deixado de fora
- [x] O formato novo da resposta está descrito [LACUNA: a spec diz "código, máquina e versão do modelo" e "contagem por tipo e por prioridade" sem nomear os campos; o plano define `modeloVersao`, `emAbertoPorTipo` e `emAbertoPorPrioridade`]
- [x] O que "perfil atribuível" significa está dito [LACUNA: a spec cita administrador e externo como excluídos; o plano usa a regra existente (`OPERADOR` e `GESTOR`)]

## Clareza

- [x] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [x] Os dois padrões adotados pelo redator (mensagem de conta inativa e corte das melhorias) foram aceitos pelo revisor
- [x] "Edição registrada no histórico" diz **como** aparece [LACUNA: o plano registra como atividade do tipo comentário, para não quebrar a linha do tempo da v1.5.0; a spec só pede autor, data e valores anteriores]
- [x] O nome do parâmetro de busca é `q` (issue #133) e a lista de campos da busca está fechada: título, descrição e código do modelo
- [x] Nenhum requisito descreve implementação em vez de comportamento

## Consistência

- [x] Nenhum requisito contradiz outro: RF-17 (responsáveis só depois da triagem) convive com o frontend, que usa a triagem em A Fazer (`acoesSolicitacao.ts`)
- [x] RF-20 (operador edita só em A Fazer) muda o que o operador consegue fazer hoje; a mudança é a decisão registrada no roteiro de 2026-10-10
- [x] RF-09 muda a forma da URL pública da evidência; o frontend usa o nome da evidência para exibir [LACUNA: conferir no e2e do frontend antes do deploy]
- [x] Nenhum requisito contradiz a constituição, em especial o Princípio 9 (frontend v1.5.0 em produção) e a regra de migração sequencial (V11)

## Testabilidade

- [x] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [x] A colisão de versão (RNF-05) é provada em Postgres real, pelo índice único, não com mock
- [x] O saneamento de duplicatas (RNF-08) é provado com um banco que já tem duplicatas antes da migração

> Revisado e aceito pelo usuário em 2026-10-11 ("ok", depois de ver as lacunas uma a uma).
