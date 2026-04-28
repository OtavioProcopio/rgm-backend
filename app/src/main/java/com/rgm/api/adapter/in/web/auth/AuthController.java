package com.rgm.api.adapter.in.web.auth;

import com.rgm.api.adapter.in.web.dto.request.LoginRequest;
import com.rgm.api.adapter.in.web.dto.request.RefreshTokenRequest;
import com.rgm.api.adapter.in.web.dto.response.ErrorResponse;
import com.rgm.api.adapter.in.web.dto.response.LoginResponse;
import com.rgm.api.adapter.in.web.dto.response.RefreshTokenResponse;
import com.rgm.api.core.application.usecases.auth.LoginUseCase;
import com.rgm.api.core.application.usecases.auth.RefreshTokenUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final LoginUseCase loginUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  public AuthController(
      final LoginUseCase loginUseCase, final RefreshTokenUseCase refreshTokenUseCase) {
    this.loginUseCase = loginUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody final LoginRequest request) {
    log.info("AuthController.login iniciado");
    try {
      final LoginUseCase.Output output =
          loginUseCase.execute(new LoginUseCase.Input(request.email(), request.senha()));
      return ResponseEntity.ok(
          new LoginResponse(output.token(), output.refreshToken(), output.nome(), output.perfil()));
    } catch (final NaoAutorizadoException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@Valid @RequestBody final RefreshTokenRequest request) {
    log.info("AuthController.refresh iniciado");
    try {
      final RefreshTokenUseCase.Output output =
          refreshTokenUseCase.execute(new RefreshTokenUseCase.Input(request.refreshToken()));
      return ResponseEntity.ok(new RefreshTokenResponse(output.token(), output.refreshToken()));
    } catch (final NaoAutorizadoException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }
  }
}
