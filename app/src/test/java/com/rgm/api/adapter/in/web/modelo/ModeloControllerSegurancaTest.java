package com.rgm.api.adapter.in.web.modelo;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.RateLimitFilter;
import com.rgm.api.adapter.config.SecurityConfig;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoSolicitacoesModeloUseCase;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Os resumos passam pela regra real de seguranca, a mesma da consulta de modelos. */
@WebMvcTest(controllers = ModeloController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class})
@ActiveProfiles("test")
class ModeloControllerSegurancaTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GerenciarModelosUseCase gerenciarUseCase;
  @MockitoBean private ListarModelosUseCase listarUseCase;
  @MockitoBean private ObterResumoModelosUseCase resumoModelosUseCase;
  @MockitoBean private ObterResumoSolicitacoesModeloUseCase resumoSolicitacoesUseCase;
  @MockitoBean private ModeloRepository modeloRepository;
  @MockitoBean private EventoModeloRepository eventoModeloRepository;
  @MockitoBean private SolicitacaoRepository solicitacaoRepository;
  @MockitoBean private AtividadeSolicitacaoRepository atividadeRepository;
  @MockitoBean private ModeloPdfService modeloPdfService;
  @MockitoBean private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  @MockitoBean private UsuarioRepository usuarioRepository;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  @Test
  void shouldRecusarOAcessoWhenConsultaOResumoDeModelosSemAutenticacao() throws Exception {
    // Arrange
    final var requisicao = get("/api/modelos/resumo");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(resumoModelosUseCase);
  }

  @Test
  void shouldRecusarOAcessoWhenConsultaOResumoDeUmModeloSemAutenticacao() throws Exception {
    // Arrange
    final var requisicao = get("/api/modelos/{id}/solicitacoes/resumo", UUID.randomUUID());

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(resumoSolicitacoesUseCase);
  }
}
