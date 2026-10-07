package com.rgm.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final String TOKEN = "token-de-acesso";

  private AutenticarAcessoUseCase autenticarAcessoUseCase;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;
  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    autenticarAcessoUseCase = mock(AutenticarAcessoUseCase.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    filter = new JwtAuthenticationFilter(autenticarAcessoUseCase);
    SecurityContextHolder.clearContext();
  }

  private static Usuario usuario(final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(), "Usuario", "usuario@rgm.test", "hash", perfil, true, agora, agora);
  }

  private void verificarQueSeguiuSemMaisChamadas() throws Exception {
    verify(request, times(1)).getHeader("Authorization");
    verify(filterChain, times(1)).doFilter(request, response);
    verifyNoMoreInteractions(autenticarAcessoUseCase, request, response, filterChain);
  }

  @Test
  void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn(null);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verificarQueSeguiuSemMaisChamadas();
  }

  @Test
  void shouldNotAuthenticateWhenAuthorizationHeaderIsNotBearer() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn("Basic user:pass");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verificarQueSeguiuSemMaisChamadas();
  }

  @Test
  void shouldAuthenticateWithUserIdAsPrincipalWhenCredentialIsAccepted() throws Exception {
    // Arrange
    final Usuario usuario = usuario(PerfilUsuario.OPERADOR);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    when(autenticarAcessoUseCase.execute(TOKEN)).thenReturn(usuario);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertEquals(
        usuario.getId().toString(),
        SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verificarQueSeguiuSemMaisChamadas();
  }

  @Test
  void shouldGrantAuthorityFromCurrentProfileWhenCredentialIsAccepted() throws Exception {
    // Arrange
    final Usuario rebaixado = usuario(PerfilUsuario.OPERADOR);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    when(autenticarAcessoUseCase.execute(TOKEN)).thenReturn(rebaixado);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertEquals(
        List.of("ROLE_" + rebaixado.getPerfil().name()),
        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verificarQueSeguiuSemMaisChamadas();
  }

  @Test
  void shouldNotAuthenticateWhenCredentialIsRefused() throws Exception {
    // Arrange
    when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    when(autenticarAcessoUseCase.execute(TOKEN))
        .thenThrow(new NaoAutorizadoException("Credencial emitida antes da troca de senha"));

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verificarQueSeguiuSemMaisChamadas();
  }
}
