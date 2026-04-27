package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CadastrarPrestadorExternoUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private CadastrarPrestadorExternoUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new CadastrarPrestadorExternoUseCase(usuarioRepository);
  }

  @Test
  void deveCadastrarExternoComSucesso() {
    final Instant agora = Instant.now();
    final Usuario admin =
        new Usuario(
            UUID.randomUUID(),
            "Admin",
            "admin@test.com",
            "hash",
            PerfilUsuario.ADMINISTRADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.execute(new CadastrarPrestadorExternoUseCase.Input("Prestador", admin.getId()));

    assertNotNull(resultado);
    assertEquals(PerfilUsuario.EXTERNO, resultado.getPerfil());
    assertEquals("Prestador", resultado.getNome());
  }

  @Test
  void deveFalharSeNaoAdmin() {
    final Instant agora = Instant.now();
    final Usuario gestor =
        new Usuario(
            UUID.randomUUID(),
            "Gestor",
            "g@test.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new CadastrarPrestadorExternoUseCase.Input("Ext", gestor.getId())));
  }
}
