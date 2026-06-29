package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterHistoricoMetricasUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private ObterHistoricoMetricasUseCase useCase;

  private final UUID modeloId = UUID.randomUUID();
  private final UUID usuarioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    useCase = new ObterHistoricoMetricasUseCase(solicitacaoRepository);
  }

  private Solicitacao criarSolicitacao(
      final StatusSolicitacao status,
      final Instant criadaEm,
      final Instant concluidaEm,
      final UUID modelo) {
    final String comentario =
        (status == StatusSolicitacao.CONCLUIDA || status == StatusSolicitacao.CANCELADA)
            ? "Comentário final"
            : null;
    final Instant canceladaEm = status == StatusSolicitacao.CANCELADA ? criadaEm : null;
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        status,
        PrioridadeSolicitacao.MEDIA,
        modelo != null ? modelo : modeloId,
        usuarioId,
        comentario,
        criadaEm,
        criadaEm,
        concluidaEm,
        canceladaEm);
  }

  @Test
  void deveRetornarSerieComBucketsDiarios() {
    final Instant agora = Instant.now();
    final Solicitacao concluida =
        criarSolicitacao(
            StatusSolicitacao.CONCLUIDA, agora.minus(2, ChronoUnit.DAYS), agora, null);
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any())).thenReturn(List.of(concluida));

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    assertNotNull(output);
    assertFalse(output.series().isEmpty());
    assertEquals("Últimos 7 dias", output.periodoLabel());
    final long totalSolicitacoes = output.series().stream().mapToLong(s -> s.total()).sum();
    assertEquals(1, totalSolicitacoes);
    final long totalConcluidas = output.series().stream().mapToLong(s -> s.concluidas()).sum();
    assertEquals(1, totalConcluidas);
  }

  @Test
  void deveRetornarBucketsSemanaisParaMaisDe30Dias() {
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any())).thenReturn(List.of());

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(60, null));

    assertFalse(output.series().isEmpty());
    assertTrue(output.series().get(0).periodo().startsWith("Sem "));
  }

  @Test
  void deveContabilizarSolicitacoesCanceladasEAbertas() {
    final Instant agora = Instant.now();
    final Solicitacao cancelada =
        criarSolicitacao(StatusSolicitacao.CANCELADA, agora.minus(1, ChronoUnit.DAYS), null, null);
    final Solicitacao aberta =
        criarSolicitacao(StatusSolicitacao.A_FAZER, agora.minus(1, ChronoUnit.DAYS), null, null);
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any()))
        .thenReturn(List.of(cancelada, aberta));

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    assertEquals(1, output.series().stream().mapToLong(s -> s.canceladas()).sum());
    assertEquals(1, output.series().stream().mapToLong(s -> s.abertas()).sum());
  }

  @Test
  void deveCalcularSlaMediaGlobal() {
    final Instant agora = Instant.now();
    final Instant criadaEm = agora.minus(4, ChronoUnit.HOURS);
    final Solicitacao concluida =
        criarSolicitacao(StatusSolicitacao.CONCLUIDA, criadaEm, agora, null);
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any())).thenReturn(List.of(concluida));

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    assertEquals(4L, output.slaGlobalMediaHoras());
  }

  @Test
  void deveRetornarSlaZeroSemConcluidas() {
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any())).thenReturn(List.of());

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, null));

    assertEquals(0L, output.slaGlobalMediaHoras());
  }

  @Test
  void deveFiltrarPorModeloId() {
    final Instant agora = Instant.now();
    final UUID outroModelo = UUID.randomUUID();
    final Solicitacao doModelo =
        criarSolicitacao(StatusSolicitacao.A_FAZER, agora.minus(1, ChronoUnit.DAYS), null, null);
    final Solicitacao outroModelo1 =
        criarSolicitacao(
            StatusSolicitacao.A_FAZER, agora.minus(1, ChronoUnit.DAYS), null, outroModelo);
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any()))
        .thenReturn(List.of(doModelo, outroModelo1));

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(7, modeloId));

    assertEquals(1, output.series().stream().mapToLong(s -> s.total()).sum());
  }

  @Test
  void deveTratarDiasZeroComoPadrao30() {
    when(solicitacaoRepository.findByCriadaEmBetween(any(), any())).thenReturn(List.of());

    final var output = useCase.execute(new ObterHistoricoMetricasUseCase.Input(0, null));

    assertEquals("Últimos 30 dias", output.periodoLabel());
    assertFalse(output.series().isEmpty());
  }
}
