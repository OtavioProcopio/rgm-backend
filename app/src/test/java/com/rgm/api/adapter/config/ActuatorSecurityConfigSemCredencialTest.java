package com.rgm.api.adapter.config;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
    controllers = ActuatorSecurityConfigSemCredencialTest.ControllerDeTeste.class,
    properties = {"monitoramento.coleta.usuario=coletor", "monitoramento.coleta.senha="})
@Import({
  ActuatorSecurityConfig.class,
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  ActuatorSecurityConfigSemCredencialTest.ControllerDeTeste.class
})
@ActiveProfiles("test")
class ActuatorSecurityConfigSemCredencialTest {

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
  void shouldResponder401WhenSenhaDeColetaNaoEstaConfiguradaEOClienteEnviaSenhaVazia()
      throws Exception {
    // Arrange
    final var requisicao = get("/actuator/prometheus").with(httpBasic("coletor", ""));

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
  }

  @Test
  void shouldResponder200WhenSaudeEConsultadaSemSenhaDeColetaConfigurada() throws Exception {
    // Arrange
    final var requisicao = get("/actuator/health");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk()).andExpect(content().string(containsString("UP")));
  }
}
