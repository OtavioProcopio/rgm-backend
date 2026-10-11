# Checklist — Usuário desativado perde o acesso imediatamente / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-04 têm cenário que cita o seu ID
- [ ] A spec diz como RF-05 é verificado
- [ ] RNF-01 a RNF-04 têm número e unidade
- [ ] "Fora de escopo" cita as conexões de tempo real já abertas e a mudança de perfil

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] A spec diz qual resposta o usuário desativado recebe (401)
- [ ] Nenhum requisito cita classe, método ou biblioteca

## Consistência

- [ ] RF-03 e RNF-03 garantem que nada muda para usuário ativo
- [ ] A spec é coerente com a feature 005 (usuário inativo sem ações permitidas)

## Testabilidade

- [ ] Há cenário para usuário ativo, inativo e inexistente, na chamada comum e na conexão de tempo real
- [ ] RNF-01 pode ser verificado no log de acesso
