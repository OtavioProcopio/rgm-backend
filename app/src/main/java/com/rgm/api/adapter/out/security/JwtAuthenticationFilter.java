package com.rgm.api.adapter.out.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final SecretKey key;

  public JwtAuthenticationFilter(@Value("${jwt.secret}") final String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    final String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        final String token = header.substring(7);
        final Claims claims =
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        final String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
          filterChain.doFilter(request, response);
          return;
        }

        final String userId = claims.getSubject();
        final String perfil = claims.get("perfil", String.class);

        final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil)));

        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (final io.jsonwebtoken.ExpiredJwtException e) {
        log.debug("Token JWT expirado: {}", e.getMessage());
      } catch (final Exception e) {
        log.warn("Falha ao validar token JWT: {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
