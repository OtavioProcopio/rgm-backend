package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void deveRetornar404ParaRecursoNaoEncontrado() {
    final RecursoNaoEncontradoException ex =
        new RecursoNaoEncontradoException("Solicitacao nao encontrada");

    final ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

    assertEquals(404, response.getStatusCode().value());
    assertEquals("Not Found", response.getBody().error());
  }

  @Test
  void deveRetornar409ParaTransicaoInvalida() {
    final TransicaoStatusInvalidaException ex =
        new TransicaoStatusInvalidaException(
            StatusSolicitacao.CONCLUIDA, StatusSolicitacao.EM_ANDAMENTO);

    final ResponseEntity<ErrorResponse> response = handler.handleTransicaoInvalida(ex);

    assertEquals(409, response.getStatusCode().value());
    assertEquals("Conflict", response.getBody().error());
  }

  @Test
  void deveRetornar400ParaTypeMismatch() {
    final var ex =
        org.mockito.Mockito.mock(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class);
    org.mockito.Mockito.when(ex.getName()).thenReturn("id");

    final ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

    assertEquals(400, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("Parâmetro 'id' com formato inválido"));
  }

  @Test
  void deveRetornar405ParaMethodNotSupported() {
    final var ex = new org.springframework.web.HttpRequestMethodNotSupportedException("POST");

    final ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex);

    assertEquals(405, response.getStatusCode().value());
    assertEquals("Method Not Allowed", response.getBody().error());
  }

  @Test
  void deveRetornar403ParaAccessDenied() {
    final var ex = new org.springframework.security.access.AccessDeniedException("Negado");

    final ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

    assertEquals(403, response.getStatusCode().value());
    assertEquals("Forbidden", response.getBody().error());
  }
}
