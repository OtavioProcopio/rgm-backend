package com.rgm.api.adapter.in.web.solicitacao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase.Janela;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase.Ponto;
import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
class PainelPorPeriodoControllerTest {

  private static final String ROTA = "/api/solicitacoes/metricas/painel";
  private static final String TOKEN_OPERADOR = "token-operador";
  private static final String TOKEN_GESTOR = "token-gestor";
  private static final String TOKEN_ADMIN = "token-admin";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ObterPainelPorPeriodoUseCase obterPainelUseCase;
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

  private void autenticarComo(final String token, final PerfilUsuario perfil) {
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
  }

  private MockHttpServletRequestBuilder consulta(final String caminho, final String token) {
    return get(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private static Janela janela(final long criadas, final Double tempoMedio, final Ponto ponto) {
    return new Janela(
        Instant.parse("2026-09-04T03:00:00Z"),
        Instant.parse("2026-09-11T03:00:00Z"),
        criadas,
        2,
        1,
        tempoMedio,
        List.of(ponto));
  }

  private void stubarPainel() {
    final var atual = janela(5, 1800.0, new Ponto("10/09", 2, 1, 0, 6, 3));
    final var anterior = janela(4, null, new Ponto("03/09", 1, 1, 0, 4, 1));
    when(obterPainelUseCase.execute(any()))
        .thenReturn(new ObterPainelPorPeriodoUseCase.Output(atual, anterior, 12, 4));
  }

  @Test
  void shouldResponder200ComPeriodoEAnteriorWhenGestorConsultaOPainel() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA + "?dias=7", TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.criadas").value(5))
        .andExpect(jsonPath("$.periodo.concluidas").value(2))
        .andExpect(jsonPath("$.periodo.canceladas").value(1))
        .andExpect(jsonPath("$.periodo.tempoMedioResolucaoSegundos").value(1800.0))
        .andExpect(jsonPath("$.periodo.abertasAgora").value(12))
        .andExpect(jsonPath("$.periodo.atrasadasAgora").value(4))
        .andExpect(jsonPath("$.periodo.inicio").exists())
        .andExpect(jsonPath("$.periodo.fim").exists())
        .andExpect(jsonPath("$.periodo.serie[0].rotulo").value("10/09"))
        .andExpect(jsonPath("$.periodo.serie[0].abertasAoFimDoDia").value(6))
        .andExpect(jsonPath("$.periodo.serie[0].atrasadasAoFimDoDia").value(3))
        .andExpect(jsonPath("$.anterior.criadas").value(4))
        .andExpect(jsonPath("$.anterior.serie[0].rotulo").value("03/09"));
  }

  @Test
  void shouldOmitirAbertasAgoraDoAnteriorWhenPainelEMontado() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anterior.abertasAgora").doesNotExist());
  }

  @Test
  void shouldOmitirAtrasadasAgoraDoAnteriorWhenPainelEMontado() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anterior.atrasadasAgora").doesNotExist());
  }

  @Test
  void shouldSerializarTempoMedioNuloNoAnteriorWhenNaoHaConcluidaNaJanela() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anterior.tempoMedioResolucaoSegundos").hasJsonPath())
        .andExpect(jsonPath("$.anterior.tempoMedioResolucaoSegundos").isEmpty());
  }

  @Test
  void shouldUsar30DiasWhenDiasNaoVem() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterPainelUseCase, times(1)).execute(new ObterPainelPorPeriodoUseCase.Input(30, null));
    verifyNoMoreInteractions(obterPainelUseCase);
  }

  @Test
  void shouldRepassarOModeloWhenModeloIdVem() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);
    stubarPainel();
    final UUID modeloId = UUID.randomUUID();

    // Act
    final var resposta =
        mockMvc.perform(consulta(ROTA + "?dias=90&modeloId=" + modeloId, TOKEN_GESTOR));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterPainelUseCase, times(1))
        .execute(new ObterPainelPorPeriodoUseCase.Input(90, modeloId));
    verifyNoMoreInteractions(obterPainelUseCase);
  }

  @Test
  void shouldResponder400ListandoOs7E30E90WhenDiasE15() throws Exception {
    // Arrange
    autenticarComo(TOKEN_GESTOR, PerfilUsuario.GESTOR);

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA + "?dias=15", TOKEN_GESTOR));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Parâmetro 'dias' inválido (recebido: 15). Valores aceitos: 7, 30, 90"));
    verifyNoInteractions(obterPainelUseCase);
  }

  @Test
  void shouldResponder200WhenAdministradorConsultaOPainel() throws Exception {
    // Arrange
    autenticarComo(TOKEN_ADMIN, PerfilUsuario.ADMINISTRADOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_ADMIN));

    // Assert
    resposta.andExpect(status().isOk());
    verify(obterPainelUseCase, times(1)).execute(new ObterPainelPorPeriodoUseCase.Input(30, null));
    verifyNoMoreInteractions(obterPainelUseCase);
  }

  @Test
  void shouldResponder403ComCorpoDeErroWhenOperadorConsultaOPainel() throws Exception {
    // Arrange
    autenticarComo(TOKEN_OPERADOR, PerfilUsuario.OPERADOR);
    stubarPainel();

    // Act
    final var resposta = mockMvc.perform(consulta(ROTA, TOKEN_OPERADOR));

    // Assert
    resposta.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    verifyNoInteractions(obterPainelUseCase);
  }

  @Test
  void shouldResponder401WhenPainelEConsultadoSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get(ROTA);

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(obterPainelUseCase);
  }
}
