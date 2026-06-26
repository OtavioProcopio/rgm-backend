package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterMetricasSolicitacoesUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private ModeloRepository modeloRepository;
  private ObterMetricasSolicitacoesUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    useCase =
        new ObterMetricasSolicitacoesUseCase(
            solicitacaoRepository, usuarioRepository, modeloRepository);
  }

  @Test
  void deveCalcularMetricasCorretamente() {
    // GIVEN
    when(usuarioRepository.count()).thenReturn(10L);
    when(modeloRepository.count()).thenReturn(15L);
    when(solicitacaoRepository.count()).thenReturn(20L);

    // Contagem por status
    when(solicitacaoRepository.countByStatus(StatusSolicitacao.A_FAZER)).thenReturn(8L);
    when(solicitacaoRepository.countByStatus(StatusSolicitacao.EM_ANDAMENTO)).thenReturn(4L);
    when(solicitacaoRepository.countByStatus(StatusSolicitacao.EM_VALIDACAO)).thenReturn(3L);
    when(solicitacaoRepository.countByStatus(StatusSolicitacao.CONCLUIDA)).thenReturn(4L);
    when(solicitacaoRepository.countByStatus(StatusSolicitacao.CANCELADA)).thenReturn(1L);

    // Criação de solicitações concluídas para cálculo do SLA
    final UUID modeloId = UUID.randomUUID();
    final UUID abertaPor = UUID.randomUUID();
    final Instant agora = Instant.now();

    final Instant criada1 = agora.minus(120, ChronoUnit.MINUTES);
    final Solicitacao sol1 =
        new Solicitacao(
            UUID.randomUUID(),
            "Sol 1",
            "Desc 1",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.MEDIA,
            modeloId,
            abertaPor,
            "Comentario 1",
            criada1,
            agora,
            agora,
            null);

    final Instant criada2 = agora.minus(60, ChronoUnit.MINUTES);
    final Solicitacao sol2 =
        new Solicitacao(
            UUID.randomUUID(),
            "Sol 2",
            "Desc 2",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.MEDIA,
            modeloId,
            abertaPor,
            "Comentario 2",
            criada2,
            agora,
            agora,
            null);

    when(solicitacaoRepository.findByStatus(StatusSolicitacao.CONCLUIDA))
        .thenReturn(List.of(sol1, sol2));

    // WHEN
    final ObterMetricasSolicitacoesUseCase.Output metricas = useCase.execute();

    // THEN
    assertEquals(10L, metricas.totalUsuarios());
    assertEquals(15L, metricas.totalModelos());
    assertEquals(20L, metricas.totalSolicitacoes());

    // Abertas: A_FAZER (8) + EM_ANDAMENTO (4) + EM_VALIDACAO (3) = 15
    assertEquals(15L, metricas.solicitacoesAbertas());
    assertEquals(3L, metricas.solicitacoesPendentes());
    assertEquals(4L, metricas.solicitacoesConcluidas());

    // Lead time médio: (120min + 60min) / 2 = 90min = 5400 segundos
    assertEquals(5400L, metricas.tempoMedioResolucaoSegundos());

    // Distribuição por status
    assertEquals(8L, metricas.solicitacoesPorStatus().get("A_FAZER"));
    assertEquals(4L, metricas.solicitacoesPorStatus().get("EM_ANDAMENTO"));
    assertEquals(3L, metricas.solicitacoesPorStatus().get("EM_VALIDACAO"));
    assertEquals(4L, metricas.solicitacoesPorStatus().get("CONCLUIDA"));
    assertEquals(1L, metricas.solicitacoesPorStatus().get("CANCELADA"));

    verify(usuarioRepository).count();
    verify(modeloRepository).count();
    verify(solicitacaoRepository).count();
    verify(solicitacaoRepository).findByStatus(StatusSolicitacao.CONCLUIDA);
  }

  @Test
  void deveRetornarSlaZeroSeNaoHouverSolicitacoesConcluidas() {
    // GIVEN
    when(solicitacaoRepository.findByStatus(StatusSolicitacao.CONCLUIDA)).thenReturn(List.of());

    // WHEN
    final ObterMetricasSolicitacoesUseCase.Output metricas = useCase.execute();

    // THEN
    assertEquals(0L, metricas.tempoMedioResolucaoSegundos());
  }
}
