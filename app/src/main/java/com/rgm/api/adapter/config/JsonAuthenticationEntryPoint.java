package com.rgm.api.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde 401 com corpo JSON ({@link ErrorResponse}) para requisições não autenticadas. Não usa
 * {@code sendError}: ele provoca o despacho para /error e o 401 sairia sem corpo.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public JsonAuthenticationEntryPoint(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException authException)
      throws IOException {
    final ErrorResponse body = ErrorResponse.of(401, "Unauthorized", "Não autenticado");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getOutputStream().write(objectMapper.writeValueAsBytes(body));
  }
}
