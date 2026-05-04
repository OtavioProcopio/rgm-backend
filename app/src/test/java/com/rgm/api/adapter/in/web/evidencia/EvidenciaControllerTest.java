package com.rgm.api.adapter.in.web.evidencia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.evidencia.AnexarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.VisualizarEvidenciaUseCase;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
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
    controllers = EvidenciaController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class EvidenciaControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AnexarEvidenciaUseCase anexarUseCase;
  @MockitoBean private VisualizarEvidenciaUseCase visualizarUseCase;

  @Test
  void visualizarEvidencias() throws Exception {
    final UUID solId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Evidencia ev =
        new Evidencia(
            UUID.randomUUID(),
            "http://minio/foto.jpg",
            "image/jpeg",
            "foto.jpg",
            1024,
            UUID.randomUUID(),
            agora);

    when(visualizarUseCase.execute(any())).thenReturn(List.of(ev));

    mockMvc
        .perform(
            get("/api/solicitacoes/" + solId + "/evidencias")
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nomeArquivo").value("foto.jpg"))
        .andExpect(jsonPath("$[0].publicUrl").value("http://minio/foto.jpg"));
  }
}
