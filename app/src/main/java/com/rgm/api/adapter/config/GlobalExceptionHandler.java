package com.rgm.api.adapter.config;

import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(final RecursoNaoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(final ValidationException ex) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(400, "Validation Error", ex.getMessage()));
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRule(final BusinessRuleException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ErrorResponse.of(422, "Business Rule Error", ex.getMessage()));
  }

  @ExceptionHandler(NaoAutorizadoException.class)
  public ResponseEntity<ErrorResponse> handleNaoAutorizado(final NaoAutorizadoException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
  }

  @ExceptionHandler(TransicaoStatusInvalidaException.class)
  public ResponseEntity<ErrorResponse> handleTransicaoInvalida(
      final TransicaoStatusInvalidaException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      final MethodArgumentNotValidException ex) {
    final String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Erro de validacao");
    return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Validation Error", message));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
      final DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, "Conflict", "Registro duplicado ou violacao de integridade"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSize(
      final MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ErrorResponse.of(
                413, "Payload Too Large", "Arquivo excede o tamanho maximo permitido"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntime(final RuntimeException ex) {
    log.error("Erro interno nao tratado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(500, "Internal Server Error", "Erro interno do servidor"));
  }
}
