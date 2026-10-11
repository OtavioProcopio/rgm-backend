package com.rgm.api.core.domain.ports.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;
import org.junit.jupiter.api.Test;

class OrdenacaoSolicitacaoTest {

  @Test
  void shouldOrdenarPorCriacaoWhenOrdenacaoEPadrao() {
    // Arrange
    final OrdenacaoSolicitacao padrao = OrdenacaoSolicitacao.PADRAO;

    // Act
    final CriterioOrdenacaoSolicitacao criterio = padrao.criterio();

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.CRIADA_EM, criterio);
  }

  @Test
  void shouldSerCrescenteWhenOrdenacaoEPadrao() {
    // Arrange
    final OrdenacaoSolicitacao padrao = OrdenacaoSolicitacao.PADRAO;

    // Act
    final boolean ascendente = padrao.ascendente();

    // Assert
    assertTrue(ascendente);
  }

  @Test
  void shouldSerIgualWhenCriterioESentidoSaoOsMesmos() {
    // Arrange
    final OrdenacaoSolicitacao a =
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.PRIORIDADE, false);
    final OrdenacaoSolicitacao b =
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.PRIORIDADE, false);

    // Act
    final boolean iguais = a.equals(b);

    // Assert
    assertTrue(iguais);
  }

  @Test
  void shouldSerDiferenteWhenSentidoMuda() {
    // Arrange
    final OrdenacaoSolicitacao crescente =
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, true);

    // Act
    final OrdenacaoSolicitacao decrescente =
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, false);

    // Assert
    assertNotEquals(crescente, decrescente);
  }
}
