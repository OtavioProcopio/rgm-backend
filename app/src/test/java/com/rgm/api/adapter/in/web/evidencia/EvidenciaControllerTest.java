package com.rgm.api.adapter.in.web.evidencia;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.solicitacao.SolicitacaoAtividadeEvent;
import com.rgm.api.adapter.in.web.solicitacao.SolicitacaoEventPublisher;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.evidencia.AnexarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.ExcluirEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.VisualizarEvidenciaUseCase;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
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
  @MockitoBean private SolicitacaoEventPublisher eventPublisher;

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
            agora,
            TipoEvidencia.GERAL,
            null);

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
            agora,
            TipoEvidencia.GERAL,
            null);
    when(anexarUseCase.upload(any()))
        .thenReturn(new AnexarEvidenciaUseCase.UploadResult("http://minio/foto.jpg", null, null));
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
            Instant.now(),
            TipoEvidencia.GERAL,
            null);
    when(anexarUseCase.upload(any()))
        .thenReturn(new AnexarEvidenciaUseCase.UploadResult("http://minio/unknown", null, null));
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
    doThrow(new NaoAutorizadoException("sem permissao")).when(excluirUseCase).execute(any());

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
    doThrow(new BusinessRuleException("solicitacao encerrada")).when(excluirUseCase).execute(any());

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

  @Test
  void shouldPublishEvidenciaAdicionadaActivityEventWhenEvidenceIsAttached() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Evidencia evidencia =
        new Evidencia(
            UUID.randomUUID(),
            "http://storage/foto.jpg",
            "image/jpeg",
            "foto.jpg",
            1024,
            userId,
            Instant.now(),
            TipoEvidencia.GERAL,
            null);
    final AnexarEvidenciaUseCase.UploadResult upload =
        new AnexarEvidenciaUseCase.UploadResult("http://storage/foto.jpg", null, null);
    when(anexarUseCase.upload(any())).thenReturn(upload);
    when(anexarUseCase.persist(any(), any())).thenReturn(evidencia);
    final org.springframework.mock.web.MockMultipartFile arquivo =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(arquivo)
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(anexarUseCase, times(1)).upload(any());
    verify(anexarUseCase, times(1)).persist(any(), any());
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao_atividade", new SolicitacaoAtividadeEvent("evidencia_adicionada", solId));
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  @Test
  void shouldNotPublishEventWhenAttachmentIsDenied() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(anexarUseCase.upload(any()))
        .thenThrow(new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao"));
    final org.springframework.mock.web.MockMultipartFile arquivo =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(arquivo)
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isForbidden());
    verify(anexarUseCase, times(1)).upload(any());
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  private static org.springframework.mock.web.MockMultipartFile foto() {
    return new org.springframework.mock.web.MockMultipartFile(
        "file", "foto.jpg", "image/jpeg", "conteudo".getBytes());
  }

  private static Evidencia evidenciaAnexadaPor(final UUID userId) {
    return new Evidencia(
        UUID.randomUUID(),
        "http://storage/foto.jpg",
        "image/jpeg",
        "foto.jpg",
        1024,
        userId,
        Instant.now(),
        TipoEvidencia.SERVICO_REALIZADO,
        "Peca trocada");
  }

  @Test
  void shouldPassParsedTipoToUseCaseWhenTipoIsInformed() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final org.mockito.ArgumentCaptor<AnexarEvidenciaUseCase.Input> entrada =
        org.mockito.ArgumentCaptor.forClass(AnexarEvidenciaUseCase.Input.class);
    when(anexarUseCase.upload(any()))
        .thenReturn(new AnexarEvidenciaUseCase.UploadResult("http://storage/foto.jpg", null, null));
    when(anexarUseCase.persist(any(), any())).thenReturn(evidenciaAnexadaPor(userId));

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(foto())
                .param("tipo", "SERVICO_REALIZADO")
                .param("descricao", "Peca trocada")
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(anexarUseCase, times(1)).upload(entrada.capture());
    assertEquals(TipoEvidencia.SERVICO_REALIZADO, entrada.getValue().tipo());
    verify(anexarUseCase, times(1)).persist(any(), any());
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao_atividade", new SolicitacaoAtividadeEvent("evidencia_adicionada", solId));
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  @Test
  void shouldRejectAttachmentWhenTipoIsInvalid() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(foto())
                .param("tipo", "TIPO_INEXISTENTE")
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isBadRequest());
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  @Test
  void shouldOfferFileContentForGalleryWhenUseCaseAsksForIt() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final byte[][] conteudoParaGaleria = new byte[1][];
    when(anexarUseCase.upload(any()))
        .thenAnswer(
            chamada -> {
              final AnexarEvidenciaUseCase.Input entrada = chamada.getArgument(0);
              conteudoParaGaleria[0] = entrada.conteudoParaGaleria().get().readAllBytes();
              return new AnexarEvidenciaUseCase.UploadResult("http://storage/foto.jpg", null, null);
            });
    when(anexarUseCase.persist(any(), any())).thenReturn(evidenciaAnexadaPor(userId));

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(foto())
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isCreated());
    assertArrayEquals("conteudo".getBytes(), conteudoParaGaleria[0]);
    verify(anexarUseCase, times(1)).upload(any());
    verify(anexarUseCase, times(1)).persist(any(), any());
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao_atividade", new SolicitacaoAtividadeEvent("evidencia_adicionada", solId));
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  @Test
  void shouldFailWithoutPublishingWhenUploadedFileCannotBeRead() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final org.springframework.mock.web.MockMultipartFile ilegivel =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", "conteudo".getBytes()) {
          @Override
          public java.io.InputStream getInputStream() throws java.io.IOException {
            throw new java.io.IOException("disco indisponivel");
          }
        };

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(ilegivel)
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isInternalServerError());
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }

  @Test
  void shouldFailWithoutPublishingWhenGalleryContentCannotBeRead() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final java.util.concurrent.atomic.AtomicInteger leituras =
        new java.util.concurrent.atomic.AtomicInteger();
    final org.springframework.mock.web.MockMultipartFile legivelUmaVez =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", "conteudo".getBytes()) {
          @Override
          public java.io.InputStream getInputStream() throws java.io.IOException {
            if (leituras.incrementAndGet() > 1) {
              throw new java.io.IOException("disco indisponivel");
            }
            return super.getInputStream();
          }
        };
    when(anexarUseCase.upload(any()))
        .thenAnswer(
            chamada -> {
              final AnexarEvidenciaUseCase.Input entrada = chamada.getArgument(0);
              return entrada.conteudoParaGaleria().get();
            });

    // Act
    final var resposta =
        mockMvc.perform(
            multipart("/api/solicitacoes/{solId}/evidencias", solId)
                .file(legivelUmaVez)
                .with(user(userId.toString())));

    // Assert
    resposta.andExpect(status().isInternalServerError());
    verify(anexarUseCase, times(1)).upload(any());
    verifyNoMoreInteractions(anexarUseCase, eventPublisher);
  }
}
