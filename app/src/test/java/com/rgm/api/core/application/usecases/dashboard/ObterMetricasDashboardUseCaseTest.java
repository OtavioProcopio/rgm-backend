package com.rgm.api.core.application.usecases.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterMetricasDashboardUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private ModeloRepository modeloRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private ObterMetricasDashboardUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase =
        new ObterMetricasDashboardUseCase(
            solicitacaoRepository, usuarioRepository, modeloRepository, atividadeRepository);
  }

  @Test
  void deveRetornarMetricasComSucesso() {
    when(usuarioRepository.count()).thenReturn(5L);
    when(modeloRepository.count()).thenReturn(10L);
    when(solicitacaoRepository.count()).thenReturn(100L);

    for (final StatusSolicitacao status : StatusSolicitacao.values()) {
      when(solicitacaoRepository.countByStatus(status)).thenReturn(20L);
    }

    final UUID solId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Instant criada = agora.minus(10, ChronoUnit.HOURS);
    final Solicitacao sol =
        new Solicitacao(
            solId,
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "F",
            criada,
            agora,
            agora,
            null);

    when(solicitacaoRepository.findByStatus(StatusSolicitacao.CONCLUIDA)).thenReturn(List.of(sol));

    final AtividadeSolicitacao validacao =
        new AtividadeSolicitacao(
            UUID.randomUUID(),
            solId,
            TipoAtividadeSolicitacao.MUDANCA_STATUS,
            StatusSolicitacao.EM_ANDAMENTO,
            StatusSolicitacao.EM_VALIDACAO,
            null,
            UUID.randomUUID(),
            criada.plus(5, ChronoUnit.HOURS));

    final AtividadeSolicitacao conclusao =
        new AtividadeSolicitacao(
            UUID.randomUUID(),
            solId,
            TipoAtividadeSolicitacao.MUDANCA_STATUS,
            StatusSolicitacao.EM_VALIDACAO,
            StatusSolicitacao.CONCLUIDA,
            null,
            UUID.randomUUID(),
            criada.plus(8, ChronoUnit.HOURS));

    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of(validacao, conclusao));

    final ObterMetricasDashboardUseCase.DashboardMetricas metrics = useCase.execute();

    assertNotNull(metrics);
    assertEquals(5L, metrics.totalUsuarios());
    assertEquals(10L, metrics.totalModelos());
    assertEquals(100L, metrics.totalSolicitacoes());
    assertEquals(60L, metrics.totalSolicitacoesAbertas());
    assertEquals(20L, metrics.totalSolicitacoesConcluidas());
    assertEquals(10.0, metrics.tempoMedioResolucaoHoras());
    assertEquals(3.0, metrics.tempoMedioValidacaoHoras());
  }
}
