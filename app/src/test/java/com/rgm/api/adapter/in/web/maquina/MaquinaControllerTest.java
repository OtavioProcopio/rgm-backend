package com.rgm.api.adapter.in.web.maquina;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.maquina.ListarMaquinasUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
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

  @Test
  void listar() throws Exception {
    final Instant agora = Instant.now();
    final Maquina m = new Maquina(UUID.randomUUID(), "VICK", true, agora, agora);
    when(listarMaquinasUseCase.execute()).thenReturn(List.of(m));

    mockMvc
        .perform(get("/api/maquinas").with(user("u")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("VICK"))
        .andExpect(jsonPath("$[0].ativo").value(true));
  }
}
