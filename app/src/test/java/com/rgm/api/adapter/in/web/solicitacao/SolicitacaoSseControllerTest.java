package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SolicitacaoSseControllerTest {

  private static final String SECRET =
      "my-secret-key-must-be-at-least-32-bytes-long-for-jwt-signing";

  private SolicitacaoEventPublisher publisher;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoSseController controller;

  @BeforeEach
  void setUp() {
    publisher = mock(SolicitacaoEventPublisher.class);
    usuarioRepository = mock(UsuarioRepository.class);
    controller = new SolicitacaoSseController(publisher, SECRET, usuarioRepository);
  }

  private static Usuario usuario(final UUID id, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Usuario", "usuario@rgm.test", "hash", PerfilUsuario.OPERADOR, ativo, agora, agora);
  }

  private static String accessToken(final UUID userId) {
    final Instant agora = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("perfil", "OPERADOR")
        .claim("type", "access")
        .issuedAt(Date.from(agora))
        .expiration(Date.from(agora.plus(1, ChronoUnit.HOURS)))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  @Test
  void shouldOpenConnectionWhenTokenBelongsToActiveUser() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario(userId, true)));

    // Act
    final SseEmitter emitter = controller.subscribe(accessToken(userId));

    // Assert
    assertEquals(30L * 60L * 1000L, emitter.getTimeout());
    verify(usuarioRepository, times(1)).findById(userId);
    verify(publisher, times(1)).addEmitter(emitter);
    verifyNoMoreInteractions(usuarioRepository, publisher);
  }

  @Test
  void shouldRefuseConnectionWhenTokenBelongsToInactiveUser() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario(userId, false)));

    // Act
    final ResponseStatusException erro =
        assertThrows(
            ResponseStatusException.class, () -> controller.subscribe(accessToken(userId)));

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    verify(usuarioRepository, times(1)).findById(userId);
    verifyNoMoreInteractions(usuarioRepository, publisher);
  }

  @Test
  void shouldRefuseConnectionWhenTokenBelongsToMissingUser() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    final ResponseStatusException erro =
        assertThrows(
            ResponseStatusException.class, () -> controller.subscribe(accessToken(userId)));

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    verify(usuarioRepository, times(1)).findById(userId);
    verifyNoMoreInteractions(usuarioRepository, publisher);
  }
}
