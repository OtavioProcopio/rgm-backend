package com.rgm.api.adapter.out.security;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final SecretKey key;
  private final long expirationHours;

  public JwtAccessTokenIssuer(
      @Value("${jwt.secret}") final String secret,
      @Value("${jwt.expiration-hours:24}") final long expirationHours) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationHours = expirationHours;
  }

  @Override
  public String issue(final Usuario usuario) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(usuario.getId().toString())
        .claim("perfil", usuario.getPerfil().name())
        .claim("nome", usuario.getNome())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
        .signWith(key)
        .compact();
  }
}
