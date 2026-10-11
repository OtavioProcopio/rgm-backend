# Checklist — Heartbeat nas conexões SSE / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-03 têm cenário que cita o seu ID
- [ ] A spec declara por que RF-04 não tem cenário automatizado
- [ ] RNF-01 a RNF-04 têm número e unidade
- [ ] "Fora de escopo" cita a rgm-frontend#113 e a #87

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] A spec diz que o sinal não é um evento de solicitação
- [ ] Nenhum requisito cita classe, método ou biblioteca

## Consistência

- [ ] RF-01 (sinal novo) não contradiz RNF-03 (nenhum evento removido ou renomeado)
- [ ] O intervalo de 25 s é menor que o limite de 60 s citado no problema

## Testabilidade

- [ ] Todo cenário tem resultado observável
- [ ] Há cenário para falha de envio e para conexão encerrada
