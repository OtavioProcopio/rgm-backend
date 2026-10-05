package com.rgm.api.core.domain.validation;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Calcula as acoes que um usuario pode executar sobre uma solicitacao, a partir das mesmas regras
 * de perfil, autoria, atribuicao e status aplicadas pelos casos de uso. Nao considera pre-condicoes
 * de dados da acao, como a evidencia de servico exigida para enviar a validacao.
 */
public final class AcoesPermitidasSolicitacao {

  private AcoesPermitidasSolicitacao() {}

  /**
   * @param estaAtribuido se o usuario e responsavel ativo da solicitacao
   * @param temResponsavel se a solicitacao tem ao menos um responsavel ativo
   */
  public static Set<AcaoSolicitacao> calcular(
      final Usuario usuario,
      final Solicitacao solicitacao,
      final boolean estaAtribuido,
      final boolean temResponsavel) {
    requireNonNull(usuario, "usuario");
    requireNonNull(solicitacao, "solicitacao");

    final Set<AcaoSolicitacao> acoes = EnumSet.noneOf(AcaoSolicitacao.class);
    final PerfilUsuario perfil = usuario.getPerfil();
    if (!usuario.isAtivo() || perfil == PerfilUsuario.EXTERNO) {
      return Collections.unmodifiableSet(acoes);
    }

    final StatusSolicitacao status = solicitacao.getStatus();
    final boolean aberta = status.isNaoTerminal();
    final boolean gerencia = perfil.podeMoverQualquer();
    final boolean abriu = usuario.getId().equals(solicitacao.getAbertaPorUsuarioId());

    if (perfil.podeTriar() && status == StatusSolicitacao.A_FAZER) {
      acoes.add(AcaoSolicitacao.TRIAR);
    }
    if (status == StatusSolicitacao.EM_ANDAMENTO && (gerencia || estaAtribuido)) {
      acoes.add(AcaoSolicitacao.ENVIAR_VALIDACAO);
    }
    if (perfil.podeDevolver() && status == StatusSolicitacao.EM_VALIDACAO) {
      acoes.add(AcaoSolicitacao.DEVOLVER);
    }
    if (perfil.podeEncerrar() && status == StatusSolicitacao.EM_VALIDACAO) {
      acoes.add(AcaoSolicitacao.ENCERRAR);
    }
    if (aberta && (perfil.podeEncerrar() || autorPodeCancelar(status, abriu, temResponsavel))) {
      acoes.add(AcaoSolicitacao.CANCELAR);
    }
    if (aberta && (gerencia || abriu)) {
      acoes.add(AcaoSolicitacao.EDITAR);
    }
    if (gerencia || (aberta && (abriu || estaAtribuido))) {
      acoes.add(AcaoSolicitacao.COMENTAR);
    }
    if (perfil.podeTriar() && aberta) {
      acoes.add(AcaoSolicitacao.ALTERAR_RESPONSAVEIS);
    }
    if (aberta
        && AcessoEvidenciaSolicitacao.podeListar(usuario, solicitacao, () -> estaAtribuido)) {
      acoes.add(AcaoSolicitacao.ANEXAR_EVIDENCIA);
    }
    return Collections.unmodifiableSet(acoes);
  }

  private static boolean autorPodeCancelar(
      final StatusSolicitacao status, final boolean abriu, final boolean temResponsavel) {
    return status == StatusSolicitacao.A_FAZER && abriu && !temResponsavel;
  }
}
