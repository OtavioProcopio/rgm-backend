package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
