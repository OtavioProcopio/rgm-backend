package com.rgm.api.adapter.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
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

@WebMvcTest(
    controllers = ActuatorSecurityConfigTest.ControllerDeTeste.class,
    properties = {
      "monitoramento.coleta.usuario=coletor",
      "monitoramento.coleta.senha=segredo-de-teste"
    })
@Import({
  ActuatorSecurityConfig.class,
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  ActuatorSecurityConfigTest.ControllerDeTeste.class
})
@ActiveProfiles("test")
class ActuatorSecurityConfigTest {

  private static final String USUARIO = "coletor";
  private static final String SENHA = "segredo-de-teste";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  @RestController
  static class ControllerDeTeste {

    @GetMapping("/actuator/health")
    String saude() {
      return "{\"status\":\"UP\"}";
    }

    @GetMapping("/actuator/prometheus")
    String metricas() {
      return "metricas";
    }

    @GetMapping("/actuator")
    String indice() {
      return "{}";
    }
  }

  @Test
  void shouldResponder200WhenSaudeEConsultadaSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/health");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk()).andExpect(content().string(containsString("UP")));
  }

  @Test
  void shouldResponder401WhenMetricasSaoConsultadasSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponder200ComMetricasWhenCredencialDeColetaEValida() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus").with(httpBasic(USUARIO, SENHA));

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk()).andExpect(content().string(containsString("metricas")));
  }

  @Test
  void shouldResponder401WhenSenhaDeColetaEstaErrada() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus").with(httpBasic(USUARIO, "senha-errada"));

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponder401WhenUsuarioDeColetaEstaErrado() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus").with(httpBasic("outro", SENHA));

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponder401WhenIndiceDoActuatorEConsultadoSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/actuator");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRecusarMetricasWhenJwtDeAdministradorChegaSemCredencialDeColeta() throws Exception {
    // Arrange
    final var requisicao =
        get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, "Bearer token-admin");

    // Act
    final var resposta = mockMvc.perform(requisicao).andReturn().getResponse();

    // Assert
    assertEquals(401, resposta.getStatus());
    verifyNoInteractions(autenticarAcessoUseCase);
    assertThat(resposta.getContentAsString(), not(containsString("metricas")));
  }
}
