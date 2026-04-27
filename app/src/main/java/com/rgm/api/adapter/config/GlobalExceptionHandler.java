package com.rgm.api.adapter.config;

import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
  }
}
