package com.rgm.api.adapter.out.security;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.CredencialToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private static final String TIPO = "type";
  private static final String TIPO_ACESSO = "access";
  private static final String TIPO_RENOVACAO = "refresh";

  /** Versao da credencial do usuario quando o token foi emitido. */
  private static final String VERSAO = "ver";

  private final SecretKey key;
  private final long expirationHours;
  private final long refreshExpirationDays;

  public JwtAccessTokenIssuer(
      @Value("${jwt.secret}") final String secret,
      @Value("${jwt.expiration-hours:24}") final long expirationHours,
      @Value("${jwt.refresh-expiration-days:7}") final long refreshExpirationDays) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationHours = expirationHours;
    this.refreshExpirationDays = refreshExpirationDays;
  }

  @Override
  public String issue(final Usuario usuario) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(usuario.getId().toString())
        .claim("perfil", usuario.getPerfil().name())
        .claim("nome", usuario.getNome())
        .claim(TIPO, TIPO_ACESSO)
        .claim(VERSAO, usuario.getVersaoCredencial())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
        .signWith(key)
        .compact();
  }

  @Override
  public String issueRefreshToken(final Usuario usuario) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(usuario.getId().toString())
        .claim(TIPO, TIPO_RENOVACAO)
        .claim(VERSAO, usuario.getVersaoCredencial())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(refreshExpirationDays, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  @Override
  public CredencialToken validateAccessToken(final String accessToken) {
    return validar(accessToken, TIPO_ACESSO);
  }

  @Override
  public CredencialToken validateRefreshToken(final String refreshToken) {
    return validar(refreshToken, TIPO_RENOVACAO);
  }

  private CredencialToken validar(final String token, final String tipoEsperado) {
    final Claims claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    final String tipo = claims.get(TIPO, String.class);
    if (!tipoEsperado.equals(tipo)) {
      throw new IllegalArgumentException(
          "Token do tipo '" + tipo + "' onde se esperava '" + tipoEsperado + "'");
    }
    // Token emitido antes de a versao existir nao traz o campo e vale como versao 0.
    final Integer versao = claims.get(VERSAO, Integer.class);
    return new CredencialToken(UUID.fromString(claims.getSubject()), versao != null ? versao : 0);
  }
}
