# Checklist — Visibilidade do operador, senha e sessão seguras e limites de texto / Segurança

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Acesso

- [ ] A spec não deixa nenhum caminho de leitura de solicitação fora da regra de RF-01 (listagem, detalhe, histórico, PDF, tempo real)
- [ ] A spec diz que as evidências já seguem a mesma regra e por isso não mudam
- [ ] A spec registra a escolha de "acesso negado" em vez de "não encontrado" e o que ela revela a quem tenta
- [ ] A spec diz que gestor e administrador não perdem acesso a nada (RF-07)

## Senha

- [ ] O mínimo de 8 caracteres vale nas três operações que definem senha (RF-08)
- [ ] A spec diz que não há regra de composição nem histórico de senhas, e que isso é escolha
- [ ] Nenhum perfil que faz login pode ser criado sem senha (RF-09)

## Sessão

- [ ] Depois de troca ou redefinição, a spec não deixa nenhum uso da credencial antiga aceito: chamada comum, abertura do tempo real, renovação e conexão de tempo real já aberta (RF-11, RF-21)
- [ ] A spec diz que o rebaixamento de perfil vale na primeira chamada seguinte (RNF-06)
- [ ] A spec registra que mudar o perfil não derruba a sessão e que isso diverge do pedido da #98, por decisão do usuário
- [ ] A spec diz o que acontece com quem troca a própria senha enquanto o frontend não guarda as credenciais novas

## Entrada

- [ ] Todo campo de texto livre que a API grava tem limite na tabela ou motivo escrito para ficar de fora
- [ ] A spec diz o que acontece com textos já gravados acima do limite
- [ ] A mensagem de erro de limite não expõe detalhe interno (RF-14, RF-17)
