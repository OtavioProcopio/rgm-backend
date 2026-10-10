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
| `autenticacao` | `specs/002-id-do-usuario-na-resposta-de`, `specs/006-acoes-recusam-usuario-inativo`, `specs/008-visibilidade-do-operador-senha-e-sessao` (senha mínima de 8; trocar a senha invalida os tokens anteriores; o perfil de cada chamada é o atual), `specs/010-seguranca-e-robustez-da-api-permissoes` (401 e 403 trazem corpo JSON padrão; operador autenticado sem permissão recebe 403, não 401; `/actuator/prometheus` e o índice do actuator exigem a conta de coleta, só `health` é anônimo) |
| `solicitacoes-kanban` | `specs/003-heartbeat-nas-conexoes-sse`, `specs/004-eventos-sse-de-abertura-edicao-e`, `specs/005-acoes-permitidas-na-solicitacao`, `specs/007-filtros-e-resumos-para-a-paginacao` (filtro por data de encerramento inclui canceladas), `specs/008-visibilidade-do-operador-senha-e-sessao` (operador lista, lê e recebe eventos só do que abriu ou de que é responsável; limites de tamanho nos textos), `specs/009-nome-dos-responsaveis-na-solicitacao-e` (detalhe e listagem devolvem `responsaveis` com nome e `abertaPorNome`, além dos ids), `specs/010-seguranca-e-robustez-da-api-permissoes` (`size` de 1 a 100; entradas inválidas de corpo, data, enumeração e página respondem 400 com o campo e o formato esperado) |
| `metricas-dashboard` | `specs/010-seguranca-e-robustez-da-api-permissoes` (indicadores agregados `/api/solicitacoes/metricas`, `/historico`, `/por-modelo` e o PDF só para gestor e administrador; operador recebe 403) |
| `modelos` | `specs/007-filtros-e-resumos-para-a-paginacao` (tempos da ficha em PDF seguem a regra do ranking), `specs/008-visibilidade-do-operador-senha-e-sessao` (limites de tamanho nos textos) |
| `galeria-modelo` | `specs/009-nome-dos-responsaveis-na-solicitacao-e` (definir a foto de capa funciona e é atômico: a capa anterior só sai se a nova for gravada) |
