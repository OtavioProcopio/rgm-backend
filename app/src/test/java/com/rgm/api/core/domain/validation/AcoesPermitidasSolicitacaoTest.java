package com.rgm.api.core.domain.validation;

import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.ALTERAR_RESPONSAVEIS;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.ANEXAR_EVIDENCIA;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.CANCELAR;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.COMENTAR;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.DEVOLVER;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.EDITAR;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.ENCERRAR;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.ENVIAR_VALIDACAO;
import static com.rgm.api.core.domain.model.enums.AcaoSolicitacao.TRIAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AcoesPermitidasSolicitacaoTest {

  private static final Instant AGORA = Instant.parse("2026-10-05T12:00:00Z");

  private static Usuario usuario(final PerfilUsuario perfil, final boolean ativo) {
    return new Usuario(
        UUID.randomUUID(), "Usuario", "usuario@rgm.test", "hash", perfil, ativo, AGORA, AGORA);
  }

  private static Solicitacao solicitacao(final UUID autorId, final StatusSolicitacao status) {
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.ALTA : null,
        UUID.randomUUID(),
        null,
        null,
        null,
        autorId,
        status.isTerminal() ? "Comentario final" : null,
        AGORA,
        AGORA,
        status == StatusSolicitacao.CONCLUIDA ? AGORA : null,
        status == StatusSolicitacao.CANCELADA ? AGORA : null);
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldAllowTriageActionsWhenManagerSeesSolicitacaoAFazer(final PerfilUsuario perfil) {
    // Arrange
    final Usuario gestor = usuario(perfil, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.A_FAZER);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(gestor, sol, false, false);

    // Assert
    assertEquals(
        EnumSet.of(TRIAR, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS, ANEXAR_EVIDENCIA),
        acoes);
  }

  @Test
  void shouldAllowSendingToValidationWhenManagerSeesSolicitacaoEmAndamento() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(gestor, sol, false, true);

    // Assert
    assertEquals(
        EnumSet.of(
            ENVIAR_VALIDACAO, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS, ANEXAR_EVIDENCIA),
        acoes);
  }

  @Test
  void shouldAllowReturningAndClosingWhenManagerSeesSolicitacaoEmValidacao() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.EM_VALIDACAO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(gestor, sol, false, true);

    // Assert
    assertEquals(
        EnumSet.of(
            DEVOLVER, ENCERRAR, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS, ANEXAR_EVIDENCIA),
        acoes);
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusSolicitacao.class,
      names = {"CONCLUIDA", "CANCELADA"})
  void shouldAllowOnlyCommentingWhenManagerSeesClosedSolicitacao(final StatusSolicitacao status) {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), status);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(gestor, sol, false, true);

    // Assert
    assertEquals(EnumSet.of(COMENTAR), acoes);
  }

  @Test
  void shouldAllowCancellingWhenAuthorSeesUnassignedSolicitacaoAFazer() {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(autor.getId(), StatusSolicitacao.A_FAZER);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(autor, sol, false, false);

    // Assert
    assertEquals(EnumSet.of(CANCELAR, EDITAR, COMENTAR, ANEXAR_EVIDENCIA), acoes);
  }

  @Test
  void shouldNotAllowCancellingWhenAuthorSeesSolicitacaoThatAlreadyHasAssignee() {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(autor.getId(), StatusSolicitacao.A_FAZER);

    // Act
    final Set<AcaoSolicitacao> acoes = AcoesPermitidasSolicitacao.calcular(autor, sol, false, true);

    // Assert
    assertEquals(EnumSet.of(EDITAR, COMENTAR, ANEXAR_EVIDENCIA), acoes);
  }

  @Test
  void shouldNotAllowCancellingWhenAuthorSeesSolicitacaoEmAndamento() {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(autor.getId(), StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(autor, sol, false, false);

    // Assert
    assertEquals(EnumSet.of(EDITAR, COMENTAR, ANEXAR_EVIDENCIA), acoes);
  }

  @Test
  void shouldAllowSendingToValidationWhenAssignedOperadorSeesSolicitacaoEmAndamento() {
    // Arrange
    final Usuario responsavel = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(responsavel, sol, true, true);

    // Assert
    assertEquals(EnumSet.of(ENVIAR_VALIDACAO, COMENTAR, ANEXAR_EVIDENCIA), acoes);
  }

  @Test
  void shouldNotAllowSendingToValidationWhenAssignedOperadorSeesSolicitacaoEmValidacao() {
    // Arrange
    final Usuario responsavel = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.EM_VALIDACAO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(responsavel, sol, true, true);

    // Assert
    assertEquals(EnumSet.of(COMENTAR, ANEXAR_EVIDENCIA), acoes);
  }

  @Test
  void shouldAllowNothingWhenOperadorHasNoRelationWithTheSolicitacao() {
    // Arrange
    final Usuario estranho = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(UUID.randomUUID(), StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(estranho, sol, false, true);

    // Assert
    assertEquals(EnumSet.noneOf(AcaoSolicitacao.class), acoes);
  }

  @Test
  void shouldAllowNothingWhenAuthorSeesClosedSolicitacao() {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao sol = solicitacao(autor.getId(), StatusSolicitacao.CONCLUIDA);

    // Act
    final Set<AcaoSolicitacao> acoes = AcoesPermitidasSolicitacao.calcular(autor, sol, true, true);

    // Assert
    assertEquals(EnumSet.noneOf(AcaoSolicitacao.class), acoes);
  }

  @Test
  void shouldAllowNothingWhenUserIsInactive() {
    // Arrange
    final Usuario inativo = usuario(PerfilUsuario.GESTOR, false);
    final Solicitacao sol = solicitacao(inativo.getId(), StatusSolicitacao.A_FAZER);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(inativo, sol, false, false);

    // Assert
    assertEquals(EnumSet.noneOf(AcaoSolicitacao.class), acoes);
  }

  @Test
  void shouldAllowNothingWhenUserIsExterno() {
    // Arrange
    final Usuario externo = Usuario.criarExterno("Prestador", AGORA);
    final Solicitacao sol = solicitacao(externo.getId(), StatusSolicitacao.EM_ANDAMENTO);

    // Act
    final Set<AcaoSolicitacao> acoes =
        AcoesPermitidasSolicitacao.calcular(externo, sol, true, true);

    // Assert
    assertEquals(EnumSet.noneOf(AcaoSolicitacao.class), acoes);
  }
}
