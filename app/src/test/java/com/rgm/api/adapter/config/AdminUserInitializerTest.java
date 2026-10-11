package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

class AdminUserInitializerTest {

  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private MockEnvironment environment;
  private AdminUserInitializer initializer;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    passwordHasher = mock(PasswordHasher.class);
    environment = new MockEnvironment();
    when(passwordHasher.hash(any())).thenAnswer(i -> "hash(" + i.getArgument(0) + ")");
    initializer = new AdminUserInitializer(usuarioRepository, passwordHasher, environment);
  }

  private Usuario salvo() {
    final ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldNotCreateAdminWhenProdWithoutPassword() {
    // Arrange
    environment.setActiveProfiles("prod");

    // Act
    initializer.run(null);

    // Assert
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void shouldNotCreateAdminWhenProdPasswordIsTheDevPassword() {
    // Arrange
    environment.setActiveProfiles("prod");
    environment.setProperty("ADMIN_SENHA", "admin123");

    // Act
    initializer.run(null);

    // Assert
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void shouldNotCreateAdminWhenProdPasswordBreaksPolicy() {
    // Arrange
    environment.setActiveProfiles("prod");
    environment.setProperty("ADMIN_SENHA", "curta");

    // Act
    initializer.run(null);

    // Assert
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void shouldCreateAdminWithConfiguredEmailAndPasswordWhenProd() {
    // Arrange
    environment.setActiveProfiles("prod");
    environment.setProperty("ADMIN_EMAIL", " gestor@rei.com ");
    environment.setProperty("ADMIN_SENHA", "senha-forte-1");

    // Act
    initializer.run(null);

    // Assert
    final Usuario admin = salvo();
    assertEquals("gestor@rei.com", admin.getEmail());
    assertEquals("hash(senha-forte-1)", admin.getSenhaHash());
    assertEquals(PerfilUsuario.ADMINISTRADOR, admin.getPerfil());
  }

  @Test
  void shouldCreateSupportAdminWhenDevWithoutConfiguration() {
    // Arrange
    environment.setActiveProfiles("dev");

    // Act
    initializer.run(null);

    // Assert
    final Usuario admin = salvo();
    assertEquals("admin@rgm.com", admin.getEmail());
    assertEquals("hash(admin123)", admin.getSenhaHash());
  }

  @Test
  void shouldNotCreateAdminWhenEmailAlreadyExists() {
    // Arrange
    environment.setActiveProfiles("test");
    when(usuarioRepository.existsByEmail("admin@rgm.com")).thenReturn(true);

    // Act
    initializer.run(null);

    // Assert
    verify(usuarioRepository, never()).save(any());
  }
}
