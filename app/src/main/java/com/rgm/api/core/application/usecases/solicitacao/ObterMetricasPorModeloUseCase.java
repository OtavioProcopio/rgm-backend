package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;

/**
 * Ranking de modelos por tempo medio de resolucao e intervalo medio entre solicitacoes. Apenas
 * modelos com ao menos 1 solicitacao concluida aparecem no resultado.
 */
public final class ObterMetricasPorModeloUseCase {

  private final SolicitacaoRepository solicitacaoRepository;

  public ObterMetricasPorModeloUseCase(final SolicitacaoRepository solicitacaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
  }

  public record Input(OrdenacaoMetricaModelo sort, boolean ascendente, int page, int size) {}

  public PageResult<MetricaModeloRow> execute(final Input input) {
    return solicitacaoRepository.findMetricasPorModelo(
        input.sort(), input.ascendente(), input.page(), input.size());
  }
}
