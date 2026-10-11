# Checklist — Id do usuário na resposta de login / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01, RF-02 e RF-03 têm, cada um, um cenário em DADO/QUANDO/ENTÃO que cita o seu ID
- [ ] RNF-01, RNF-02 e RNF-03 têm critério com número e unidade
- [ ] "Fora de escopo" diz que a resposta de renovação de token não muda

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] O nome do campo novo (`id`) está escrito na spec
- [ ] Nenhum requisito cita classe, método ou biblioteca

## Consistência

- [ ] RF-01 (campo novo) não contradiz RNF-01 (nenhum campo removido ou renomeado)
- [ ] Nenhum requisito contradiz o Princípio 9 da constituição

## Testabilidade

- [ ] Todo cenário informa o código de resposta esperado
- [ ] Há cenário para a falha de login
