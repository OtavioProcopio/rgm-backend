package com.rgm.api.core.domain.ports.repositories;

import java.time.Duration;
import java.time.Instant;

/**
 * Resumo das solicitacoes de um modelo, com a mesma regra de tempos do ranking de modelos:
 * `tempoMedioResolucaoSegundos` e a media das concluidas (nulo sem concluida) e
 * `intervaloMedioSegundos` considera a abertura de todas as solicitacoes (nulo com menos de 2).
 */
public record ResumoSolicitacoesModelo(
    long total,
    long emAberto,
    long concluidas,
    long canceladas,
    Double tempoMedioResolucaoSegundos,
    Double intervaloMedioSegundos) {

  /**
   * A media dos intervalos entre aberturas consecutivas e igual ao tempo entre a primeira e a
   * ultima abertura dividido pela quantidade de intervalos.
   */
  public static ResumoSolicitacoesModelo of(
      final long total,
      final long emAberto,
      final long concluidas,
      final long canceladas,
      final Double tempoMedioResolucaoSegundos,
      final Instant primeiraAbertura,
      final Instant ultimaAbertura) {
    final Double intervalo =
        total < 2 || primeiraAbertura == null || ultimaAbertura == null
            ? null
            : Duration.between(primeiraAbertura, ultimaAbertura).toMillis() / 1000.0 / (total - 1);
    return new ResumoSolicitacoesModelo(
        total, emAberto, concluidas, canceladas, tempoMedioResolucaoSegundos, intervalo);
  }
}
