package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.solicitacao.ResolverDestinatariosEventoUseCase;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SolicitacaoEventPublisherTest {

  private static final String TIPO = "solicitacao";
  private static final String DADOS = "payload";

  private ResolverDestinatariosEventoUseCase resolverDestinatarios;
  private SolicitacaoEventPublisher publisher;
  private UUID solicitacaoId;

  @BeforeEach
  void setUp() {
    resolverDestinatarios = mock(ResolverDestinatariosEventoUseCase.class);
    publisher = new SolicitacaoEventPublisher(resolverDestinatarios);
    solicitacaoId = UUID.randomUUID();
  }

  private SseEmitter conectar(final UUID usuarioId) {
    final SseEmitter emitter = mock(SseEmitter.class);
    publisher.addEmitter(usuarioId, emitter);
    return emitter;
  }

  private ResolverDestinatariosEventoUseCase.Input entrada(
      final Set<UUID> conectados, final Set<UUID> comAcessoAnterior) {
    return new ResolverDestinatariosEventoUseCase.Input(
        solicitacaoId, conectados, comAcessoAnterior);
  }

  private static String conteudoEnviado(final SseEmitter emitter) throws IOException {
    final ArgumentCaptor<SseEmitter.SseEventBuilder> evento =
        ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
    verify(emitter, times(1)).send(evento.capture());
    return evento.getValue().build().stream()
        .map(parte -> String.valueOf(parte.getData()))
        .collect(Collectors.joining());
  }

  private static void verificarRegistroDeCallbacks(final SseEmitter emitter) {
    verify(emitter, times(1)).onCompletion(any());
    verify(emitter, times(1)).onTimeout(any());
    verify(emitter, times(1)).onError(any());
  }

  @Test
  void shouldSendEventOnlyToResolvedRecipientsWhenPublishing() throws IOException {
    // Arrange
    final UUID comAcesso = UUID.randomUUID();
    final UUID semAcesso = UUID.randomUUID();
    final SseEmitter destinatario = conectar(comAcesso);
    final SseEmitter excluido = conectar(semAcesso);
    final var entrada = entrada(Set.of(comAcesso, semAcesso), Set.of());
    when(resolverDestinatarios.execute(entrada)).thenReturn(Set.of(comAcesso));

    // Act
    publisher.publish(TIPO, DADOS, solicitacaoId);

    // Assert
    assertEquals("event:" + TIPO + "\ndata:" + DADOS + "\n\n", conteudoEnviado(destinatario));
    verify(resolverDestinatarios, times(1)).execute(entrada);
    verificarRegistroDeCallbacks(destinatario);
    verificarRegistroDeCallbacks(excluido);
    verifyNoMoreInteractions(resolverDestinatarios, destinatario, excluido);
  }

  @Test
  void shouldSendEventToEveryConnectionOfTheRecipientWhenUserHasTwoTabs() throws IOException {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final SseEmitter primeiraAba = conectar(usuarioId);
    final SseEmitter segundaAba = conectar(usuarioId);
    final var entrada = entrada(Set.of(usuarioId), Set.of());
    when(resolverDestinatarios.execute(entrada)).thenReturn(Set.of(usuarioId));

    // Act
    publisher.publish(TIPO, DADOS, solicitacaoId);

    // Assert
    verify(primeiraAba, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(segundaAba, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(resolverDestinatarios, times(1)).execute(entrada);
    verificarRegistroDeCallbacks(primeiraAba);
    verificarRegistroDeCallbacks(segundaAba);
    verifyNoMoreInteractions(resolverDestinatarios, primeiraAba, segundaAba);
  }

  @Test
  void shouldPassPreviousAccessToTheResolverWhenPublishingAfterRemoval() throws IOException {
    // Arrange
    final UUID removido = UUID.randomUUID();
    final SseEmitter emitter = conectar(removido);
    final var entrada = entrada(Set.of(removido), Set.of(removido));
    when(resolverDestinatarios.execute(entrada)).thenReturn(Set.of(removido));

    // Act
    publisher.publish(TIPO, DADOS, solicitacaoId, List.of(removido));

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(resolverDestinatarios, times(1)).execute(entrada);
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(resolverDestinatarios, emitter);
  }

  @Test
  void shouldNotResolveRecipientsWhenNobodyIsConnected() {
    // Arrange
    final UUID semConexoes = solicitacaoId;

    // Act
    publisher.publish(TIPO, DADOS, semConexoes);

    // Assert
    verifyNoMoreInteractions(resolverDestinatarios);
  }

  @Test
  void shouldDropConnectionWhenSendingEventFails() throws IOException {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final SseEmitter emitter = conectar(usuarioId);
    final var entrada = entrada(Set.of(usuarioId), Set.of());
    when(resolverDestinatarios.execute(entrada)).thenReturn(Set.of(usuarioId));
    doThrow(new IOException("fechado")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
    publisher.publish(TIPO, DADOS, solicitacaoId);

    // Act
    publisher.publish(TIPO, DADOS, solicitacaoId);

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(resolverDestinatarios, times(1)).execute(entrada);
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(resolverDestinatarios, emitter);
  }

  @Test
  void shouldDropConnectionWhenEmitterCompletes() throws IOException {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final SseEmitter emitter = mock(SseEmitter.class);
    doAnswer(
            chamada -> {
              chamada.<Runnable>getArgument(0).run();
              return null;
            })
        .when(emitter)
        .onCompletion(any());
    publisher.addEmitter(usuarioId, emitter);

    // Act
    publisher.enviarHeartbeat();

    // Assert
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(resolverDestinatarios, emitter);
  }

  @Test
  void shouldCompleteOnlyTheConnectionsOfTheUserWhenClosingConnections() throws IOException {
    // Arrange
    final UUID alvo = UUID.randomUUID();
    final SseEmitter primeiraDoAlvo = conectar(alvo);
    final SseEmitter segundaDoAlvo = conectar(alvo);
    final SseEmitter deOutroUsuario = conectar(UUID.randomUUID());

    // Act
    publisher.encerrarConexoes(alvo);

    // Assert
    verify(primeiraDoAlvo, times(1)).complete();
    verify(segundaDoAlvo, times(1)).complete();
    verificarRegistroDeCallbacks(primeiraDoAlvo);
    verificarRegistroDeCallbacks(segundaDoAlvo);
    verificarRegistroDeCallbacks(deOutroUsuario);
    verifyNoMoreInteractions(resolverDestinatarios, primeiraDoAlvo, segundaDoAlvo, deOutroUsuario);
  }

  @Test
  void shouldStopSendingToClosedConnectionsWhenHeartbeatRunsAfterClosing() throws IOException {
    // Arrange
    final UUID alvo = UUID.randomUUID();
    final SseEmitter encerrado = conectar(alvo);
    final SseEmitter mantido = conectar(UUID.randomUUID());
    publisher.encerrarConexoes(alvo);

    // Act
    publisher.enviarHeartbeat();

    // Assert
    assertEquals(":ping\n\n", conteudoEnviado(mantido));
    verify(encerrado, times(1)).complete();
    verificarRegistroDeCallbacks(encerrado);
    verificarRegistroDeCallbacks(mantido);
    verifyNoMoreInteractions(resolverDestinatarios, encerrado, mantido);
  }

  @Test
  void shouldSendPingCommentToEveryConnectionWhenHeartbeatRuns() throws IOException {
    // Arrange
    final SseEmitter primeiro = conectar(UUID.randomUUID());
    final SseEmitter segundo = conectar(UUID.randomUUID());

    // Act
    publisher.enviarHeartbeat();

    // Assert
    assertEquals(":ping\n\n", conteudoEnviado(primeiro));
    assertEquals(":ping\n\n", conteudoEnviado(segundo));
    verificarRegistroDeCallbacks(primeiro);
    verificarRegistroDeCallbacks(segundo);
    verifyNoMoreInteractions(resolverDestinatarios, primeiro, segundo);
  }

  @Test
  void shouldStopSendingHeartbeatWhenEmitterFailsWithIoError() throws IOException {
    // Arrange
    final SseEmitter emitter = conectar(UUID.randomUUID());
    doThrow(new IOException("conexao fechada"))
        .when(emitter)
        .send(any(SseEmitter.SseEventBuilder.class));
    publisher.enviarHeartbeat();

    // Act
    publisher.enviarHeartbeat();

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(resolverDestinatarios, emitter);
  }

  @Test
  void shouldStopSendingHeartbeatWhenEmitterIsAlreadyCompleted() throws IOException {
    // Arrange
    final SseEmitter emitter = conectar(UUID.randomUUID());
    doThrow(new IllegalStateException("emitter completo"))
        .when(emitter)
        .send(any(SseEmitter.SseEventBuilder.class));
    publisher.enviarHeartbeat();

    // Act
    publisher.enviarHeartbeat();

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(resolverDestinatarios, emitter);
  }
}
