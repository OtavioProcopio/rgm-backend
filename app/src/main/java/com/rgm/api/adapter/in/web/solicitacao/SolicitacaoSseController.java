package com.rgm.api.adapter.in.web.solicitacao;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/solicitacoes/events")
public class SolicitacaoSseController {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoSseController.class);
  private static final long TIMEOUT_MS = 300_000L;

  private final SolicitacaoEventPublisher publisher;
  private final SecretKey key;

  public SolicitacaoSseController(
      final SolicitacaoEventPublisher publisher, @Value("${jwt.secret}") final String secret) {
    this.publisher = publisher;
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@RequestParam(required = false) final String token) {
    validateToken(token);

    final SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    publisher.addEmitter(emitter);
    try {
      emitter.send(SseEmitter.event().name("connected").data("ok"));
    } catch (final IOException e) {
      log.debug("Falha ao enviar evento inicial SSE: {}", e.getMessage());
    }
    return emitter;
  }

  private void validateToken(final String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token ausente");
    }
    try {
      final Claims claims =
          Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      if (!"access".equals(claims.get("type", String.class))) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
      }
    } catch (final ResponseStatusException e) {
      throw e;
    } catch (final Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
    }
  }
}
