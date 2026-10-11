package com.rgm.api.adapter.in.web.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.LoginRequest;
import com.rgm.api.adapter.in.web.dto.request.RefreshTokenRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.LoginUseCase;
import com.rgm.api.core.application.usecases.auth.RefreshTokenUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private LoginUseCase loginUseCase;
  @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;

  @Test
  void loginComSucesso() throws Exception {
    when(loginUseCase.execute(any()))
        .thenReturn(
            new LoginUseCase.Output(
                UUID.randomUUID(), "token-123", "refresh-456", "Joao", "OPERADOR"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("j@t.com", "senha"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-123"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-456"))
        .andExpect(jsonPath("$.nome").value("Joao"))
        .andExpect(jsonPath("$.perfil").value("OPERADOR"));
  }

  @Test
  void loginComCredenciaisInvalidas() throws Exception {
    when(loginUseCase.execute(any()))
        .thenThrow(new NaoAutorizadoException("Credenciais invalidas"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("j@t.com", "errada"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshComSucesso() throws Exception {
    when(refreshTokenUseCase.execute(any()))
        .thenReturn(new RefreshTokenUseCase.Output("new-access", "new-refresh"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old-refresh"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new-access"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
  }

  @Test
  void refreshComTokenInvalido() throws Exception {
    when(refreshTokenUseCase.execute(any()))
        .thenThrow(new NaoAutorizadoException("Refresh token invalido"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest("bad"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturnUserIdWhenLoginSucceeds() throws Exception {
    // Arrange
    final UUID usuarioId = UUID.fromString("7b0f6c1e-2d3a-4b5c-8d9e-0f1a2b3c4d5e");
    final LoginUseCase.Input entrada = new LoginUseCase.Input("joao@rgm.test", "senha-correta");
    when(loginUseCase.execute(entrada))
        .thenReturn(
            new LoginUseCase.Output(usuarioId, "token-123", "refresh-456", "Joao", "OPERADOR"));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("joao@rgm.test", "senha-correta"))));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("7b0f6c1e-2d3a-4b5c-8d9e-0f1a2b3c4d5e"));
    verify(loginUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(loginUseCase, refreshTokenUseCase);
  }

  @Test
  void shouldNotReturnUserIdWhenCredentialsAreInvalid() throws Exception {
    // Arrange
    final LoginUseCase.Input entrada = new LoginUseCase.Input("joao@rgm.test", "senha-errada");
    when(loginUseCase.execute(entrada))
        .thenThrow(new NaoAutorizadoException("Credenciais invalidas"));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("joao@rgm.test", "senha-errada"))));

    // Assert
    resposta.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.id").doesNotExist());
    verify(loginUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(loginUseCase, refreshTokenUseCase);
  }
}
