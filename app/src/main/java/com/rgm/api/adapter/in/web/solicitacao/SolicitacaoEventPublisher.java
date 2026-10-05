package com.rgm.api.adapter.in.web.solicitacao;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SolicitacaoEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventPublisher.class);
  private static final long INTERVALO_HEARTBEAT_MS = 25_000L;

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public void addEmitter(final SseEmitter emitter) {
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));
  }

  public void publish(final String eventType, final Object data) {
    enviarParaTodos(() -> SseEmitter.event().name(eventType).data(data));
  }

  /**
   * Mantem as conexoes SSE vivas atras de proxies que encerram conexoes sem trafego. Envia um
   * comentario, que o cliente ignora, e descarta os emitters que falharem.
   */
  @Scheduled(fixedRate = INTERVALO_HEARTBEAT_MS)
  public void enviarHeartbeat() {
    enviarParaTodos(() -> SseEmitter.event().comment("ping"));
  }

  private void enviarParaTodos(final Supplier<SseEmitter.SseEventBuilder> evento) {
    for (final SseEmitter emitter : emitters) {
      try {
        emitter.send(evento.get());
      } catch (final IOException | IllegalStateException e) {
        log.debug("Removendo emitter SSE inativo: {}", e.getMessage());
        emitters.remove(emitter);
      }
    }
  }
}
