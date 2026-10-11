package com.rgm.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.services.CredencialToken;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtAccessTokenIssuerTest {

  private static final String SECRET =
      "my-secret-key-must-be-at-least-32-bytes-long-for-jwt-signing";
  private static final int VERSAO = 4;

  private JwtAccessTokenIssuer issuer;

  @BeforeEach
  void setUp() {
    issuer = new JwtAccessTokenIssuer(SECRET, 1, 7);
  }

  private static Usuario criarUsuario() {
    return new Usuario(
        UUID.randomUUID(),
        "Teste User",
        "teste@rgm.com",
        "senhaHash",
        PerfilUsuario.OPERADOR,
        true,
        Instant.now(),
        Instant.now(),
        VERSAO);
  }

  private static String tokenSemVersao(final UUID usuarioId, final String tipo) {
    return Jwts.builder()
        .subject(usuarioId.toString())
        .claim("type", tipo)
        .expiration(Date.from(Instant.now().plusSeconds(60)))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  @Test
  void shouldCarryUserAndCredentialVersionWhenAccessTokenIsIssued() {
    // Arrange
    final Usuario usuario = criarUsuario();

    // Act
    final CredencialToken credencial = issuer.validateAccessToken(issuer.issue(usuario));

    // Assert
    assertEquals(new CredencialToken(usuario.getId(), VERSAO), credencial);
  }

  @Test
  void shouldCarryUserAndCredentialVersionWhenRefreshTokenIsIssued() {
    // Arrange
    final Usuario usuario = criarUsuario();

    // Act
    final CredencialToken credencial =
        issuer.validateRefreshToken(issuer.issueRefreshToken(usuario));

    // Assert
    assertEquals(new CredencialToken(usuario.getId(), VERSAO), credencial);
  }

  @Test
  void shouldRejectAccessTokenWhenUsedAsRefreshToken() {
    // Arrange
    final String accessToken = issuer.issue(criarUsuario());

    // Act
    final IllegalArgumentException erro =
        assertThrows(
            IllegalArgumentException.class, () -> issuer.validateRefreshToken(accessToken));

    // Assert
    assertEquals("Token do tipo 'access' onde se esperava 'refresh'", erro.getMessage());
  }

  @Test
  void shouldRejectRefreshTokenWhenUsedAsAccessToken() {
    // Arrange
    final String refreshToken = issuer.issueRefreshToken(criarUsuario());

    // Act
    final IllegalArgumentException erro =
        assertThrows(
            IllegalArgumentException.class, () -> issuer.validateAccessToken(refreshToken));

    // Assert
    assertEquals("Token do tipo 'refresh' onde se esperava 'access'", erro.getMessage());
  }

  @Test
  void shouldReadVersionZeroWhenTokenWasIssuedBeforeVersionExisted() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final String antigo = tokenSemVersao(usuarioId, "access");

    // Act
    final CredencialToken credencial = issuer.validateAccessToken(antigo);

    // Assert
    assertEquals(new CredencialToken(usuarioId, 0), credencial);
  }

  @Test
  void shouldRejectTokenWhenSignedWithAnotherKey() {
    // Arrange
    final String deOutraChave =
        new JwtAccessTokenIssuer("another-secret-key-with-at-least-32-bytes-of-length", 1, 7)
            .issue(criarUsuario());

    // Act
    final Runnable leitura = () -> issuer.validateAccessToken(deOutraChave);

    // Assert
    assertThrows(JwtException.class, leitura::run);
  }
}
