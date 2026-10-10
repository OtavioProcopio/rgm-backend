# Checklist — Segurança e robustez da API / Segurança

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Quem pode o quê

- [ ] A spec diz exatamente quais quatro rotas ficam restritas a gestor e administrador, e quais perfis recebem 403
- [ ] A spec diz que sem credencial válida a resposta continua 401 (não vira 403)
- [ ] A spec diz o que o operador deixa de ver e confirma que o frontend o esconde (Esclarecimento 1)
- [ ] Gestor e administrador seguem com acesso idêntico ao de hoje aos indicadores

## Exposição

- [ ] Só `/actuator/health` responde sem credencial, e o RNF-02 o torna mensurável
- [ ] A conta de coleta é distinta de usuário humano, e a spec diz que sem credencial configurada ninguém coleta
- [ ] Um administrador humano **não** abre `/actuator/prometheus` pela sessão dele (cenário próprio)
- [ ] O Swagger UI não é servido em produção e segue em desenvolvimento (RF-10)

## Vazamento de informação

- [ ] A mensagem de erro de enumeração inválida não expõe nome de classe ou pacote (RF-08)
- [ ] A spec não pede e não permite que a mensagem de erro revele se um usuário existe (isso é a #100, fora de escopo)
- [ ] O corpo do 401 e do 403 não traz detalhe interno

## Robustez

- [ ] Nenhuma entrada inválida do cliente produz 5xx (RNF-01), com os casos listados
- [ ] O teto de 100 itens por página vale para as quatro listagens paginadas, e a spec as nomeia
- [ ] O pedido de `size` acima do teto é **recusado** (400) e não silenciosamente reduzido
