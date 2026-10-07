package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/solicitacoes/events")
public class SolicitacaoSseController {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoSseController.class);
  private static final long TIMEOUT_MS = 30L * 60L * 1000L;

  private final SolicitacaoEventPublisher publisher;
  private final AutenticarAcessoUseCase autenticarAcessoUseCase;

  public SolicitacaoSseController(
      final SolicitacaoEventPublisher publisher,
      final AutenticarAcessoUseCase autenticarAcessoUseCase) {
    this.publisher = publisher;
    this.autenticarAcessoUseCase = autenticarAcessoUseCase;
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@RequestParam(required = false) final String token) {
    final Usuario usuario = autenticar(token);

    final SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    publisher.addEmitter(usuario.getId(), emitter);
    try {
      emitter.send(SseEmitter.event().name("connected").data("ok"));
    } catch (final IOException | IllegalStateException e) {
      // IllegalStateException: a conexao foi encerrada entre o registro e o primeiro envio.
      log.debug("Falha ao enviar evento inicial SSE: {}", e.getMessage());
    }
    return emitter;
  }

  private Usuario autenticar(final String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token ausente");
    }
    try {
      return autenticarAcessoUseCase.execute(token);
    } catch (final NaoAutorizadoException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
    }
  }
}
