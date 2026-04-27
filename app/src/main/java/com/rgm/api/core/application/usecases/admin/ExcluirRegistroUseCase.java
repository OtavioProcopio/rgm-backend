package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
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

  public ExcluirRegistroUseCase(
      final UsuarioRepository usuarioRepository,
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final MaquinaRepository maquinaRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository) {
    this.usuarioRepository = usuarioRepository;
    this.solicitacaoRepository = solicitacaoRepository;
    this.modeloRepository = modeloRepository;
    this.maquinaRepository = maquinaRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
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
      case MODELO -> modeloRepository.deleteById(input.recursoId());
      case USUARIO -> usuarioRepository.deleteById(input.recursoId());
      case MAQUINA -> maquinaRepository.deleteById(input.recursoId());
    }
  }

  private void excluirSolicitacao(final UUID solicitacaoId) {
    solicitacaoRepository
        .findById(solicitacaoId)
        .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    atribuicaoRepository.deleteBySolicitacaoId(solicitacaoId);
    atividadeRepository.deleteBySolicitacaoId(solicitacaoId);
    solicitacaoEvidenciaRepository.deleteBySolicitacaoId(solicitacaoId);
    solicitacaoRepository.deleteById(solicitacaoId);
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new ValidationException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeExcluir()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode excluir registros");
    }
  }
}
