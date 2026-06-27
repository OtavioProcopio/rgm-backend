package com.rgm.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final String SECRET =
      "my-secret-key-must-be-at-least-32-bytes-long-for-jwt-signing";
  private JwtAuthenticationFilter filter;
  private SecretKey key;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(SECRET);
    key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_semHeaderAuthorization_deveApenasPassarFiltro() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_comHeaderNaoBearer_deveApenasPassarFiltro() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Basic user:pass");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_comTokenValidoAccess_deveAutenticarESeguir() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    final Instant now = Instant.now();
    final UUID userId = UUID.randomUUID();
    final String token =
        Jwts.builder()
            .subject(userId.toString())
            .claim("perfil", "OPERADOR")
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(
        userId.toString(), SecurityContextHolder.getContext().getAuthentication().getPrincipal());
  }

  @Test
  void doFilterInternal_comTokenTipoIncorreto_deveRejeitarESeguirSemAutenticar() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    final Instant now = Instant.now();
    final String token =
        Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_comTokenExpirado_deveSeguirSemAutenticar() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    final Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
    final String token =
        Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("perfil", "OPERADOR")
            .claim("type", "access")
            .issuedAt(Date.from(past))
            .expiration(Date.from(past.plus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doFilterInternal_comTokenAssinaturaInvalida_deveSeguirSemAutenticar() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Bearer invalidTokenValueHere");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
