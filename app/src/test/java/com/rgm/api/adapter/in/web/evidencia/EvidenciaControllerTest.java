package com.rgm.api.adapter.in.web.evidencia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.evidencia.AnexarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.ExcluirEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.VisualizarEvidenciaUseCase;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
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
  @MockitoBean private ExcluirEvidenciaUseCase excluirUseCase;

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

  @Test
  void anexarEvidencia() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Evidencia ev =
        new Evidencia(
            UUID.randomUUID(),
            "http://minio/foto.jpg",
            "image/jpeg",
            "foto.jpg",
            1024,
            userId,
            agora);
    when(anexarUseCase.upload(any())).thenReturn("http://minio/foto.jpg");
    when(anexarUseCase.persist(any(), any())).thenReturn(ev);

    final org.springframework.mock.web.MockMultipartFile arquivo =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", "fake-content".getBytes());

    mockMvc
        .perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(arquivo)
                .with(user(userId.toString())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nomeArquivo").value("foto.jpg"));
  }

  @Test
  void anexarUsaDefaultsQuandoMetadadosAusentes() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Evidencia ev =
        new Evidencia(
            UUID.randomUUID(),
            "http://minio/unknown",
            "application/octet-stream",
            "unknown",
            12,
            userId,
            Instant.now());
    when(anexarUseCase.upload(any())).thenReturn("http://minio/unknown");
    when(anexarUseCase.persist(any(), any())).thenReturn(ev);

    final org.springframework.mock.web.MockMultipartFile arquivo =
        new org.springframework.mock.web.MockMultipartFile(
            "file", null, null, "fake-content".getBytes());

    mockMvc
        .perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(arquivo)
                .with(user(userId.toString())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nomeArquivo").value("unknown"));
  }

  @Test
  void excluirEvidencia() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    doNothing().when(excluirUseCase).execute(any());

    mockMvc
        .perform(
            delete("/api/solicitacoes/{solId}/evidencias/{evId}", solId, evId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isNoContent());

    verify(excluirUseCase).execute(any());
  }

  @Test
  void excluirEvidenciaNaoAutorizadoRetorna403() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    doThrow(new NaoAutorizadoException("sem permissao"))
        .when(excluirUseCase)
        .execute(any());

    mockMvc
        .perform(
            delete("/api/solicitacoes/{solId}/evidencias/{evId}", solId, evId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isForbidden());
  }

  @Test
  void excluirEvidenciaTerminalRetorna422() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    doThrow(new BusinessRuleException("solicitacao encerrada"))
        .when(excluirUseCase)
        .execute(any());

    mockMvc
        .perform(
            delete("/api/solicitacoes/{solId}/evidencias/{evId}", solId, evId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void listarSolicitacaoInexistenteRetorna404() throws Exception {
    final UUID solId = UUID.randomUUID();
    when(visualizarUseCase.execute(any()))
        .thenThrow(new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    mockMvc
        .perform(
            get("/api/solicitacoes/" + solId + "/evidencias")
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isNotFound());
  }
}
