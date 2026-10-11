package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AcessoEvidenciaSolicitacaoTest {

  private static final Instant AGORA = Instant.parse("2026-10-05T12:00:00Z");

  private BooleanSupplier estaAtribuido;

  @BeforeEach
  void setUp() {
    estaAtribuido = mock(BooleanSupplier.class);
  }

  private static Usuario usuario(final PerfilUsuario perfil, final boolean ativo) {
    return new Usuario(
        UUID.randomUUID(), "Usuario", "usuario@rgm.test", "hash", perfil, ativo, AGORA, AGORA);
  }

  private static Solicitacao solicitacaoAbertaPor(final UUID autorId) {
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.A_FAZER,
        null,
        UUID.randomUUID(),
        null,
        null,
        null,
        autorId,
        null,
        AGORA,
        AGORA,
        null,
        null);
  }

  @Test
  void shouldAllowListingWhenUserOpenedTheSolicitacao() {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(autor.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    assertDoesNotThrow(
        () -> AcessoEvidenciaSolicitacao.validarListagem(autor, solicitacao, estaAtribuido));

    // Assert
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldAllowListingWhenUserIsAssigned() {
    // Arrange
    final Usuario responsavel = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(true);

    // Act
    assertDoesNotThrow(
        () -> AcessoEvidenciaSolicitacao.validarListagem(responsavel, solicitacao, estaAtribuido));

    // Assert
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldAllowListingWithoutCheckingAssignmentWhenProfileManages(final PerfilUsuario perfil) {
    // Arrange
    final Usuario gestor = usuario(perfil, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());

    // Act
    assertDoesNotThrow(
        () -> AcessoEvidenciaSolicitacao.validarListagem(gestor, solicitacao, estaAtribuido));

    // Assert
    verify(estaAtribuido, never()).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldDenyListingWhenOperadorHasNoRelation() {
    // Arrange
    final Usuario estranho = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> AcessoEvidenciaSolicitacao.validarListagem(estranho, solicitacao, estaAtribuido));

    // Assert
    assertEquals("Usuario nao tem acesso a esta solicitacao", erro.getMessage());
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"OPERADOR", "GESTOR", "ADMINISTRADOR"})
  void shouldDenyListingWhenUserIsInactive(final PerfilUsuario perfil) {
    // Arrange
    final Usuario inativo = usuario(perfil, false);
    final Solicitacao solicitacao = solicitacaoAbertaPor(inativo.getId());

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> AcessoEvidenciaSolicitacao.validarListagem(inativo, solicitacao, estaAtribuido));

    // Assert
    assertEquals("Usuario inativo", erro.getMessage());
    verify(estaAtribuido, never()).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(
      value = TipoEvidencia.class,
      names = {"ABERTURA", "GERAL"})
  void shouldAllowAttachingWhenAuthorSendsOpeningOrGeneralEvidence(final TipoEvidencia tipo) {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(autor.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    assertDoesNotThrow(
        () -> AcessoEvidenciaSolicitacao.validarAnexo(autor, solicitacao, estaAtribuido, tipo));

    // Assert
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(
      value = TipoEvidencia.class,
      names = {"INSTRUCAO_SERVICO", "SERVICO_REALIZADO", "CONCLUSAO", "DEVOLUCAO"})
  void shouldDenyAttachingWhenAuthorSendsServiceEvidenceWithoutBeingAssigned(
      final TipoEvidencia tipo) {
    // Arrange
    final Usuario autor = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(autor.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> AcessoEvidenciaSolicitacao.validarAnexo(autor, solicitacao, estaAtribuido, tipo));

    // Assert
    assertEquals(
        "Quem abriu a solicitacao so pode anexar evidencia do tipo ABERTURA ou GERAL",
        erro.getMessage());
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(TipoEvidencia.class)
  void shouldAllowAttachingAnyTypeWhenUserIsAssigned(final TipoEvidencia tipo) {
    // Arrange
    final Usuario autorEResponsavel = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(autorEResponsavel.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(true);

    // Act
    assertDoesNotThrow(
        () ->
            AcessoEvidenciaSolicitacao.validarAnexo(
                autorEResponsavel, solicitacao, estaAtribuido, tipo));

    // Assert
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldAllowAttachingWithoutCheckingAssignmentWhenProfileManages(final PerfilUsuario perfil) {
    // Arrange
    final Usuario gestor = usuario(perfil, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());

    // Act
    assertDoesNotThrow(
        () ->
            AcessoEvidenciaSolicitacao.validarAnexo(
                gestor, solicitacao, estaAtribuido, TipoEvidencia.CONCLUSAO));

    // Assert
    verify(estaAtribuido, never()).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldDenyAttachingWhenOperadorHasNoRelation() {
    // Arrange
    final Usuario estranho = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () ->
                AcessoEvidenciaSolicitacao.validarAnexo(
                    estranho, solicitacao, estaAtribuido, TipoEvidencia.GERAL));

    // Assert
    assertEquals("Usuario nao tem acesso a esta solicitacao", erro.getMessage());
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldDenyAttachingWhenUserIsInactive() {
    // Arrange
    final Usuario inativo = usuario(PerfilUsuario.OPERADOR, false);
    final Solicitacao solicitacao = solicitacaoAbertaPor(inativo.getId());

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () ->
                AcessoEvidenciaSolicitacao.validarAnexo(
                    inativo, solicitacao, estaAtribuido, TipoEvidencia.ABERTURA));

    // Assert
    assertEquals("Usuario inativo", erro.getMessage());
    verify(estaAtribuido, never()).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }
}
