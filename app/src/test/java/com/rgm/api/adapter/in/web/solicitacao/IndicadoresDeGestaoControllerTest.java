package com.rgm.api.adapter.in.web.solicitacao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.config.RateLimitFilter;
import com.rgm.api.adapter.config.SecurityConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Backlog;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Carga;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Devolucao;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Etapa;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Etapas;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Prazo;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Resolucao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = IndicadoresDeGestaoController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class IndicadoresDeGestaoControllerTest {

  private static final String ROTA = "/api/solicitacoes/metricas/gestao";
  private static final String TOKEN_OPERADOR = "token-operador";
  private static final String TOKEN_GESTOR = "token-gestor";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ObterIndicadoresDeGestaoUseCase obterUseCase;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;
  @MockitoBean private UsuarioRepository usuarioRepository;

  private void autenticarComo(final String token, final PerfilUsuario perfil) {
    final var usuario =
        new Usuario(
            UUID.randomUUID(),
            "Usuario",
            "usuario@rgm.com",
            "senhaHash",
            perfil,
            true,
            Instant.now(),
            Instant.now(),
            1);
    when(autenticarAcessoUseCase.execute(token)).thenReturn(usuario);
  }

  private MockHttpServletRequestBuilder consulta(final String caminho, final String token) {
    return get(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private void stubar() {
    final Etapa vazia = new Etapa(0, null, null);
    when(obterUseCase.execute(any()))
        .thenReturn(
            new ObterIndicadoresDeGestaoUseCase.Output(
                30,
                Instant.parse("2026-09-12T03:00:00Z"),
                Instant.parse("2026-10-12T03:00:00Z"),
                new Prazo(4, 3, 75.0, 1, List.of()),
                new Resolucao(4, 5.5 * 3600, 9.1 * 3600),
                new Backlog(3, 1, 1, 1, 2),
                new Etapas(vazia, vazia, vazia),
                new Devolucao(2, 1, 50.0),
                List.of(),
                List.of(),
                new Carga(0, List.of())));
  }

  @Test
  void shouldRespond200WithTheIndicatorsWhenManagerAsks() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubar();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA + "?dias=30", TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias").value(30))
        .andExpect(jsonPath("$.prazo.percentualNoPrazo").value(75.0))
        .andExpect(jsonPath("$.prazo.concluidasSemPrioridade").value(1))
        .andExpect(jsonPath("$.backlog.semResponsavel").value(2))
        .andExpect(jsonPath("$.devolucao.taxa").value(50.0))
        .andExpect(jsonPath("$.etapas.execucao.amostras").value(0))
        .andExpect(jsonPath("$.etapas.execucao.medianaSegundos").doesNotExist());
    verify(obterUseCase, times(1)).execute(new ObterIndicadoresDeGestaoUseCase.Input(30));
  }

  @Test
  void shouldDefaultToThirtyDaysWhenPeriodIsMissing() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubar();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterUseCase, times(1)).execute(new ObterIndicadoresDeGestaoUseCase.Input(30));
  }

  @Test
  void shouldRespond400NamingAcceptedValuesWhenPeriodIsInvalid() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA + "?dias=15", TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("7, 30, 90")));
    verifyNoInteractions(obterUseCase);
  }

  @Test
  void shouldRespond403WhenOperatorAsks() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterUseCase);
  }

  @Test
  void shouldRespond401WhenThereIsNoToken() throws Exception {
    // Arrange
    final var requisicao = get(ROTA);

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(obterUseCase);
  }
}
