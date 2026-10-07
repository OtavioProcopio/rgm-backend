package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.core.application.usecases.solicitacao.ResolverDestinatariosEventoUseCase;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SolicitacaoEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventPublisher.class);
  private static final long INTERVALO_HEARTBEAT_MS = 25_000L;

  /** Uma conexao de tempo real e o usuario que a abriu. */
  private record Conexao(UUID usuarioId, SseEmitter emitter) {}

  private final CopyOnWriteArrayList<Conexao> conexoes = new CopyOnWriteArrayList<>();
  private final ResolverDestinatariosEventoUseCase resolverDestinatariosUseCase;

  public SolicitacaoEventPublisher(
      final ResolverDestinatariosEventoUseCase resolverDestinatariosUseCase) {
    this.resolverDestinatariosUseCase = resolverDestinatariosUseCase;
  }

  public void addEmitter(final UUID usuarioId, final SseEmitter emitter) {
    final Conexao conexao = new Conexao(usuarioId, emitter);
    conexoes.add(conexao);
    emitter.onCompletion(() -> conexoes.remove(conexao));
    emitter.onTimeout(() -> conexoes.remove(conexao));
    emitter.onError(e -> conexoes.remove(conexao));
  }

  /** Envia o evento de uma solicitacao a quem, entre os conectados, tem acesso a ela. */
  public void publish(final String eventType, final Object data, final UUID solicitacaoId) {
    publish(eventType, data, solicitacaoId, Set.of());
  }

  /**
   * Envia o evento de uma solicitacao a quem tem acesso a ela e a quem tinha acesso antes da
   * mudanca que o evento descreve ({@code comAcessoAnterior}).
   */
  public void publish(
      final String eventType,
      final Object data,
      final UUID solicitacaoId,
      final Collection<UUID> comAcessoAnterior) {
    final Set<UUID> conectados =
        conexoes.stream().map(Conexao::usuarioId).collect(Collectors.toSet());
    if (conectados.isEmpty()) {
      return;
    }
    final Set<UUID> destinatarios =
        resolverDestinatariosUseCase.execute(
            new ResolverDestinatariosEventoUseCase.Input(
                solicitacaoId, conectados, Set.copyOf(comAcessoAnterior)));
    for (final Conexao conexao : conexoes) {
      if (destinatarios.contains(conexao.usuarioId())) {
        enviar(conexao, () -> SseEmitter.event().name(eventType).data(data));
      }
    }
  }

  /** Encerra as conexoes abertas de um usuario; usado quando a senha dele muda. */
  public void encerrarConexoes(final UUID usuarioId) {
    for (final Conexao conexao : conexoes) {
      if (conexao.usuarioId().equals(usuarioId)) {
        conexoes.remove(conexao);
        conexao.emitter().complete();
      }
    }
  }

  /**
   * Mantem as conexoes SSE vivas atras de proxies que encerram conexoes sem trafego. Envia um
   * comentario, que o cliente ignora, e descarta os emitters que falharem.
   */
  @Scheduled(fixedRate = INTERVALO_HEARTBEAT_MS)
  public void enviarHeartbeat() {
    for (final Conexao conexao : conexoes) {
      enviar(conexao, () -> SseEmitter.event().comment("ping"));
    }
  }

  private void enviar(final Conexao conexao, final Supplier<SseEmitter.SseEventBuilder> evento) {
    try {
      conexao.emitter().send(evento.get());
    } catch (final IOException | IllegalStateException e) {
      log.debug("Removendo emitter SSE inativo: {}", e.getMessage());
      conexoes.remove(conexao);
    }
  }
}
