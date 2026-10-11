package com.rgm.api.adapter.in.web.tipomodelo;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.tipomodelo.ListarTiposDeModeloUseCase;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
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
    controllers = TipoDeModeloController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class TipoDeModeloControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ListarTiposDeModeloUseCase listarTiposDeModeloUseCase;

  @Test
  void listar() throws Exception {
    final Instant agora = Instant.now();
    final TipoDeModelo m = new TipoDeModelo(UUID.randomUUID(), "VICK", true, agora, agora);
    when(listarTiposDeModeloUseCase.execute()).thenReturn(List.of(m));

    mockMvc
        .perform(get("/api/tipos-modelo").with(user("u")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("VICK"))
        .andExpect(jsonPath("$[0].ativo").value(true));
  }
}
