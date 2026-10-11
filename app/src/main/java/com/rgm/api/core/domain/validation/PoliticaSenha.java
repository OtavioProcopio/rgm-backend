package com.rgm.api.core.domain.validation;

import com.rgm.api.core.domain.exceptions.ValidationException;

/** Regra unica de senha para criar usuario, trocar a propria senha e redefinir a de outro. */
public final class PoliticaSenha {

  public static final int TAMANHO_MINIMO = 8;

  private PoliticaSenha() {}

  /** Recusa senha ausente, em branco ou com menos caracteres que o minimo. */
  public static void validar(final String senha) {
    if (senha == null || senha.isBlank() || senha.length() < TAMANHO_MINIMO) {
      throw new ValidationException("Senha deve ter no minimo " + TAMANHO_MINIMO + " caracteres");
    }
  }
}
