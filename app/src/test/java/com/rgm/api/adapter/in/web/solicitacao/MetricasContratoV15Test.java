package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SolicitacaoController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class MetricasContratoV15Test {

  private static final String ROTA_METRICAS = "/api/solicitacoes/metricas";
  private static final String ROTA_HISTORICO = "/api/solicitacoes/metricas/historico?dias=7";
  private static final String TOKEN_GESTOR = "token-gestor";

  private static final Set<String> CAMPOS_INTEIROS_DE_METRICAS =
      Set.of(
          "totalUsuarios",
          "totalModelos",
          "totalSolicitacoes",
          "solicitacoesAbertas",
          "solicitacoesPendentes",
          "solicitacoesConcluidas",
          "tempoMedioResolucaoSegundos");

  private static final Set<String> CAMPOS_DE_OBJETO_DE_METRICAS =
      Set.of("solicitacoesPorStatus", "solicitacoesPorModelo");

  private static final Set<String> CAMPOS_DO_HISTORICO =
      Set.of("series", "slaGlobalMediaHoras", "periodoLabel");

  private static final Set<String> CAMPOS_TEXTUAIS_DO_PONTO = Set.of("periodo");

  private static final Set<String> CAMPOS_INTEIROS_DO_PONTO =
      Set.of("total", "abertas", "concluidas", "canceladas", "slaMediaHoras");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;
  @MockitoBean private ObterMetricasSolicitacoesUseCase obterMetricasUseCase;
  @MockitoBean private ObterHistoricoMetricasUseCase obterHistoricoMetricasUseCase;
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
  @MockitoBean private ObterMetricasPorModeloUseCase obterMetricasPorModeloUseCase;
  @MockitoBean private GerenciarResponsaveisUseCase gerenciarResponsaveisUseCase;
  @MockitoBean private ObterSolicitacaoUseCase obterUseCase;
  @MockitoBean private ListarAtividadesUseCase listarAtividadesUseCase;
  @MockitoBean private SolicitacaoPdfService pdfService;
  @MockitoBean private ModeloPdfService modeloPdfService;
  @MockitoBean private UsuarioRepository usuarioRepository;
  @MockitoBean private SolicitacaoEventPublisher eventPublisher;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  @BeforeEach
  void autenticarGestor() {
    final var gestor =
        new Usuario(
            UUID.randomUUID(),
            "Gestor",
            "gestor@rgm.com",
            "senhaHash",
            PerfilUsuario.GESTOR,
            true,
            Instant.now(),
            Instant.now(),
            1);
    when(autenticarAcessoUseCase.execute(TOKEN_GESTOR)).thenReturn(gestor);
  }

  private void stubarMetricas() {
    when(obterMetricasUseCase.execute())
        .thenReturn(
            new ObterMetricasSolicitacoesUseCase.Output(
                1, 2, 3, Map.of("A_FAZER", 3L), 3, 0, 0, 0, Map.of(UUID.randomUUID(), 3L)));
  }

  private void stubarHistorico() {
    final var ponto = new ObterHistoricoMetricasUseCase.PontoDeSerie("10/09", 3, 1, 1, 1, 2);
    when(obterHistoricoMetricasUseCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null)))
        .thenReturn(new ObterHistoricoMetricasUseCase.Output(List.of(ponto), 2, "Últimos 7 dias"));
  }

  private JsonNode corpoDe(final String rota) throws Exception {
    final String corpo =
        mockMvc
            .perform(get(rota).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_GESTOR))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(corpo);
  }

  private static Set<String> nomesDosCampos(final JsonNode no) {
    final Set<String> nomes = new HashSet<>();
    no.fieldNames().forEachRemaining(nomes::add);
    return nomes;
  }

  private static boolean todosSatisfazem(
      final JsonNode no, final Set<String> campos, final Predicate<JsonNode> tipo) {
    return campos.stream().allMatch(campo -> tipo.test(no.path(campo)));
  }

  @Test
  void shouldManterOsCamposDeMetricasWhenRespostaDoEndpointDeMetricas() throws Exception {
    // Arrange
    stubarMetricas();
    final Set<String> esperados = new HashSet<>(CAMPOS_INTEIROS_DE_METRICAS);
    esperados.addAll(CAMPOS_DE_OBJETO_DE_METRICAS);

    // Act
    final Set<String> nomes = nomesDosCampos(corpoDe(ROTA_METRICAS));

    // Assert
    assertTrue(nomes.containsAll(esperados), "Campos de /metricas ausentes; recebido: " + nomes);
    verify(obterMetricasUseCase, times(1)).execute();
    verifyNoMoreInteractions(obterMetricasUseCase);
  }

  @Test
  void shouldSerializarContagensDeMetricasComoInteirosWhenRespostaDoEndpointDeMetricas()
      throws Exception {
    // Arrange
    stubarMetricas();

    // Act
    final JsonNode corpo = corpoDe(ROTA_METRICAS);

    // Assert
    assertTrue(
        todosSatisfazem(corpo, CAMPOS_INTEIROS_DE_METRICAS, JsonNode::isIntegralNumber),
        "Contagens de /metricas devem ser inteiras; recebido: " + corpo);
  }

  @Test
  void shouldSerializarMapasDeMetricasComoObjetosWhenRespostaDoEndpointDeMetricas()
      throws Exception {
    // Arrange
    stubarMetricas();

    // Act
    final JsonNode corpo = corpoDe(ROTA_METRICAS);

    // Assert
    assertTrue(
        todosSatisfazem(corpo, CAMPOS_DE_OBJETO_DE_METRICAS, JsonNode::isObject),
        "Mapas de /metricas devem ser objetos; recebido: " + corpo);
  }

  @Test
  void shouldManterOsCamposDoHistoricoWhenRespostaDoEndpointDeHistorico() throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final Set<String> nomes = nomesDosCampos(corpoDe(ROTA_HISTORICO));

    // Assert
    assertTrue(
        nomes.containsAll(CAMPOS_DO_HISTORICO),
        "Campos de /metricas/historico ausentes; recebido: " + nomes);
    verify(obterHistoricoMetricasUseCase, times(1))
        .execute(new ObterHistoricoMetricasUseCase.Input(7, null));
    verifyNoMoreInteractions(obterHistoricoMetricasUseCase);
  }

  @Test
  void shouldSerializarSeriesComoListaWhenRespostaDoEndpointDeHistorico() throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final JsonNode series = corpoDe(ROTA_HISTORICO).path("series");

    // Assert
    assertTrue(series.isArray(), "series deve ser lista; recebido: " + series);
  }

  @Test
  void shouldSerializarSlaGlobalComoInteiroWhenRespostaDoEndpointDeHistorico() throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final JsonNode sla = corpoDe(ROTA_HISTORICO).path("slaGlobalMediaHoras");

    // Assert
    assertTrue(sla.isIntegralNumber(), "slaGlobalMediaHoras deve ser inteiro; recebido: " + sla);
  }

  @Test
  void shouldSerializarPeriodoLabelComoTextoWhenRespostaDoEndpointDeHistorico() throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final JsonNode periodoLabel = corpoDe(ROTA_HISTORICO).path("periodoLabel");

    // Assert
    assertTrue(periodoLabel.isTextual(), "periodoLabel deve ser texto; recebido: " + periodoLabel);
  }

  @Test
  void shouldManterOsCamposDoPontoWhenSerieDoHistoricoTemPontos() throws Exception {
    // Arrange
    stubarHistorico();
    final Set<String> esperados = new HashSet<>(CAMPOS_INTEIROS_DO_PONTO);
    esperados.addAll(CAMPOS_TEXTUAIS_DO_PONTO);

    // Act
    final JsonNode ponto = corpoDe(ROTA_HISTORICO).path("series").path(0);

    // Assert
    assertTrue(
        nomesDosCampos(ponto).containsAll(esperados),
        "Campos do ponto de serie ausentes; recebido: " + nomesDosCampos(ponto));
  }

  @Test
  void shouldSerializarContagensDoPontoComoInteirosWhenSerieDoHistoricoTemPontos()
      throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final JsonNode ponto = corpoDe(ROTA_HISTORICO).path("series").path(0);

    // Assert
    assertTrue(
        todosSatisfazem(ponto, CAMPOS_INTEIROS_DO_PONTO, JsonNode::isIntegralNumber),
        "Contagens do ponto devem ser inteiras; recebido: " + ponto);
  }

  @Test
  void shouldSerializarPeriodoDoPontoComoTextoWhenSerieDoHistoricoTemPontos() throws Exception {
    // Arrange
    stubarHistorico();

    // Act
    final JsonNode ponto = corpoDe(ROTA_HISTORICO).path("series").path(0);

    // Assert
    assertTrue(
        todosSatisfazem(ponto, CAMPOS_TEXTUAIS_DO_PONTO, JsonNode::isTextual),
        "periodo do ponto deve ser texto; recebido: " + ponto);
  }
}
