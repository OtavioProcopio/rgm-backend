package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-02: Abrir solicitacao (A_FAZER). */
public final class AbrirSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final ModeloRepository modeloRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;

  public AbrirSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.modeloRepository = modeloRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(
      String titulo,
      String descricao,
      TipoSolicitacao tipo,
      UUID modeloId,
      UUID abertaPorUsuarioId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario usuario =
        usuarioRepository
            .findById(input.abertaPorUsuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode abrir solicitacoes");
    }

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

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
