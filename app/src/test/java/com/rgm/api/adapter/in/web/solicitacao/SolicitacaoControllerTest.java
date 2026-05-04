package com.rgm.api.adapter.in.web.solicitacao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.AbrirSolicitacaoRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
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
  @MockitoBean private RegistrarComentarioUseCase comentarioUseCase;
  @MockitoBean private ListarSolicitacoesUseCase listarUseCase;
  @MockitoBean private SolicitacaoRepository solicitacaoRepository;
  @MockitoBean private AtividadeSolicitacaoRepository atividadeRepository;

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
}
