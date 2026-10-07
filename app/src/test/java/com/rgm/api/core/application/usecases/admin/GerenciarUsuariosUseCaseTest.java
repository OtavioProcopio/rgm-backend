package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import com.rgm.api.core.domain.validation.PoliticaSenha;
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

  private static final String SENHA_VALIDA = "s".repeat(PoliticaSenha.TAMANHO_MINIMO);

  @Test
  void shouldSaveUserWithHashedPasswordWhenAdminCreatesInternalUser() {
    // Arrange
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.existsByEmail("novo@test.com")).thenReturn(false);
    when(passwordHasher.hash(SENHA_VALIDA)).thenReturn("hashed");
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Usuario resultado =
        useCase.criar(
            new GerenciarUsuariosUseCase.CriarInput(
                "Novo", "novo@test.com", SENHA_VALIDA, PerfilUsuario.OPERADOR, admin.getId()));

    // Assert
    assertEquals("hashed", resultado.getSenhaHash());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verify(usuarioRepository, times(1)).existsByEmail("novo@test.com");
    verify(passwordHasher, times(1)).hash(SENHA_VALIDA);
    verify(usuarioRepository, times(1)).save(resultado);
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldRejectCreationWhenEmailIsAlreadyRegistered() {
    // Arrange
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.existsByEmail("dup@test.com")).thenReturn(true);

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.criar(
                    new GerenciarUsuariosUseCase.CriarInput(
                        "Dup",
                        "dup@test.com",
                        SENHA_VALIDA,
                        PerfilUsuario.OPERADOR,
                        admin.getId())));

    // Assert
    assertEquals("Email ja cadastrado", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verify(usuarioRepository, times(1)).existsByEmail("dup@test.com");
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldRejectCreationWhenProfileIsExternal() {
    // Arrange
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.criar(
                    new GerenciarUsuariosUseCase.CriarInput(
                        "Ext",
                        "ext@test.com",
                        SENHA_VALIDA,
                        PerfilUsuario.EXTERNO,
                        admin.getId())));

    // Assert
    assertEquals("Use o caso de uso de cadastrar prestador externo", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldRejectCreationWhenRequesterIsNotAdministrator() {
    // Arrange
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

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () ->
                useCase.criar(
                    new GerenciarUsuariosUseCase.CriarInput(
                        "Novo",
                        "novo@test.com",
                        SENHA_VALIDA,
                        PerfilUsuario.OPERADOR,
                        gestor.getId())));

    // Assert
    assertEquals("Somente ADMINISTRADOR pode gerenciar usuarios", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(gestor.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
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

  @Test
  void deveFalharAoEditarSeEmailJaExistirParaOutroUsuario() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario alvo =
        new Usuario(
            UUID.randomUUID(),
            "Alvo",
            "alvo@t.com",
            "h",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.existsByEmailAndIdNot("existente@t.com", alvo.getId())).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.editar(
                new GerenciarUsuariosUseCase.EditarInput(
                    alvo.getId(), "Nome", "existente@t.com", admin.getId())));
  }

  @Test
  void shouldRejectResetWhenPasswordIsBlank() {
    // Arrange
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () ->
                useCase.redefinirSenha(
                    new GerenciarUsuariosUseCase.RedefinirSenhaInput(
                        UUID.randomUUID(), "  ", admin.getId())));

    // Assert
    assertEquals(MENSAGEM_SENHA, erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  private static final String MENSAGEM_SENHA =
      "Senha deve ter no minimo " + PoliticaSenha.TAMANHO_MINIMO + " caracteres";

  private Usuario criarOperador() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Alvo",
        "alvo@test.com",
        "hash",
        PerfilUsuario.OPERADOR,
        true,
        agora,
        agora,
        4);
  }

  @Test
  void shouldRejectCreationWhenPasswordIsBelowMinimumLength() {
    // Arrange
    final Usuario admin = criarAdmin();
    final String curta = "s".repeat(PoliticaSenha.TAMANHO_MINIMO - 1);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () ->
                useCase.criar(
                    new GerenciarUsuariosUseCase.CriarInput(
                        "Novo", "novo@test.com", curta, PerfilUsuario.OPERADOR, admin.getId())));

    // Assert
    assertEquals(MENSAGEM_SENHA, erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldRejectCreationWhenLoginProfileHasNoPassword() {
    // Arrange
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () ->
                useCase.criar(
                    new GerenciarUsuariosUseCase.CriarInput(
                        "Novo", "novo@test.com", null, PerfilUsuario.GESTOR, admin.getId())));

    // Assert
    assertEquals(MENSAGEM_SENHA, erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldRejectResetWhenPasswordIsBelowMinimumLength() {
    // Arrange
    final Usuario admin = criarAdmin();
    final String curta = "s".repeat(PoliticaSenha.TAMANHO_MINIMO - 1);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () ->
                useCase.redefinirSenha(
                    new GerenciarUsuariosUseCase.RedefinirSenhaInput(
                        UUID.randomUUID(), curta, admin.getId())));

    // Assert
    assertEquals(MENSAGEM_SENHA, erro.getMessage());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldIncrementCredentialVersionWhenPasswordIsReset() {
    // Arrange
    final Usuario admin = criarAdmin();
    final Usuario alvo = criarOperador();
    final String nova = "s".repeat(PoliticaSenha.TAMANHO_MINIMO);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(passwordHasher.hash(nova)).thenReturn("novoHash");
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Usuario resultado =
        useCase.redefinirSenha(
            new GerenciarUsuariosUseCase.RedefinirSenhaInput(alvo.getId(), nova, admin.getId()));

    // Assert
    assertEquals(alvo.getVersaoCredencial() + 1, resultado.getVersaoCredencial());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verify(usuarioRepository, times(1)).findById(alvo.getId());
    verify(passwordHasher, times(1)).hash(nova);
    verify(usuarioRepository, times(1)).save(resultado);
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void shouldKeepCredentialVersionWhenProfileChanges() {
    // Arrange
    final Usuario admin = criarAdmin();
    final Usuario alvo = criarOperador();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Usuario resultado =
        useCase.alterarPerfil(
            new GerenciarUsuariosUseCase.AlterarPerfilInput(
                alvo.getId(), PerfilUsuario.GESTOR, admin.getId()));

    // Assert
    assertEquals(alvo.getVersaoCredencial(), resultado.getVersaoCredencial());
    verify(usuarioRepository, times(1)).findById(admin.getId());
    verify(usuarioRepository, times(1)).findById(alvo.getId());
    verify(usuarioRepository, times(1)).save(resultado);
    verifyNoMoreInteractions(usuarioRepository, passwordHasher);
  }

  @Test
  void deveFalharAoRedefinirSenhaSeUsuarioForExterno() {
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
            useCase.redefinirSenha(
                new GerenciarUsuariosUseCase.RedefinirSenhaInput(
                    externo.getId(), "novasenhade12", admin.getId())));
  }

  @Test
  void deveFalharAoAlterarPerfilDeExternoParaInterno() {
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
            useCase.alterarPerfil(
                new GerenciarUsuariosUseCase.AlterarPerfilInput(
                    externo.getId(), PerfilUsuario.OPERADOR, admin.getId())));
  }

  @Test
  void deveFalharAoAlterarPerfilDeInternoParaExterno() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Usuario interno =
        new Usuario(
            UUID.randomUUID(), "Int", "i@t.com", "h", PerfilUsuario.OPERADOR, true, agora, agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(interno.getId())).thenReturn(Optional.of(interno));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.alterarPerfil(
                new GerenciarUsuariosUseCase.AlterarPerfilInput(
                    interno.getId(), PerfilUsuario.EXTERNO, admin.getId())));
  }

  @Test
  void deveFalharSeUsuarioAlvoNaoExistir() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.ativar(
                new GerenciarUsuariosUseCase.AtivarInput(UUID.randomUUID(), admin.getId())));
  }
}
