package com.rgm.api.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Responde 403 com corpo JSON ({@link ErrorResponse}) quando o acesso é negado. Não usa {@code
 * sendError}, evitando o despacho para /error, que exigiria autenticação e retornaria 401 sem
 * corpo.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private static final String MENSAGEM = "Acesso negado: permissões insuficientes";

  private final ObjectMapper objectMapper;

  public JsonAccessDeniedHandler(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AccessDeniedException accessDeniedException)
      throws IOException {
    final ErrorResponse body = ErrorResponse.of(403, "Forbidden", MENSAGEM);
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    final String json = objectMapper.writeValueAsString(body);
    response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
  }
}
