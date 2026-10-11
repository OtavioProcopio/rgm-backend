package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase.Ponto;
import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import com.rgm.api.core.domain.ports.repositories.PainelAgregado;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterPainelPorPeriodoUseCaseTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  private static final Instant AGORA_QUINTA_10_09_2026_12H_BRASILIA =
      Instant.parse("2026-09-10T15:00:00Z");
  private static final Instant AGORA = AGORA_QUINTA_10_09_2026_12H_BRASILIA;

  private static final Instant FIM_DO_DIA_ATUAL = Instant.parse("2026-09-11T03:00:00Z");
  private static final Instant INICIO_DE_7_DIAS = Instant.parse("2026-09-04T03:00:00Z");
  private static final Instant INICIO_ANTERIOR_A_7_DIAS = Instant.parse("2026-08-28T03:00:00Z");
  private static final Instant INICIO_DE_30_DIAS = Instant.parse("2026-08-12T03:00:00Z");
  private static final Instant INICIO_ANTERIOR_A_30_DIAS = Instant.parse("2026-07-13T03:00:00Z");
  private static final Instant INICIO_DE_90_DIAS = Instant.parse("2026-06-13T03:00:00Z");
  private static final Instant INICIO_ANTERIOR_A_90_DIAS = Instant.parse("2026-03-15T03:00:00Z");

  private SolicitacaoRepository solicitacaoRepository;
  private ObterPainelPorPeriodoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    useCase = new ObterPainelPorPeriodoUseCase(solicitacaoRepository, FUSO, relogio);
    devolverPainelVazio();
  }

  private void devolverPainelVazio() {
    final PainelAgregado vazio =
        new PainelAgregado(new PainelAgregado.Totais(0, 0, 0, null), List.of());
    when(solicitacaoRepository.agregarPainel(any(), any(), any(), any(), any(), any()))
        .thenReturn(vazio);
    when(solicitacaoRepository.situacaoAgora(any(), any()))
        .thenReturn(new PainelAgregado.Agora(0, 0));
  }

  private void devolverNoPeriodoDe7Dias(final PainelAgregado agregado) {
    when(solicitacaoRepository.agregarPainel(
            INICIO_DE_7_DIAS, FIM_DO_DIA_ATUAL, null, GranularidadeSerie.DIA, FUSO, AGORA))
        .thenReturn(agregado);
  }

  private static ObterPainelPorPeriodoUseCase.Input entrada(final int dias, final UUID modeloId) {
    return new ObterPainelPorPeriodoUseCase.Input(dias, modeloId);
  }

  @Test
  void shouldMontarJanelaAtualEAnteriorAdjacentesDeMesmoTamanhoWhenPeriodoE7Dias() {
    // Arrange
    final ObterPainelPorPeriodoUseCase.Input input = entrada(7, null);

    // Act
    final var output = useCase.execute(input);

    // Assert
    assertEquals(INICIO_DE_7_DIAS, output.periodo().inicio());
    assertEquals(FIM_DO_DIA_ATUAL, output.periodo().fim());
    assertEquals(INICIO_ANTERIOR_A_7_DIAS, output.anterior().inicio());
    assertEquals(output.periodo().inicio(), output.anterior().fim());
  }

  @Test
  void shouldChamarAPortaUmaVezPorJanelaESemListarSolicitacoesWhenMontaOPainel() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();

    // Act
    useCase.execute(entrada(7, modeloId));

    // Assert
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_DE_7_DIAS, FIM_DO_DIA_ATUAL, modeloId, GranularidadeSerie.DIA, FUSO, AGORA);
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_ANTERIOR_A_7_DIAS,
            INICIO_DE_7_DIAS,
            modeloId,
            GranularidadeSerie.DIA,
            FUSO,
            AGORA);
    verify(solicitacaoRepository, times(1)).situacaoAgora(modeloId, AGORA);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldAgruparPorSemanaWhenPeriodoPassaDeTrintaDias() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();

    // Act
    useCase.execute(entrada(90, modeloId));

    // Assert
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_DE_90_DIAS, FIM_DO_DIA_ATUAL, modeloId, GranularidadeSerie.SEMANA, FUSO, AGORA);
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_ANTERIOR_A_90_DIAS,
            INICIO_DE_90_DIAS,
            modeloId,
            GranularidadeSerie.SEMANA,
            FUSO,
            AGORA);
    verify(solicitacaoRepository, times(1)).situacaoAgora(modeloId, AGORA);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldAgruparPorDiaWhenPeriodoETrintaDias() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();

    // Act
    useCase.execute(entrada(30, modeloId));

    // Assert
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_DE_30_DIAS, FIM_DO_DIA_ATUAL, modeloId, GranularidadeSerie.DIA, FUSO, AGORA);
    verify(solicitacaoRepository, times(1))
        .agregarPainel(
            INICIO_ANTERIOR_A_30_DIAS,
            INICIO_DE_30_DIAS,
            modeloId,
            GranularidadeSerie.DIA,
            FUSO,
            AGORA);
    verify(solicitacaoRepository, times(1)).situacaoAgora(modeloId, AGORA);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldRotularOPontoComDiaEMesWhenAgrupaPorDia() {
    // Arrange
    devolverNoPeriodoDe7Dias(
        new PainelAgregado(
            new PainelAgregado.Totais(5, 4, 1, null),
            List.of(
                new PainelAgregado.Ponto(LocalDate.of(2026, 9, 9), 2, 1, 0, 6, 3),
                new PainelAgregado.Ponto(LocalDate.of(2026, 9, 10), 3, 3, 1, 4, 1))));

    // Act
    final var periodo = useCase.execute(entrada(7, null)).periodo();

    // Assert
    assertEquals(List.of("09/09", "10/09"), periodo.serie().stream().map(Ponto::rotulo).toList());
  }

  @Test
  void shouldCopiarAsContagensDoPontoWhenMontaASerie() {
    // Arrange
    devolverNoPeriodoDe7Dias(
        new PainelAgregado(
            new PainelAgregado.Totais(2, 1, 0, null),
            List.of(new PainelAgregado.Ponto(LocalDate.of(2026, 9, 9), 2, 1, 0, 6, 3))));

    // Act
    final var periodo = useCase.execute(entrada(7, null)).periodo();

    // Assert
    assertEquals(List.of(new Ponto("09/09", 2, 1, 0, 6, 3)), periodo.serie());
  }

  @Test
  void shouldCopiarOsTotaisDaJanelaWhenAgregadoTrazTotais() {
    // Arrange
    devolverNoPeriodoDe7Dias(
        new PainelAgregado(new PainelAgregado.Totais(5, 4, 1, 1234.5678), List.of()));

    // Act
    final var periodo = useCase.execute(entrada(7, null)).periodo();

    // Assert
    assertEquals(
        List.of(5L, 4L, 1L),
        List.of(periodo.criadas(), periodo.concluidas(), periodo.canceladas()));
  }

  @Test
  void shouldArredondarOTempoMedioParaUmaCasaWhenMediaTemVariasCasas() {
    // Arrange
    devolverNoPeriodoDe7Dias(
        new PainelAgregado(new PainelAgregado.Totais(5, 4, 1, 1234.5678), List.of()));

    // Act
    final var periodo = useCase.execute(entrada(7, null)).periodo();

    // Assert
    assertEquals(1234.6, periodo.tempoMedioResolucaoSegundos());
  }

  @Test
  void shouldRotularASemanaComONumeroDaSemanaWhenAgrupaPorSemana() {
    // Arrange
    final PainelAgregado atual =
        new PainelAgregado(
            new PainelAgregado.Totais(1, 0, 0, null),
            List.of(new PainelAgregado.Ponto(LocalDate.of(2026, 9, 7), 1, 0, 0, 1, 0)));
    when(solicitacaoRepository.agregarPainel(
            INICIO_DE_90_DIAS, FIM_DO_DIA_ATUAL, null, GranularidadeSerie.SEMANA, FUSO, AGORA))
        .thenReturn(atual);

    // Act
    final var periodo = useCase.execute(entrada(90, null)).periodo();

    // Assert
    assertEquals(List.of(new Ponto("Sem 37", 1, 0, 0, 1, 0)), periodo.serie());
    verify(solicitacaoRepository)
        .agregarPainel(
            INICIO_DE_90_DIAS, FIM_DO_DIA_ATUAL, null, GranularidadeSerie.SEMANA, FUSO, AGORA);
  }

  @Test
  void shouldManterTempoMedioNuloWhenNaoHaConcluidaNaJanela() {
    // Arrange
    final ObterPainelPorPeriodoUseCase.Input input = entrada(7, null);

    // Act
    final var output = useCase.execute(input);

    // Assert
    assertNull(output.periodo().tempoMedioResolucaoSegundos());
    assertNull(output.anterior().tempoMedioResolucaoSegundos());
  }

  @Test
  void shouldDar1800SegundosWhenResolucaoFoiDeTrintaMinutos() {
    // Arrange
    devolverNoPeriodoDe7Dias(
        new PainelAgregado(new PainelAgregado.Totais(1, 1, 0, 1800.0), List.of()));

    // Act
    final var output = useCase.execute(entrada(7, null));

    // Assert
    assertEquals(1800.0, output.periodo().tempoMedioResolucaoSegundos());
  }

  @Test
  void shouldTrazerAbertasAgoraWhenSituacaoAgoraTemAbertas() {
    // Arrange
    when(solicitacaoRepository.situacaoAgora(null, AGORA))
        .thenReturn(new PainelAgregado.Agora(12, 4));

    // Act
    final var output = useCase.execute(entrada(7, null));

    // Assert
    assertEquals(12, output.abertasAgora());
    verify(solicitacaoRepository).situacaoAgora(null, AGORA);
  }

  @Test
  void shouldTrazerAtrasadasAgoraWhenSituacaoAgoraTemAtrasadas() {
    // Arrange
    when(solicitacaoRepository.situacaoAgora(null, AGORA))
        .thenReturn(new PainelAgregado.Agora(12, 4));

    // Act
    final var output = useCase.execute(entrada(7, null));

    // Assert
    assertEquals(4, output.atrasadasAgora());
    verify(solicitacaoRepository).situacaoAgora(null, AGORA);
  }
}
