package com.rgm.api.adapter.in.web.auth;

import com.rgm.api.adapter.in.web.dto.request.LoginRequest;
import com.rgm.api.adapter.in.web.dto.response.LoginResponse;
import com.rgm.api.core.application.usecases.auth.LoginUseCase;
import jakarta.validation.Valid;
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
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
    final LoginUseCase.Output output =
        loginUseCase.execute(new LoginUseCase.Input(request.email(), request.senha()));
    return ResponseEntity.ok(new LoginResponse(output.token(), output.nome(), output.perfil()));
  }
}
