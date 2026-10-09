# Checklist — Nome dos responsáveis na solicitação e capa da galeria sem erro / requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [x] Todo requisito funcional tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO
- [x] Todo requisito não funcional tem critério mensurável, com número e unidade
- [x] O que está fora de escopo está escrito

## Clareza

- [x] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [x] Nenhum requisito admite duas leituras conflitantes
- [x] Nenhum requisito descreve implementação em vez de comportamento

## Consistência

- [x] Nenhum requisito contradiz outro
- [x] Nenhum requisito contradiz a constituição
- [x] Vocabulário do domínio é o mesmo em todo o documento

## Testabilidade

- [x] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [x] Todo caminho de erro relevante tem cenário próprio

## Específicos desta feature

- [x] RF-01 a RF-04 deixam claro que o contrato antigo (`responsavelIds`, `abertaPorUsuarioId`) não muda
- [x] RF-05 e RF-12 definem lista vazia, nome nulo e preservação de ordem e tamanho sem lacuna
- [x] RF-08 diz que eventos e respostas de ação não ganham os campos novos, e há cenário para isso
- [x] RF-09 e RF-10 descrevem o resultado da troca de capa no sucesso e na falha, e há cenário para cada um
- [x] O cenário "Falha ao gravar a nova capa" é executável: a falha provocada está definida em Esclarecimentos
- [x] A divisão entre as duas issues está clara: nenhum requisito mistura #114 e #115
