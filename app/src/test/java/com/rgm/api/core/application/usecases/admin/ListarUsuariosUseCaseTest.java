package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListarUsuariosUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private ListarUsuariosUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new ListarUsuariosUseCase(usuarioRepository);
  }

  @Test
  void execute_comFiltrosNulos_deveChamarFindAll() {
    final ListarUsuariosUseCase.Input input = new ListarUsuariosUseCase.Input(null, null, 0, 20);
    final PageResult<Usuario> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(usuarioRepository.findAll(anyInt(), anyInt())).thenReturn(expectedPage);

    final PageResult<Usuario> result = useCase.execute(input);

    assertNotNull(result);
    verify(usuarioRepository).findAll(0, 20);
  }

  @Test
  void execute_comPerfilNaoNulo_deveChamarFindByFilters() {
    final ListarUsuariosUseCase.Input input =
        new ListarUsuariosUseCase.Input(PerfilUsuario.OPERADOR, null, 0, 20);
    final PageResult<Usuario> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(usuarioRepository.findByFilters(any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    final PageResult<Usuario> result = useCase.execute(input);

    assertNotNull(result);
    verify(usuarioRepository).findByFilters(PerfilUsuario.OPERADOR, null, 0, 20);
  }

  @Test
  void execute_comAtivoNaoNulo_deveChamarFindByFilters() {
    final ListarUsuariosUseCase.Input input = new ListarUsuariosUseCase.Input(null, true, 0, 20);
    final PageResult<Usuario> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(usuarioRepository.findByFilters(any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    final PageResult<Usuario> result = useCase.execute(input);

    assertNotNull(result);
    verify(usuarioRepository).findByFilters(null, true, 0, 20);
  }
}
