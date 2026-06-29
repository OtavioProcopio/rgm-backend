package com.rgm.api.adapter.in.web.solicitacao;

import static org.mockito.ArgumentMatchers.any;
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
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
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

  @MockitoBean
  private com.rgm.api.core.domain.ports.repositories.UsuarioRepository usuarioRepository;

  @MockitoBean
  private com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase
      obterHistoricoMetricasUseCase;

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
                            "Titulo", "Desc", "REPARO", UUID.randomUUID()))))
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
  void buscarPorId() throws Exception {
    final UUID solId = UUID.randomUUID();
    final Solicitacao sol = criarSolicitacao();
    when(obterUseCase.execute(solId))
        .thenReturn(new ObterSolicitacaoUseCase.Output(sol, List.of()));

    mockMvc
        .perform(get("/api/solicitacoes/{id}", solId).with(user("u")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Titulo"));
  }

  @Test
  void buscarPorId_naoEncontrado() throws Exception {
    final UUID solId = UUID.randomUUID();
    when(obterUseCase.execute(solId))
        .thenThrow(new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    mockMvc
        .perform(get("/api/solicitacoes/{id}", solId).with(user("u")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listarAtividades() throws Exception {
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.abertura(solId, autorId, Instant.now());

    when(listarAtividadesUseCase.execute(solId))
        .thenReturn(List.of(new ListarAtividadesUseCase.AtividadeComAutor(atividade, "Alice")));

    mockMvc
        .perform(get("/api/solicitacoes/{id}/atividades", solId).with(user("u")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].autorNome").value("Alice"));
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
                        new EditarSolicitacaoRequest("Novo Titulo", "Nova Desc"))))
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
  void gerenciarResponsaveis() throws Exception {
    final Solicitacao sol = criarSolicitacao();
    final UUID userId = UUID.randomUUID();
    when(gerenciarResponsaveisUseCase.execute(any())).thenReturn(sol);

    mockMvc
        .perform(
            patch("/api/solicitacoes/{id}/responsaveis", sol.getId())
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new GerenciarResponsaveisRequest(List.of(UUID.randomUUID())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(sol.getId().toString()));
  }
}
