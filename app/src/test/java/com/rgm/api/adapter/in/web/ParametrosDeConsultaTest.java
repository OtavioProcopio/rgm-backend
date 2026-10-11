package com.rgm.api.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.OrdenacaoSolicitacao;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParametrosDeConsultaTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 20, 100})
  void shouldAceitarWhenTamanhoDaPaginaEstaNoIntervalo(final int size) {
    // Arrange
    final int page = 0;

    // Act
    final Executable chamada = () -> ParametrosDeConsulta.pagina(page, size);

    // Assert
    assertDoesNotThrow(chamada);
  }

  @Test
  void shouldRecusarWhenPaginaENegativa() {
    // Arrange
    final int page = -1;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, 20));

    // Assert
    assertTrue(erro.getMessage().contains("'page'"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5, 101, 5000})
  void shouldRecusarWhenTamanhoDaPaginaEstaForaDoIntervalo(final int size) {
    // Arrange
    final int page = 0;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, size));

    // Assert
    assertTrue(erro.getMessage().contains("'size'"));
    assertTrue(erro.getMessage().contains("entre 1 e 100"));
  }

  @Test
  void shouldDevolverInstanteWhenDataEstaEmIso8601() {
    // Arrange
    final String valor = "2026-10-10T12:30:00Z";

    // Act
    final Instant data = ParametrosDeConsulta.data("dataInicio", valor);

    // Assert
    assertEquals(Instant.parse(valor), data);
  }

  @Test
  void shouldDevolverNuloWhenDataNaoFoiInformada() {
    // Arrange
    final String valor = null;

    // Act
    final Instant data = ParametrosDeConsulta.data("dataInicio", valor);

    // Assert
    assertNull(data);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ontem", "32/13/2026", "xx", ""})
  void shouldRecusarWhenDataNaoEstaEmIso8601(final String valor) {
    // Arrange
    final String campo = "criadaEmInicio";

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data(campo, valor));

    // Assert
    assertTrue(erro.getMessage().contains("'criadaEmInicio'"));
    assertTrue(erro.getMessage().contains("ISO-8601"));
  }

  @Test
  void shouldDevolverConstanteWhenEnumeracaoEValida() {
    // Arrange
    final String valor = "EM_ANDAMENTO";

    // Act
    final StatusSolicitacao status =
        ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class);

    // Assert
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, status);
  }

  @Test
  void shouldDevolverNuloWhenEnumeracaoNaoFoiInformada() {
    // Arrange
    final String valor = null;

    // Act
    final StatusSolicitacao status =
        ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class);

    // Assert
    assertNull(status);
  }

  @Test
  void shouldDizerOCampoEOsValoresAceitosWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertTrue(erro.getMessage().contains("'status'"));
    assertTrue(erro.getMessage().contains("A_FAZER"));
    assertTrue(erro.getMessage().contains("CANCELADA"));
  }

  @Test
  void shouldNaoExporClasseInternaWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertFalse(erro.getMessage().contains("com.rgm"));
    assertFalse(erro.getMessage().contains("No enum constant"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenTamanhoDaPaginaEstaForaDoIntervalo() {
    // Arrange
    final int size = 5000;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(0, size));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: 5000"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenPaginaENegativa() {
    // Arrange
    final int page = -3;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, 20));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: -3"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenDataNaoEstaEmIso8601() {
    // Arrange
    final String valor = "ontem";

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data("dataFim", valor));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: ontem"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: INVALIDO"));
  }

  @ParameterizedTest
  @CsvSource({
    "criadaEm,CRIADA_EM",
    "prazoLimite,PRAZO_LIMITE",
    "prioridade,PRIORIDADE",
  })
  void shouldDevolverCriterioWhenSortEValido(final String sort, final String esperado) {
    // Arrange
    final String dir = "asc";

    // Act
    final OrdenacaoSolicitacao ordenacao = ParametrosDeConsulta.ordenacao(sort, dir);

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.valueOf(esperado), ordenacao.criterio());
  }

  @ParameterizedTest
  @CsvSource({"asc,true", "desc,false"})
  void shouldDevolverSentidoWhenDirEValido(final String dir, final boolean ascendente) {
    // Arrange
    final String sort = "prazoLimite";

    // Act
    final OrdenacaoSolicitacao ordenacao = ParametrosDeConsulta.ordenacao(sort, dir);

    // Assert
    assertEquals(ascendente, ordenacao.ascendente());
  }

  @Test
  void shouldDevolverOrdemPadraoWhenSortEDirNaoForamInformados() {
    // Arrange
    final String sort = null;
    final String dir = null;

    // Act
    final OrdenacaoSolicitacao ordenacao = ParametrosDeConsulta.ordenacao(sort, dir);

    // Assert
    assertEquals(OrdenacaoSolicitacao.PADRAO, ordenacao);
  }

  @Test
  void shouldAplicarSentidoACriacaoWhenSoDirFoiInformado() {
    // Arrange
    final String dir = "desc";

    // Act
    final OrdenacaoSolicitacao ordenacao = ParametrosDeConsulta.ordenacao(null, dir);

    // Assert
    assertEquals(
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.CRIADA_EM, false), ordenacao);
  }

  @Test
  void shouldDizerOParametroEOsValoresAceitosWhenSortEInvalido() {
    // Arrange
    final String sort = "titulo";

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.ordenacao(sort, "asc"));

    // Assert
    assertEquals(
        "Parâmetro 'sort' inválido (recebido: titulo)."
            + " Valores aceitos: criadaEm, prazoLimite, prioridade, concluidaEm",
        erro.getMessage());
  }

  @Test
  void shouldDizerOParametroEOsValoresAceitosWhenDirEInvalido() {
    // Arrange
    final String dir = "up";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class, () -> ParametrosDeConsulta.ordenacao("criadaEm", dir));

    // Assert
    assertEquals(
        "Parâmetro 'dir' inválido (recebido: up). Valores aceitos: asc, desc", erro.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 30, 365})
  void shouldAceitarWhenDiasDoHistoricoEstaEntre1E365(final int dias) {
    // Arrange
    final int valor = dias;

    // Act
    final Executable chamada = () -> ParametrosDeConsulta.diasDoHistorico(valor);

    // Assert
    assertDoesNotThrow(chamada);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 366})
  void shouldDizerOValorRecebidoEAFaixaWhenDiasDoHistoricoEstaFora(final int dias) {
    // Arrange
    final int valor = dias;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.diasDoHistorico(valor));

    // Assert
    assertEquals(
        "Parâmetro 'dias' deve estar entre 1 e 365 (recebido: " + dias + ")", erro.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {7, 30, 90})
  void shouldAceitarWhenPeriodoDoPainelE7Ou30Ou90(final int dias) {
    // Arrange
    final int valor = dias;

    // Act
    final Executable chamada = () -> ParametrosDeConsulta.periodoDoPainel(valor);

    // Assert
    assertDoesNotThrow(chamada);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 15, 91, -7})
  void shouldListarOs7E30E90WhenPeriodoDoPainelNaoEAceito(final int dias) {
    // Arrange
    final int valor = dias;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.periodoDoPainel(valor));

    // Assert
    assertEquals(
        "Parâmetro 'dias' inválido (recebido: " + dias + "). Valores aceitos: 7, 30, 90",
        erro.getMessage());
  }

  @Test
  void shouldTruncarOValorRecebidoWhenEleEMuitoLongo() {
    // Arrange
    final String valor = "x".repeat(200);

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data("dataFim", valor));

    // Assert
    assertFalse(erro.getMessage().contains("x".repeat(51)));
    assertTrue(erro.getMessage().contains("x".repeat(50) + "…"));
  }

  @Test
  void shouldMapConcluidaEmToEncerradaEmWhenSortIsConcluidaEm() {
    // Arrange
    final String sort = "concluidaEm";

    // Act
    final OrdenacaoSolicitacao ordenacao = ParametrosDeConsulta.ordenacao(sort, "desc");

    // Assert
    assertEquals(CriterioOrdenacaoSolicitacao.ENCERRADA_EM, ordenacao.criterio());
    assertFalse(ordenacao.ascendente());
  }

  @Test
  void shouldReturnNullWhenBuscaIsMissingOrBlank() {
    // Arrange
    final String vazio = "   ";

    // Act
    final String ausente = ParametrosDeConsulta.busca("q", null);
    final String branco = ParametrosDeConsulta.busca("q", vazio);

    // Assert
    assertNull(ausente);
    assertNull(branco);
  }

  @Test
  void shouldTrimBuscaWhenWithinLimits() {
    // Arrange
    final String valor = "  correia  ";

    // Act
    final String termo = ParametrosDeConsulta.busca("q", valor);

    // Assert
    assertEquals("correia", termo);
  }

  @Test
  void shouldRejectBuscaWhenOneCharacterOrOverHundred() {
    // Arrange
    final String curta = "a";
    final String longa = "a".repeat(101);

    // Act
    final ValidationException curtaErro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.busca("q", curta));
    final ValidationException longaErro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.busca("busca", longa));

    // Assert
    assertEquals(
        "Parâmetro 'q' deve ter de 2 a 100 caracteres (recebido: 1)", curtaErro.getMessage());
    assertEquals(
        "Parâmetro 'busca' deve ter de 2 a 100 caracteres (recebido: 101)", longaErro.getMessage());
  }
}
