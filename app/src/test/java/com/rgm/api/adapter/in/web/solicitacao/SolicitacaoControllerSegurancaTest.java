package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.config.RateLimitFilter;
import com.rgm.api.adapter.config.SecurityConfig;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.adapter.out.report.SolicitacaoPdfService;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.CancelarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EditarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.GerenciarResponsaveisUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarAtividadesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasPorModeloUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = SolicitacaoController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class SolicitacaoControllerSegurancaTest {

  private static final String TOKEN_OPERADOR = "token-operador";
  private static final String TOKEN_GESTOR = "token-gestor";
  private static final String TOKEN_ADMIN = "token-admin";

  @Autowired private MockMvc mockMvc;
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
  @MockitoBean private ObterHistoricoMetricasUseCase obterHistoricoMetricasUseCase;
  @MockitoBean private ObterMetricasPorModeloUseCase obterMetricasPorModeloUseCase;
  @MockitoBean private GerenciarResponsaveisUseCase gerenciarResponsaveisUseCase;
  @MockitoBean private ObterSolicitacaoUseCase obterUseCase;
  @MockitoBean private ListarAtividadesUseCase listarAtividadesUseCase;
  @MockitoBean private SolicitacaoPdfService pdfService;
  @MockitoBean private ModeloPdfService modeloPdfService;
  @MockitoBean private UsuarioRepository usuarioRepository;
  @MockitoBean private SolicitacaoEventPublisher eventPublisher;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  private Usuario autenticarComo(final String token, final PerfilUsuario perfil) {
    final var usuario =
        new Usuario(
            UUID.randomUUID(),
            "Usuario",
            "usuario@rgm.com",
            "senhaHash",
            perfil,
            true,
            Instant.now(),
            Instant.now(),
            1);
    when(autenticarAcessoUseCase.execute(token)).thenReturn(usuario);
    return usuario;
  }

  private MockHttpServletRequestBuilder consulta(final String caminho, final String token) {
    return get(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private MockHttpServletRequestBuilder consultaHead(final String caminho, final String token) {
    return head(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private void stubarMetricasPorModeloVazia() {
    when(obterMetricasPorModeloUseCase.execute(any()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
  }

  private void stubarMetricasGerais() {
    when(obterMetricasUseCase.execute())
        .thenReturn(
            new ObterMetricasSolicitacoesUseCase.Output(
                1, 2, 3, Map.of("A_FAZER", 3L), 3, 0, 0, 0, Map.of()));
  }

  @Test
  void shouldRecusarMetricasWhenOperadorConsultaAsMetricasGerais() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasGerais();

    // Act
    final var resposta = mockMvc.perform(consulta("/api/solicitacoes/metricas", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasUseCase);
  }

  @Test
  void shouldRecusarHistoricoWhenOperadorConsultaOHistoricoDeMetricas() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    when(obterHistoricoMetricasUseCase.execute(any()))
        .thenReturn(new ObterHistoricoMetricasUseCase.Output(List.of(), 0, "Ultimos 30 dias"));

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/historico", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterHistoricoMetricasUseCase);
  }

  @Test
  void shouldRecusarRankingWhenOperadorConsultaAsMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasPorModeloVazia();

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/por-modelo", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasPorModeloUseCase);
  }

  @Test
  void shouldRecusarPdfWhenOperadorExportaAsMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasPorModeloVazia();
    when(modeloPdfService.gerarRankingModelos(any(), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/por-modelo/pdf", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasPorModeloUseCase);
    verifyNoInteractions(modeloPdfService);
  }

  @Test
  void shouldRecusarHeadWhenOperadorConsultaAsMetricasGerais() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasGerais();

    // Act
    final var resposta =
        mockMvc.perform(consultaHead("/api/solicitacoes/metricas", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasUseCase);
  }

  @Test
  void shouldRecusarHeadWhenOperadorConsultaOHistoricoDeMetricas() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    when(obterHistoricoMetricasUseCase.execute(any()))
        .thenReturn(new ObterHistoricoMetricasUseCase.Output(List.of(), 0, "Ultimos 30 dias"));

    // Act
    final var resposta =
        mockMvc.perform(consultaHead("/api/solicitacoes/metricas/historico", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterHistoricoMetricasUseCase);
  }

  @Test
  void shouldRecusarHeadWhenOperadorConsultaAsMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasPorModeloVazia();

    // Act
    final var resposta =
        mockMvc.perform(consultaHead("/api/solicitacoes/metricas/por-modelo", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasPorModeloUseCase);
  }

  @Test
  void shouldRecusarHeadWhenOperadorExportaOPdfDasMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasPorModeloVazia();
    when(modeloPdfService.gerarRankingModelos(any(), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(consultaHead("/api/solicitacoes/metricas/por-modelo/pdf", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(obterMetricasPorModeloUseCase);
    verifyNoInteractions(modeloPdfService);
  }

  @Test
  void shouldResponderCorpoJsonWhenOperadorEhRecusadoNasMetricas() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarMetricasGerais();

    // Act
    final var resposta = mockMvc.perform(consulta("/api/solicitacoes/metricas", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void shouldResponder200WhenGestorConsultaAsMetricasGerais() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarMetricasGerais();

    // Act
    final var resposta = mockMvc.perform(consulta("/api/solicitacoes/metricas", TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterMetricasUseCase, times(1)).execute();
    verifyNoMoreInteractions(obterMetricasUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_GESTOR);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder200WhenGestorConsultaOHistoricoDeMetricas() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    when(obterHistoricoMetricasUseCase.execute(any()))
        .thenReturn(new ObterHistoricoMetricasUseCase.Output(List.of(), 0, "Ultimos 30 dias"));

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/historico", TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterHistoricoMetricasUseCase, times(1))
        .execute(new ObterHistoricoMetricasUseCase.Input(30, null));
    verifyNoMoreInteractions(obterHistoricoMetricasUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_GESTOR);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder200WhenGestorConsultaAsMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarMetricasPorModeloVazia();

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/por-modelo", TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterMetricasPorModeloUseCase, times(1))
        .execute(
            new ObterMetricasPorModeloUseCase.Input(
                OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, false, 0, 20));
    verifyNoMoreInteractions(obterMetricasPorModeloUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_GESTOR);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder200WhenGestorExportaOPdfDasMetricasPorModelo() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarMetricasPorModeloVazia();
    when(modeloPdfService.gerarRankingModelos(any(), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(consulta("/api/solicitacoes/metricas/por-modelo/pdf", TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterMetricasPorModeloUseCase, times(1))
        .execute(
            new ObterMetricasPorModeloUseCase.Input(
                OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, false, 0, Integer.MAX_VALUE));
    verifyNoMoreInteractions(obterMetricasPorModeloUseCase);
    verify(modeloPdfService, times(1)).gerarRankingModelos(List.of(), "Sistema");
    verifyNoMoreInteractions(modeloPdfService);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_GESTOR);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder200WhenAdministradorConsultaAsMetricasGerais() throws Exception {
    // Arrange
    autenticarComo(TOKEN_ADMIN, PerfilUsuario.ADMINISTRADOR);
    stubarMetricasGerais();

    // Act
    final var resposta = mockMvc.perform(consulta("/api/solicitacoes/metricas", TOKEN_ADMIN));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterMetricasUseCase, times(1)).execute();
    verifyNoMoreInteractions(obterMetricasUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_ADMIN);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder401WhenMetricasSaoConsultadasSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get("/api/solicitacoes/metricas");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(obterMetricasUseCase);
  }

  @Test
  void shouldResponder200WhenOperadorListaAsSolicitacoes() throws Exception {
    // Arrange
    final Usuario operador = autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(obterUseCase.listarResponsaveisBatch(any())).thenReturn(Map.of());
    final ArgumentCaptor<ListarSolicitacoesUseCase.Input> captor =
        ArgumentCaptor.forClass(ListarSolicitacoesUseCase.Input.class);

    // Act
    final var resposta = mockMvc.perform(consulta("/api/solicitacoes", TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1)).execute(captor.capture());
    assertEquals(operador.getId(), captor.getValue().usuarioAutenticadoId());
    assertEquals(0, captor.getValue().page());
    assertEquals(20, captor.getValue().size());
    verifyNoMoreInteractions(listarUseCase);
    verify(obterUseCase, times(1)).listarResponsaveisBatch(List.of());
    verify(obterUseCase, times(1)).resolverNomes(List.of());
    verifyNoMoreInteractions(obterUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN_OPERADOR);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }
}
