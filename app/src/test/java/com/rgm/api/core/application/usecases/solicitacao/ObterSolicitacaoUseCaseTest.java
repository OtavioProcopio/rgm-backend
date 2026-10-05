package com.rgm.api.core.application.usecases.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObterSolicitacaoUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @InjectMocks private ObterSolicitacaoUseCase useCase;

  @Test
  void execute_retornaSolicitacaoComResponsaveis() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao atrib =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), null);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atrib));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(operador(userId)));

    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId));

    assertThat(output.solicitacao()).isEqualTo(sol);
    assertThat(output.responsavelIds()).containsExactly(userId);
  }

  @Test
  void execute_filtraRemovidoEm() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao removida =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(removida));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(operador(userId)));

    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId));

    assertThat(output.responsavelIds()).isEmpty();
  }

  @Test
  void execute_lancaExcecaoSeNaoEncontrado() {
    final UUID solId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(solId, UUID.randomUUID())))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void listarResponsaveisBatch_vazio() {
    final var result = useCase.listarResponsaveisBatch(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void listarResponsaveisBatch_agrupaPorSolicitacao() {
    final UUID solId1 = UUID.randomUUID();
    final UUID solId2 = UUID.randomUUID();
    final UUID user1 = UUID.randomUUID();
    final UUID user2 = UUID.randomUUID();

    final var atrib1 =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId1, user1, UUID.randomUUID(), Instant.now(), null);
    final var atrib2 =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId2, user2, UUID.randomUUID(), Instant.now(), null);
    final var removida =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId1, user2, UUID.randomUUID(), Instant.now(), Instant.now());

    when(atribuicaoRepository.findBySolicitacaoIdIn(List.of(solId1, solId2)))
        .thenReturn(List.of(atrib1, atrib2, removida));

    final var result = useCase.listarResponsaveisBatch(List.of(solId1, solId2));

    assertThat(result.get(solId1)).containsExactly(user1);
    assertThat(result.get(solId2)).containsExactly(user2);
  }

  private static Usuario operador(final UUID id) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Operador", "operador@rgm.test", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
  }

  @Test
  void shouldReturnAllowedActionsWhenAuthorReadsUnassignedSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), autorId));

    // Assert
    assertThat(output)
        .isEqualTo(
            new ObterSolicitacaoUseCase.Output(
                sol,
                List.of(),
                EnumSet.of(
                    AcaoSolicitacao.CANCELAR,
                    AcaoSolicitacao.EDITAR,
                    AcaoSolicitacao.COMENTAR,
                    AcaoSolicitacao.ANEXAR_EVIDENCIA)));
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldFailWhenRequestingUserDoesNotExist() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), usuarioId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act
    final var erro =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class,
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), usuarioId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao encontrado");
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }
}
