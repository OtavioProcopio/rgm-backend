# Checklist — Nome dos responsáveis na solicitação e capa da galeria sem erro / dados

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Exposição e compatibilidade

- [x] Os campos novos expõem apenas identificador e nome, e o nome já era visível ao operador (histórico e comentários de atribuição)
- [x] Nenhum campo existente muda de nome, tipo ou valor (RNF-02) e a spec não exige migração de banco
- [x] Usuário inativo tem o nome exibido de forma explícita (RF-06), sem expor a situação dele
- [x] O operador só recebe os campos em solicitações que já pode ler (RF-07)

## Integridade da capa

- [x] RNF-03 deixa explícito que nunca há duas capas num modelo, nem depois de falha
- [x] A spec diz o que acontece com legenda e `principal: false` (RF-11)
