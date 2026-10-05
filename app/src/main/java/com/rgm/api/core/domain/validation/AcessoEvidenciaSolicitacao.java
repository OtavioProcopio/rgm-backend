package com.rgm.api.core.domain.validation;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Regra de acesso as evidencias de uma solicitacao. Gestor, administrador e responsavel atribuido
 * anexam qualquer tipo e listam; quem abriu a solicitacao lista e anexa apenas ABERTURA e GERAL.
 * Usuario inativo nao tem acesso.
 */
public final class AcessoEvidenciaSolicitacao {

  private static final Set<TipoEvidencia> TIPOS_PERMITIDOS_AO_AUTOR =
      EnumSet.of(TipoEvidencia.ABERTURA, TipoEvidencia.GERAL);

  private AcessoEvidenciaSolicitacao() {}

  /** Indica se o usuario pode listar as evidencias da solicitacao. */
  public static boolean podeListar(
      final Usuario usuario, final Solicitacao solicitacao, final BooleanSupplier estaAtribuido) {
    requireNonNull(usuario, "usuario");
    return usuario.isAtivo()
        && (temAcessoPleno(usuario, estaAtribuido) || abriu(usuario, solicitacao));
  }

  /** Valida se o usuario pode listar as evidencias da solicitacao. */
  public static void validarListagem(
      final Usuario usuario, final Solicitacao solicitacao, final BooleanSupplier estaAtribuido) {
    exigirAtivo(usuario);
    if (!podeListar(usuario, solicitacao, estaAtribuido)) {
      throw semAcesso();
    }
  }

  /** Valida se o usuario pode anexar uma evidencia do tipo informado a solicitacao. */
  public static void validarAnexo(
      final Usuario usuario,
      final Solicitacao solicitacao,
      final BooleanSupplier estaAtribuido,
      final TipoEvidencia tipo) {
    requireNonNull(tipo, "tipo");
    exigirAtivo(usuario);
    if (temAcessoPleno(usuario, estaAtribuido)) {
      return;
    }
    if (!abriu(usuario, solicitacao)) {
      throw semAcesso();
    }
    if (!TIPOS_PERMITIDOS_AO_AUTOR.contains(tipo)) {
      throw new NaoAutorizadoException(
          "Quem abriu a solicitacao so pode anexar evidencia do tipo ABERTURA ou GERAL");
    }
  }

  private static void exigirAtivo(final Usuario usuario) {
    requireNonNull(usuario, "usuario");
    if (!usuario.isAtivo()) {
      throw new NaoAutorizadoException("Usuario inativo");
    }
  }

  private static boolean temAcessoPleno(
      final Usuario usuario, final BooleanSupplier estaAtribuido) {
    requireNonNull(estaAtribuido, "estaAtribuido");
    return usuario.getPerfil().podeGerenciarModelos()
        || usuario.getPerfil().podeGerenciarUsuariosEMaquinas()
        || estaAtribuido.getAsBoolean();
  }

  private static boolean abriu(final Usuario usuario, final Solicitacao solicitacao) {
    requireNonNull(solicitacao, "solicitacao");
    return usuario.getId().equals(solicitacao.getAbertaPorUsuarioId());
  }

  private static NaoAutorizadoException semAcesso() {
    return new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao");
  }
}
