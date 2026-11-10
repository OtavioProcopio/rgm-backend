package com.rgm.api.core.domain.model;

import static com.rgm.api.support.SolicitacaoTestBuilder.umaSolicitacao;
import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.ConflitoException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SolicitacaoTest {

  private final Instant agora = Instant.now();
  private final UUID modeloId = UUID.randomUUID();
  private final UUID usuarioId = UUID.randomUUID();

  @Test
  void deveAbrirComStatusAFazer() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertEquals(StatusSolicitacao.A_FAZER, sol.getStatus());
    assertNull(sol.getPrioridade());
    assertNotNull(sol.getId());
  }

  @Test
  void deveTriarComSucesso() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.ALTA, "Trocar o gabarito", agora);

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, triada.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, triada.getPrioridade());
  }

  @Test
  void deveFalharTransicaoInvalida() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertThrows(TransicaoStatusInvalidaException.class, () -> sol.enviarParaValidacao(agora));
  }

  @Test
  void deveConcluir() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao concluida = emValidacao.concluir("Feito", agora);

    assertEquals(StatusSolicitacao.CONCLUIDA, concluida.getStatus());
    assertNotNull(concluida.getConcluidaEm());
    assertEquals("Feito", concluida.getComentarioFinal());
  }

  @Test
  void deveCancelar() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao cancelada = emValidacao.cancelar("Motivo", agora);

    assertEquals(StatusSolicitacao.CANCELADA, cancelada.getStatus());
    assertNotNull(cancelada.getCanceladaEm());
  }

  @Test
  void deveFalharSemTitulo() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Solicitacao.abrir("", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora));
  }

  @Test
  void deveDevolver() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao devolvida = emValidacao.devolver(PrioridadeSolicitacao.ALTA, agora);

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, devolvida.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, devolvida.getPrioridade());
  }

  @Test
  void prazoLimiteENuloAntesDaTriagem() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertNull(sol.getPrazoLimite());
    assertNull(sol.getTempoRestanteSegundos(agora));
    assertFalse(sol.isAtrasada(agora));
    assertNull(sol.getTempoResolucaoSegundos());
  }

  @Test
  void prazoLimiteEComputadoAPartirDaPrioridadeAposTriagem() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, "Trocar o gabarito", agora);

    assertEquals(agora.plusSeconds(4 * 3600L), triada.getPrazoLimite());
    assertFalse(triada.isAtrasada(agora));
  }

  @Test
  void tempoRestanteFicaNegativoQuandoPrazoVenceu() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, "Trocar o gabarito", agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);

    assertTrue(triada.getTempoRestanteSegundos(depoisDoPrazo) < 0);
    assertTrue(triada.isAtrasada(depoisDoPrazo));
  }

  @Test
  void tempoRestanteENuloEmStatusTerminal() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);
    final Solicitacao concluida = emValidacao.concluir("Feito", depoisDoPrazo);

    assertNull(concluida.getTempoRestanteSegundos(depoisDoPrazo));
    assertTrue(concluida.isAtrasada(depoisDoPrazo));
    assertEquals(5 * 3600L, concluida.getTempoResolucaoSegundos());
  }

  @Test
  void canceladaNuncaContaComoAtrasada() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, "Trocar o gabarito", agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);
    final Solicitacao cancelada = triada.cancelar("Motivo", depoisDoPrazo);

    assertFalse(cancelada.isAtrasada(depoisDoPrazo));
  }

  @Test
  void abrirCriacaoNaoTemModeloVinculado() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao pretendida", "COD-XYZ", "FBOX", null, usuarioId, agora);

    assertEquals(TipoSolicitacao.CRIACAO, sol.getTipo());
    assertEquals(StatusSolicitacao.A_FAZER, sol.getStatus());
    assertNull(sol.getModeloId());
    assertEquals("COD-XYZ", sol.getModeloCodigo());
    assertEquals("FBOX", sol.getModeloMaquina());
  }

  @Test
  void abrirNaoCriacaoExigeModeloId() {
    assertThrows(
        ValidationException.class,
        () ->
            Solicitacao.builder()
                .id(UUID.randomUUID())
                .titulo("T")
                .descricao("D")
                .tipo(TipoSolicitacao.REPARO)
                .status(StatusSolicitacao.A_FAZER)
                .abertaPorUsuarioId(usuarioId)
                .criadaEm(agora)
                .atualizadaEm(agora)
                .build());
  }

  @Test
  void criacaoExigeCodigoEMaquina() {
    assertThrows(
        ValidationException.class,
        () ->
            Solicitacao.builder()
                .id(UUID.randomUUID())
                .titulo("T")
                .descricao("D")
                .tipo(TipoSolicitacao.CRIACAO)
                .status(StatusSolicitacao.A_FAZER)
                .abertaPorUsuarioId(usuarioId)
                .criadaEm(agora)
                .atualizadaEm(agora)
                .build());
  }

  @Test
  void criacaoNaoPodeTerModeloIdAntesDeConcluida() {
    assertThrows(
        ValidationException.class,
        () ->
            Solicitacao.builder()
                .id(UUID.randomUUID())
                .titulo("T")
                .descricao("D")
                .tipo(TipoSolicitacao.CRIACAO)
                .status(StatusSolicitacao.A_FAZER)
                .modeloId(modeloId)
                .modeloCodigo("COD")
                .modeloMaquina("FBOX")
                .abertaPorUsuarioId(usuarioId)
                .criadaEm(agora)
                .atualizadaEm(agora)
                .build());
  }

  @Test
  void concluirCriacaoVinculaOModeloRecemCriado() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao", "COD-XYZ", "FBOX", null, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final UUID modeloRecemCriado = UUID.randomUUID();

    final Solicitacao concluida =
        emValidacao.concluirCriacao("Modelo criado", modeloRecemCriado, agora);

    assertEquals(StatusSolicitacao.CONCLUIDA, concluida.getStatus());
    assertEquals(modeloRecemCriado, concluida.getModeloId());
  }

  @Test
  void concluirNaoSeAplicaAoTipoCriacao() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao", "COD-XYZ", "FBOX", null, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);

    assertThrows(BusinessRuleException.class, () -> emValidacao.concluir("Feito", agora));
  }

  @Test
  void concluirCriacaoNaoSeAplicaAOutrosTipos() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, "Trocar o gabarito", agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);

    assertThrows(
        BusinessRuleException.class,
        () -> emValidacao.concluirCriacao("Feito", UUID.randomUUID(), agora));
  }

  @Test
  void editarRejeitaMudancaDeTipo() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertThrows(
        BusinessRuleException.class,
        () -> sol.editar("Novo titulo", "Nova desc", TipoSolicitacao.INSPECAO, agora));
  }

  @Test
  void editarPermiteManterOMesmoTipo() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    final Solicitacao editada =
        sol.editar("Novo titulo", "Nova desc", TipoSolicitacao.REPARO, agora);

    assertEquals("Novo titulo", editada.getTitulo());
    assertEquals(TipoSolicitacao.REPARO, editada.getTipo());
  }

  @Test
  void shouldOnlyChangeUpdatedAtWhenRegisteringResponsaveisChange() {
    // Arrange
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Instant depois = agora.plusSeconds(60);

    // Act
    final Solicitacao alterada = sol.registrarAlteracaoDeResponsaveis(depois);

    // Assert
    assertEquals(depois, alterada.getAtualizadaEm());
    assertEquals(sol.getId(), alterada.getId());
    assertEquals(sol.getTitulo(), alterada.getTitulo());
    assertEquals(sol.getStatus(), alterada.getStatus());
    assertEquals(sol.getCriadaEm(), alterada.getCriadaEm());
  }

  @Test
  void gestorPodeAbrirCriacao() {
    assertDoesNotThrow(() -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.GESTOR));
    assertDoesNotThrow(
        () -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.ADMINISTRADOR));
  }

  @Test
  void operadorNaoPodeAbrirCriacao() {
    assertThrows(
        NaoAutorizadoException.class,
        () -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.OPERADOR));
  }

  // ── instrucao: triagem e correcao ─────────────────────────────────────────

  private Solicitacao aFazer() {
    return umaSolicitacao().aFazer().build();
  }

  @Test
  void shouldRecordTrimmedInstrucaoWhenTriar() {
    // Act
    final Solicitacao triada =
        aFazer().triar(PrioridadeSolicitacao.ALTA, "  Trocar o gabarito  ", agora);

    // Assert
    assertEquals("Trocar o gabarito", triada.getInstrucao());
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, triada.getStatus());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\n\t "})
  void shouldFailWhenTriarInstrucaoIsBlank(final String instrucao) {
    // Arrange
    final Solicitacao sol = aFazer();

    // Act + Assert
    assertThrows(
        ValidationException.class, () -> sol.triar(PrioridadeSolicitacao.ALTA, instrucao, agora));
  }

  @Test
  void shouldAcceptInstrucaoWithExactlyTheLimitAndRejectOneMore() {
    // Arrange
    final Solicitacao sol = aFazer();

    // Act + Assert
    assertEquals(
        2000,
        sol.triar(PrioridadeSolicitacao.ALTA, "x".repeat(2000), agora).getInstrucao().length());
    assertThrows(
        ValidationException.class,
        () -> sol.triar(PrioridadeSolicitacao.ALTA, "x".repeat(2001), agora));
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusSolicitacao.class,
      names = {"EM_ANDAMENTO", "EM_VALIDACAO", "CONCLUIDA", "CANCELADA"})
  void shouldNotTriarOutsideAFazerAndKeepTheInstrucao(final StatusSolicitacao status) {
    // Arrange
    final Solicitacao sol =
        umaSolicitacao().noStatus(status).comInstrucao("Instrucao original").build();

    // Act + Assert
    assertThrows(
        TransicaoStatusInvalidaException.class,
        () -> sol.triar(PrioridadeSolicitacao.URGENTE, "Outra instrucao", agora));
    assertEquals("Instrucao original", sol.getInstrucao());
  }

  @Test
  void shouldNotAllowInstrucaoWhileAFazer() {
    // Arrange
    final Solicitacao.Builder builder =
        Solicitacao.builder()
            .id(UUID.randomUUID())
            .titulo("T")
            .descricao("D")
            .tipo(TipoSolicitacao.REPARO)
            .status(StatusSolicitacao.A_FAZER)
            .modeloId(modeloId)
            .abertaPorUsuarioId(usuarioId)
            .instrucao("Nao pode existir antes da triagem")
            .criadaEm(agora)
            .atualizadaEm(agora);

    // Act + Assert
    assertThrows(ValidationException.class, builder::build);
  }

  @Test
  void shouldReplaceTrimmedInstrucaoWhenEditarInstrucaoEmAndamento() {
    // Arrange
    final Solicitacao antes = umaSolicitacao().emAndamento().comInstrucao("Antiga").build();

    // Act
    final Solicitacao depois = antes.editarInstrucao("  Nova instrucao  ", agora);

    // Assert
    assertEquals("Nova instrucao", depois.getInstrucao());
    assertEquals("Antiga", antes.getInstrucao());
    assertEquals(agora, depois.getAtualizadaEm());
  }

  @Test
  void shouldReturnTheSameInstanceWhenEditarInstrucaoWithTheSameText() {
    // Arrange
    final Solicitacao antes = umaSolicitacao().emAndamento().comInstrucao("Igual").build();

    // Act
    final Solicitacao depois = antes.editarInstrucao("  Igual ", agora);

    // Assert
    assertSame(antes, depois);
  }

  @Test
  void shouldSetTheFirstInstrucaoWhenEmAndamentoHadNone() {
    // Arrange: triada antes do campo existir
    final Solicitacao antes = umaSolicitacao().emAndamento().build();

    // Act
    final Solicitacao depois = antes.editarInstrucao("Primeira", agora);

    // Assert
    assertNull(antes.getInstrucao());
    assertEquals("Primeira", depois.getInstrucao());
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusSolicitacao.class,
      names = {"A_FAZER", "EM_VALIDACAO", "CONCLUIDA", "CANCELADA"})
  void shouldNotEditarInstrucaoOutsideEmAndamento(final StatusSolicitacao status) {
    // Arrange
    final Solicitacao sol = umaSolicitacao().noStatus(status).build();

    // Act + Assert
    assertThrows(ConflitoException.class, () -> sol.editarInstrucao("Nova", agora));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldFailWhenEditarInstrucaoIsBlank(final String instrucao) {
    // Arrange
    final Solicitacao sol = umaSolicitacao().emAndamento().comInstrucao("Antiga").build();

    // Act + Assert
    assertThrows(ValidationException.class, () -> sol.editarInstrucao(instrucao, agora));
  }

  @Test
  void shouldFailWhenEditarInstrucaoExceedsTheLimit() {
    // Arrange
    final Solicitacao sol = umaSolicitacao().emAndamento().comInstrucao("Antiga").build();

    // Act + Assert
    assertThrows(ValidationException.class, () -> sol.editarInstrucao("x".repeat(2001), agora));
  }

  @Test
  void shouldAllowEditarInstrucaoAgainAfterDevolver() {
    // Arrange
    final Solicitacao emValidacao = umaSolicitacao().emValidacao().comInstrucao("Antiga").build();

    // Act
    final Solicitacao devolvida = emValidacao.devolver(null, agora);
    final Solicitacao editada = devolvida.editarInstrucao("Corrigida", agora);

    // Assert
    assertEquals("Corrigida", editada.getInstrucao());
  }

  // ── invariantes do estado ─────────────────────────────────────────────────

  private Solicitacao.Builder base() {
    return Solicitacao.builder()
        .id(UUID.randomUUID())
        .titulo("T")
        .descricao("D")
        .tipo(TipoSolicitacao.REPARO)
        .status(StatusSolicitacao.EM_ANDAMENTO)
        .prioridade(PrioridadeSolicitacao.MEDIA)
        .modeloId(modeloId)
        .abertaPorUsuarioId(usuarioId)
        .criadaEm(agora)
        .atualizadaEm(agora);
  }

  private Solicitacao.Builder criacaoBase() {
    return base()
        .tipo(TipoSolicitacao.CRIACAO)
        .modeloId(null)
        .modeloCodigo("COD-1")
        .modeloMaquina("FBOX");
  }

  static Stream<Arguments> estadosInvalidos() {
    return Stream.of(
        Arguments.of(
            "em andamento sem prioridade",
            (Function<SolicitacaoTest, Solicitacao.Builder>) t -> t.base().prioridade(null),
            "prioridade e obrigatoria"),
        Arguments.of(
            "concluida sem concluidaEm",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.base().status(StatusSolicitacao.CONCLUIDA).comentarioFinal("ok"),
            "concluidaEm e obrigatorio"),
        Arguments.of(
            "concluida com canceladaEm",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t ->
                    t.base()
                        .status(StatusSolicitacao.CONCLUIDA)
                        .comentarioFinal("ok")
                        .concluidaEm(t.agora)
                        .canceladaEm(t.agora),
            "canceladaEm deve ser nulo"),
        Arguments.of(
            "concluida sem comentario final",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.base().status(StatusSolicitacao.CONCLUIDA).concluidaEm(t.agora),
            "comentarioFinal e obrigatorio ao concluir"),
        Arguments.of(
            "cancelada sem canceladaEm",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.base().status(StatusSolicitacao.CANCELADA).comentarioFinal("ok"),
            "canceladaEm e obrigatorio"),
        Arguments.of(
            "cancelada com concluidaEm",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t ->
                    t.base()
                        .status(StatusSolicitacao.CANCELADA)
                        .comentarioFinal("ok")
                        .canceladaEm(t.agora)
                        .concluidaEm(t.agora),
            "concluidaEm deve ser nulo"),
        Arguments.of(
            "cancelada sem comentario final",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.base().status(StatusSolicitacao.CANCELADA).canceladaEm(t.agora),
            "comentarioFinal e obrigatorio ao cancelar"),
        Arguments.of(
            "em andamento com data terminal",
            (Function<SolicitacaoTest, Solicitacao.Builder>) t -> t.base().concluidaEm(t.agora),
            "datas terminais devem ser nulas"),
        Arguments.of(
            "criacao sem codigo do modelo",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.criacaoBase().modeloCodigo(null),
            "modeloCodigo e obrigatorio"),
        Arguments.of(
            "criacao sem maquina do modelo",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.criacaoBase().modeloMaquina(null),
            "modeloMaquina e obrigatorio"),
        Arguments.of(
            "criacao concluida sem modelo criado",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t ->
                    t.criacaoBase()
                        .status(StatusSolicitacao.CONCLUIDA)
                        .comentarioFinal("ok")
                        .concluidaEm(t.agora),
            "modeloId e obrigatorio quando a solicitacao CRIACAO esta concluida"),
        Arguments.of(
            "criacao em andamento com modelo ja vinculado",
            (Function<SolicitacaoTest, Solicitacao.Builder>)
                t -> t.criacaoBase().modeloId(t.modeloId),
            "modeloId deve ser nulo ate"),
        Arguments.of(
            "reparo sem modelo",
            (Function<SolicitacaoTest, Solicitacao.Builder>) t -> t.base().modeloId(null),
            "modeloId e obrigatorio para este tipo"),
        Arguments.of(
            "reparo com dados de modelo em preparacao",
            (Function<SolicitacaoTest, Solicitacao.Builder>) t -> t.base().modeloCodigo("COD-1"),
            "dados de modelo em preparacao nao se aplicam"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("estadosInvalidos")
  void shouldRejectAnInconsistentState(
      final String nome,
      final Function<SolicitacaoTest, Solicitacao.Builder> montagem,
      final String mensagem) {
    // Arrange
    final Solicitacao.Builder builder = montagem.apply(this);

    // Act
    final ValidationException erro = assertThrows(ValidationException.class, builder::build);

    // Assert
    assertTrue(erro.getMessage().contains(mensagem), erro.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusSolicitacao.class,
      names = {"CONCLUIDA", "CANCELADA"})
  void shouldNotEditTituloOrDescricaoOfAnEncerrada(final StatusSolicitacao status) {
    // Arrange
    final Solicitacao encerrada = umaSolicitacao().noStatus(status).build();

    // Act + Assert
    assertThrows(
        BusinessRuleException.class,
        () -> encerrada.editar("Novo titulo", "Nova descricao", null, agora));
  }

  @Test
  void shouldNotAuthorizeExternoToMoveSolicitacoes() {
    assertThrows(
        NaoAutorizadoException.class,
        () ->
            Solicitacao.validarAutorizacaoMover(
                PerfilUsuario.EXTERNO,
                StatusSolicitacao.EM_ANDAMENTO,
                StatusSolicitacao.EM_VALIDACAO,
                true));
  }

  @Test
  void shouldOnlyLetAnAssignedOperatorSendToValidation() {
    assertThrows(
        NaoAutorizadoException.class,
        () ->
            Solicitacao.validarAutorizacaoMover(
                PerfilUsuario.OPERADOR,
                StatusSolicitacao.A_FAZER,
                StatusSolicitacao.EM_ANDAMENTO,
                true));
  }
}
