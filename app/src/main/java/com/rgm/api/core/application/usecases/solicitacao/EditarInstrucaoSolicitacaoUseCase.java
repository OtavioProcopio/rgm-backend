package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige a instrucao do que fazer de uma solicitacao em andamento (gestor e administrador). A
 * correcao e gravada junto com a atividade "instrucao alterada", que guarda o texto anterior.
 */
@Transactional
public class EditarInstrucaoSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public EditarInstrucaoSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(UUID solicitacaoId, String instrucao, UUID usuarioId) {}

  /** {@code alterada}: falso quando o texto era igual ao atual e nada foi gravado. */
  public record Output(Solicitacao solicitacao, boolean alterada) {}

  public Output execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    if (!usuario.isAtivo() || !usuario.getPerfil().podeTriar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para editar a instrucao");
    }

    final Solicitacao atual =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final Solicitacao editada = atual.editarInstrucao(input.instrucao(), agora);
    if (editada == atual) {
      return new Output(atual, false);
    }

    final Solicitacao salva = solicitacaoRepository.save(editada);
    atividadeRepository.save(
        AtividadeSolicitacao.instrucaoAlterada(
            salva.getId(), atual.getInstrucao(), input.usuarioId(), agora));
    return new Output(salva, true);
  }
}
