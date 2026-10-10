# Checklist — Segurança e robustez da API / Operação

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Publicação

- [ ] A ordem frontend depois backend está escrita e tem justificativa (a tela de perfil chama os indicadores para qualquer perfil)
- [ ] A spec diz que, publicado o frontend sozinho, nada quebra (ele só deixa de pedir)
- [ ] A spec diz o que acontece com o frontend antigo se o backend novo for publicado antes: a seção "Visão Geral do Sistema" do operador some por erro, sem derrubar a sessão

## Configuração

- [ ] As duas variáveis de ambiente da conta de coleta são opcionais e vazias por padrão
- [ ] Produção não precisa configurá-las, e a spec diz isso
- [ ] O `rgm-infra` está declarado como fora de escopo e a razão (não é usado) está registrada

## Compatibilidade

- [ ] Nenhuma rota, campo ou evento existente é removido ou renomeado (RF-09, RNF-05)
- [ ] A spec não pede migração de banco
- [ ] O limite de 100 por página é compatível com o maior pedido atual do frontend (100)

## Medição

- [ ] A seção "Métricas de sucesso" diz como cada número será medido
- [ ] A ausência de operador deslogado por 401 de permissão é observável em algum registro existente
