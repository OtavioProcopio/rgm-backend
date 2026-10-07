package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AcessoSolicitacaoTest {

  private static final Instant AGORA = Instant.parse("2026-10-07T12:00:00Z");

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

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldAllowReadingWhenProfileSeesEverySolicitacao(final PerfilUsuario perfil) {
    // Arrange
    final Usuario usuario = usuario(perfil, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());

    // Act
    final boolean podeVer = AcessoSolicitacao.podeVer(usuario, solicitacao, estaAtribuido);

    // Assert
    assertTrue(podeVer);
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldAllowReadingWhenOperatorIsAssigned() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(true);

    // Act
    final boolean podeVer = AcessoSolicitacao.podeVer(operador, solicitacao, estaAtribuido);

    // Assert
    assertTrue(podeVer);
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldAllowReadingWhenOperatorOpenedTheSolicitacao() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(operador.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final boolean podeVer = AcessoSolicitacao.podeVer(operador, solicitacao, estaAtribuido);

    // Assert
    assertTrue(podeVer);
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldDenyReadingWhenOperatorNeitherOpenedNorIsAssigned() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final boolean podeVer = AcessoSolicitacao.podeVer(operador, solicitacao, estaAtribuido);

    // Assert
    assertFalse(podeVer);
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldDenyReadingWhenUserIsInactive() {
    // Arrange
    final Usuario gestorInativo = usuario(PerfilUsuario.GESTOR, false);
    final Solicitacao solicitacao = solicitacaoAbertaPor(gestorInativo.getId());

    // Act
    final boolean podeVer = AcessoSolicitacao.podeVer(gestorInativo, solicitacao, estaAtribuido);

    // Assert
    assertFalse(podeVer);
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldThrowWhenValidatingReadingWithoutAccess() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(UUID.randomUUID());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> AcessoSolicitacao.validarLeitura(operador, solicitacao, estaAtribuido));

    // Assert
    assertEquals("Usuario nao tem acesso a esta solicitacao", erro.getMessage());
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }

  @Test
  void shouldNotThrowWhenValidatingReadingWithAccess() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR, true);
    final Solicitacao solicitacao = solicitacaoAbertaPor(operador.getId());
    when(estaAtribuido.getAsBoolean()).thenReturn(false);

    // Act
    AcessoSolicitacao.validarLeitura(operador, solicitacao, estaAtribuido);

    // Assert
    verify(estaAtribuido, times(1)).getAsBoolean();
    verifyNoMoreInteractions(estaAtribuido);
  }
}
