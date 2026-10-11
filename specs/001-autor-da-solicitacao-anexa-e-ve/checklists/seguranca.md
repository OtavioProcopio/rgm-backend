# Checklist — Autor da solicitação anexa e vê evidências / Segurança

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Controle de acesso

- [x] A spec diz, para cada papel (autor, responsável, GESTOR, ADMINISTRADOR, operador sem relação), se pode anexar e se pode listar
- [x] A ampliação de acesso se limita ao autor da solicitação; nenhum outro papel ganha permissão
- [x] O autor não responsável não pode anexar os tipos que comprovam serviço (INSTRUCAO_SERVICO, SERVICO_REALIZADO, CONCLUSAO, DEVOLUCAO)
- [x] Usuário inativo é recusado no anexo e na listagem
- [x] A regra de exclusão de evidência está declarada como inalterada

## Exposição de dados

- [x] A listagem liberada ao autor se restringe às evidências da solicitação que ele abriu
- [x] A spec não altera quem pode ver ou listar solicitações
