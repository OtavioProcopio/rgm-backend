package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requirePositiveOrZero;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.time.Instant;
import java.util.UUID;

/** Entidade de usuario do sistema (inclui prestador EXTERNO). */
public final class Usuario {

  private final UUID id;
  private final String nome;
  private final String email;
  private final String senhaHash;
  private final PerfilUsuario perfil;
  private final boolean ativo;
  private final Instant criadoEm;
  private final Instant atualizadoEm;
  private final int versaoCredencial;

  /** Usuario cujas credenciais nunca foram invalidadas (versao 0). */
  public Usuario(
      final UUID id,
      final String nome,
      final String email,
      final String senhaHash,
      final PerfilUsuario perfil,
      final boolean ativo,
      final Instant criadoEm,
      final Instant atualizadoEm) {
    this(id, nome, email, senhaHash, perfil, ativo, criadoEm, atualizadoEm, 0);
  }

  public Usuario(
      final UUID id,
      final String nome,
      final String email,
      final String senhaHash,
      final PerfilUsuario perfil,
      final boolean ativo,
      final Instant criadoEm,
      final Instant atualizadoEm,
      final int versaoCredencial) {
    this.id = requireNonNull(id, "id");
    this.nome = requireNonBlank(nome, "nome");
    this.perfil = requireNonNull(perfil, "perfil");
    this.ativo = ativo;
    this.criadoEm = requireNonNull(criadoEm, "criadoEm");
    this.atualizadoEm = requireNonNull(atualizadoEm, "atualizadoEm");
    this.versaoCredencial = requirePositiveOrZero(versaoCredencial, "versaoCredencial");

    this.email = optionalTrimToNull(email);
    this.senhaHash = optionalTrimToNull(senhaHash);

    validatePerfilInvariants();
  }

  /** Cria um novo usuario interno (OPERADOR, GESTOR ou ADMINISTRADOR). */
  public static Usuario criarInterno(
      final String nome,
      final String email,
      final String senhaHash,
      final PerfilUsuario perfil,
      final Instant agora) {
    if (perfil == PerfilUsuario.EXTERNO) {
      throw new ValidationException("Use criarExterno() para perfil EXTERNO");
    }
    return new Usuario(UUID.randomUUID(), nome, email, senhaHash, perfil, true, agora, agora);
  }

  /** Cria um prestador externo (sem login). */
  public static Usuario criarExterno(final String nome, final Instant agora) {
    return new Usuario(
        UUID.randomUUID(), nome, null, null, PerfilUsuario.EXTERNO, true, agora, agora);
  }

  private void validatePerfilInvariants() {
    if (perfil == PerfilUsuario.EXTERNO) {
      if (senhaHash != null) {
        throw new ValidationException("senhaHash deve ser nulo para perfil EXTERNO");
      }
    } else {
      if (email == null) {
        throw new ValidationException("email e obrigatorio para perfis internos");
      }
      if (senhaHash == null) {
        throw new ValidationException("senhaHash e obrigatorio para perfis internos");
      }
    }
  }

  public Usuario editar(final String novoNome, final String novoEmail, final Instant agora) {
    return new Usuario(
        id, novoNome, novoEmail, senhaHash, perfil, ativo, criadoEm, agora, versaoCredencial);
  }

  public Usuario withAtivo(final boolean novoAtivo, final Instant novoAtualizadoEm) {
    return new Usuario(
        id,
        nome,
        email,
        senhaHash,
        perfil,
        novoAtivo,
        criadoEm,
        novoAtualizadoEm,
        versaoCredencial);
  }

  public Usuario alterarPerfil(final PerfilUsuario novoPerfil, final Instant agora) {
    return new Usuario(
        id, nome, email, senhaHash, novoPerfil, ativo, criadoEm, agora, versaoCredencial);
  }

  /** Troca a senha e invalida as credenciais emitidas ate aqui. */
  public Usuario withSenha(final String novaSenhaHash, final Instant agora) {
    return new Usuario(
        id, nome, email, novaSenhaHash, perfil, ativo, criadoEm, agora, versaoCredencial + 1);
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public PerfilUsuario getPerfil() {
    return perfil;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  /** Numero que as credenciais emitidas carregam; muda quando a senha muda. */
  public int getVersaoCredencial() {
    return versaoCredencial;
  }
}
