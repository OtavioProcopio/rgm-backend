package com.rgm.api.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guarda da copia do agregado: cada transicao devolve uma nova {@link Solicitacao} e so pode mudar
 * os campos que declara. A comparacao e recursiva sobre TODOS os campos, entao um campo
 * acrescentado ao agregado e esquecido numa copia faz estes testes falharem.
 */
class SolicitacaoCopiaTest {

  private static final Instant CRIADA = Instant.parse("2026-01-01T10:00:00Z");
  private static final Instant ATUALIZADA = Instant.parse("2026-01-02T10:00:00Z");
  private static final Instant AGORA = CRIADA.plus(5, ChronoUnit.DAYS);
  private static final Long VERSION = 7L;
  private static final UUID ID = UUID.randomUUID();
  private static final UUID MODELO = UUID.randomUUID();
  private static final UUID AUTOR = UUID.randomUUID();

  /** REPARO totalmente preenchido para o status pedido; todo campo opcional tem valor. */
  private static Solicitacao reparo(final StatusSolicitacao status) {
    final boolean concluida = status == StatusSolicitacao.CONCLUIDA;
    final boolean cancelada = status == StatusSolicitacao.CANCELADA;
    return Solicitacao.builder()
        .id(ID)
        .titulo("Titulo original")
        .descricao("Descricao original")
        .tipo(TipoSolicitacao.REPARO)
        .status(status)
        .prioridade(status == StatusSolicitacao.A_FAZER ? null : PrioridadeSolicitacao.MEDIA)
        .modeloId(MODELO)
        .modeloCodigo(null)
        .modeloMaquina(null)
        .modeloObservacoes(null)
        .abertaPorUsuarioId(AUTOR)
        .comentarioFinal(concluida || cancelada ? "Comentario final" : null)
        .criadaEm(CRIADA)
        .atualizadaEm(ATUALIZADA)
        .concluidaEm(concluida ? ATUALIZADA : null)
        .canceladaEm(cancelada ? ATUALIZADA : null)
        .version(VERSION)
        .build();
  }

  /** CRIACAO totalmente preenchida: so aqui existem os dados de modelo em preparacao. */
  private static Solicitacao criacao(final StatusSolicitacao status) {
    final boolean concluida = status == StatusSolicitacao.CONCLUIDA;
    final boolean cancelada = status == StatusSolicitacao.CANCELADA;
    return Solicitacao.builder()
        .id(ID)
        .titulo("Titulo original")
        .descricao("Descricao original")
        .tipo(TipoSolicitacao.CRIACAO)
        .status(status)
        .prioridade(status == StatusSolicitacao.A_FAZER ? null : PrioridadeSolicitacao.MEDIA)
        .modeloId(concluida ? MODELO : null)
        .modeloCodigo("COD-1")
        .modeloMaquina("FBOX")
        .modeloObservacoes("Observacoes do modelo")
        .abertaPorUsuarioId(AUTOR)
        .comentarioFinal(concluida || cancelada ? "Comentario final" : null)
        .criadaEm(CRIADA)
        .atualizadaEm(ATUALIZADA)
        .concluidaEm(concluida ? ATUALIZADA : null)
        .canceladaEm(cancelada ? ATUALIZADA : null)
        .version(VERSION)
        .build();
  }

  /** Os dois tipos de solicitacao: cada um preenche campos que o outro nao pode ter. */
  static Stream<Arguments> tipos() {
    return Stream.of(
        Arguments.of(
            "REPARO", (Function<StatusSolicitacao, Solicitacao>) SolicitacaoCopiaTest::reparo),
        Arguments.of(
            "CRIACAO", (Function<StatusSolicitacao, Solicitacao>) SolicitacaoCopiaTest::criacao));
  }

