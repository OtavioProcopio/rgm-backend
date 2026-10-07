package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolverDestinatariosEventoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-10-07T12:00:00Z");

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @InjectMocks private ResolverDestinatariosEventoUseCase useCase;

  private static Usuario usuario(final PerfilUsuario perfil) {
    return new Usuario(
        UUID.randomUUID(), "Usuario", "usuario@rgm.test", "hash", perfil, true, AGORA, AGORA);
  }

  private static Solicitacao abertaPor(final UUID autorId) {
    return Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, AGORA);
  }

  private static SolicitacaoAtribuicao atribuicao(
      final UUID solicitacaoId, final UUID usuarioId, final Instant removidoEm) {
    return new SolicitacaoAtribuicao(
        UUID.randomUUID(), solicitacaoId, usuarioId, UUID.randomUUID(), AGORA, removidoEm);
  }

  private void preparar(
      final Solicitacao sol,
      final List<SolicitacaoAtribuicao> atribuicoes,
      final Usuario conectado) {
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(atribuicoes);
    when(usuarioRepository.findAllByIdIn(List.of(conectado.getId())))
        .thenReturn(List.of(conectado));
  }

  private static ResolverDestinatariosEventoUseCase.Input entrada(
      final Solicitacao sol, final Usuario conectado, final Set<UUID> comAcessoAnterior) {
    return new ResolverDestinatariosEventoUseCase.Input(
        sol.getId(), Set.of(conectado.getId()), comAcessoAnterior);
  }

  private void verificarLeituras(final Solicitacao sol, final Usuario conectado) {
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(conectado.getId()));
    verifyNoMoreInteractions(solicitacaoRepository, atribuicaoRepository, usuarioRepository);
  }

  @Test
  void shouldIncludeManagerWhenSolicitacaoBelongsToSomeoneElse() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());

    preparar(sol, List.of(), gestor);

    // Act
    final Set<UUID> destinatarios = useCase.execute(entrada(sol, gestor, Set.of()));

    // Assert
    assertEquals(Set.of(gestor.getId()), destinatarios);
    verificarLeituras(sol, gestor);
  }

  @Test
  void shouldIncludeOperatorWhenOperatorOpenedTheSolicitacao() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(operador.getId());

    preparar(sol, List.of(), operador);

    // Act
    final Set<UUID> destinatarios = useCase.execute(entrada(sol, operador, Set.of()));

    // Assert
    assertEquals(Set.of(operador.getId()), destinatarios);
    verificarLeituras(sol, operador);
  }

  @Test
  void shouldIncludeOperatorWhenOperatorIsActiveResponsavel() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final List<SolicitacaoAtribuicao> atribuicoes =
        List.of(atribuicao(sol.getId(), operador.getId(), null));

    preparar(sol, atribuicoes, operador);

    // Act
    final Set<UUID> destinatarios = useCase.execute(entrada(sol, operador, Set.of()));

    // Assert
    assertEquals(Set.of(operador.getId()), destinatarios);
    verificarLeituras(sol, operador);
  }

  @Test
  void shouldExcludeOperatorWhenOperatorHasNoRelationToTheSolicitacao() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final List<SolicitacaoAtribuicao> atribuicoes =
        List.of(atribuicao(sol.getId(), UUID.randomUUID(), null));

    preparar(sol, atribuicoes, operador);

    // Act
    final Set<UUID> destinatarios = useCase.execute(entrada(sol, operador, Set.of()));

    // Assert
    assertEquals(Set.of(), destinatarios);
    verificarLeituras(sol, operador);
  }

  @Test
  void shouldExcludeOperatorWhenAssignmentWasRemovedBeforeThisEvent() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final List<SolicitacaoAtribuicao> atribuicoes =
        List.of(atribuicao(sol.getId(), operador.getId(), AGORA));

    preparar(sol, atribuicoes, operador);

    // Act
    final Set<UUID> destinatarios = useCase.execute(entrada(sol, operador, Set.of()));

    // Assert
    assertEquals(Set.of(), destinatarios);
    verificarLeituras(sol, operador);
  }

  @Test
  void shouldIncludeOperatorWhenThisEventRemovedTheAccess() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final List<SolicitacaoAtribuicao> atribuicoes =
        List.of(atribuicao(sol.getId(), operador.getId(), AGORA));

    preparar(sol, atribuicoes, operador);

    // Act
    final Set<UUID> destinatarios =
        useCase.execute(entrada(sol, operador, Set.of(operador.getId())));

    // Assert
    assertEquals(Set.of(operador.getId()), destinatarios);
    verificarLeituras(sol, operador);
  }

  @Test
  void shouldReadNothingWhenNobodyIsConnected() {
    // Arrange
    final UUID solicitacaoId = UUID.randomUUID();

    // Act
    final Set<UUID> destinatarios =
        useCase.execute(
            new ResolverDestinatariosEventoUseCase.Input(solicitacaoId, Set.of(), Set.of()));

    // Assert
    assertEquals(Set.of(), destinatarios);
    verifyNoMoreInteractions(solicitacaoRepository, atribuicaoRepository, usuarioRepository);
  }

  @Test
  void shouldReturnNobodyWhenSolicitacaoDoesNotExist() {
    // Arrange
    final UUID solicitacaoId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solicitacaoId)).thenReturn(Optional.empty());

    // Act
    final Set<UUID> destinatarios =
        useCase.execute(
            new ResolverDestinatariosEventoUseCase.Input(
                solicitacaoId, Set.of(UUID.randomUUID()), Set.of()));

    // Assert
    assertEquals(Set.of(), destinatarios);
    verify(solicitacaoRepository, times(1)).findById(solicitacaoId);
    verifyNoMoreInteractions(solicitacaoRepository, atribuicaoRepository, usuarioRepository);
  }

  @Test
  void shouldAskForEveryConnectedUserWhenSeveralAreConnected() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR);
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    final Solicitacao sol = abertaPor(UUID.randomUUID());
    final Set<UUID> conectados = Set.of(gestor.getId(), operador.getId());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());
    when(usuarioRepository.findAllByIdIn(
            argThat(ids -> ids.size() == conectados.size() && conectados.containsAll(ids))))
        .thenReturn(List.of(gestor, operador));

    // Act
    final Set<UUID> destinatarios =
        useCase.execute(
            new ResolverDestinatariosEventoUseCase.Input(sol.getId(), conectados, Set.of()));

    // Assert
    assertEquals(Set.of(gestor.getId()), destinatarios);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verify(usuarioRepository, times(1))
        .findAllByIdIn(
            argThat(ids -> ids.size() == conectados.size() && conectados.containsAll(ids)));
    verifyNoMoreInteractions(solicitacaoRepository, atribuicaoRepository, usuarioRepository);
  }
}
