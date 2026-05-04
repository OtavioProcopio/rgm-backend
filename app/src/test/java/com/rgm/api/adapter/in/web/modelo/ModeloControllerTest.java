package com.rgm.api.adapter.in.web.modelo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.modelo.AtualizarFotoCapaUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ModeloController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class ModeloControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private GerenciarModelosUseCase gerenciarUseCase;
  @MockitoBean private AtualizarFotoCapaUseCase fotoCapaUseCase;
  @MockitoBean private ListarModelosUseCase listarUseCase;
  @MockitoBean private ModeloRepository modeloRepository;
  @MockitoBean private EventoModeloRepository eventoModeloRepository;

  private Modelo criarModelo() {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        "MOD-001",
        1,
        "Desc",
        "Obs",
        null,
        null,
        null,
        null,
        true,
        UUID.randomUUID(),
        false,
        agora,
        agora);
  }

  @Test
  void listarModelos() throws Exception {
    final Modelo modelo = criarModelo();
    when(listarUseCase.execute(anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(modelo), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/modelos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigo").value("MOD-001"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void deveCriarModelo() throws Exception {
    final Modelo modelo = criarModelo();
    when(gerenciarUseCase.criar(any())).thenReturn(modelo);

    mockMvc
        .perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CriarModeloRequest("MOD-001", "Desc", "Obs", UUID.randomUUID()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }
}
