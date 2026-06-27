package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleNotFound_deveRetornar404() {
    final RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("Não encontrado");
    final ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(404, response.getBody().status());
    assertEquals("Não encontrado", response.getBody().message());
  }

  @Test
  void handleValidation_deveRetornar400() {
    final ValidationException ex = new ValidationException("Erro de validação");
    final ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("Erro de validação", response.getBody().message());
  }

  @Test
  void handleBusinessRule_deveRetornar422() {
    final BusinessRuleException ex = new BusinessRuleException("Erro de negócio");
    final ResponseEntity<ErrorResponse> response = handler.handleBusinessRule(ex);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertEquals(422, response.getBody().status());
    assertEquals("Erro de negócio", response.getBody().message());
  }

  @Test
  void handleNaoAutorizado_deveRetornar403() {
    final NaoAutorizadoException ex = new NaoAutorizadoException("Não autorizado");
    final ResponseEntity<ErrorResponse> response = handler.handleNaoAutorizado(ex);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals(403, response.getBody().status());
    assertEquals("Não autorizado", response.getBody().message());
  }

  @Test
  void handleTransicaoInvalida_deveRetornar409() {
    final TransicaoStatusInvalidaException ex = new TransicaoStatusInvalidaException(StatusSolicitacao.A_FAZER, StatusSolicitacao.CONCLUIDA);
    final ResponseEntity<ErrorResponse> response = handler.handleTransicaoInvalida(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(409, response.getBody().status());
  }

  @Test
  void handleDataIntegrity_deveRetornar409() {
    final DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicado");
    final ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(409, response.getBody().status());
    assertEquals("Registro duplicado ou violacao de integridade", response.getBody().message());
  }

  @Test
  void handleMaxUploadSize_deveRetornar413() {
    final MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1000L);
    final ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(ex);

    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
    assertEquals(413, response.getBody().status());
    assertEquals("Arquivo excede o tamanho maximo permitido", response.getBody().message());
  }

  @Test
  void handleIllegalArgument_deveRetornar400() {
    final IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");
    final ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("Argumento inválido", response.getBody().message());
  }

  @Test
  void handleTypeMismatch_deveRetornar400() {
    final MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
    when(ex.getName()).thenReturn("id");
    final ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("Parâmetro 'id' com formato inválido", response.getBody().message());
  }

  @Test
  void handleMethodNotSupported_deveRetornar405() {
    final HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
    final ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex);

    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    assertEquals(405, response.getBody().status());
    assertEquals("Método HTTP não suportado para este recurso", response.getBody().message());
  }

  @Test
  void handleAccessDenied_deveRetornar403() {
    final AccessDeniedException ex = new AccessDeniedException("Negado");
    final ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals(403, response.getBody().status());
    assertEquals("Acesso negado: permissões insuficientes", response.getBody().message());
  }

  @Test
  void handleRuntime_deveRetornar500() {
    final RuntimeException ex = new RuntimeException("Runtime");
    final ResponseEntity<ErrorResponse> response = handler.handleRuntime(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(500, response.getBody().status());
    assertEquals("Erro interno do servidor", response.getBody().message());
  }

  @Test
  void handleMethodArgumentNotValid_deveRetornar400ComDetalhes() {
    final MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    final BindingResult bindingResult = mock(BindingResult.class);
    final FieldError fieldError = new FieldError("dto", "email", "Formato inválido");
    
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    final ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(400, response.getBody().status());
    assertEquals("email: Formato inválido", response.getBody().message());
  }
}
