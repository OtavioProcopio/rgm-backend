## 1. Levantamento

- [x] 1.1 Confirmar no `EvidenciaController` como `upload()`/`persist()` de `AnexarEvidenciaUseCase` são orquestrados hoje (mesma requisição, bytes ainda disponíveis?) e decidir entre reler do storage ou manter os bytes em memória para a duplicação (ver Open Question do design.md) — decidido: `MultipartFile.getInputStream()` é chamado uma segunda vez pelo controller via `Supplier<InputStream>`, sem precisar de `StorageService.download()`

## 2. Galeria — caminho automático

- [x] 2.1 Adicionar `uploadAutomatico(Input)` em `AdicionarFotoGaleriaUseCase`, idêntico a `upload()` porém sem chamar `validarPermissao(...)`, com teste cobrindo que nenhuma exceção de autorização é lançada para um usuário sem permissão de galeria
- [x] 2.2 Gerar a identificação automática (`"<Rótulo do tipo> — <título da solicitação>"`) em um helper reaproveitável, com teste cobrindo os rótulos de SERVICO_REALIZADO e CONCLUSAO

## 3. Gatilho em AnexarEvidenciaUseCase

- [x] 3.1 Em `AnexarEvidenciaUseCase.persist()` (ou onde os bytes ainda estiverem disponíveis), após salvar a `Evidencia`, checar se `tipo` in {SERVICO_REALIZADO, CONCLUSAO}, `mimeType` in {image/jpeg, image/png, image/webp} e `solicitacao.getModeloId() != null`; se tudo verdadeiro, duplicar o arquivo no storage e chamar `uploadAutomatico`/`persist` da galeria — implementado em `upload()` (onde a `Solicitacao` já é buscada), não em `persist()`
- [x] 3.2 Capturar e logar qualquer exceção do passo acima sem propagar — a evidência precisa permanecer salva mesmo se a cópia para galeria falhar; teste cobrindo esse isolamento (mock do storage/galeria lançando erro, evidência ainda é persistida)
- [x] 3.3 Testes cobrindo os cenários da spec: SERVICO_REALIZADO/CONCLUSAO em imagem com modelo vinculado gera foto; GERAL/ABERTURA/INSTRUCAO_SERVICO/DEVOLUCAO não geram; GIF/PDF/MP4 não geram. **Nota:** o cenário "solicitação CRIACAO sem `modeloId` não gera foto" não pôde ser testado ainda — `TipoSolicitacao.CRIACAO` e o `modeloId` nulo só existirão após a change `solicitacao-tipo-criacao-modelo`; a lógica já trata `modeloId == null` defensivamente (`elegivelParaGaleria` checa `solicitacao.getModeloId() != null`), mas falta um teste de regressão específico quando aquela change for implementada

## 4. Regressão e qualidade

- [x] 4.1 Rodar `make test-backend` e confirmar que toda a suíte passa, incluindo os testes já existentes de `AnexarEvidenciaUseCaseTest`/`AdicionarFotoGaleriaUseCaseTest` — 524 testes, 0 falhas
- [x] 4.2 Confirmar cobertura JaCoCo >= 95% nos arquivos alterados — `mvnw verify` (que roda o `jacoco:check` no build) passou
- [x] 4.3 Validar manualmente via `make up`: como OPERADOR, enviar uma solicitação para validação com evidência SERVICO_REALIZADO (imagem) numa solicitação com modelo vinculado, e conferir que a foto aparece na galeria do modelo mesmo sem o OPERADOR ter permissão de gestão de galeria — confirmado via API real (login OPERADOR, anexar evidência CONCLUSAO, foto apareceu na galeria com autor = o OPERADOR, `principal=false` como segunda foto)
