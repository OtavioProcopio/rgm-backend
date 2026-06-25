package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.UUID;

/** UC-15: Exclusao permanente (hard delete) de registros. Ator: Administrador. */
public final class ExcluirRegistroUseCase {

  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoRepository solicitacaoRepository;
  private final ModeloRepository modeloRepository;
  private final MaquinaRepository maquinaRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final EventoModeloRepository eventoModeloRepository;

  public ExcluirRegistroUseCase(
      final UsuarioRepository usuarioRepository,
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final MaquinaRepository maquinaRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final EventoModeloRepository eventoModeloRepository) {
    this.usuarioRepository = usuarioRepository;
    this.solicitacaoRepository = solicitacaoRepository;
    this.modeloRepository = modeloRepository;
    this.maquinaRepository = maquinaRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.eventoModeloRepository = eventoModeloRepository;
  }

  public enum TipoRecurso {
    SOLICITACAO,
    MODELO,
    USUARIO,
    MAQUINA
  }

  public record Input(TipoRecurso tipo, UUID recursoId, UUID adminId) {}

  public void execute(final Input input) {
    validarPermissao(input.adminId());

    switch (input.tipo()) {
      case SOLICITACAO -> excluirSolicitacao(input.recursoId());
      case MODELO -> excluirModelo(input.recursoId());
      case USUARIO -> excluirUsuario(input.recursoId());
      case MAQUINA -> excluirMaquina(input.recursoId());
    }
  }

  private void excluirSolicitacao(final UUID solicitacaoId) {
    solicitacaoRepository
        .findById(solicitacaoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    atribuicaoRepository.deleteBySolicitacaoId(solicitacaoId);
    atividadeRepository.deleteBySolicitacaoId(solicitacaoId);
    solicitacaoEvidenciaRepository.deleteBySolicitacaoId(solicitacaoId);
    solicitacaoRepository.deleteById(solicitacaoId);
  }

  private void excluirModelo(final UUID modeloId) {
    modeloRepository
        .findById(modeloId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    if (solicitacaoRepository.existsByModeloId(modeloId)) {
      throw new BusinessRuleException(
          "Nao e possivel excluir modelo com solicitacoes vinculadas. Desative o modelo em vez de excluir.");
    }

    modeloRepository.deleteById(modeloId);
  }

  private void excluirMaquina(final UUID maquinaId) {
    maquinaRepository
        .findById(maquinaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Maquina nao encontrada"));

    if (modeloRepository.existsByMaquinaId(maquinaId)) {
      throw new BusinessRuleException(
          "Nao e possivel excluir maquina com modelos vinculados. Desative a maquina em vez de excluir.");
    }

    maquinaRepository.deleteById(maquinaId);
  }

  private void excluirUsuario(final UUID usuarioId) {
    usuarioRepository
        .findById(usuarioId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    final boolean temSolicitacoesAbertas = solicitacaoRepository.existsByAbertaPorUsuarioId(usuarioId);
    final boolean temAtribuicoes = atribuicaoRepository.existsByUsuarioIdAndRemovidoEmIsNull(usuarioId);
    final boolean temAtividades = atividadeRepository.existsByAutorId(usuarioId);
    final boolean temEventos = eventoModeloRepository.existsByExecutadoPorUsuarioId(usuarioId);

    if (temSolicitacoesAbertas || temAtribuicoes || temAtividades || temEventos) {
      throw new BusinessRuleException(
          "Nao e possivel excluir usuario com historico de solicitacoes ou atribucoes. Desative o usuario em vez de excluir.");
    }

    usuarioRepository.deleteById(usuarioId);
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeExcluir()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode excluir registros");
    }
  }
}
