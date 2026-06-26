package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Caso de uso para obter métricas e KPIs consolidados do sistema. */
public final class ObterMetricasSolicitacoesUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final ModeloRepository modeloRepository;

  public ObterMetricasSolicitacoesUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.modeloRepository = modeloRepository;
  }

  public record Output(
      long totalUsuarios,
      long totalModelos,
      long totalSolicitacoes,
      Map<String, Long> solicitacoesPorStatus,
      long solicitacoesAbertas,
      long solicitacoesPendentes,
      long solicitacoesConcluidas,
      long tempoMedioResolucaoSegundos) {}

  public Output execute() {
    final long totalUsuarios = usuarioRepository.count();
    final long totalModelos = modeloRepository.count();
    final long totalSolicitacoes = solicitacaoRepository.count();

    final Map<String, Long> solicitacoesPorStatus = new HashMap<>();
    long solicitacoesAbertas = 0;
    long solicitacoesPendentes = 0;
    long solicitacoesConcluidas = 0;

    for (final StatusSolicitacao status : StatusSolicitacao.values()) {
      final long count = solicitacaoRepository.countByStatus(status);
      solicitacoesPorStatus.put(status.name(), count);

      if (status.isNaoTerminal()) {
        solicitacoesAbertas += count;
      }
      if (status == StatusSolicitacao.EM_VALIDACAO) {
        solicitacoesPendentes += count; // Em validação aguardando ação de gestores
      }
      if (status == StatusSolicitacao.CONCLUIDA) {
        solicitacoesConcluidas = count;
      }
    }

    // Calcular tempo médio de resolução (lead time) em segundos para maior precisão
    final List<Solicitacao> concluidas =
        solicitacaoRepository.findByStatus(StatusSolicitacao.CONCLUIDA);
    long totalSegundos = 0;
    int comTimestamp = 0;
    for (final Solicitacao s : concluidas) {
      if (s.getCriadaEm() != null && s.getConcluidaEm() != null) {
        totalSegundos += Duration.between(s.getCriadaEm(), s.getConcluidaEm()).toSeconds();
        comTimestamp++;
      }
    }
    final long tempoMedioResolucaoSegundos = comTimestamp == 0 ? 0 : (totalSegundos / comTimestamp);

    return new Output(
        totalUsuarios,
        totalModelos,
        totalSolicitacoes,
        solicitacoesPorStatus,
        solicitacoesAbertas,
        solicitacoesPendentes,
        solicitacoesConcluidas,
        tempoMedioResolucaoSegundos);
  }
}
