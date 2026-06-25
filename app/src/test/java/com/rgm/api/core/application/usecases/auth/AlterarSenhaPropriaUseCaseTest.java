package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlterarSenhaPropriaUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private AlterarSenhaPropriaUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    passwordHasher = mock(PasswordHasher.class);
    useCase = new AlterarSenhaPropriaUseCase(usuarioRepository, passwordHasher);
  }

  private Usuario criarUsuario() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "User",
        "user@test.com",
        "hashAtual",
        PerfilUsuario.OPERADOR,
        true,
        agora,
        agora);
  }

  @Test
  void deveAlterarSenhaComSucesso() {
    final Usuario usuario = criarUsuario();

    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("senhaAtual", "hashAtual")).thenReturn(true);
    when(passwordHasher.hash("novaSenha")).thenReturn("novaSenhaHash");
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.execute(
            new AlterarSenhaPropriaUseCase.Input(
                usuario.getId(), "senhaAtual", "novaSenha"));

    assertNotNull(resultado);
    assertEquals("novaSenhaHash", resultado.getSenhaHash());
    verify(usuarioRepository).save(any(Usuario.class));
  }

  @Test
  void deveFalharSeSenhaAtualIncorreta() {
    final Usuario usuario = criarUsuario();

    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("senhaIncorreta", "hashAtual")).thenReturn(false);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new AlterarSenhaPropriaUseCase.Input(
                    usuario.getId(), "senhaIncorreta", "novaSenha")));
  }

  @Test
  void deveFalharSeUsuarioForExterno() {
    final Instant agora = Instant.now();
    final Usuario externo = Usuario.criarExterno("Externo", agora);

    when(usuarioRepository.findById(externo.getId())).thenReturn(Optional.of(externo));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new AlterarSenhaPropriaUseCase.Input(
                    externo.getId(), "senhaAtual", "novaSenha")));
  }

  @Test
  void deveFalharSeUsuarioNaoExistir() {
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new AlterarSenhaPropriaUseCase.Input(userId, "senhaAtual", "novaSenha")));
  }
}
