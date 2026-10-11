package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ListarResponsaveisDisponiveisUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private ListarResponsaveisDisponiveisUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new ListarResponsaveisDisponiveisUseCase(usuarioRepository);
  }

  private Usuario usuario(final PerfilUsuario perfil) {
    return Usuario.criarInterno("Nome", "n@rgm.com", "hash", perfil, Instant.now());
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldListAssignableUsersWhenUserCanTriage(final PerfilUsuario perfil) {
    // Arrange
    final Usuario quemTria = usuario(perfil);
    final PageResult<Usuario> esperado = new PageResult<>(List.of(), 1, 10, 0, 0);
    when(usuarioRepository.findById(quemTria.getId())).thenReturn(Optional.of(quemTria));
    when(usuarioRepository.buscarAtribuiveis("zelia", 1, 10)).thenReturn(esperado);

    // Act
    final PageResult<Usuario> resultado =
        useCase.execute(
            new ListarResponsaveisDisponiveisUseCase.Input("zelia", 1, 10, quemTria.getId()));

    // Assert
    assertSame(esperado, resultado);
    verify(usuarioRepository, times(1)).buscarAtribuiveis("zelia", 1, 10);
  }

  @Test
  void shouldRejectWhenUserIsOperator() {
    // Arrange
    final Usuario operador = usuario(PerfilUsuario.OPERADOR);
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    // Act
    final var acao =
        (org.junit.jupiter.api.function.Executable)
            () ->
                useCase.execute(
                    new ListarResponsaveisDisponiveisUseCase.Input(null, 0, 20, operador.getId()));

    // Assert
    assertThrows(NaoAutorizadoException.class, acao);
    verify(usuarioRepository, never()).buscarAtribuiveis(null, 0, 20);
  }

  @Test
  void shouldRejectWhenUserDoesNotExist() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

    // Act
    final var acao =
        (org.junit.jupiter.api.function.Executable)
            () -> useCase.execute(new ListarResponsaveisDisponiveisUseCase.Input(null, 0, 20, id));

    // Assert
    assertThrows(RecursoNaoEncontradoException.class, acao);
  }
}
