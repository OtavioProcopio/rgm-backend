package com.rgm.api.core.domain.ports.repositories;

import java.time.LocalDate;
import java.util.List;

/**
 * Linhas agregadas do painel por periodo para uma janela [inicio, fim): os totais da janela e a
 * serie, um ponto por dia local (ou por semana, de segunda a domingo). Nenhuma solicitacao
 * atravessa a porta; so numeros.
 */
public record PainelAgregado(Totais totais, List<Ponto> serie) {

  /**
   * Totais da janela: criadas pela data de criacao, concluidas pela de conclusao e canceladas pela
   * de cancelamento; o tempo medio, em segundos, e das concluidas na janela e nulo sem elas.
   */
  public record Totais(
      long criadas, long concluidas, long canceladas, Double tempoMedioResolucaoSegundos) {}

  /**
   * Ponto da serie. {@code periodo} e o dia local ou a segunda-feira da semana; as aberta e
   * atrasadas sao as do fim do periodo (ultimo dia dele dentro da janela), com a prioridade de
   * hoje.
   */
  public record Ponto(
      LocalDate periodo,
      long criadas,
      long concluidas,
      long canceladas,
      long abertasAoFimDoDia,
      long atrasadasAoFimDoDia) {}

  /** Solicitacoes em aberto e em atraso no instante pedido; independe da janela. */
  public record Agora(long abertas, long atrasadas) {}
}
