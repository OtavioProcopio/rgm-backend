# Checklist — Ações permitidas na solicitação / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-11 têm, cada um, ao menos um cenário que cita o seu ID
- [ ] As nove ações da issue aparecem nos requisitos
- [ ] RNF-01 a RNF-04 têm número e unidade
- [ ] "Fora de escopo" cita a rgm-frontend#115 e as pré-condições de dados

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] A spec diz o que significa o campo nulo
- [ ] Cada ação tem a condição de perfil e de status escrita
- [ ] Nenhum requisito cita classe, método ou biblioteca

## Consistência

- [ ] RF-09 é coerente com a feature 001 (acesso a evidências)
- [ ] RF-05 (cancelar pelo autor) é coerente com a regra atual de cancelamento
- [ ] Nenhum requisito muda regra de permissão existente
- [ ] Nenhum requisito contradiz o Princípio 9 da constituição

## Testabilidade

- [ ] Cada cenário de cálculo lista o conjunto exato de ações esperado
- [ ] Há cenário para usuário inativo, perfil EXTERNO e operador sem relação
- [ ] Há cenário para a listagem (campo nulo)
