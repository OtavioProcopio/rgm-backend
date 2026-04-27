package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-02: Abrir solicitacao (A_FAZER). */
public final class AbrirSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final ModeloRepository modeloRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public AbrirSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.modeloRepository = modeloRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(
      String titulo,
      String descricao,
      TipoSolicitacao tipo,
      UUID modeloId,
      UUID abertaPorUsuarioId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new ValidationException("Modelo nao encontrado"));

    if (!modelo.isAtivo()) {
      throw new ValidationException("Modelo inativo");
    }

    final Solicitacao solicitacao =
        Solicitacao.abrir(
            input.titulo(),
            input.descricao(),
            input.tipo(),
            input.modeloId(),
            input.abertaPorUsuarioId(),
            agora);

    final Solicitacao salva = solicitacaoRepository.save(solicitacao);

    atividadeRepository.save(
        AtividadeSolicitacao.abertura(salva.getId(), input.abertaPorUsuarioId(), agora));

    if (!modelo.isTemPendenciaAberta()) {
      modeloRepository.save(modelo.withTemPendenciaAberta(true, agora));
    }

    return salva;
  }
}
