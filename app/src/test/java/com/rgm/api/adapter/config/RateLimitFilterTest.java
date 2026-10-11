package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RateLimitFilterTest {

  private RateLimitFilter filter;
  private int maxRequests = 3;
  private int windowSeconds = 60;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter(maxRequests, windowSeconds, "*");
  }

  @Test
  void doFilterInternal_quandoUriNaoForLogin_deveApenasPassarFiltro() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/api/modelos");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_quandoUriForLoginELimiteNaoExcedido_devePassarFiltro() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    for (int i = 0; i < maxRequests; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(maxRequests)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_quandoUriForLoginELimiteExcedido_deveRetornar429() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.2");
    when(request.getHeader("Origin")).thenReturn("http://localhost:3000");

    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    when(response.getWriter()).thenReturn(pw);

    // Envia requisições até o limite
    for (int i = 0; i < maxRequests; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }
    verify(filterChain, times(maxRequests)).doFilter(request, response);

    // A próxima deve ser bloqueada (429)
    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    verify(response).setContentType("application/json");
    verify(response).setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
    verify(response).setHeader("Access-Control-Allow-Credentials", "true");
    verify(filterChain, times(maxRequests))
        .doFilter(request, response); // Não incrementou o count de doFilter

    final String responseContent = sw.toString();
    assertEquals(
        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Limite de requisicoes excedido. Tente novamente em 60 segundos.\"}",
        responseContent);
  }

  @Test
  void shouldKeepServingDistinctClientsWhenExpiredEntriesAreEvictedAtTheHundredthRequest()
      throws Exception {
    // Arrange
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // Act: 105 clientes diferentes forçam a limpeza de entradas vencidas (a cada 100 requests)
    for (int i = 0; i < 105; i++) {
      final HttpServletRequest req = mock(HttpServletRequest.class);
      when(req.getRequestURI()).thenReturn("/api/auth/login");
      when(req.getRemoteAddr()).thenReturn("127.0.0." + i);
      filter.doFilterInternal(req, response, filterChain);
    }

    // Assert
    verify(filterChain, times(105)).doFilter(any(), any());
    verify(response, never()).setStatus(429);
  }
}
