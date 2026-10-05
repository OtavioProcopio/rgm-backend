# Checklist — Autor da solicitação anexa e vê evidências / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [x] RF-01 a RF-08 têm, cada um, ao menos um cenário em DADO/QUANDO/ENTÃO que cita o seu ID
- [x] RNF-01, RNF-02 e RNF-03 têm critério com número e unidade
- [x] A seção "Fora de escopo" cita as issues relacionadas (#90, #91 e rgm-frontend#112)
- [x] A spec diz o que acontece com o autor depois da triagem e depois do encerramento

## Clareza

- [x] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [x] RF-07 lista de forma fechada os tipos que o autor não responsável pode anexar (ABERTURA e GERAL)
- [x] RF-08 deixa claro que a recusa de usuário inativo vale para todos os perfis
- [x] Nenhum requisito cita classe, método, tabela ou biblioteca

## Consistência

- [x] RF-04 (não anexar em encerrada) não contradiz RF-02 (listar sempre)
- [x] RF-05 (acesso atual mantido) não contradiz RF-08 (inativo recusado)
- [x] Nenhum requisito contradiz o Princípio 9 da constituição (compatibilidade com a v1.5.0)
- [x] Os termos autor, responsável atribuído e solicitação encerrada têm o mesmo sentido em todo o documento

## Testabilidade

- [x] Todo cenário informa o código de resposta ou o resultado esperado de forma observável
- [x] Há cenário de recusa para: operador sem relação, tipo não permitido ao autor, solicitação encerrada e usuário inativo
- [ ] As métricas de sucesso podem ser conferidas em produção sem instrumentação nova
