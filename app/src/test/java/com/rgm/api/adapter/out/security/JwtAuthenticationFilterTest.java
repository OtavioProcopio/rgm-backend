package com.rgm.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final String SECRET =
      "my-secret-key-must-be-at-least-32-bytes-long-for-jwt-signing";
  private JwtAuthenticationFilter filter;
  private UsuarioRepository usuarioRepository;
  private SecretKey key;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    filter = new JwtAuthenticationFilter(SECRET, usuarioRepository);
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
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario(userId, true)));

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

  private static Usuario usuario(final UUID id, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Usuario", "usuario@rgm.test", "hash", PerfilUsuario.OPERADOR, ativo, agora, agora);
  }

  private String accessToken(final UUID userId) {
    final Instant agora = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("perfil", "OPERADOR")
        .claim("type", "access")
        .issuedAt(Date.from(agora))
        .expiration(Date.from(agora.plus(1, ChronoUnit.HOURS)))
        .signWith(key)
        .compact();
  }

  @Test
  void shouldAuthenticateWhenTokenBelongsToActiveUser() throws Exception {
    // Arrange
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);
    final UUID userId = UUID.randomUUID();
    when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken(userId));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario(userId, true)));

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertEquals(
        userId.toString(), SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(usuarioRepository, times(1)).findById(userId);
    verify(filterChain, times(1)).doFilter(request, response);
    verifyNoMoreInteractions(usuarioRepository, filterChain);
  }

  @Test
  void shouldNotAuthenticateWhenTokenBelongsToInactiveUser() throws Exception {
    // Arrange
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);
    final UUID userId = UUID.randomUUID();
    when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken(userId));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario(userId, false)));

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(usuarioRepository, times(1)).findById(userId);
    verify(filterChain, times(1)).doFilter(request, response);
    verifyNoMoreInteractions(usuarioRepository, filterChain);
  }

  @Test
  void shouldNotAuthenticateWhenTokenBelongsToMissingUser() throws Exception {
    // Arrange
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);
    final UUID userId = UUID.randomUUID();
    when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken(userId));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(usuarioRepository, times(1)).findById(userId);
    verify(filterChain, times(1)).doFilter(request, response);
    verifyNoMoreInteractions(usuarioRepository, filterChain);
  }
}
