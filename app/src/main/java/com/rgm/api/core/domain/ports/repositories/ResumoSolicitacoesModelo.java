package com.rgm.api.core.domain.ports.repositories;

import java.time.Duration;
import java.time.Instant;

/**
 * Resumo das solicitacoes de um modelo. A criacao conta no `total`, mas fica fora dos tempos:
 * `tempoMedioResolucaoSegundos` e a media das concluidas que nao sao criacao (nulo sem elas) e
 * `intervaloMedioSegundos` considera a abertura das manutencoes (nulo com menos de 2).
 */
public record ResumoSolicitacoesModelo(
    long total,
    long emAberto,
    long concluidas,
    long canceladas,
    Double tempoMedioResolucaoSegundos,
    Double intervaloMedioSegundos) {

  /**
   * A media dos intervalos entre aberturas consecutivas de manutencao e igual ao tempo entre a
   * primeira e a ultima abertura dividido pela quantidade de intervalos (manutencoes - 1).
   */
  public static ResumoSolicitacoesModelo of(
      final long total,
      final long emAberto,
      final long concluidas,
      final long canceladas,
      final Double tempoMedioResolucaoSegundos,
      final long manutencoes,
      final Instant primeiraAbertura,
      final Instant ultimaAbertura) {
    final Double intervalo =
        manutencoes < 2 || primeiraAbertura == null || ultimaAbertura == null
            ? null
            : Duration.between(primeiraAbertura, ultimaAbertura).toMillis()
                / 1000.0
                / (manutencoes - 1);
    return new ResumoSolicitacoesModelo(
        total, emAberto, concluidas, canceladas, tempoMedioResolucaoSegundos, intervalo);
  }
}
