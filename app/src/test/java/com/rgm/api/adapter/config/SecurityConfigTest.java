package com.rgm.api.adapter.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.ControllerDeTeste.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  SecurityConfigTest.ControllerDeTeste.class
})
@ActiveProfiles("test")
class SecurityConfigTest {

  private static final String JSON = "application/json";
  private static final String ROTA_DO_PAINEL = "/api/solicitacoes/metricas/painel";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  @RestController
  static class ControllerDeTeste {

    @GetMapping("/api/admin/usuarios")
    String usuarios() {
      return "{}";
    }

    @GetMapping("/api/solicitacoes")
    String solicitacoes() {
      return "{}";
    }

    @GetMapping("/api/solicitacoes/metricas")
    String metricas() {
      return "{}";
    }

    @GetMapping("/api/solicitacoes/metricas/painel")
    String painel() {
      return "{}";
    }

    @GetMapping("/api/auth/ping")
    String ping() {
      return "{}";
    }

    @GetMapping("/actuator/prometheus")
    String prometheus() {
      return "{}";
    }
  }

  private static Usuario usuarioComPerfil(final PerfilUsuario perfil) {
    return new Usuario(
        UUID.randomUUID(),
        "Usuario",
        "usuario@rgm.com",
        "senhaHash",
        perfil,
        true,
        Instant.now(),
        Instant.now(),
        1);
  }

  private void autenticarComo(final String token, final PerfilUsuario perfil) {
    when(autenticarAcessoUseCase.execute(token)).thenReturn(usuarioComPerfil(perfil));
  }

  @Test
  void shouldResponder401ComCorpoJsonWhenRequisicaoChegaSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/api/solicitacoes");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Não autenticado"));
  }

  @Test
  void shouldResponder403ComCorpoJsonWhenOperadorAcessaAreaDeAdministracao() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message", not(emptyString())));
  }

  @Test
  void shouldResponder200WhenGestorLeUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-gestor", PerfilUsuario.GESTOR);
    final var requisicao =
        get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer token-gestor");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
  }

  @Test
  void shouldResponder200WhenAdministradorLeUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-admin", PerfilUsuario.ADMINISTRADOR);
    final var requisicao =
        get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer token-admin");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
  }

  @Test
  void shouldResponder403ComCorpoJsonWhenOperadorConsultaOPainel() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        get(ROTA_DO_PAINEL).header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(content().contentTypeCompatibleWith(JSON));
  }

  @Test
  void shouldResponder403WhenOperadorConsultaOPainelPorHead() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        head(ROTA_DO_PAINEL).header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isForbidden());
  }

  @Test
  void shouldResponder401WhenPainelEConsultadoPorHeadSemCredencial() throws Exception {
    // Arrange
    final var requisicao = head(ROTA_DO_PAINEL);

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponder200WhenGestorConsultaOPainel() throws Exception {
    // Arrange
    autenticarComo("token-gestor", PerfilUsuario.GESTOR);
    final var requisicao =
        get(ROTA_DO_PAINEL).header(HttpHeaders.AUTHORIZATION, "Bearer token-gestor");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
  }

  @Test
  void shouldResponder200WhenAdministradorConsultaOPainelPorHead() throws Exception {
    // Arrange
    autenticarComo("token-admin", PerfilUsuario.ADMINISTRADOR);
    final var requisicao =
        head(ROTA_DO_PAINEL).header(HttpHeaders.AUTHORIZATION, "Bearer token-admin");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
  }

  @Test
  void shouldResponder200WhenRotaDeAutenticacaoEConsultadaSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/api/auth/ping");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
  }

  @Test
  void shouldResponder401WhenActuatorEConsultadoSemCredencialNaCadeiaPrincipal() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponderJsonWhenRespostaE401() throws Exception {
    // Arrange
    final var requisicao = get("/api/solicitacoes");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(JSON))
        .andExpect(content().string(containsString("\"status\":401")));
  }

  @Test
  void shouldResponderJsonWhenRespostaE403() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(JSON))
        .andExpect(content().string(containsString("\"status\":403")));
  }
}
