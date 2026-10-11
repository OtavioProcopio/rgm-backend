# openspec/ — registro histórico (congelado)

Esta pasta descreve o comportamento do sistema **até a v1.5.0** e não recebe mais
alterações. O padrão de especificação do repositório é o Byte Union: funcionalidade nova
nasce em `specs/NNN-slug/` (spec, plan, checklists e tasks), conforme o Princípio 12 de
`.specify/project.md`.

- `specs/` — as 10 capacidades do sistema na v1.5.0. Leia como ponto de partida; o que
  mudou depois está na tabela abaixo.
- As 3 mudanças feitas em OpenSpec (`changes/archive/`) e o `config.yaml` da ferramenta foram
  removidos: todas estavam entregues e sincronizadas em `specs/`, e o histórico segue
  recuperável pelo git. Os comandos `/opsx:*` e as skills `openspec-*` também saíram de `.claude/`.

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
| `metricas-dashboard` | `specs/011-painel-de-metricas-por-periodo-e` — painel por período (`GET /api/solicitacoes/metricas/painel`, 7, 30 ou 90 dias) com o período anterior na mesma resposta, concluídas pela data de conclusão, tempo de resolução em segundos, abertas e atrasadas por dia e agora; histórico no fuso da fábrica com `dias` de 1 a 365 e agregado no banco; solicitação de criação fora do tempo médio e do intervalo do modelo |
| `solicitacoes-kanban` | `specs/011-painel-de-metricas-por-periodo-e` — a listagem de solicitações ordena por `sort` (criação, prazo limite, prioridade) e `dir`; sem `sort`, a ordem de antes; sem prioridade vai sempre para o fim |
| `autenticacao` | `specs/012-correcao-dos-bugs-abertos-do-backend` — o primeiro administrador de produção vem de `ADMIN_EMAIL` e `ADMIN_SENHA` (sem senha válida, nada é criado; `admin123` é recusada); conta inativa com senha errada responde igual a e-mail inexistente |
| `evidencias` | `specs/012-correcao-dos-bugs-abertos-do-backend` — tipo obrigatório, conteúdo conferido contra o tipo declarado e nome do objeto gerado pelo servidor |
| `modelos` | `specs/012-correcao-dos-bugs-abertos-do-backend` — versão única por máquina e código (maior + 1), edição que colide responde 409, modelo de máquina inativa continua editável sem trocar a máquina; o tipo do modelo vira catálogo administrável como a máquina (`/api/tipos-modelo`, `/api/admin/tipos-modelo`) e renomear máquina ou tipo atualiza os modelos |
| `solicitacoes-kanban` | `specs/012-correcao-dos-bugs-abertos-do-backend` — busca `q`, `sort=concluidaEm`, código, máquina e versão do modelo na listagem e no detalhe, `GET /api/solicitacoes/responsaveis-disponiveis`, edição com histórico (operador só em A Fazer), troca de responsáveis só depois da triagem, mesmo histórico ao cancelar pelos dois caminhos |
| `metricas-dashboard` | `specs/012-correcao-dos-bugs-abertos-do-backend` — `/api/solicitacoes/metricas` traz `emAbertoPorTipo` e `emAbertoPorPrioridade` |
| `metricas-dashboard` | `specs/013-indicadores-de-gestao-relatorios-em-pdf` — `GET /api/solicitacoes/metricas/gestao` (prazo cumprido, mediana e p90, idade do backlog, tempo por etapa, devolução, por máquina, reincidentes e carga por responsável; 7, 30 ou 90 dias) |
| `solicitacoes-kanban` | `specs/013-indicadores-de-gestao-relatorios-em-pdf` — o PDF da lista segue os filtros, a busca `q` e a ordenação da tela e escreve os filtros e o total no cabeçalho; sem corte silencioso (aviso acima de 5000 linhas) |
| `modelos` | `specs/013-indicadores-de-gestao-relatorios-em-pdf` — a lista em PDF acompanha os filtros e traz o tipo; a ficha traz histórico e fotos da galeria |
