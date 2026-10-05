package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SolicitacaoEventPublisherTest {

  private SolicitacaoEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new SolicitacaoEventPublisher();
  }

  @Test
  void deveFuncionarSemEmitters() {
    assertDoesNotThrow(() -> publisher.publish("solicitacao", "payload"));
  }

  @Test
  void deveRegistrarCallbacksNoEmitter() {
    final SseEmitter emitter = mock(SseEmitter.class);
    publisher.addEmitter(emitter);

    verify(emitter).onCompletion(any());
    verify(emitter).onTimeout(any());
    verify(emitter).onError(any());
  }

  @Test
  void deveRemoverEmitterQuandoLancaIOException() throws IOException {
    final SseEmitter emitter = spy(new SseEmitter());
    doThrow(new IOException("fechado")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

    publisher.addEmitter(emitter);
    publisher.publish("solicitacao", "payload");

    // Segunda publicação não deve tentar enviar ao emitter removido
    publisher.publish("solicitacao", "payload2");
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void deveInvocarCallbackDeCompletion() {
    final AtomicBoolean removido = new AtomicBoolean(false);
    final SseEmitter emitter =
        new SseEmitter() {
          @Override
          public void onCompletion(final Runnable callback) {
            callback.run();
          }

          @Override
          public void onTimeout(final Runnable callback) {}

          @Override
          public void onError(final java.util.function.Consumer<Throwable> callback) {}
        };

    publisher.addEmitter(emitter);
    // onCompletion ran immediately → emitter removed
    // publish should not throw even with empty list
    assertDoesNotThrow(() -> publisher.publish("solicitacao", "payload"));
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
  void shouldSendPingCommentToEveryEmitterWhenHeartbeatRuns() throws IOException {
    // Arrange
    final SseEmitter primeiro = mock(SseEmitter.class);
    final SseEmitter segundo = mock(SseEmitter.class);
    publisher.addEmitter(primeiro);
    publisher.addEmitter(segundo);

    // Act
    publisher.enviarHeartbeat();

    // Assert
    assertEquals(":ping\n\n", conteudoEnviado(primeiro));
    assertEquals(":ping\n\n", conteudoEnviado(segundo));
    verificarRegistroDeCallbacks(primeiro);
    verificarRegistroDeCallbacks(segundo);
    verifyNoMoreInteractions(primeiro, segundo);
  }

  @Test
  void shouldStopSendingHeartbeatWhenEmitterFailsWithIoError() throws IOException {
    // Arrange
    final SseEmitter emitter = mock(SseEmitter.class);
    doThrow(new IOException("conexao fechada"))
        .when(emitter)
        .send(any(SseEmitter.SseEventBuilder.class));
    publisher.addEmitter(emitter);
    publisher.enviarHeartbeat();

    // Act
    publisher.enviarHeartbeat();

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(emitter);
  }

  @Test
  void shouldStopSendingHeartbeatWhenEmitterIsAlreadyCompleted() throws IOException {
    // Arrange
    final SseEmitter emitter = mock(SseEmitter.class);
    doThrow(new IllegalStateException("emitter completo"))
        .when(emitter)
        .send(any(SseEmitter.SseEventBuilder.class));
    publisher.addEmitter(emitter);
    publisher.enviarHeartbeat();

    // Act
    publisher.enviarHeartbeat();

    // Assert
    verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verificarRegistroDeCallbacks(emitter);
    verifyNoMoreInteractions(emitter);
  }

  @Test
  void shouldDoNothingWhenHeartbeatRunsWithoutEmitters() {
    // Arrange
    final SolicitacaoEventPublisher semEmitters = new SolicitacaoEventPublisher();

    // Act
    assertDoesNotThrow(semEmitters::enviarHeartbeat);

    // Assert
    assertDoesNotThrow(() -> semEmitters.publish("solicitacao", "payload"));
  }
}
