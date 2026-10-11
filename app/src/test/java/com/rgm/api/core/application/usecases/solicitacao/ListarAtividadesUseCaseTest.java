package com.rgm.api.core.application.usecases.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarAtividadesUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-10-07T12:00:00Z");

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private AtividadeSolicitacaoRepository atividadeRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @InjectMocks private ListarAtividadesUseCase useCase;

  private static Usuario usuario(final UUID id, final String nome, final PerfilUsuario perfil) {
    return new Usuario(id, nome, "usuario@rgm.test", "hash", perfil, true, AGORA, AGORA);
  }

  private static Solicitacao abertaPor(final UUID autorId) {
    return Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, AGORA);
  }

  private void verificarSemMaisChamadas() {
    verifyNoMoreInteractions(
        solicitacaoRepository, atividadeRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldReturnActivitiesWithAuthorNameWhenUserOpenedTheSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = abertaPor(autorId);
    final UUID solId = sol.getId();
    final Usuario autor = usuario(autorId, "Alice", PerfilUsuario.OPERADOR);
    final AtividadeSolicitacao atividade = AtividadeSolicitacao.abertura(solId, autorId, AGORA);
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, autorId))
        .thenReturn(false);
    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atividade));
    when(usuarioRepository.findAllByIdIn(List.of(autorId))).thenReturn(List.of(autor));

    // Act
    final var result = useCase.execute(new ListarAtividadesUseCase.Input(solId, autorId));

    // Assert
    assertThat(result)
        .containsExactly(new ListarAtividadesUseCase.AtividadeComAutor(atividade, "Alice"));
    verify(solicitacaoRepository, times(1)).findById(solId);
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, autorId);
    verify(atividadeRepository, times(1)).findBySolicitacaoId(solId);
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(autorId));
    verificarSemMaisChamadas();
  }

  @Test
  void shouldUseDefaultNameWhenAuthorIsNotFound() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = abertaPor(autorId);
    final UUID solId = sol.getId();
    final AtividadeSolicitacao atividade = AtividadeSolicitacao.abertura(solId, autorId, AGORA);
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, "Gestor", PerfilUsuario.GESTOR)));
    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atividade));
    when(usuarioRepository.findAllByIdIn(List.of(autorId))).thenReturn(List.of());

    // Act
    final var result = useCase.execute(new ListarAtividadesUseCase.Input(solId, gestorId));

    // Assert
    assertThat(result)
        .containsExactly(new ListarAtividadesUseCase.AtividadeComAutor(atividade, "Usuário"));
    verify(solicitacaoRepository, times(1)).findById(solId);
    verify(usuarioRepository, times(1)).findById(gestorId);
    verify(atividadeRepository, times(1)).findBySolicitacaoId(solId);
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(autorId));
    verificarSemMaisChamadas();
  }

  @Test
  void shouldReturnActivitiesWhenOperatorIsAssignedButDidNotOpenTheSolicitacao() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final UUID solId = sol.getId();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(operadorId))
        .thenReturn(Optional.of(usuario(operadorId, "Bruno", PerfilUsuario.OPERADOR)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solId, operadorId))
        .thenReturn(true);
    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of());
    when(usuarioRepository.findAllByIdIn(List.of())).thenReturn(List.of());

    // Act
    final var result = useCase.execute(new ListarAtividadesUseCase.Input(solId, operadorId));

    // Assert
    assertThat(result).isEmpty();
    verify(solicitacaoRepository, times(1)).findById(solId);
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, operadorId);
    verify(atividadeRepository, times(1)).findBySolicitacaoId(solId);
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of());
    verificarSemMaisChamadas();
  }

  @Test
  void shouldDenyWhenOperatorNeitherOpenedNorIsAssigned() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final UUID solId = sol.getId();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(operadorId))
        .thenReturn(Optional.of(usuario(operadorId, "Bruno", PerfilUsuario.OPERADOR)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solId, operadorId))
        .thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new ListarAtividadesUseCase.Input(solId, operadorId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao tem acesso a esta solicitacao");
    verify(solicitacaoRepository, times(1)).findById(solId);
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, operadorId);
    verificarSemMaisChamadas();
  }

  @Test
  void shouldFailWhenSolicitacaoDoesNotExist() {
    // Arrange
    final UUID solId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    // Act
    final RecursoNaoEncontradoException erro =
        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> useCase.execute(new ListarAtividadesUseCase.Input(solId, UUID.randomUUID())));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Solicitacao nao encontrada");
    verify(solicitacaoRepository, times(1)).findById(solId);
    verificarSemMaisChamadas();
  }

  @Test
  void shouldFailWhenRequestingUserDoesNotExist() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final UUID solId = sol.getId();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act
    final RecursoNaoEncontradoException erro =
        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> useCase.execute(new ListarAtividadesUseCase.Input(solId, usuarioId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao encontrado");
    verify(solicitacaoRepository, times(1)).findById(solId);
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verificarSemMaisChamadas();
  }
}
