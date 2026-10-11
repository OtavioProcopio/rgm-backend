package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Output;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.BacklogAgora;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.CargaDoResponsavel;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.DesempenhoDaMaquina;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.Devolucoes;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.ModeloReincidente;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.PrazoPorPrioridade;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.ResolucaoNoPeriodo;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.TempoDeEtapa;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterIndicadoresDeGestaoUseCaseTest {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final Instant AGORA = Instant.parse("2026-10-11T15:00:00Z");

  private IndicadoresDeGestaoRepository repositorio;
  private ObterIndicadoresDeGestaoUseCase useCase;

  @BeforeEach
  void setUp() {
    repositorio = mock(IndicadoresDeGestaoRepository.class);
    when(repositorio.resolucao(any(), any())).thenReturn(new ResolucaoNoPeriodo(0, null, null));
    when(repositorio.backlog(any())).thenReturn(new BacklogAgora(0, 0, 0, 0, 0));
    when(repositorio.devolucoes(any(), any())).thenReturn(new Devolucoes(0, 0));
    when(repositorio.prazoPorPrioridade(any(), any())).thenReturn(List.of());
    when(repositorio.tempoPorEtapa(any(), any())).thenReturn(List.of());
    when(repositorio.porMaquina(any(), any(), any())).thenReturn(List.of());
    when(repositorio.reincidentes(any(), any(), anyInt())).thenReturn(List.of());
    when(repositorio.cargaPorResponsavel()).thenReturn(List.of());
    useCase =
        new ObterIndicadoresDeGestaoUseCase(repositorio, FUSO, Clock.fixed(AGORA, ZoneOffset.UTC));
  }

  @Test
  void shouldUseTheLastNLocalDaysIncludingTodayWhenBuildingTheWindow() {
    // Arrange
    final var input = new ObterIndicadoresDeGestaoUseCase.Input(7);

    // Act
    final Output output = useCase.execute(input);

    // Assert (11/10 12:00 em Brasília; de 05/10 00:00 a 12/10 00:00, UTC-3)
    assertEquals(Instant.parse("2026-10-05T03:00:00Z"), output.inicio());
    assertEquals(Instant.parse("2026-10-12T03:00:00Z"), output.fim());
    verify(repositorio, times(1)).resolucao(output.inicio(), output.fim());
  }

  @Test
  void shouldComputeDeadlinePercentagesTotalAndPerPriority() {
    // Arrange
    when(repositorio.prazoPorPrioridade(any(), any()))
        .thenReturn(
            List.of(
                new PrazoPorPrioridade(PrioridadeSolicitacao.ALTA, 4, 3),
                new PrazoPorPrioridade(PrioridadeSolicitacao.BAIXA, 1, 0)));
    when(repositorio.concluidasSemPrioridade(any(), any())).thenReturn(2L);

    // Act
    final var prazo = useCase.execute(new ObterIndicadoresDeGestaoUseCase.Input(30)).prazo();

    // Assert
    assertEquals(5, prazo.concluidasComPrioridade());
    assertEquals(3, prazo.noPrazo());
    assertEquals(60.0, prazo.percentualNoPrazo());
    assertEquals(2, prazo.concluidasSemPrioridade());
    assertEquals(
        List.of("URGENTE", "ALTA", "MEDIA", "BAIXA"),
        prazo.porPrioridade().stream().map(p -> p.prioridade()).toList());
    assertEquals(75.0, prazo.porPrioridade().get(1).percentualNoPrazo());
    assertNull(prazo.porPrioridade().get(0).percentualNoPrazo());
  }

  @Test
  void shouldReturnNullPercentagesAndEmptyStagesWhenThereIsNoData() {
    // Arrange
    final var input = new ObterIndicadoresDeGestaoUseCase.Input(7);

    // Act
    final Output output = useCase.execute(input);

    // Assert
    assertNull(output.prazo().percentualNoPrazo());
    assertNull(output.resolucao().medianaSegundos());
    assertNull(output.devolucao().taxa());
    assertEquals(0, output.etapas().execucao().amostras());
    assertNull(output.etapas().execucao().medianaSegundos());
    assertNull(output.etapas().esperaAteIniciar().mediaSegundos());
  }

  @Test
  void shouldMapStagesReturnRateMachinesRecurringModelsAndLoad() {
    // Arrange
    final UUID modelo = UUID.randomUUID();
    final UUID usuario = UUID.randomUUID();
    when(repositorio.tempoPorEtapa(any(), any()))
        .thenReturn(
            List.of(
                new TempoDeEtapa("A_FAZER", 2, 7200.0, 7500.0),
                new TempoDeEtapa("EM_ANDAMENTO", 3, 18000.0, 19000.0),
                new TempoDeEtapa("EM_VALIDACAO", 1, 3600.0, 3600.0)));
    when(repositorio.devolucoes(any(), any())).thenReturn(new Devolucoes(4, 1));
    when(repositorio.porMaquina(any(), any(), any()))
        .thenReturn(List.of(new DesempenhoDaMaquina("FBOX", 5, 2, 36000.0)));
    when(repositorio.reincidentes(any(), any(), anyInt()))
        .thenReturn(List.of(new ModeloReincidente(modelo, "MOD-1", 2, "FBOX", 3)));
    when(repositorio.abertasComResponsavel()).thenReturn(7L);
    when(repositorio.cargaPorResponsavel())
        .thenReturn(List.of(new CargaDoResponsavel(usuario, "Ana", 4)));

    // Act
    final Output output = useCase.execute(new ObterIndicadoresDeGestaoUseCase.Input(90));

    // Assert
    assertEquals(7200.0, output.etapas().esperaAteIniciar().medianaSegundos());
    assertEquals(19000.0, output.etapas().execucao().mediaSegundos());
    assertEquals(3600.0, output.etapas().esperaDeValidacao().medianaSegundos());
    assertEquals(25.0, output.devolucao().taxa());
    assertEquals("FBOX", output.porMaquina().get(0).maquina());
    assertEquals(2, output.porMaquina().get(0).emAtraso());
    assertEquals(modelo, output.reincidentes().get(0).modeloId());
    assertEquals(7, output.carga().solicitacoesDistintas());
    assertEquals("Ana", output.carga().responsaveis().get(0).nome());
    verify(repositorio, times(1)).reincidentes(any(), any(), anyInt());
  }

  @Test
  void shouldAskForRecurringModelsWithThePluralLimitOfThree() {
    // Arrange
    final var input = new ObterIndicadoresDeGestaoUseCase.Input(30);

    // Act
    useCase.execute(input);

    // Assert
    verify(repositorio, times(1)).reincidentes(any(), any(), org.mockito.ArgumentMatchers.eq(3));
  }
}
