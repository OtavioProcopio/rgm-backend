package com.rgm.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtAccessTokenIssuerTest {

  private static final String SECRET = "my-secret-key-must-be-at-least-32-bytes-long-for-jwt-signing";
  private JwtAccessTokenIssuer issuer;

  @BeforeEach
  void setUp() {
    issuer = new JwtAccessTokenIssuer(SECRET, 1, 7);
  }

  private Usuario criarUsuario() {
    return new Usuario(
        UUID.randomUUID(),
        "Teste User",
        "teste@rgm.com",
        "senhaHash",
        PerfilUsuario.OPERADOR,
        true,
        Instant.now(),
        Instant.now());
  }

  @Test
  void issue_deveCriarAccessTokenValido() {
    final Usuario usuario = criarUsuario();
    final String token = issuer.issue(usuario);

    assertNotNull(token);
  }

  @Test
  void issueRefreshToken_deveCriarRefreshTokenValido() {
    final Usuario usuario = criarUsuario();
    final String token = issuer.issueRefreshToken(usuario);

    assertNotNull(token);
    final UUID userId = issuer.validateRefreshToken(token);
    assertEquals(usuario.getId(), userId);
  }

  @Test
  void validateRefreshToken_quandoNaoForRefreshToken_deveLancarExcecao() {
    final Usuario usuario = criarUsuario();
    // Gera um access token (que possui type "access")
    final String accessToken = issuer.issue(usuario);

    assertThrows(IllegalArgumentException.class, () -> {
      issuer.validateRefreshToken(accessToken);
    });
  }
}
