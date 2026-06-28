package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-04/UC-05: Enviar para validacao (EM_ANDAMENTO -> EM_VALIDACAO) com autorizacao central. */
public final class EnviarParaValidacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;

  public EnviarParaValidacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId, String comentario) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    if (input.comentario() == null || input.comentario().isBlank()) {
      throw new ValidationException(
          "Comentário é obrigatório ao enviar para validação");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final boolean estaAtribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), usuario.getId());

    Solicitacao.validarAutorizacaoMover(
        usuario.getPerfil(),
        solicitacao.getStatus(),
        StatusSolicitacao.EM_VALIDACAO,
        estaAtribuido);

    if ((solicitacao.getTipo() == TipoSolicitacao.REPARO
            || solicitacao.getTipo() == TipoSolicitacao.INSPECAO)
        && solicitacaoEvidenciaRepository.findBySolicitacaoId(solicitacao.getId()).isEmpty()) {
      throw new BusinessRuleException(
          "Solicitações de REPARO ou INSPEÇÃO exigem o anexo de pelo menos 1 evidência do serviço realizado antes de serem enviadas para validação.");
    }

    final Solicitacao atualizada = solicitacao.enviarParaValidacao(agora);
    final Solicitacao salva = solicitacaoRepository.save(atualizada);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(),
            StatusSolicitacao.EM_ANDAMENTO,
            StatusSolicitacao.EM_VALIDACAO,
            input.usuarioId(),
            agora));

    atividadeRepository.save(
        AtividadeSolicitacao.comentario(
            salva.getId(), input.comentario(), input.usuarioId(), agora));

    return salva;
  }
}