  private static void assertSoMudou(
      final Solicitacao antes, final Solicitacao depois, final String... campos) {
    assertThat(depois).isNotSameAs(antes);
    assertThat(depois).usingRecursiveComparison().ignoringFields(campos).isEqualTo(antes);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldChangeOnlyStatusPrioridadeAndAtualizadaEmWhenTriar(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.A_FAZER);

    // Act
    final Solicitacao depois = antes.triar(PrioridadeSolicitacao.ALTA, AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "prioridade", "atualizadaEm");
    assertThat(depois.getStatus()).isEqualTo(StatusSolicitacao.EM_ANDAMENTO);
    assertThat(depois.getPrioridade()).isEqualTo(PrioridadeSolicitacao.ALTA);
    assertThat(depois.getAtualizadaEm()).isEqualTo(AGORA);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldChangeOnlyStatusAndAtualizadaEmWhenEnviarParaValidacao(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Solicitacao depois = antes.enviarParaValidacao(AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "atualizadaEm");
    assertThat(depois.getStatus()).isEqualTo(StatusSolicitacao.EM_VALIDACAO);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldKeepPrioridadeWhenDevolverWithoutNovaPrioridade(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_VALIDACAO);

    // Act
    final Solicitacao depois = antes.devolver(null, AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "atualizadaEm");
    assertThat(depois.getStatus()).isEqualTo(StatusSolicitacao.EM_ANDAMENTO);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldChangePrioridadeTooWhenDevolverWithNovaPrioridade(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_VALIDACAO);

    // Act
    final Solicitacao depois = antes.devolver(PrioridadeSolicitacao.URGENTE, AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "prioridade", "atualizadaEm");
    assertThat(depois.getPrioridade()).isEqualTo(PrioridadeSolicitacao.URGENTE);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldChangeOnlyTituloDescricaoAndAtualizadaEmWhenEditar(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Solicitacao depois = antes.editar("Novo titulo", "Nova descricao", null, AGORA);

    // Assert
    assertSoMudou(antes, depois, "titulo", "descricao", "atualizadaEm");
    assertThat(depois.getTitulo()).isEqualTo("Novo titulo");
    assertThat(depois.getDescricao()).isEqualTo("Nova descricao");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldChangeOnlyAtualizadaEmWhenRegistrarAlteracaoDeResponsaveis(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Solicitacao depois = antes.registrarAlteracaoDeResponsaveis(AGORA);

    // Assert
    assertSoMudou(antes, depois, "atualizadaEm");
    assertThat(depois.getAtualizadaEm()).isEqualTo(AGORA);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldKeepTerminalFieldsWhenRegistrarAlteracaoDeResponsaveisOnConcluida(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.CONCLUIDA);

    // Act
    final Solicitacao depois = antes.registrarAlteracaoDeResponsaveis(AGORA);

    // Assert
    assertSoMudou(antes, depois, "atualizadaEm");
    assertThat(depois.getComentarioFinal()).isEqualTo("Comentario final");
    assertThat(depois.getConcluidaEm()).isEqualTo(ATUALIZADA);
  }

  @Test
  void shouldSetComentarioFinalAndConcluidaEmWhenConcluir() {
    // Arrange
    final Solicitacao antes = reparo(StatusSolicitacao.EM_VALIDACAO);

    // Act
    final Solicitacao depois = antes.concluir("Servico feito", AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "comentarioFinal", "atualizadaEm", "concluidaEm");
    assertThat(depois.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
    assertThat(depois.getComentarioFinal()).isEqualTo("Servico feito");
    assertThat(depois.getConcluidaEm()).isEqualTo(AGORA);
  }

  @Test
  void shouldLinkModeloAndSetComentarioFinalWhenConcluirCriacao() {
    // Arrange
    final Solicitacao antes = criacao(StatusSolicitacao.EM_VALIDACAO);
    final UUID modeloCriado = UUID.randomUUID();

    // Act
    final Solicitacao depois = antes.concluirCriacao("Modelo criado", modeloCriado, AGORA);

    // Assert
    assertSoMudou(
        antes, depois, "status", "modeloId", "comentarioFinal", "atualizadaEm", "concluidaEm");
    assertThat(depois.getModeloId()).isEqualTo(modeloCriado);
    assertThat(depois.getModeloCodigo()).isEqualTo("COD-1");
    assertThat(depois.getModeloObservacoes()).isEqualTo("Observacoes do modelo");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tipos")
  void shouldSetComentarioFinalAndCanceladaEmWhenCancelar(
      final String tipo, final Function<StatusSolicitacao, Solicitacao> fabrica) {
    // Arrange
    final Solicitacao antes = fabrica.apply(StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Solicitacao depois = antes.cancelar("Nao precisa mais", AGORA);

    // Assert
    assertSoMudou(antes, depois, "status", "comentarioFinal", "atualizadaEm", "canceladaEm");
    assertThat(depois.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA);
    assertThat(depois.getCanceladaEm()).isEqualTo(AGORA);
  }
}
