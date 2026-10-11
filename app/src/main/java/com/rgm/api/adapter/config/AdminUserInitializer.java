package com.rgm.api.adapter.config;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import com.rgm.api.core.domain.validation.PoliticaSenha;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador. Em {@code dev} e {@code test} vale a conta de apoio {@code
 * admin@rgm.com} / {@code admin123}. Em {@code prod} o e-mail ({@code ADMIN_EMAIL}) e a senha
 * ({@code ADMIN_SENHA}) vem do ambiente; sem senha valida, nada e criado.
 */
@Component
@Profile({"dev", "test", "prod"})
public class AdminUserInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

  static final String EMAIL_PADRAO = "admin@rgm.com";
  static final String SENHA_DE_APOIO = "admin123";
  private static final String ADMIN_NAME = "Administrador";

  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;
  private final Environment environment;

  public AdminUserInitializer(
      final UsuarioRepository usuarioRepository,
      final PasswordHasher passwordHasher,
      final Environment environment) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
    this.environment = environment;
  }

  @Override
  public void run(final ApplicationArguments args) {
    final boolean producao = environment.acceptsProfiles(Profiles.of("prod"));
    final String emailConfigurado = environment.getProperty("ADMIN_EMAIL");
    final String email =
        emailConfigurado == null || emailConfigurado.isBlank()
            ? EMAIL_PADRAO
            : emailConfigurado.trim();
    final String senha = senhaInicial(producao);

    if (senha == null) {
      return;
    }
    if (usuarioRepository.existsByEmail(email)) {
      log.info("Admin user '{}' already exists, skipping creation", email);
      return;
    }

    final Usuario admin =
        Usuario.criarInterno(
            ADMIN_NAME,
            email,
            passwordHasher.hash(senha),
            PerfilUsuario.ADMINISTRADOR,
            Instant.now());
    usuarioRepository.save(admin);
    log.info("Admin user '{}' created successfully", email);
  }

  /** Senha a usar, ou {@code null} quando o administrador nao deve ser criado. */
  private String senhaInicial(final boolean producao) {
    final String configurada = environment.getProperty("ADMIN_SENHA");
    final boolean semSenha = configurada == null || configurada.isBlank();

    if (!producao) {
      return semSenha ? SENHA_DE_APOIO : configurada;
    }
    if (semSenha) {
      log.warn(
          "ADMIN_SENHA nao definida: nenhum administrador foi criado. Defina ADMIN_EMAIL e"
              + " ADMIN_SENHA para criar o primeiro administrador.");
      return null;
    }
    if (SENHA_DE_APOIO.equals(configurada)) {
      log.warn("ADMIN_SENHA recusada: a senha de apoio de desenvolvimento nao vale em producao.");
      return null;
    }
    try {
      PoliticaSenha.validar(configurada);
    } catch (final ValidationException e) {
      log.warn("ADMIN_SENHA recusada: {}", e.getMessage());
      return null;
    }
    return configurada;
  }
}
