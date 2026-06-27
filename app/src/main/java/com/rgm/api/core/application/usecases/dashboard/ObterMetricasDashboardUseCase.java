package com.rgm.api.core.application.usecases.dashboard;

import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ObterMetricasDashboardUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final ModeloRepository modeloRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public ObterMetricasDashboardUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.modeloRepository = modeloRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record DashboardMetricas(
      long totalUsuarios,
      long totalModelos,
      long totalSolicitacoes,
      long totalSolicitacoesAbertas,
      long totalSolicitacoesConcluidas,
      Map<StatusSolicitacao, Long> solicitacoesPorStatus,
      double tempoMedioResolucaoHoras,
      double tempoMedioValidacaoHoras) {}

  public DashboardMetricas execute() {
    final long totalUsuarios = usuarioRepository.count();
    final long totalModelos = modeloRepository.count();
    final long totalSolicitacoes = solicitacaoRepository.count();

    final Map<StatusSolicitacao, Long> statusCounts = new EnumMap<>(StatusSolicitacao.class);
    long abertas = 0;
    for (final StatusSolicitacao status : StatusSolicitacao.values()) {
      final long c = solicitacaoRepository.countByStatus(status);
      statusCounts.put(status, c);
      if (!status.isTerminal()) {
        abertas += c;
      }
    }

    final long totalSolicitacoesConcluidas =
        statusCounts.getOrDefault(StatusSolicitacao.CONCLUIDA, 0L);

    final List<com.rgm.api.core.domain.model.aggregates.Solicitacao> concluidas =
        solicitacaoRepository.findByStatus(StatusSolicitacao.CONCLUIDA);

    double tempoMedioResolucaoHoras = 0.0;
    if (!concluidas.isEmpty()) {
      long totalDurationHours = 0;
      for (final var sol : concluidas) {
        if (sol.getConcluidaEm() != null && sol.getCriadaEm() != null) {
          totalDurationHours += Duration.between(sol.getCriadaEm(), sol.getConcluidaEm()).toHours();
        }
      }
      tempoMedioResolucaoHoras = (double) totalDurationHours / concluidas.size();
    }

    double tempoMedioValidacaoHoras = 0.0;
    int countValidacao = 0;
    double totalValidacaoHours = 0.0;

    for (final var sol : concluidas) {
      final List<AtividadeSolicitacao> atividades =
          atividadeRepository.findBySolicitacaoId(sol.getId());
      Instant enteredValidacao = null;
      Instant enteredConcluida = null;
      for (final var atividade : atividades) {
        if (atividade.getParaStatus() == StatusSolicitacao.EM_VALIDACAO) {
          enteredValidacao = atividade.getCriadaEm();
        } else if (atividade.getParaStatus() == StatusSolicitacao.CONCLUIDA) {
          enteredConcluida = atividade.getCriadaEm();
        }
      }
      if (enteredValidacao != null
          && enteredConcluida != null
          && enteredConcluida.isAfter(enteredValidacao)) {
        totalValidacaoHours += Duration.between(enteredValidacao, enteredConcluida).toHours();
        countValidacao++;
      }
    }
    if (countValidacao > 0) {
      tempoMedioValidacaoHoras = totalValidacaoHours / countValidacao;
    }

    return new DashboardMetricas(
        totalUsuarios,
        totalModelos,
        totalSolicitacoes,
        abertas,
        totalSolicitacoesConcluidas,
        statusCounts,
        tempoMedioResolucaoHoras,
        tempoMedioValidacaoHoras);
  }
}
