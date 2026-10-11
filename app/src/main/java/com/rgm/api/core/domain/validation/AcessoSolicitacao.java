package com.rgm.api.core.domain.validation;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.util.function.BooleanSupplier;

/**
 * Regra de leitura de uma solicitacao. Gestor e administrador veem todas; operador ve a que abriu
 * ou da qual e responsavel ativo. Usuario inativo nao ve nenhuma.
 */
public final class AcessoSolicitacao {

  private AcessoSolicitacao() {}

  /** Indica se o perfil do usuario le todas as solicitacoes, sem depender de vinculo com elas. */
  public static boolean veTodas(final Usuario usuario) {
    requireNonNull(usuario, "usuario");
    return usuario.getPerfil().podeMoverQualquer();
  }

  /** Indica se o usuario pode ler a solicitacao. */
  public static boolean podeVer(
      final Usuario usuario, final Solicitacao solicitacao, final BooleanSupplier estaAtribuido) {
    requireNonNull(usuario, "usuario");
    requireNonNull(solicitacao, "solicitacao");
    requireNonNull(estaAtribuido, "estaAtribuido");
    return usuario.isAtivo()
        && (veTodas(usuario)
            || estaAtribuido.getAsBoolean()
            || usuario.getId().equals(solicitacao.getAbertaPorUsuarioId()));
  }

  /** Valida se o usuario pode ler a solicitacao. */
  public static void validarLeitura(
      final Usuario usuario, final Solicitacao solicitacao, final BooleanSupplier estaAtribuido) {
    if (!podeVer(usuario, solicitacao, estaAtribuido)) {
      throw new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao");
    }
  }
}
