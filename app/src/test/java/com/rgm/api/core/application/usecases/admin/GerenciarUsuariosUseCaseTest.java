package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GerenciarUsuariosUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private GerenciarUsuariosUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    passwordHasher = mock(PasswordHasher.class);
    useCase = new GerenciarUsuariosUseCase(usuarioRepository, passwordHasher);
  }

  private Usuario criarAdmin() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Admin",
        "admin@test.com",
        "hash",
        PerfilUsuario.ADMINISTRADOR,
        true,
        agora,
        agora);
  }

  @Test
  void deveCriarUsuarioComSucesso() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.existsByEmail("novo@test.com")).thenReturn(false);
    when(passwordHasher.hash("senha")).thenReturn("hashed");
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.criar(
            new GerenciarUsuariosUseCase.CriarInput(
                "Novo", "novo@test.com", "senha", PerfilUsuario.OPERADOR, admin.getId()));

    assertNotNull(resultado);
    assertEquals("Novo", resultado.getNome());
    assertEquals(PerfilUsuario.OPERADOR, resultado.getPerfil());
  }

  @Test
  void deveFalharComEmailDuplicado() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.existsByEmail("dup@test.com")).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.criar(
                new GerenciarUsuariosUseCase.CriarInput(
                    "Dup", "dup@test.com", "senha", PerfilUsuario.OPERADOR, admin.getId())));
  }

  @Test
  void deveFalharComPerfilExterno() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.criar(
                new GerenciarUsuariosUseCase.CriarInput(
                    "Ext", "ext@test.com", "senha", PerfilUsuario.EXTERNO, admin.getId())));
  }

  @Test
  void deveFalharSeNaoAdmin() {
    final Instant agora = Instant.now();
    final Usuario gestor =
        new Usuario(
            UUID.randomUUID(),
            "Gestor",
            "gestor@test.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.criar(
                new GerenciarUsuariosUseCase.CriarInput(
                    "Novo", "novo@test.com", "senha", PerfilUsuario.OPERADOR, gestor.getId())));
  }

  @Test
  void deveDesativarUsuario() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.desativar(new GerenciarUsuariosUseCase.DesativarInput(alvo.getId(), admin.getId()));

    assertFalse(resultado.isAtivo());
  }

  @Test
  void deveFalharAoDesativarSiMesmo() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.desativar(
                new GerenciarUsuariosUseCase.DesativarInput(admin.getId(), admin.getId())));
  }

  @Test
  void deveRedefinirSenhaComSucesso() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(passwordHasher.hash("novaSenha")).thenReturn("novoHash");
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.redefinirSenha(
            new GerenciarUsuariosUseCase.RedefinirSenhaInput(
                alvo.getId(), "novaSenha", admin.getId()));

    assertEquals("novoHash", resultado.getSenhaHash());
  }

  @Test
  void deveAlterarPerfilComSucesso() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.alterarPerfil(
            new GerenciarUsuariosUseCase.AlterarPerfilInput(
                alvo.getId(), PerfilUsuario.GESTOR, admin.getId()));

    assertEquals(PerfilUsuario.GESTOR, resultado.getPerfil());
  }

  @Test
  void deveAtivarUsuario() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            false,
            agora,
            agora);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.ativar(new GerenciarUsuariosUseCase.AtivarInput(alvo.getId(), admin.getId()));

    assertTrue(resultado.isAtivo());
  }

  @Test
  void deveEditarUsuario() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Usuario resultado =
        useCase.editar(
            new GerenciarUsuariosUseCase.EditarInput(
                alvo.getId(), "Novo Nome", "novo@test.com", admin.getId()));

    assertEquals("Novo Nome", resultado.getNome());
  }

  @Test
  void deveFalharAoEditarUsuarioExterno() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario externo =
        new Usuario(
            UUID.randomUUID(), "Ext", null, null, PerfilUsuario.EXTERNO, true, agora, agora);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(externo.getId())).thenReturn(Optional.of(externo));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.editar(
                new GerenciarUsuariosUseCase.EditarInput(
                    externo.getId(), "Nome", "e@t.com", admin.getId())));
  }

  @Test
  void deveFalharAoAlterarProprioPerfilDeAdmin() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.alterarPerfil(
                new GerenciarUsuariosUseCase.AlterarPerfilInput(
                    admin.getId(), PerfilUsuario.OPERADOR, admin.getId())));
  }
}
