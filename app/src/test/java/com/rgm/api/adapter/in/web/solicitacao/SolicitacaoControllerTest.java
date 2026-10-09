package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.AbrirSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.CancelarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.ComentarioRequest;
import com.rgm.api.adapter.in.web.dto.request.DevolverSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.EncerrarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.GerenciarResponsaveisRequest;
import com.rgm.api.adapter.in.web.dto.request.TriarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.response.SolicitacaoResponse;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.CancelarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EditarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.GerenciarResponsaveisUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarAtividadesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.validation.LimitesTexto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(
    controllers = SolicitacaoController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class SolicitacaoControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private AbrirSolicitacaoUseCase abrirUseCase;
  @MockitoBean private TriarSolicitacaoUseCase triarUseCase;
  @MockitoBean private EnviarParaValidacaoUseCase enviarUseCase;
  @MockitoBean private DevolverSolicitacaoUseCase devolverUseCase;
  @MockitoBean private EncerrarSolicitacaoUseCase encerrarUseCase;
  @MockitoBean private CancelarSolicitacaoUseCase cancelarUseCase;
  @MockitoBean private RegistrarComentarioUseCase comentarioUseCase;
  @MockitoBean private EditarSolicitacaoUseCase editarUseCase;
  @MockitoBean private ListarSolicitacoesUseCase listarUseCase;
  @MockitoBean private ObterMetricasSolicitacoesUseCase obterMetricasUseCase;
  @MockitoBean private GerenciarResponsaveisUseCase gerenciarResponsaveisUseCase;
  @MockitoBean private ObterSolicitacaoUseCase obterUseCase;
  @MockitoBean private ListarAtividadesUseCase listarAtividadesUseCase;
  @MockitoBean private com.rgm.api.adapter.out.report.SolicitacaoPdfService pdfService;
  @MockitoBean private com.rgm.api.adapter.out.report.ModeloPdfService modeloPdfService;

  @MockitoBean
  private com.rgm.api.core.domain.ports.repositories.UsuarioRepository usuarioRepository;

  @MockitoBean
  private com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase
      obterHistoricoMetricasUseCase;

  @MockitoBean
  private com.rgm.api.core.application.usecases.solicitacao.ObterMetricasPorModeloUseCase
      obterMetricasPorModeloUseCase;

  @MockitoBean private SolicitacaoEventPublisher eventPublisher;

  private Solicitacao criarSolicitacao() {
    return Solicitacao.abrir(
        "Titulo",
        "Desc",
        TipoSolicitacao.REPARO,
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void listarSolicitacoes() throws Exception {
    final Solicitacao sol = criarSolicitacao();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(sol), 0, 20, 1, 1));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(java.util.Map.of());

    mockMvc
        .perform(get("/api/solicitacoes").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].titulo").value("Titulo"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void listarSolicitacoesPorStatus() throws Exception {
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(java.util.Map.of());

    mockMvc
        .perform(get("/api/solicitacoes").param("status", "A_FAZER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void listarSolicitacoesPorMaquina() throws Exception {
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(java.util.Map.of());

    mockMvc
        .perform(get("/api/solicitacoes").param("maquina", "VICK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    final org.mockito.ArgumentCaptor<ListarSolicitacoesUseCase.Input> captor =
        org.mockito.ArgumentCaptor.forClass(ListarSolicitacoesUseCase.Input.class);
    verify(listarUseCase).execute(captor.capture());
    assertEquals("VICK", captor.getValue().maquina());
  }

  @Test
  void abrirSolicitacao() throws Exception {
    final Solicitacao sol = criarSolicitacao();
    when(abrirUseCase.execute(any())).thenReturn(sol);
    final UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/solicitacoes")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AbrirSolicitacaoRequest(
                            "Titulo", "Desc", "REPARO", UUID.randomUUID(), null, null, null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.titulo").value("Titulo"))
        .andExpect(jsonPath("$.status").value("A_FAZER"));
  }

  @Test
  void cancelarSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Solicitacao cancelada =
        new Solicitacao(
            solId,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CANCELADA,
            null,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            "Cancelado via API",
            agora,
            agora,
            null,
            agora);

    when(cancelarUseCase.execute(any())).thenReturn(cancelada);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/solicitacoes/{id}/cancelar", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CancelarSolicitacaoRequest("Cancelado via API"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELADA"))
        .andExpect(jsonPath("$.comentarioFinal").value("Cancelado via API"));
  }

  @Test
  void obterMetricas() throws Exception {
    when(obterMetricasUseCase.execute())
        .thenReturn(
            new ObterMetricasSolicitacoesUseCase.Output(
                10L,
                15L,
                42L,
                java.util.Map.of("A_FAZER", 42L),
                42L,
                0L,
                0L,
                120L,
                java.util.Map.of()));

    mockMvc
        .perform(get("/api/solicitacoes/metricas").with(user("admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsuarios").value(10))
        .andExpect(jsonPath("$.totalModelos").value(15))
        .andExpect(jsonPath("$.totalSolicitacoes").value(42))
        .andExpect(jsonPath("$.solicitacoesPorStatus.A_FAZER").value(42))
        .andExpect(jsonPath("$.tempoMedioResolucaoSegundos").value(120));
  }

  @Test
  void obterMetricasPorModelo() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    final var row =
        new com.rgm.api.core.domain.ports.repositories.MetricaModeloRow(
            modeloId, "MDL-001", 3600.0, 7200.0);
    final var pageResult =
        new com.rgm.api.core.domain.ports.repositories.PageResult<>(
            java.util.List.of(row), 0, 20, 1L, 1);
    when(obterMetricasPorModeloUseCase.execute(any())).thenReturn(pageResult);

    mockMvc
        .perform(
            get("/api/solicitacoes/metricas/por-modelo")
                .param("sort", "TEMPO_RESOLUCAO")
                .param("dir", "desc")
                .with(user("admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].modeloId").value(modeloId.toString()))
        .andExpect(jsonPath("$.content[0].codigo").value("MDL-001"))
        .andExpect(jsonPath("$.content[0].tempoMedioResolucaoSegundos").value(3600.0))
        .andExpect(jsonPath("$.content[0].intervaloMedioSegundos").value(7200.0));
  }

  @Test
  void exportarMetricasPorModeloPdf() throws Exception {
    final var row =
        new com.rgm.api.core.domain.ports.repositories.MetricaModeloRow(
            UUID.randomUUID(), "MDL-001", 3600.0, 7200.0);
    final var pageResult =
        new com.rgm.api.core.domain.ports.repositories.PageResult<>(
            java.util.List.of(row), 0, Integer.MAX_VALUE, 1L, 1);
    when(obterMetricasPorModeloUseCase.execute(any())).thenReturn(pageResult);
    when(modeloPdfService.gerarRankingModelos(any(), any())).thenReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(get("/api/solicitacoes/metricas/por-modelo/pdf").with(user("admin")))
        .andExpect(status().isOk());
  }

  @Test
  void obterMetricasPorModeloComOrdenacaoInvalidaRetorna400() throws Exception {
    mockMvc
        .perform(
            get("/api/solicitacoes/metricas/por-modelo")
                .param("sort", "CAMPO_INEXISTENTE")
                .with(user("admin")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obterMetricasPorModeloComDirecaoInvalidaRetorna400() throws Exception {
    mockMvc
        .perform(
            get("/api/solicitacoes/metricas/por-modelo")
                .param("dir", "lateral")
                .with(user("admin")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void buscarPorId() throws Exception {
    final UUID solId = UUID.randomUUID();
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    when(obterUseCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId)))
        .thenReturn(
            new ObterSolicitacaoUseCase.Output(
                sol, List.of(), List.of(), null, java.util.Set.of()));

    mockMvc
        .perform(get("/api/solicitacoes/{id}", solId).with(user(userId.toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Titulo"));
  }

  @Test
  void buscarPorId_naoEncontrado() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(obterUseCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId)))
        .thenThrow(new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    mockMvc
        .perform(get("/api/solicitacoes/{id}", solId).with(user(userId.toString())))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnHistoryWithAuthorNameWhenUserHasAccess() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.abertura(solId, autorId, Instant.now());
    final ListarAtividadesUseCase.Input entrada = new ListarAtividadesUseCase.Input(solId, autorId);
    when(listarAtividadesUseCase.execute(entrada))
        .thenReturn(List.of(new ListarAtividadesUseCase.AtividadeComAutor(atividade, "Alice")));

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/solicitacoes/{id}/atividades", solId).with(user(autorId.toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].autorNome").value("Alice"));
    verify(listarAtividadesUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(listarAtividadesUseCase, eventPublisher);
  }

  @Test
  void editarSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol = criarSolicitacao();
    when(editarUseCase.execute(any())).thenReturn(sol);

    mockMvc
        .perform(
            put("/api/solicitacoes/{id}", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EditarSolicitacaoRequest("Novo Titulo", "Nova Desc", "REPARO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Titulo"));
  }

  @Test
  void triarSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Solicitacao triada =
        new Solicitacao(
            solId,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);
    when(triarUseCase.execute(any())).thenReturn(triada);

    mockMvc
        .perform(
            patch("/api/solicitacoes/{id}/triar", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new TriarSolicitacaoRequest("ALTA", List.of(UUID.randomUUID())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
  }

  @Test
  void enviarParaValidacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Solicitacao emValidacao =
        new Solicitacao(
            solId,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_VALIDACAO,
            PrioridadeSolicitacao.MEDIA,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);
    when(enviarUseCase.execute(any())).thenReturn(emValidacao);

    mockMvc
        .perform(
            patch("/api/solicitacoes/{id}/enviar-validacao", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new com.rgm.api.adapter.in.web.dto.request.EnviarParaValidacaoRequest(
                            "Serviço de reparo concluído com sucesso"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_VALIDACAO"));
  }

  @Test
  void devolverSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Solicitacao devolvida =
        new Solicitacao(
            solId,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);
    when(devolverUseCase.execute(any())).thenReturn(devolvida);

    mockMvc
        .perform(
            patch("/api/solicitacoes/{id}/devolver", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new DevolverSolicitacaoRequest("Motivo", "ALTA"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
  }

  @Test
  void encerrarSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Solicitacao concluida =
        new Solicitacao(
            solId,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.MEDIA,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            "Encerrado OK",
            agora,
            agora,
            agora,
            null);
    when(encerrarUseCase.execute(any())).thenReturn(concluida);

    mockMvc
        .perform(
            patch("/api/solicitacoes/{id}/encerrar", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EncerrarSolicitacaoRequest(true, "Encerrado OK"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONCLUIDA"));
  }

  @Test
  void comentarSolicitacao() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(comentarioUseCase.execute(any())).thenReturn(null);

    mockMvc
        .perform(
            post("/api/solicitacoes/{id}/comentarios", solId)
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ComentarioRequest("Ótimo trabalho"))))
        .andExpect(status().isCreated());
  }

  @Test
  void gerarRelatorio() throws Exception {
    final Solicitacao sol = criarSolicitacao();
    when(listarUseCase.execute(any()))
        .thenReturn(new PageResult<>(List.of(sol), 0, Integer.MAX_VALUE, 1, 1));
    when(pdfService.gerar(any(), any(), any()))
        .thenReturn(new byte[] {37, 80, 68, 70}); // %PDF magic bytes
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(java.util.List.of());
    when(usuarioRepository.findById(any())).thenReturn(java.util.Optional.empty());

    mockMvc.perform(get("/api/solicitacoes/relatorio").with(user("u"))).andExpect(status().isOk());
  }

  @Test
  void shouldLevarEmAbertoAoCasoDeUsoWhenListagemRecebeOFiltro() throws Exception {
    // Arrange
    final var captor = org.mockito.ArgumentCaptor.forClass(ListarSolicitacoesUseCase.Input.class);
    when(listarUseCase.execute(captor.capture()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(java.util.Map.of());

    // Act
    final var resposta = mockMvc.perform(get("/api/solicitacoes").param("emAberto", "true"));

    // Assert
    resposta.andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, captor.getValue().emAberto());
  }

  @Test
  void shouldDeixarEmAbertoVazioWhenListagemNaoRecebeOFiltro() throws Exception {
    // Arrange
    final var captor = org.mockito.ArgumentCaptor.forClass(ListarSolicitacoesUseCase.Input.class);
    when(listarUseCase.execute(captor.capture()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(java.util.Map.of());

    // Act
    final var resposta = mockMvc.perform(get("/api/solicitacoes"));

    // Assert
    resposta.andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertNull(captor.getValue().emAberto());
  }

  @Test
  void shouldLevarEmAbertoAoCasoDeUsoWhenRelatorioRecebeOFiltro() throws Exception {
    // Arrange
    final var captor = org.mockito.ArgumentCaptor.forClass(ListarSolicitacoesUseCase.Input.class);
    when(listarUseCase.execute(captor.capture()))
        .thenReturn(new PageResult<>(List.of(), 0, Integer.MAX_VALUE, 0, 0));
    when(pdfService.gerar(any(), any(), any())).thenReturn(new byte[] {37, 80, 68, 70});
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(java.util.List.of());
    when(usuarioRepository.findById(any())).thenReturn(java.util.Optional.empty());

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/solicitacoes/relatorio").param("emAberto", "true").with(user("u")));

    // Assert
    resposta.andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, captor.getValue().emAberto());
  }

  @Test
  void shouldPublishAbertaEventWhenSolicitacaoIsOpened() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final AbrirSolicitacaoUseCase.Input entrada =
        new AbrirSolicitacaoUseCase.Input(
            "Titulo", "Desc", TipoSolicitacao.REPARO, modeloId, null, null, null, userId);
    when(abrirUseCase.execute(entrada)).thenReturn(sol);

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/solicitacoes")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AbrirSolicitacaoRequest(
                            "Titulo", "Desc", "REPARO", modeloId, null, null, null))));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(abrirUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent("aberta", SolicitacaoResponse.from(sol)),
            sol.getId(),
            List.of());
    verifyNoMoreInteractions(abrirUseCase, eventPublisher);
  }

  @Test
  void shouldPublishEditadaEventWhenSolicitacaoIsEdited() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final EditarSolicitacaoUseCase.Input entrada =
        new EditarSolicitacaoUseCase.Input(
            sol.getId(), "Novo Titulo", "Nova Desc", TipoSolicitacao.REPARO, userId);
    when(editarUseCase.execute(entrada)).thenReturn(sol);

    // Act
    final var resposta =
        mockMvc.perform(
            put("/api/solicitacoes/{id}", sol.getId())
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EditarSolicitacaoRequest("Novo Titulo", "Nova Desc", "REPARO"))));

    // Assert
    resposta.andExpect(status().isOk());
    verify(editarUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent("editada", SolicitacaoResponse.from(sol)),
            sol.getId(),
            List.of());
    verifyNoMoreInteractions(editarUseCase, eventPublisher);
  }

  @Test
  void shouldPublishResponsaveisAlteradosEventWhenAssigneesChange() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID gestorId = UUID.randomUUID();
    final List<UUID> responsaveis = List.of(UUID.randomUUID());
    final GerenciarResponsaveisUseCase.Input entrada =
        new GerenciarResponsaveisUseCase.Input(sol.getId(), responsaveis, gestorId);
    when(gerenciarResponsaveisUseCase.execute(entrada))
        .thenReturn(new GerenciarResponsaveisUseCase.Output(sol, List.of()));

    // Act
    final var resposta =
        mockMvc.perform(
            patch("/api/solicitacoes/{id}/responsaveis", sol.getId())
                .with(user(gestorId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new GerenciarResponsaveisRequest(responsaveis))));

    // Assert
    resposta.andExpect(status().isOk());
    verify(gerenciarResponsaveisUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent(
                "responsaveis_alterados", SolicitacaoResponse.from(sol, responsaveis)),
            sol.getId(),
            List.of());
    verifyNoMoreInteractions(gerenciarResponsaveisUseCase, eventPublisher);
  }

  @Test
  void shouldReturnAllowedActionsWhenSolicitacaoIsFetchedById() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final ObterSolicitacaoUseCase.Input entrada =
        new ObterSolicitacaoUseCase.Input(sol.getId(), userId);
    when(obterUseCase.execute(entrada))
        .thenReturn(
            new ObterSolicitacaoUseCase.Output(
                sol,
                List.of(),
                List.of(),
                null,
                java.util.EnumSet.of(
                    com.rgm.api.core.domain.model.enums.AcaoSolicitacao.EDITAR,
                    com.rgm.api.core.domain.model.enums.AcaoSolicitacao.COMENTAR)));

    // Act
    final var resposta =
        mockMvc.perform(get("/api/solicitacoes/{id}", sol.getId()).with(user(userId.toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acoesPermitidas.length()").value(2))
        .andExpect(jsonPath("$.acoesPermitidas[0]").value("EDITAR"))
        .andExpect(jsonPath("$.acoesPermitidas[1]").value("COMENTAR"));
    verify(obterUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(obterUseCase, eventPublisher);
  }

  @Test
  void shouldReturnNullAllowedActionsWhenSolicitacoesAreListed() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(sol), 0, 20, 1, 1));
    when(obterUseCase.listarResponsaveisBatch(List.of(sol.getId()))).thenReturn(java.util.Map.of());
    when(obterUseCase.resolverNomes(List.of(sol.getAbertaPorUsuarioId())))
        .thenReturn(java.util.Map.of());

    // Act
    final var resposta = mockMvc.perform(get("/api/solicitacoes"));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[0].acoesPermitidas").value(org.hamcrest.Matchers.nullValue()));
    verify(obterUseCase, times(1)).listarResponsaveisBatch(List.of(sol.getId()));
    verify(obterUseCase, times(1)).resolverNomes(List.of(sol.getAbertaPorUsuarioId()));
    verifyNoMoreInteractions(obterUseCase, eventPublisher);
  }

  @Test
  void shouldReturnNamesAndKeepIdsWhenSolicitacaoIsFetchedById() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final UUID brunoId = UUID.randomUUID();
    final UUID carlaId = UUID.randomUUID();
    final ObterSolicitacaoUseCase.Input entrada =
        new ObterSolicitacaoUseCase.Input(sol.getId(), userId);
    when(obterUseCase.execute(entrada))
        .thenReturn(
            new ObterSolicitacaoUseCase.Output(
                sol,
                List.of(brunoId, carlaId),
                List.of(
                    new ObterSolicitacaoUseCase.ResponsavelNome(brunoId, "Bruno"),
                    new ObterSolicitacaoUseCase.ResponsavelNome(carlaId, "Carla")),
                "Ana",
                java.util.Set.of()));

    // Act
    final var resposta =
        mockMvc.perform(get("/api/solicitacoes/{id}", sol.getId()).with(user(userId.toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsavelIds[0]").value(brunoId.toString()))
        .andExpect(jsonPath("$.responsavelIds[1]").value(carlaId.toString()))
        .andExpect(jsonPath("$.abertaPorUsuarioId").value(sol.getAbertaPorUsuarioId().toString()))
        .andExpect(jsonPath("$.responsaveis.length()").value(2))
        .andExpect(jsonPath("$.responsaveis[0].id").value(brunoId.toString()))
        .andExpect(jsonPath("$.responsaveis[0].nome").value("Bruno"))
        .andExpect(jsonPath("$.responsaveis[1].nome").value("Carla"))
        .andExpect(jsonPath("$.abertaPorNome").value("Ana"));
    verify(obterUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(obterUseCase, eventPublisher);
  }

  @Test
  void shouldReturnNamesInOneLookupWhenSolicitacoesAreListed() throws Exception {
    // Arrange
    final Solicitacao primeira = criarSolicitacao();
    final Solicitacao segunda = criarSolicitacao();
    final UUID brunoId = UUID.randomUUID();
    final UUID carlaId = UUID.randomUUID();
    when(listarUseCase.execute(any()))
        .thenReturn(new PageResult<>(List.of(primeira, segunda), 0, 20, 2, 1));
    when(obterUseCase.listarResponsaveisBatch(List.of(primeira.getId(), segunda.getId())))
        .thenReturn(java.util.Map.of(primeira.getId(), List.of(brunoId, carlaId)));
    when(obterUseCase.resolverNomes(
            List.of(
                brunoId,
                carlaId,
                primeira.getAbertaPorUsuarioId(),
                segunda.getAbertaPorUsuarioId())))
        .thenReturn(
            java.util.Map.of(
                brunoId, "Bruno", carlaId, "Carla", primeira.getAbertaPorUsuarioId(), "Ana"));

    // Act
    final var resposta = mockMvc.perform(get("/api/solicitacoes"));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].responsavelIds.length()").value(2))
        .andExpect(jsonPath("$.content[0].responsaveis[0].nome").value("Bruno"))
        .andExpect(jsonPath("$.content[0].responsaveis[1].nome").value("Carla"))
        .andExpect(jsonPath("$.content[0].abertaPorNome").value("Ana"))
        .andExpect(jsonPath("$.content[1].responsavelIds.length()").value(0))
        .andExpect(jsonPath("$.content[1].responsaveis.length()").value(0))
        .andExpect(jsonPath("$.content[1].abertaPorNome").value(org.hamcrest.Matchers.nullValue()));
    verify(obterUseCase, times(1))
        .listarResponsaveisBatch(List.of(primeira.getId(), segunda.getId()));
    verify(obterUseCase, times(1))
        .resolverNomes(
            List.of(
                brunoId,
                carlaId,
                primeira.getAbertaPorUsuarioId(),
                segunda.getAbertaPorUsuarioId()));
    verifyNoMoreInteractions(obterUseCase, eventPublisher);
  }

  @Test
  void shouldNotReturnNamesNorPublishThemWhenSolicitacaoIsOpened() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    when(abrirUseCase.execute(any())).thenReturn(sol);

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/solicitacoes")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AbrirSolicitacaoRequest(
                            "Titulo", "Desc", "REPARO", modeloId, null, null, null))));

    // Assert
    resposta
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.responsaveis").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.abertaPorNome").value(org.hamcrest.Matchers.nullValue()));
    final org.mockito.ArgumentCaptor<SolicitacaoEvent> evento =
        org.mockito.ArgumentCaptor.forClass(SolicitacaoEvent.class);
    verify(eventPublisher, times(1)).publish(eq("solicitacao"), evento.capture(), any(), any());
    assertNull(evento.getValue().solicitacao().responsaveis());
    assertNull(evento.getValue().solicitacao().abertaPorNome());
    verifyNoMoreInteractions(eventPublisher);
  }

  @Test
  void shouldReturnResultingAssigneesWhenAssigneesChange() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID gestorId = UUID.randomUUID();
    final UUID responsavelId = UUID.randomUUID();
    final GerenciarResponsaveisUseCase.Input entrada =
        new GerenciarResponsaveisUseCase.Input(sol.getId(), List.of(responsavelId), gestorId);
    when(gerenciarResponsaveisUseCase.execute(entrada))
        .thenReturn(new GerenciarResponsaveisUseCase.Output(sol, List.of()));

    // Act
    final var resposta =
        mockMvc.perform(
            patch("/api/solicitacoes/{id}/responsaveis", sol.getId())
                .with(user(gestorId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new GerenciarResponsaveisRequest(List.of(responsavelId)))));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responsavelIds.length()").value(1))
        .andExpect(jsonPath("$.responsavelIds[0]").value(responsavelId.toString()));
    verify(gerenciarResponsaveisUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent(
                "responsaveis_alterados", SolicitacaoResponse.from(sol, List.of(responsavelId))),
            sol.getId(),
            List.of());
    verifyNoMoreInteractions(gerenciarResponsaveisUseCase, eventPublisher);
  }

  @Test
  void shouldPublishComentadaActivityEventWhenCommentIsRegistered() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final RegistrarComentarioUseCase.Input entrada =
        new RegistrarComentarioUseCase.Input(solId, "Peca trocada", autorId);
    when(comentarioUseCase.execute(entrada)).thenReturn(null);

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/solicitacoes/{id}/comentarios", solId)
                .with(user(autorId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ComentarioRequest("Peca trocada"))));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(comentarioUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish("solicitacao_atividade", new SolicitacaoAtividadeEvent("comentada", solId), solId);
    verifyNoMoreInteractions(comentarioUseCase, eventPublisher);
  }

  @Test
  void shouldNotPublishEventWhenCommentIsDenied() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final RegistrarComentarioUseCase.Input entrada =
        new RegistrarComentarioUseCase.Input(solId, "Peca trocada", autorId);
    when(comentarioUseCase.execute(entrada))
        .thenThrow(new NaoAutorizadoException("Usuario nao tem permissao para comentar"));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/solicitacoes/{id}/comentarios", solId)
                .with(user(autorId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ComentarioRequest("Peca trocada"))));

    // Assert
    resposta.andExpect(status().isForbidden());
    verify(comentarioUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(comentarioUseCase, eventPublisher);
  }

  @Test
  void shouldAnswerForbiddenWhenOperatorReadsSolicitacaoOfSomeoneElse() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID operadorId = UUID.randomUUID();
    final ObterSolicitacaoUseCase.Input entrada =
        new ObterSolicitacaoUseCase.Input(solId, operadorId);
    when(obterUseCase.execute(entrada))
        .thenThrow(new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao"));

    // Act
    final var resposta =
        mockMvc.perform(get("/api/solicitacoes/{id}", solId).with(user(operadorId.toString())));

    // Assert
    resposta
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Usuario nao tem acesso a esta solicitacao"));
    verify(obterUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(obterUseCase, eventPublisher);
  }

  @Test
  void shouldAnswerForbiddenWhenOperatorReadsHistoryOfSomeoneElse() throws Exception {
    // Arrange
    final UUID solId = UUID.randomUUID();
    final UUID operadorId = UUID.randomUUID();
    final ListarAtividadesUseCase.Input entrada =
        new ListarAtividadesUseCase.Input(solId, operadorId);
    when(listarAtividadesUseCase.execute(entrada))
        .thenThrow(new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao"));

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/solicitacoes/{id}/atividades", solId).with(user(operadorId.toString())));

    // Assert
    resposta
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Usuario nao tem acesso a esta solicitacao"));
    verify(listarAtividadesUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(listarAtividadesUseCase, eventPublisher);
  }

  @Test
  void shouldTellPublisherWhoLostAccessWhenAssigneesAreRemoved() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID gestorId = UUID.randomUUID();
    final UUID removido = UUID.randomUUID();
    final List<UUID> responsaveis = List.of(UUID.randomUUID());
    final GerenciarResponsaveisUseCase.Input entrada =
        new GerenciarResponsaveisUseCase.Input(sol.getId(), responsaveis, gestorId);
    when(gerenciarResponsaveisUseCase.execute(entrada))
        .thenReturn(new GerenciarResponsaveisUseCase.Output(sol, List.of(removido)));

    // Act
    final var resposta =
        mockMvc.perform(
            patch("/api/solicitacoes/{id}/responsaveis", sol.getId())
                .with(user(gestorId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new GerenciarResponsaveisRequest(responsaveis))));

    // Assert
    resposta.andExpect(status().isOk());
    verify(gerenciarResponsaveisUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent(
                "responsaveis_alterados", SolicitacaoResponse.from(sol, responsaveis)),
            sol.getId(),
            List.of(removido));
    verifyNoMoreInteractions(gerenciarResponsaveisUseCase, eventPublisher);
  }

  private static String texto(final int tamanho) {
    return "x".repeat(tamanho);
  }

  private static Stream<Arguments> textosAcimaDoLimite() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    return Stream.of(
        Arguments.of(
            post("/api/solicitacoes"),
            new AbrirSolicitacaoRequest(
                texto(LimitesTexto.SOLICITACAO_TITULO + 1),
                "D",
                "REPARO",
                modeloId,
                null,
                null,
                null),
            "titulo",
            LimitesTexto.SOLICITACAO_TITULO),
        Arguments.of(
            post("/api/solicitacoes"),
            new AbrirSolicitacaoRequest(
                "T",
                texto(LimitesTexto.SOLICITACAO_DESCRICAO + 1),
                "REPARO",
                modeloId,
                null,
                null,
                null),
            "descricao",
            LimitesTexto.SOLICITACAO_DESCRICAO),
        Arguments.of(
            post("/api/solicitacoes"),
            new AbrirSolicitacaoRequest(
                "T",
                "D",
                "CRIACAO",
                null,
                texto(LimitesTexto.MODELO_PRETENDIDO_CODIGO + 1),
                "FBOX",
                null),
            "modeloCodigo",
            LimitesTexto.MODELO_PRETENDIDO_CODIGO),
        Arguments.of(
            post("/api/solicitacoes"),
            new AbrirSolicitacaoRequest(
                "T",
                "D",
                "CRIACAO",
                null,
                "COD",
                texto(LimitesTexto.MODELO_PRETENDIDO_MAQUINA + 1),
                null),
            "modeloMaquina",
            LimitesTexto.MODELO_PRETENDIDO_MAQUINA),
        Arguments.of(
            post("/api/solicitacoes"),
            new AbrirSolicitacaoRequest(
                "T",
                "D",
                "CRIACAO",
                null,
                "COD",
                "FBOX",
                texto(LimitesTexto.MODELO_PRETENDIDO_OBSERVACOES + 1)),
            "modeloObservacoes",
            LimitesTexto.MODELO_PRETENDIDO_OBSERVACOES),
        Arguments.of(
            put("/api/solicitacoes/{id}", id),
            new EditarSolicitacaoRequest(texto(LimitesTexto.SOLICITACAO_TITULO + 1), "D", "REPARO"),
            "titulo",
            LimitesTexto.SOLICITACAO_TITULO),
        Arguments.of(
            put("/api/solicitacoes/{id}", id),
            new EditarSolicitacaoRequest(
                "T", texto(LimitesTexto.SOLICITACAO_DESCRICAO + 1), "REPARO"),
            "descricao",
            LimitesTexto.SOLICITACAO_DESCRICAO),
        Arguments.of(
            post("/api/solicitacoes/{id}/comentarios", id),
            new ComentarioRequest(texto(LimitesTexto.COMENTARIO + 1)),
            "comentario",
            LimitesTexto.COMENTARIO),
        Arguments.of(
            patch("/api/solicitacoes/{id}/cancelar", id),
            new CancelarSolicitacaoRequest(texto(LimitesTexto.COMENTARIO + 1)),
            "motivo",
            LimitesTexto.COMENTARIO),
        Arguments.of(
            patch("/api/solicitacoes/{id}/devolver", id),
            new DevolverSolicitacaoRequest(texto(LimitesTexto.COMENTARIO + 1), null),
            "motivo",
            LimitesTexto.COMENTARIO),
        Arguments.of(
            patch("/api/solicitacoes/{id}/encerrar", id),
            new EncerrarSolicitacaoRequest(true, texto(LimitesTexto.COMENTARIO + 1)),
            "comentario",
            LimitesTexto.COMENTARIO));
  }

  @ParameterizedTest
  @MethodSource("textosAcimaDoLimite")
  void shouldAnswerBadRequestNamingFieldAndLimitWhenTextIsOneCharacterAboveLimit(
      final MockHttpServletRequestBuilder chamada,
      final Object corpo,
      final String campo,
      final int limite)
      throws Exception {
    // Arrange
    final String mensagem = campo + ": deve ter no máximo " + limite + " caracteres";

    // Act
    final var resposta =
        mockMvc.perform(
            chamada
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));

    // Assert
    resposta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensagem));
    verifyNoMoreInteractions(
        abrirUseCase,
        editarUseCase,
        comentarioUseCase,
        cancelarUseCase,
        devolverUseCase,
        encerrarUseCase,
        eventPublisher);
  }

  @Test
  void shouldOpenSolicitacaoWhenTitleIsExactlyAtLimit() throws Exception {
    // Arrange
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final String noLimite = texto(LimitesTexto.SOLICITACAO_TITULO);
    final AbrirSolicitacaoUseCase.Input entrada =
        new AbrirSolicitacaoUseCase.Input(
            noLimite, "Desc", TipoSolicitacao.REPARO, modeloId, null, null, null, userId);
    when(abrirUseCase.execute(entrada)).thenReturn(sol);

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/solicitacoes")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AbrirSolicitacaoRequest(
                            noLimite, "Desc", "REPARO", modeloId, null, null, null))));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(abrirUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1))
        .publish(
            "solicitacao",
            new SolicitacaoEvent("aberta", SolicitacaoResponse.from(sol)),
            sol.getId(),
            List.of());
    verifyNoMoreInteractions(abrirUseCase, eventPublisher);
  }
}
