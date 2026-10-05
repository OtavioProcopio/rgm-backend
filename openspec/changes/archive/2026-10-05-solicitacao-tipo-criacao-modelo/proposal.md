## Why

Hoje um Modelo só nasce por um cadastro direto (`GerenciarModelosUseCase.criar`,
tela "Novo modelo"), sem passar por nenhum fluxo de aprovação/acompanhamento.
O usuário quer poder abrir uma **solicitação do tipo CRIAÇÃO** — um pedido
formal de "novo modelo" — que percorre o mesmo Kanban usado para manutenção
(Triagem → Em Andamento → Em Validação → Concluída) e, ao ser **concluída**,
gera automaticamente o cadastro do Modelo. Isso dá rastreabilidade e um
checklist de revisão antes do modelo existir de fato, em vez de um cadastro
direto sem trilha de auditoria.

## What Changes

- Novo valor `CRIACAO` em `TipoSolicitacao` (hoje: REPARO, INSPECAO,
  REENGENHARIA).
- Abrir uma solicitação de tipo CRIACAO passa a ser restrito a
  GESTOR/ADMINISTRADOR (diferente das demais, abertas por qualquer usuário
  interno) — mesma autoridade que já pode cadastrar um modelo diretamente
  hoje.
- Uma solicitação de tipo CRIACAO **nasce sem `modeloId`** (hoje
  obrigatório para todo tipo) e carrega, em vez disso, os dados do modelo a
  ser criado: código, máquina, descrição e observações — os mesmos campos
  que `GerenciarModelosUseCase.criar()` já exige.
- **BREAKING** (schema): `solicitacoes.modelo_id` passa de `NOT NULL` para
  nullable; novas colunas nullable armazenam os dados do modelo em
  preparação (só usadas quando `tipo = CRIACAO`).
- Ao concluir (EM_VALIDACAO → CONCLUIDA) uma solicitação de tipo CRIACAO, o
  sistema cria o Modelo com os dados carregados pela solicitação e associa
  `modeloId` ao registro recém-criado — a partir daí a solicitação se
  comporta como qualquer outra solicitação concluída, permanentemente ligada
  ao modelo que ela deu origem.
- O tipo de uma solicitação passa a ser imutável após a abertura (hoje é
  editável) — trocar de/para CRIACAO no meio do fluxo violaria a
  obrigatoriedade condicional de `modeloId`.
- Novo valor `CADASTRO` em `TipoEventoModelo`: ao nascer um Modelo — seja
  pelo cadastro direto (`GerenciarModelosUseCase.criar`), seja pela
  conclusão de uma solicitação CRIACAO — um evento CADASTRO é registrado
  automaticamente como o marco inicial do prontuário do modelo, com autor o
  usuário responsável pela criação.
- O cadastro direto de modelo (tela "Novo modelo") continua existindo sem
  mudanças — a solicitação de CRIACAO é um caminho adicional, não uma
  substituição.

## Capabilities

### New Capabilities

(nenhuma)

### Modified Capabilities

- `solicitacoes-kanban`: novo tipo CRIACAO com regras próprias de abertura
  (restrita a GESTOR/ADMINISTRADOR, sem modelo vinculado, carrega dados do
  modelo a criar) e de conclusão (gera o Modelo e vincula `modeloId`); tipo
  passa a ser imutável após a abertura.
- `eventos-modelo`: novo requisito — nascimento de um Modelo (por qualquer
  caminho) gera automaticamente um evento CADASTRO no seu histórico.

## Impact

- `TipoSolicitacao`: adicionar `CRIACAO`.
- `TipoEventoModelo`: adicionar `CADASTRO`.
- `Solicitacao` (agregado): `modeloId` deixa de ser `requireNonNull`
  incondicional — passa a ser obrigatório apenas quando `tipo != CRIACAO`,
  e nulo enquanto `tipo == CRIACAO` e status não-terminal; adicionar campos
  para os dados do modelo em preparação (código, máquina, descrição,
  observações), obrigatórios apenas quando `tipo == CRIACAO`.
- `db/migration/`: nova migration Flyway — `solicitacoes.modelo_id` vira
  nullable; novas colunas nullable para os dados do modelo em preparação;
  `CHECK` garantindo consistência (`tipo = CRIACAO` ⟺ `modelo_id` pode ser
  nulo antes de concluída).
- `AbrirSolicitacaoUseCase`: novo branch para tipo CRIACAO — validação de
  perfil (GESTOR/ADMINISTRADOR), validação dos dados do modelo (reaproveitar
  validações de `GerenciarModelosUseCase`), sem exigir/validar modelo
  existente.
- `EncerrarSolicitacaoUseCase`: ao concluir uma solicitação CRIACAO, invocar
  a criação do Modelo (reaproveitando `GerenciarModelosUseCase.criar`) antes
  de persistir a transição para CONCLUIDA, e gravar `modeloId` no
  resultado.
- `EditarSolicitacaoUseCase`: bloquear alteração de `tipo` após a abertura
  (novo invariante, independente de CRIACAO).
- `GerenciarModelosUseCase.criar`: passa a também registrar o evento
  CADASTRO (compartilhado com o caminho de conclusão de CRIACAO).
- Endpoints REST (`SolicitacaoController`, DTOs de request/response):
  aceitar e expor os novos campos quando `tipo = CRIACAO`; `modeloId`
  passa a ser opcional na resposta.
- Fora deste repo: `rgm-frontend` precisa de uma change equivalente —
  formulário de abertura com campos condicionais por tipo, card do Kanban
  sem modelo, e reconhecimento do novo tipo/evento nos labels e permissões
  (`shared/lib/permissions.ts`, `solicitacaoMessages.ts`,
  `KanbanCard.tsx`).
- Testes: `SolicitacaoTest`, `AbrirSolicitacaoUseCaseTest`,
  `EncerrarSolicitacaoUseCaseTest`, `EditarSolicitacaoUseCaseTest`,
  `GerenciarModelosUseCaseTest`, mappers/adapters de persistência — mantendo
  JaCoCo >= 95%.
