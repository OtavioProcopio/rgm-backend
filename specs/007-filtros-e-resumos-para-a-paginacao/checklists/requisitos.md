# Checklist — Filtros e resumos para a paginação / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-08 e RF-10 a RF-12 têm cenário que os exercita
- [ ] A spec diz como RF-09 é verificado
- [ ] RNF-01 a RNF-03 têm número e unidade
- [ ] "Fora de escopo" cita o limite de tamanho de página (rgm-backend#89)

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] A spec diz quais status contam como "em aberto"
- [ ] A spec diz o que acontece ao combinar "em aberto" com um status
- [ ] Nenhum requisito cita classe, método, consulta ou biblioteca

## Consistência

- [ ] RF-03 e RNF-02 garantem que nada muda para quem não usa os filtros novos
- [ ] A regra dos tempos do resumo do modelo é a mesma em toda a spec
- [ ] A spec é coerente com a feature 005 do frontend (RF-07, RF-10, RF-13 e RF-14 de lá)

## Testabilidade

- [ ] Há cenário para o filtro ligado, desligado e combinado com outro filtro
- [ ] Há cenário para modelo sem solicitações concluídas, com uma única concluída e para modelo inexistente
- [ ] RNF-01 pode ser verificado contando consultas num teste
