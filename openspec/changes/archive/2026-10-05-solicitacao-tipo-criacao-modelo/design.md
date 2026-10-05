## Context

`Solicitacao.modeloId` hoje é `requireNonNull` no domínio e
`solicitacoes.modelo_id UUID NOT NULL REFERENCES modelos(id)` no schema
(V1__create_schema.sql). `Modelo.maquina` é uma `String` livre (validada
contra o catálogo em `GerenciarModelosUseCase.validarMaquina`, não uma FK —
ver V2/V3 migrations), e `GerenciarModelosUseCase.criar()` calcula
`versao = countByMaquinaAndCodigo(...) + 1`, ou seja, código+máquina
repetidos geram uma nova versão do mesmo item em vez de rejeitar. Ver
`proposal.md` para a motivação.

`eventos_modelo.tipo` é `VARCHAR(50)` sem `CHECK` — igual ao caso já
investigado na tentativa anterior deste change, adicionar `CADASTRO` ao
enum não exige migration de schema para essa coluna especificamente.

## Goals / Non-Goals

**Goals:**
- `Solicitacao` aceita `modeloId = null` se e somente se `tipo = CRIACAO` e
  status ainda não é CONCLUIDA.
- Abrir uma CRIACAO é restrito a GESTOR/ADMINISTRADOR; concluir qualquer
  solicitação (inclusive CRIACAO) já é restrito a GESTOR/ADMINISTRADOR pela
  regra existente de autorização de movimento — nenhuma mudança adicional
  necessária aí.
- Concluir uma CRIACAO cria o Modelo e preenche `modeloId` atomicamente com
  a transição para CONCLUIDA.
- Reaproveitar `GerenciarModelosUseCase.criar()` para a criação do modelo
  (mesma validação de máquina, mesmo cálculo de versão) em vez de duplicar
  lógica.

**Non-Goals:**
- Não permite editar os dados do modelo em preparação (código/máquina/
  descrição/observações) depois da abertura — se o usuário errou um dado,
  cancela e reabre. Simplifica o escopo desta mudança; pode virar uma
  mudança futura se necessário.
- Não altera o filtro por máquina em "Listar solicitações" para também
  casar com o campo de máquina em preparação de uma CRIACAO ainda não
  concluída — CRIACAO só aparece nesse filtro depois de concluída (quando
  já tem `modeloId`). Ver Open Questions.
- Não exige evidência SERVICO_REALIZADO para concluir uma CRIACAO — a
  regra existente já lista explicitamente REPARO/INSPECAO/REENGENHARIA como
  gatilho, então CRIACAO fica fora dela sem precisar tocar nesse requisito.
- Não cobre o formulário/telas do `rgm-frontend` — fica para uma change
  equivalente nesse repo.

## Decisions

**1. Novas colunas nullable em `solicitacoes`, não uma tabela separada.**
Adicionar `modelo_codigo VARCHAR(50)`, `modelo_maquina VARCHAR(100)`,
`modelo_observacoes TEXT` a `solicitacoes`, todas nullable, usadas apenas
quando `tipo = CRIACAO`. A descrição do modelo em preparação reaproveita a
coluna `solicitacoes.descricao` já existente (a descrição da solicitação
*é* a descrição do modelo pretendido, para esse tipo) — evita uma coluna
redundante.

Alternativa considerada: tabela `solicitacao_criacao_modelo` 1:1
apensa. Rejeitada por adicionar um join e uma entidade nova só para 3
colunas nullable, quando o padrão do projeto (ex.: `evidencias`,
`solicitacoes`) já usa colunas nullable diretamente na tabela principal
para dados condicionais por tipo.

**2. `modelo_id` vira nullable, com `CHECK` de consistência.**
```sql
ALTER TABLE solicitacoes ALTER COLUMN modelo_id DROP NOT NULL;
ALTER TABLE solicitacoes ADD CONSTRAINT chk_solicitacao_criacao_dados
  CHECK (
    (tipo <> 'CRIACAO' AND modelo_id IS NOT NULL)
    OR
    (tipo = 'CRIACAO' AND (status = 'CONCLUIDA' OR modelo_id IS NULL))
  );
```
Isso garante no banco que só CRIACAO pode ter `modelo_id` nulo, e só
enquanto não concluída — reforça no schema o invariante que o domínio já
aplica em `Solicitacao`, pego mais cedo em caso de bug de persistência.

