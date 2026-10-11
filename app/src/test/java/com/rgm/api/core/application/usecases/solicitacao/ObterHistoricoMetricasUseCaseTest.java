package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase.PontoDeSerie;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import com.rgm.api.core.domain.ports.repositories.LinhaDeSerie;
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

class ObterHistoricoMetricasUseCaseTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  private static final Instant AGORA_QUINTA_10_09_2026_12H_BRASILIA =
      Instant.parse("2026-09-10T15:00:00Z");

  private SolicitacaoRepository solicitacaoRepository;
  private ObterHistoricoMetricasUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    final Clock relogio = Clock.fixed(AGORA_QUINTA_10_09_2026_12H_BRASILIA, ZoneOffset.UTC);
    useCase = new ObterHistoricoMetricasUseCase(solicitacaoRepository, FUSO, relogio);
  }

  private void devolverLinhas(final LinhaDeSerie... linhas) {
    when(solicitacaoRepository.agregarSerie(any(), any(), any(), any(), any()))
        .thenReturn(List.of(linhas));
  }

  private static LinhaDeSerie linha(
      final String dia,
      final long total,
      final long abertas,
      final long concluidas,
      final long canceladas,
      final Double tempoMedioSegundos) {
    return new LinhaDeSerie(
        LocalDate.parse(dia), total, abertas, concluidas, canceladas, tempoMedioSegundos);
  }

  @Test
  void shouldMontarUmPontoPorDiaComZerosWhenPeriodoEAteTrintaDias() {
    // Arrange
    devolverLinhas(linha("2026-09-09", 3, 1, 1, 1, 7200.0));

    // Act
    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(3, null));

    // Assert
    assertEquals(
        List.of("07/09", "08/09", "09/09", "10/09"),
        output.series().stream().map(PontoDeSerie::periodo).toList());
    assertEquals(new PontoDeSerie("09/09", 3, 1, 1, 1, 2), output.series().get(2));
    assertEquals(new PontoDeSerie("10/09", 0, 0, 0, 0, 0), output.series().get(3));
    assertEquals("Últimos 3 dias", output.periodoLabel());
  }

  @Test
  void shouldPedirAAgregacaoNoFusoComOModeloWhenMontaASerie() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    devolverLinhas();
    final Instant inicioEsperado = Instant.parse("2026-09-07T03:00:00Z");
    final Instant fimEsperado = Instant.parse("2026-09-11T03:00:00Z");

    // Act
    useCase.execute(new ObterHistoricoMetricasUseCase.Input(3, modeloId));

    // Assert
    verify(solicitacaoRepository, times(1))
        .agregarSerie(inicioEsperado, fimEsperado, modeloId, GranularidadeSerie.DIA, FUSO);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldAgruparPorSemanaDeSegundaWhenPeriodoPassaDeTrintaDias() {
    // Arrange
    devolverLinhas(linha("2026-09-07", 2, 2, 0, 0, null));

    // Act
    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(60, null));

    // Assert
    verify(solicitacaoRepository, times(1))
        .agregarSerie(
            Instant.parse("2026-07-12T03:00:00Z"),
            Instant.parse("2026-09-11T03:00:00Z"),
            null,
            GranularidadeSerie.SEMANA,
            FUSO);
    verifyNoMoreInteractions(solicitacaoRepository);
    assertEquals(10, output.series().size());
    assertEquals("Sem 28", output.series().get(0).periodo());
    assertEquals(new PontoDeSerie("Sem 37", 2, 2, 0, 0, 0), output.series().get(9));
  }

  @Test
  void shouldCalcularSlaMediaGlobalPonderadaPelasConcluidasWhenHaMaisDeUmDia() {
    // Arrange
    devolverLinhas(
        linha("2026-09-08", 1, 0, 1, 0, 3600.0), linha("2026-09-09", 3, 0, 3, 0, 7200.0));

    // Act
    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    // Assert
    assertEquals(1L, output.slaGlobalMediaHoras());
  }

  @Test
  void shouldTruncarAsHorasWhenMediaEmSegundosNaoFechaHora() {
    // Arrange
    devolverLinhas(linha("2026-09-09", 1, 0, 1, 0, 7199.0));

    // Act
    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    // Assert
    final PontoDeSerie ponto = output.series().get(6);
    assertEquals(1L, ponto.slaMediaHoras());
  }

  @Test
  void shouldRetornarSlaZeroWhenNaoHaConcluidas() {
    // Arrange
    devolverLinhas();

    // Act
    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    // Assert
    assertEquals(0L, output.slaGlobalMediaHoras());
  }

  @Test
  void shouldRecusarWhenDiasPassaDoTetoDe365() {
    // Arrange
    final int dias = 366;

    // Act
    final var erro =
        assertThrows(
            ValidationException.class,
            () -> useCase.execute(new ObterHistoricoMetricasUseCase.Input(dias, null)));

    // Assert
    assertEquals("Parâmetro 'dias' deve estar entre 1 e 365 (recebido: 366)", erro.getMessage());
    verifyNoInteractions(solicitacaoRepository);
  }

  @Test
  void shouldRecusarWhenDiasNaoEPositivo() {
    // Arrange
    final int dias = 0;

    // Act
    final var erro =
        assertThrows(
            ValidationException.class,
            () -> useCase.execute(new ObterHistoricoMetricasUseCase.Input(dias, null)));

    // Assert
    assertEquals("Parâmetro 'dias' deve estar entre 1 e 365 (recebido: 0)", erro.getMessage());
    verifyNoInteractions(solicitacaoRepository);
  }
}
