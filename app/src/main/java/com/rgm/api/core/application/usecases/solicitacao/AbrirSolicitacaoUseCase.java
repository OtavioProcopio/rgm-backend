package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
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
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** UC-02: Abrir solicitacao (A_FAZER). */
@Transactional
public class AbrirSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final ModeloRepository modeloRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;
  private final MaquinaRepository maquinaRepository;

  public AbrirSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository,
      final MaquinaRepository maquinaRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.modeloRepository = modeloRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
    this.maquinaRepository = maquinaRepository;
  }

  public record Input(
      String titulo,
      String descricao,
      TipoSolicitacao tipo,
      UUID modeloId,
      String modeloCodigo,
      String modeloMaquina,
      String modeloObservacoes,
      UUID abertaPorUsuarioId) {

    /** Abertura de solicitacao para um modelo existente (todos os tipos exceto CRIACAO). */
    public Input(
        final String titulo,
        final String descricao,
        final TipoSolicitacao tipo,
        final UUID modeloId,
        final UUID abertaPorUsuarioId) {
      this(titulo, descricao, tipo, modeloId, null, null, null, abertaPorUsuarioId);
    }
  }

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario usuario =
        usuarioRepository
            .findById(input.abertaPorUsuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode abrir solicitacoes");
    }

    if (input.tipo() == TipoSolicitacao.CRIACAO) {
      return abrirCriacao(input, usuario, agora);
    }

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    if (!modelo.isAtivo()) {
      throw new BusinessRuleException("Modelo inativo");
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

  private Solicitacao abrirCriacao(final Input input, final Usuario usuario, final Instant agora) {
    Solicitacao.validarAutorizacaoAbrirCriacao(usuario.getPerfil());
    validarMaquina(input.modeloMaquina());

    final Solicitacao solicitacao =
        Solicitacao.abrirCriacao(
            input.titulo(),
            input.descricao(),
            input.modeloCodigo(),
            input.modeloMaquina(),
            input.modeloObservacoes(),
            input.abertaPorUsuarioId(),
            agora);

    final Solicitacao salva = solicitacaoRepository.save(solicitacao);

    atividadeRepository.save(
        AtividadeSolicitacao.abertura(salva.getId(), input.abertaPorUsuarioId(), agora));

    return salva;
  }

  private void validarMaquina(final String maquina) {
    if (maquina == null || !maquinaRepository.existsByNomeAndAtivoTrue(maquina.trim())) {
      throw new ValidationException("Maquina invalida ou inativa: " + maquina);
    }
  }
}
