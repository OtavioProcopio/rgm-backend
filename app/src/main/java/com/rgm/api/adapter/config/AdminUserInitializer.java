package com.rgm.api.adapter.config;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class AdminUserInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

  private static final String ADMIN_EMAIL = "admin@rgm.com";
  private static final String ADMIN_NAME = "Administrador";
  private static final String ADMIN_PASSWORD = "admin123";

  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;

  public AdminUserInitializer(
      final UsuarioRepository usuarioRepository, final PasswordHasher passwordHasher) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public void run(final ApplicationArguments args) {
    if (usuarioRepository.existsByEmail(ADMIN_EMAIL)) {
      log.info("Admin user '{}' already exists, skipping creation", ADMIN_EMAIL);
      return;
    }

    final Instant agora = Instant.now();
    final String senhaHash = passwordHasher.hash(ADMIN_PASSWORD);
    final Usuario admin =
        Usuario.criarInterno(
            ADMIN_NAME, ADMIN_EMAIL, senhaHash, PerfilUsuario.ADMINISTRADOR, agora);

    usuarioRepository.save(admin);
    log.info("Admin user '{}' created successfully", ADMIN_EMAIL);
  }
}
