package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-10: Recalcular Modelo.temPendenciaAberta (consistencia). */
public final class RecalcularPendenciaUseCase {
  private static final Logger log = LoggerFactory.getLogger(RecalcularPendenciaUseCase.class);

  private static final List<StatusSolicitacao> STATUSES_NAO_TERMINAIS =
      List.of(
          StatusSolicitacao.A_FAZER,
          StatusSolicitacao.EM_ANDAMENTO,
          StatusSolicitacao.EM_VALIDACAO);

  private final ModeloRepository modeloRepository;
  private final SolicitacaoRepository solicitacaoRepository;

  public RecalcularPendenciaUseCase(
      final ModeloRepository modeloRepository, final SolicitacaoRepository solicitacaoRepository) {
    this.modeloRepository = modeloRepository;
    this.solicitacaoRepository = solicitacaoRepository;
  }

  public void execute(final UUID modeloId) {
    log.info("RecalcularPendenciaUseCase.execute iniciado");
    final Instant agora = Instant.now();

    final Modelo modelo =
        modeloRepository
            .findById(modeloId)
            .orElseThrow(() -> new ValidationException("Modelo nao encontrado"));

    final boolean temPendencia =
        solicitacaoRepository.existsByModeloIdAndStatusIn(modeloId, STATUSES_NAO_TERMINAIS);

    if (modelo.isTemPendenciaAberta() != temPendencia) {
      modeloRepository.save(modelo.withTemPendenciaAberta(temPendencia, agora));
    }
  }
}
