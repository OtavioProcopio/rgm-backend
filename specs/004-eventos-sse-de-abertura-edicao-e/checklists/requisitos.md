# Checklist — Eventos SSE de abertura, edição e responsáveis / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-04 e RF-07 a RF-11 têm cenário que cita o seu ID
- [ ] A spec diz como RF-05 e RF-06 são verificados
- [ ] RNF-01 a RNF-04 têm número e unidade
- [ ] "Fora de escopo" cita evidência excluída e a rgm-frontend#113

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] Os cinco tipos de aviso novos estão escritos por extenso
- [ ] A spec diz por que o aviso de atividade usa um nome de evento próprio
- [ ] Nenhum requisito cita classe, método ou biblioteca

## Consistência

- [ ] Os nomes dos tipos novos seguem o padrão dos cinco existentes
- [ ] RF-05 e RNF-01 dizem a mesma coisa sobre os avisos existentes

## Testabilidade

- [ ] Cada cenário informa o código de resposta e o aviso esperado
- [ ] RNF-03 (exatamente 1 aviso) pode ser verificado por contagem
