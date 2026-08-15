## 1. Persistencia

- [ ] 1.1 Adicionar `findMetricasPorModelo(SortField sort, SortDirection dir, int page, int size)` a `SolicitacaoRepository` (porta de dominio)
- [ ] 1.2 Implementar a query nativa com `LAG()` window function no adapter JPA (`SolicitacaoRepositoryAdapter`/`SolicitacaoJpaRepository`), agrupando por modelo e calculando tempo medio de resolucao e intervalo medio entre solicitacoes
- [ ] 1.3 Confirmar via `EXPLAIN ANALYZE` (com dados de teste em volume representativo) se os indices existentes em `(modelo_id, status)`/`(modelo_id, criada_em)` sao suficientes; criar migration `V6__...sql` se necessario

## 2. Caso de uso

- [ ] 2.1 Criar `ObterMetricasPorModeloUseCase` em `core/application/usecases/solicitacao`, com `Input(sort, dir, page, size)` e `Output` paginado (lista de `{modeloId, codigo, tempoMedioResolucaoSegundos, intervaloMedioEntreSolicitacoesSegundos}`, total, pagina)
- [ ] 2.2 Testes unitarios do caso de uso (mock do repository), cobrindo: ordenacao pelas duas metricas, paginacao, modelo com 1 solicitacao concluida (intervalo nulo)

## 3. API

- [ ] 3.1 Adicionar `GET /api/solicitacoes/metricas/por-modelo` a `SolicitacaoController`, com `sort`/`dir`/`page`/`size` validados contra whitelist (nunca interpolar nome de coluna a partir do input)
- [ ] 3.2 Criar `MetricasPorModeloResponse` (DTO de resposta paginada)
- [ ] 3.3 Testes de integracao do endpoint (`@WebMvcTest` ou equivalente), incluindo parametro de ordenacao invalido (400, nao 500)
- [ ] 3.4 Registrar o novo bean do caso de uso em `UseCaseConfig.java`

## 4. Relatorio PDF do ranking

- [ ] 4.1 Adicionar metodo de geracao do PDF de ranking em `ModeloPdfService` (ou service dedicado), reaproveitando o estilo visual existente
- [ ] 4.2 Adicionar endpoint de export (`GET /api/solicitacoes/metricas/por-modelo/pdf` ou similar) com os mesmos parametros de ordenacao
- [ ] 4.3 Testes do service de PDF (gera bytes nao vazios, cabecalho correto)

## 5. Ficha do modelo com metricas

- [ ] 5.1 Estender `ModeloPdfService.gerarFicha` para aceitar as duas metricas como parametros opcionais (nulos quando o modelo nao tem dados suficientes)
- [ ] 5.2 Atualizar `ModeloController` (endpoint de ficha) para calcular e passar as metricas daquele modelo especifico ao service
- [ ] 5.3 Testes cobrindo modelo com 2+ solicitacoes concluidas (metricas aparecem) e com menos de 2 (omitidas sem erro)

## 6. Qualidade e documentacao

- [ ] 6.1 `make lint` (spotless + checkstyle) e `./mvnw clean verify` com cobertura JaCoCo >= 95% nos arquivos tocados
- [ ] 6.2 Atualizar `docs/casos-de-uso.md` com o novo caso de uso
- [ ] 6.3 Rodar `/opsx:archive` ao final, sincronizando `openspec/specs/metricas-dashboard/spec.md` e `openspec/specs/modelos/spec.md` com as mudancas deste change
