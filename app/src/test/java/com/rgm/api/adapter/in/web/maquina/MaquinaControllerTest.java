package com.rgm.api.adapter.in.web.maquina;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.ListarMaquinasUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = MaquinaController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class MaquinaControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ListarMaquinasUseCase listarMaquinasUseCase;

  private Maquina criarMaquina() {
    final Instant agora = Instant.now();
    return new Maquina(
        UUID.randomUUID(), "Torno CNC", "MQ-001", "Torno de precisão", true, agora, agora);
  }

  @Test
  void deveListarMaquinasComPaginacaoPadrao() throws Exception {
    final Maquina maquina = criarMaquina();
    when(listarMaquinasUseCase.execute(anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(maquina), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/maquinas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigo").value("MQ-001"))
        .andExpect(jsonPath("$.content[0].nome").value("Torno CNC"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.page").value(0));
  }

  @Test
  void deveListarMaquinasComPaginacaoCustomizada() throws Exception {
    when(listarMaquinasUseCase.execute(2, 5)).thenReturn(new PageResult<>(List.of(), 2, 5, 0, 0));

    mockMvc
        .perform(get("/api/maquinas").param("page", "2").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.page").value(2));
  }

  @Test
  void deveRetornarListaVaziaQuandoNaoHaMaquinas() throws Exception {
    when(listarMaquinasUseCase.execute(anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(get("/api/maquinas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.content").isEmpty());
  }
}
