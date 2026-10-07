package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import com.rgm.api.core.domain.validation.PoliticaSenha;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlterarSenhaPropriaUseCaseTest {

  private static final String SENHA_ATUAL = "senhaAtual";
  private static final String NOVA_SENHA = "n".repeat(PoliticaSenha.TAMANHO_MINIMO);

  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private AccessTokenIssuer tokenIssuer;
  private AlterarSenhaPropriaUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    passwordHasher = mock(PasswordHasher.class);
    tokenIssuer = mock(AccessTokenIssuer.class);
    useCase = new AlterarSenhaPropriaUseCase(usuarioRepository, passwordHasher, tokenIssuer);
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

  private void verificarSemMaisChamadas() {
    verifyNoMoreInteractions(usuarioRepository, passwordHasher, tokenIssuer);
  }

  private Usuario verificarTrocaEObterSalvo(final Usuario usuario) {
    final ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository, times(1)).findById(usuario.getId());
    verify(passwordHasher, times(1)).matches(SENHA_ATUAL, usuario.getSenhaHash());
    verify(passwordHasher, times(1)).hash(NOVA_SENHA);
    verify(usuarioRepository, times(1)).save(salvo.capture());
    verify(tokenIssuer, times(1)).issue(salvo.getValue());
    verify(tokenIssuer, times(1)).issueRefreshToken(salvo.getValue());
    verificarSemMaisChamadas();
    return salvo.getValue();
  }

  private void prepararTroca(final Usuario usuario) {
    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches(SENHA_ATUAL, usuario.getSenhaHash())).thenReturn(true);
    when(passwordHasher.hash(NOVA_SENHA)).thenReturn("novaSenhaHash");
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    when(tokenIssuer.issue(any(Usuario.class))).thenReturn("novo-acesso");
    when(tokenIssuer.issueRefreshToken(any(Usuario.class))).thenReturn("nova-renovacao");
  }

  @Test
  void shouldSaveTheNewHashWhenPasswordChanges() {
    // Arrange
    final Usuario usuario = criarUsuario();
    prepararTroca(usuario);

    // Act
    useCase.execute(new AlterarSenhaPropriaUseCase.Input(usuario.getId(), SENHA_ATUAL, NOVA_SENHA));

    // Assert
    assertEquals("novaSenhaHash", verificarTrocaEObterSalvo(usuario).getSenhaHash());
  }

  @Test
  void shouldInvalidatePreviousCredentialsWhenPasswordChanges() {
    // Arrange
    final Usuario usuario = criarUsuario();
    prepararTroca(usuario);

    // Act
    useCase.execute(new AlterarSenhaPropriaUseCase.Input(usuario.getId(), SENHA_ATUAL, NOVA_SENHA));

    // Assert
    assertEquals(
        usuario.getVersaoCredencial() + 1,
        verificarTrocaEObterSalvo(usuario).getVersaoCredencial());
  }

  @Test
  void shouldReturnCredentialsIssuedForTheSavedUserWhenPasswordChanges() {
    // Arrange
    final Usuario usuario = criarUsuario();
    prepararTroca(usuario);

    // Act
    final AlterarSenhaPropriaUseCase.Output resultado =
        useCase.execute(
            new AlterarSenhaPropriaUseCase.Input(usuario.getId(), SENHA_ATUAL, NOVA_SENHA));

    // Assert
    assertEquals(
        new AlterarSenhaPropriaUseCase.Output(
            verificarTrocaEObterSalvo(usuario), "novo-acesso", "nova-renovacao"),
        resultado);
  }

  @Test
  void shouldRejectNewPasswordWhenBelowMinimumLength() {
    // Arrange
    final String curta = "n".repeat(PoliticaSenha.TAMANHO_MINIMO - 1);

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () ->
                useCase.execute(
                    new AlterarSenhaPropriaUseCase.Input(UUID.randomUUID(), SENHA_ATUAL, curta)));

    // Assert
    assertEquals(
        "Senha deve ter no minimo " + PoliticaSenha.TAMANHO_MINIMO + " caracteres",
        erro.getMessage());
    verificarSemMaisChamadas();
  }

  @Test
  void shouldRejectWhenCurrentPasswordIsBlank() {
    // Arrange
    final String senhaAtualEmBranco = " ";

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.execute(
                    new AlterarSenhaPropriaUseCase.Input(
                        UUID.randomUUID(), senhaAtualEmBranco, NOVA_SENHA)));

    // Assert
    assertEquals("Senha atual e obrigatoria", erro.getMessage());
    verificarSemMaisChamadas();
  }

  @Test
  void shouldRejectWhenCurrentPasswordDoesNotMatch() {
    // Arrange
    final Usuario usuario = criarUsuario();
    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("senhaIncorreta", usuario.getSenhaHash())).thenReturn(false);

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.execute(
                    new AlterarSenhaPropriaUseCase.Input(
                        usuario.getId(), "senhaIncorreta", NOVA_SENHA)));

    // Assert
    assertEquals("Senha atual incorreta", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(usuario.getId());
    verify(passwordHasher, times(1)).matches("senhaIncorreta", usuario.getSenhaHash());
    verificarSemMaisChamadas();
  }

  @Test
  void shouldRejectWhenUserIsExternal() {
    // Arrange
    final Usuario externo = Usuario.criarExterno("Externo", Instant.now());
    when(usuarioRepository.findById(externo.getId())).thenReturn(Optional.of(externo));

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.execute(
                    new AlterarSenhaPropriaUseCase.Input(
                        externo.getId(), SENHA_ATUAL, NOVA_SENHA)));

    // Assert
    assertEquals("Prestador externo nao possui senha", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(externo.getId());
    verificarSemMaisChamadas();
  }

  @Test
  void shouldFailWhenUserDoesNotExist() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    final RecursoNaoEncontradoException erro =
        assertThrows(
            RecursoNaoEncontradoException.class,
            () ->
                useCase.execute(
                    new AlterarSenhaPropriaUseCase.Input(userId, SENHA_ATUAL, NOVA_SENHA)));

    // Assert
    assertEquals("Usuario nao encontrado", erro.getMessage());
    verify(usuarioRepository, times(1)).findById(userId);
    verificarSemMaisChamadas();
  }
}
