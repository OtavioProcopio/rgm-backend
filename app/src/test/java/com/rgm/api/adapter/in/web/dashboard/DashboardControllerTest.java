package com.rgm.api.adapter.in.web.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.dashboard.ObterMetricasDashboardUseCase;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = DashboardController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ObterMetricasDashboardUseCase obterMetricasUseCase;

  @Test
  void deveRetornarMetricasComSucesso() throws Exception {
    final Map<StatusSolicitacao, Long> statusCounts = new EnumMap<>(StatusSolicitacao.class);
    statusCounts.put(StatusSolicitacao.CONCLUIDA, 5L);

    final var output =
        new ObterMetricasDashboardUseCase.DashboardMetricas(
            10L, 20L, 30L, 25L, 5L, statusCounts, 4.5, 2.1);

    when(obterMetricasUseCase.execute()).thenReturn(output);

    mockMvc
        .perform(get("/api/dashboard/metricas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsuarios").value(10))
        .andExpect(jsonPath("$.totalModelos").value(20))
        .andExpect(jsonPath("$.totalSolicitacoes").value(30))
        .andExpect(jsonPath("$.totalSolicitacoesAbertas").value(25))
        .andExpect(jsonPath("$.totalSolicitacoesConcluidas").value(5))
        .andExpect(jsonPath("$.tempoMedioResolucaoHoras").value(4.5))
        .andExpect(jsonPath("$.tempoMedioValidacaoHoras").value(2.1));
  }
}
