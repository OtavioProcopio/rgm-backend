# Checklist — Segurança e robustez da API / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] Todo requisito funcional (RF-01 a RF-10, com o RF-04 retirado) tem ao menos um critério de aceite em DADO/QUANDO/ENTÃO
- [ ] Todo requisito não funcional (RNF-01 a RNF-05) tem critério mensurável, com número e unidade
- [ ] O que está fora de escopo está escrito (#100, #101, métricas novas, #93, relatórios em PDF, totais do operador, `rgm-infra`)
- [ ] A tabela "Fatos medidos" lista os 12 casos de entrada inválida que o RNF-01 cita
- [ ] A ordem de publicação entre frontend e backend está escrita (RF-06)

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante (a seção Ambiguidades diz "nenhuma em aberto")
- [ ] "Credencial de coleta própria" (RF-03) distingue o usuário de serviço de qualquer usuário humano
- [ ] "Produção" (RF-10) é identificável sem suposição: a configuração de produção da aplicação
- [ ] Nenhum requisito descreve implementação em vez de comportamento (sem nome de classe, biblioteca ou arquivo de código)

## Consistência

- [ ] O RF-01 (só gestor e administrador nos indicadores) não contradiz o Esclarecimento 3 (PDFs de lista fora desta spec)
- [ ] O RF-02 (403 com corpo padrão) é coerente com o cenário "Sem credencial continua 401"
- [ ] O RF-07 (`size` de 1 a 100, acima disso 400) é coerente com o RF-05 (nenhuma entrada inválida vira 5xx)
- [ ] O RF-09 (nada removido ou renomeado) é coerente com o RF-02: trocar 401 por 403 em recusa de permissão não conta como quebra de contrato
- [ ] Nenhum requisito contradiz a constituição (Princípios 9 e 10 em especial)
- [ ] O RF-04 retirado não deixa cenário nem referência órfã

## Testabilidade

- [ ] Todo critério de aceite pode virar cenário executável sem reinterpretação
- [ ] Todo caminho de erro relevante tem cenário próprio (corpo malformado, data inválida, página fora do intervalo, enumeração inválida)
- [ ] O cenário do frontend (RF-06) deixa claro que é verificado no `rgm-frontend`, não neste repositório
