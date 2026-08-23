## Why

Hoje a galeria de apresentação do modelo e o histórico de evidências de
solicitação são deliberadamente separados — a galeria só é alimentada
manualmente por GESTOR/ADMINISTRADOR. Isso significa que fotos boas
anexadas ao longo de uma solicitação (provando o serviço feito) nunca
aparecem como fotos de apresentação do modelo, a menos que alguém as
baixe e suba de novo manualmente na galeria. O usuário quer que, conforme
uma solicitação avança, as fotos relevantes (o resultado do trabalho) já
alimentem automaticamente a galeria do modelo, sem esse passo manual
duplicado.

## What Changes

- Ao anexar uma evidência dos tipos **SERVICO_REALIZADO** ou **CONCLUSAO**
  a uma solicitação vinculada a um modelo, e o arquivo sendo uma imagem
  nos formatos aceitos pela galeria (JPEG/PNG/WEBP — GIF, PDF e MP4 nunca
  entram na galeria), o sistema SHALL automaticamente também adicionar
  essa mesma foto à galeria daquele modelo.
- Aplica-se a **qualquer tipo de solicitação** (REPARO, INSPECAO,
  REENGENHARIA, CRIACAO), sempre que a solicitação já tiver `modeloId` no
  momento em que a evidência é anexada. Para uma solicitação CRIACAO ainda
  sem `modeloId` (antes de concluída), a cópia automática simplesmente não
  ocorre — não há retroatividade (ver design.md).
- Esse envio automático **não** passa pela regra "somente
  GESTOR/ADMINISTRADOR gerencia a galeria" — é um efeito colateral do
  sistema, não uma ação direta de gestão de galeria; o autor registrado na
  foto é quem de fato anexou a evidência, seja qual for o perfil.
- A identificação (rótulo obrigatório da foto na galeria) é gerada
  automaticamente a partir do tipo de evidência e da solicitação de
  origem (ex.: "Serviço realizado — SOL-123").
- Os demais tipos de evidência (GERAL, ABERTURA, INSTRUCAO_SERVICO,
  DEVOLUCAO) **não** alimentam a galeria — continuam só como evidência.
- O arquivo é duplicado no storage (novo objeto), não compartilhado com o
  registro de evidência original — cada um (evidência e foto de galeria)
  tem ciclo de vida independente; excluir um não afeta o outro.

## Capabilities

### New Capabilities

(nenhuma)

### Modified Capabilities

- `evidencias`: anexar uma evidência do tipo SERVICO_REALIZADO ou
  CONCLUSAO, quando a solicitação já tem modelo vinculado e o arquivo é
  imagem compatível com a galeria, passa a também gerar uma foto na
  galeria do modelo automaticamente.
- `galeria-modelo`: a galeria passa a ter um segundo gatilho de adição de
  foto, além do manual por GESTOR/ADMINISTRADOR — automático a partir de
  evidências elegíveis, sem checagem de perfil nesse gatilho específico.

## Impact

- `AnexarEvidenciaUseCase.persist()`: após salvar a `Evidencia`, se
  `tipo` in {SERVICO_REALIZADO, CONCLUSAO} e `mimeType` in
  {image/jpeg, image/png, image/webp} e a solicitação tem `modeloId`,
  invocar a duplicação do arquivo e a criação da foto de galeria.
- `AdicionarFotoGaleriaUseCase`: precisa de um caminho de uso interno que
  não exija `podeGerenciarGaleriaModelo()` do autor (hoje `upload()`
  sempre valida essa permissão) — ver design.md para a decisão.
- Sem migration de schema — reaproveita a tabela `fotos_galeria_modelo`
  já existente, sem novas colunas.
- Testes: `AnexarEvidenciaUseCaseTest` (novos casos: SERVICO_REALIZADO com
  modelo gera foto, CONCLUSAO gera foto, tipos não elegíveis não geram,
  mimeType não compatível não gera, solicitação CRIACAO sem modelo não
  gera), `AdicionarFotoGaleriaUseCaseTest` (novo caminho sem checagem de
  permissão), mantendo JaCoCo >= 95%.
- Fora deste repo: `rgm-frontend` não precisa de mudança para este change
  especificamente — a galeria já é lida e exibida do mesmo jeito,
  independente de como a foto chegou lá.
