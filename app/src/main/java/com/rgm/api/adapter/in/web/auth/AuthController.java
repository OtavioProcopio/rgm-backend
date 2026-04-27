package com.rgm.api.adapter.in.web.auth;

import com.rgm.api.adapter.in.web.dto.request.LoginRequest;
import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.adapter.in.web.dto.response.LoginResponse;
import com.rgm.api.core.application.usecases.auth.LoginUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;

  public AuthController(final LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody final LoginRequest request) {
    try {
      final LoginUseCase.Output output =
          loginUseCase.execute(new LoginUseCase.Input(request.email(), request.senha()));
      return ResponseEntity.ok(new LoginResponse(output.token(), output.nome(), output.perfil()));
    } catch (final NaoAutorizadoException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }
  }
}
