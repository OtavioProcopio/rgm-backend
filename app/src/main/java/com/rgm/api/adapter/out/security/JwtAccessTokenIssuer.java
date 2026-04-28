package com.rgm.api.adapter.out.security;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
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
        .claim("type", "access")
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
        .claim("type", "refresh")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(refreshExpirationDays, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  @Override
  public UUID validateRefreshToken(final String refreshToken) {
    final Claims claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();
    final String type = claims.get("type", String.class);
    if (!"refresh".equals(type)) {
      throw new IllegalArgumentException("Token nao e do tipo refresh");
    }
    return UUID.fromString(claims.getSubject());
  }
}
