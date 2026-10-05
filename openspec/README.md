# openspec/ — registro histórico (congelado)

Esta pasta descreve o comportamento do sistema **até a v1.5.0** e não recebe mais
alterações. O padrão de especificação do repositório é o Byte Union: funcionalidade nova
nasce em `specs/NNN-slug/` (spec, plan, checklists e tasks), conforme o Princípio 12 de
`.specify/project.md`.

- `specs/` — as 10 capacidades do sistema na v1.5.0. Leia como ponto de partida; o que
  mudou depois está na tabela abaixo.
- `changes/archive/` — as 3 mudanças feitas em OpenSpec, todas entregues e sincronizadas.
  Não há mudança aberta.
- Os comandos `/opsx:*` e as skills `openspec-*` foram removidos de `.claude/`. O histórico
  segue recuperável pelo git.

## O que mudou depois da v1.5.0

Quando uma feature em `specs/` altera comportamento descrito aqui, vale a feature. A linha
entra nesta tabela na mesma entrega.

| Capacidade (`openspec/specs/`) | Feature que altera o comportamento |
|---|---|
| `evidencias` | `specs/001-autor-da-solicitacao-anexa-e-ve` |
| `autenticacao` | `specs/002-id-do-usuario-na-resposta-de`, `specs/006-acoes-recusam-usuario-inativo` |
| `solicitacoes-kanban` | `specs/003-heartbeat-nas-conexoes-sse`, `specs/004-eventos-sse-de-abertura-edicao-e`, `specs/005-acoes-permitidas-na-solicitacao` |
