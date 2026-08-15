## Why

Com mais de 2000 modelos cadastrados, gestores nao tem hoje como identificar,
sem revisar solicitacao por solicitacao, quais modelos consomem mais tempo de
manutencao (tempo medio de resolucao) e quais modelos voltam a quebrar com
mais frequencia (intervalo medio entre aberturas de solicitacao). Essa
comparacao e o que permite priorizar reengenharia/substituicao dos modelos
mais problematicos em vez de tratar cada chamado isoladamente.

## What Changes

- Novo endpoint `GET /api/solicitacoes/metricas/por-modelo`, paginado e
  ordenavel, retornando por modelo: tempo medio de resolucao (segundos) e
  intervalo medio entre aberturas de solicitacoes consecutivas (segundos).
  So inclui modelos com dados suficientes para o calculo (ver design.md).
- A ficha PDF de um modelo individual passa a exibir as duas metricas quando
  aplicavel.
- Novo relatorio PDF "Ranking de modelos por tempo", exportando a mesma
  comparacao paginada/ordenada da tela.

## Capabilities

### New Capabilities
(nenhuma - estende capacidades existentes)

### Modified Capabilities
- `metricas-dashboard`: novo requirement de ranking de tempo de resolucao e
  intervalo entre solicitacoes por modelo, com paginacao/ordenacao e novo
  relatorio PDF do ranking
- `modelos`: a ficha PDF de um modelo passa a incluir as duas metricas
  quando o modelo tiver dados suficientes

## Impact

- Novo caso de uso `ObterMetricasPorModeloUseCase` (`core/application/usecases/solicitacao`)
- Novo metodo de agregacao em `SolicitacaoRepository`
  (`findMetricasPorModelo`), implementado via query agregada no banco (nao
  carrega solicitacoes em memoria — necessario dado o volume de modelos)
- Novo endpoint em `SolicitacaoController` + DTOs de request/response
- `ModeloPdfService.gerarFicha` ganha as duas metricas como parametros
  opcionais
- Novo `RankingModelosPdfService` (ou metodo em `ModeloPdfService`) para o
  relatorio comparativo
- Nenhuma migration necessaria (calculo e derivado, nao persistido)
