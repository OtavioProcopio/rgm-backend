package com.rgm.api.adapter.in.web.solicitacao;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SolicitacaoEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventPublisher.class);

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public void addEmitter(final SseEmitter emitter) {
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));
  }

  public void publish(final String eventType, final Object data) {
    for (final SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(eventType).data(data));
      } catch (final IOException | IllegalStateException e) {
        log.debug("Removendo emitter SSE inativo: {}", e.getMessage());
        emitters.remove(emitter);
      }
    }
  }
}
