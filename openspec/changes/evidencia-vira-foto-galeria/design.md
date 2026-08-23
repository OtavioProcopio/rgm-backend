## Context

`AnexarEvidenciaUseCase` e `AdicionarFotoGaleriaUseCase` já seguem o mesmo
padrão two-phase (`upload()` sobe o arquivo pro storage fora da transação,
`persist()` grava o registro). `AdicionarFotoGaleriaUseCase.upload()` hoje
sempre chama `validarPermissao(gestorId)`, que rejeita qualquer perfil sem
`podeGerenciarGaleriaModelo()` — isso precisa ser contornável para o
gatilho automático, sem enfraquecer a checagem para o caminho manual (API
de gerenciar galeria). Ver `proposal.md` para a motivação.

`FotoGaleriaModelo.criar(modeloId, publicUrl, identificacao, principal,
usuarioId, agora)` não distingue de onde veio a chamada — só grava
`usuarioId` como autor, sem checar permissão dentro do agregado (a checagem
de perfil é toda no use case).

## Goals / Non-Goals

**Goals:**
- Reaproveitar ao máximo o código já existente de
  `AdicionarFotoGaleriaUseCase` (validação de tamanho/mime, cálculo de
  `principal`, persistência) para o caminho automático, em vez de duplicar
  lógica.
- O gatilho automático nunca falha a operação principal (anexar a
  evidência) — se copiar para a galeria der erro, a evidência ainda é
  salva normalmente (ver Risks).
- Nenhuma mudança de schema.

**Non-Goals:**
- Não staga/retroage fotos para quando um modelo CRIACAO é criado depois
  (evidências elegíveis anexadas antes da conclusão de uma CRIACAO
  simplesmente não geram foto de galeria — ver spec).
- Não desduplica fotos (se a mesma evidência for reenviada/reanexada duas
  vezes, duas fotos de galeria são criadas — mesmo comportamento que
  duas evidências separadas hoje).
- Não altera o fluxo de exclusão de evidência nem de foto de galeria —
  cada um continua removendo só o seu próprio registro/arquivo.

## Decisions

**1. Novo método interno em `AdicionarFotoGaleriaUseCase` sem checagem de
permissão, usado só pelo gatilho automático.**
Adicionar `uploadAutomatico(Input)` e reaproveitar `persist(Input,
publicUrl)` (que já não faz checagem de permissão — só
`upload()` faz). `uploadAutomatico` roda as mesmas validações de
tamanho/mimeType/modelo existente, mas pula `validarPermissao(...)`. O
`Input.gestorId()` nesse caminho passa a significar "usuário responsável
pelo registro" (o autor real da evidência), não "gestor autorizado" — o
nome do campo pode ser mantido por ora (renomear é cosmético, fora de
escopo) ou renomeado para `usuarioId` durante a implementação, à critério
de quem implementar.

Alternativa considerada: expor um método `public` sem nenhuma checagem e
deixar quem chama decidir. Rejeitada — manter os dois caminhos (`upload`
com checagem, `uploadAutomatico` sem) deixa explícito, no próprio nome do
método, que o caminho automático é intencionalmente diferente, evitando
que alguém religue permissão nele por engano no futuro.

**2. Duplicar bytes no storage, não reaproveitar a mesma `publicUrl`.**
`AnexarEvidenciaUseCase.persist()` recebe o `InputStream` original só uma
vez (consumido no `upload()`); para duplicar o arquivo é necessário ler os
bytes do storage já persistido (`StorageService` precisa expor uma leitura
por `publicUrl`, ou o controller precisa manter os bytes em memória para
subir duas vezes — a decidir na implementação, ver Open Questions) e
re-fazer upload como um novo objeto, obtendo uma nova `publicUrl` própria
da galeria. Isso preserva o invariante "excluir evidência não afeta
galeria e vice-versa", já que MinIO não tem reference counting nesse
projeto.

**3. Identificação automática.**
Formato: `"<Rótulo do tipo> — <titulo da solicitacao>"`, ex.: `"Serviço
realizado — Troca de rolamento"` ou `"Conclusão — Troca de rolamento"`.
Trim para o limite de tamanho do campo `identificacao`, se houver.

**4. Ponto de disparo: dentro de `AnexarEvidenciaUseCase.persist()`, depois
de salvar a `Evidencia`.**
Reusa a `Solicitacao` já buscada (ou busca de novo, se `persist()` não a
tiver em mão — verificar assinatura atual no controller) para checar
`tipo` elegível + `modeloId` não nulo antes de disparar a cópia. Se
`modeloId` for nulo (CRIACAO ainda não concluída) ou o tipo/mimeType não
qualificar, não faz nada — sem exceção, sem log de erro (é o caminho
normal, não uma falha).

**5. Falha na cópia para galeria não derruba o anexo da evidência.**
Se `uploadAutomatico`/`persist` da foto de galeria lançar (ex.: erro de
storage), a exceção é capturada e logada dentro de
`AnexarEvidenciaUseCase`, mas a evidência já persistida permanece válida —
mesma filosofia já usada no frontend hoje para uploads acessórios (ex.:
`KanbanBoard.tsx` já trata falha de upload de evidência de triagem como
best-effort, sem bloquear o fluxo principal). Evita que um problema no
storage da galeria transforme um simples anexo de evidência em erro 500.

## Risks / Trade-offs

- [Duplicar o arquivo exige reler os bytes do storage ou mantê-los em
  memória por mais tempo na mesma requisição] → Mitigação: decisão de
  implementação (ler de volta via `StorageService` vs. manter o
  `byte[]`/stream em memória no controller antes de chamar os dois use
  cases) fica registrada como Open Question, não trava o design.
- [Falha silenciosa da cópia para galeria pode confundir quem espera ver
  a foto lá e ela não aparece] → Mitigação: logar a falha com nível
  suficiente para debug; considerar métrica/alerta futuro, fora de escopo
  aqui.
- [Volume: toda solicitação concluída com evidência de CONCLUSAO em
  imagem agora sempre gera uma foto de galeria — pode encher a galeria de
  fotos repetitivas] → Aceito como comportamento desejado pelo usuário;
  GESTOR/ADMINISTRADOR ainda pode remover manualmente fotos indesejadas
  depois (fluxo de remoção já existente, inalterado).

## Open Questions

- Reler os bytes do storage (`StorageService` ganha um método de
  download) vs. manter o array de bytes em memória na mesma requisição
  para subir duas vezes: qual o custo/latência aceitável aqui? Não muda o
  comportamento observável (spec/scenarios), só a implementação — pode
  ser decidido durante a codificação.
