package com.rgm.api.core.domain.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CriterioOrdenacaoSolicitacaoTest {

  @Test
  void shouldExporExatamenteQuatroCriteriosNaOrdemDeclaradaWhenListaOsValores() {
    // Arrange
    final List<CriterioOrdenacaoSolicitacao> esperados =
        List.of(
            CriterioOrdenacaoSolicitacao.CRIADA_EM,
            CriterioOrdenacaoSolicitacao.PRAZO_LIMITE,
            CriterioOrdenacaoSolicitacao.PRIORIDADE,
            CriterioOrdenacaoSolicitacao.ENCERRADA_EM);

    // Act
    final List<CriterioOrdenacaoSolicitacao> valores =
        List.of(CriterioOrdenacaoSolicitacao.values());

    // Assert
    assertEquals(esperados, valores);
  }

  @Test
  void shouldResolverCriadaEmWhenNomeECriadaEm() {
    // Arrange
    final String nome = "CRIADA_EM";

    // Act
    final CriterioOrdenacaoSolicitacao criterio = CriterioOrdenacaoSolicitacao.valueOf(nome);

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.CRIADA_EM, criterio);
  }

  @Test
  void shouldResolverPrazoLimiteWhenNomeEPrazoLimite() {
    // Arrange
    final String nome = "PRAZO_LIMITE";

    // Act
    final CriterioOrdenacaoSolicitacao criterio = CriterioOrdenacaoSolicitacao.valueOf(nome);

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, criterio);
  }

  @Test
  void shouldResolverPrioridadeWhenNomeEPrioridade() {
    // Arrange
    final String nome = "PRIORIDADE";

    // Act
    final CriterioOrdenacaoSolicitacao criterio = CriterioOrdenacaoSolicitacao.valueOf(nome);

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.PRIORIDADE, criterio);
  }
}