**3. Criação do modelo dentro do próprio `EncerrarSolicitacaoUseCase`,
reaproveitando `GerenciarModelosUseCase.criar()`.**
`EncerrarSolicitacaoUseCase` passa a receber `GerenciarModelosUseCase` como
dependência. Ao concluir uma solicitação com `tipo = CRIACAO`: chama
`gerenciarModelosUseCase.criar(new CriarInput(solicitacao.getModeloCodigo(),
solicitacao.getDescricao(), solicitacao.getModeloObservacoes(),
solicitacao.getModeloMaquina(), gestorId))`, usa o `Modelo` retornado para
obter o novo `modeloId`, e só então persiste a solicitação com
`modeloId` preenchido e `status = CONCLUIDA`. Tudo dentro da mesma
transação do use case (mesmo padrão de `EncerrarSolicitacaoUseCase` já
criar o `EventoModelo` inline hoje) — se `criar()` lançar (ex.:
`ValidationException` de máquina inválida), a exceção propaga e a
transição inteira é desfeita.

Alternativa considerada: criar o Modelo antes de mover para CONCLUIDA, em
uma chamada separada do controller (duas requisições). Rejeitada — quebra
a atomicidade exigida pelo spec ("Se a criação do modelo falhar... a
conclusão inteira SHALL falhar") e exporia um estado intermediário
inconsistente via API.

**4. Evento CADASTRO: unificar o gatilho em um método utilitário
compartilhado.**
Tanto `GerenciarModelosUseCase.criar()` quanto o novo branch de
`EncerrarSolicitacaoUseCase` chamam a mesma criação de `EventoModelo` tipo
CADASTRO (mesmo padrão de `EncerrarSolicitacaoUseCase.mapearTipoEvento`
já existente para REPARO/INSPECAO/REENGENHARIA). No caminho via
solicitação, `solicitacaoRelacionadaId` é preenchido; no cadastro direto,
fica nulo.

**5. Tipo imutável — validação no agregado, não só no use case.**
`Solicitacao.editar(...)` passa a receber o `tipo` atual implicitamente (o
método já opera sobre a instância) e rejeita qualquer tentativa de
`EditarSolicitacaoUseCase` de passar um `tipo` diferente do já
persistido — a assinatura de `EditarInput` deixa de aceitar `tipo` como
parâmetro alterável (ou mantém o campo só para leitura/no-op, a decidir na
implementação; o requisito de spec é o que importa: tipo não muda).

## Risks / Trade-offs

- [`solicitacoes.modelo_id` nullable é uma mudança de schema em coluna já
  populada em produção — precisa de migration segura] → Mitigação: `ALTER
  COLUMN ... DROP NOT NULL` é não-destrutivo e instantâneo em Postgres
  (não reescreve a tabela); o `CHECK` novo só valida linhas novas/alteradas
  a partir de então (validação de linhas existentes é automática e todas
  já satisfazem a condição `tipo <> 'CRIACAO'`).
- [Qualquer código existente que assume `getModeloId()` nunca nulo (ex.:
  métricas por modelo, filtro por máquina, PDF, `KanbanCard` no frontend)
  pode quebrar com uma CRIACAO não concluída na lista] → Mitigação:
  mapear todos os pontos de leitura de `modeloId` como tarefa explícita de
  levantamento em `tasks.md` antes de codar; não assumir que só o teste
  unitário pega isso.
- [Reaproveitar `GerenciarModelosUseCase` dentro de
  `EncerrarSolicitacaoUseCase` cria uma dependência cruzada entre pacotes
  de use case (`solicitacao` → `modelo`)] → Aceito: já existe precedente
  de use cases de solicitação dependerem de repositórios de modelo; usar o
  use case em vez de duplicar a lógica de versionamento é a opção mais
  segura.

## Open Questions

- O filtro "por máquina" em "Listar solicitações" deve também casar
  solicitações CRIACAO ainda não concluídas (usando o campo de máquina em
  preparação)? Não muda a abordagem central — pode ser resolvido na
  implementação ou adiado sem impacto no restante do design.
