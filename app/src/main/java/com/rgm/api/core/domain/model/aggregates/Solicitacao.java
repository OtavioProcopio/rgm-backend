package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;

/** Entidade de Solicitacao (card Kanban). Agregado raiz do fluxo de solicitacoes. */
public final class Solicitacao {

  private final UUID id;
  private final String titulo;
  private final String descricao;
  private final TipoSolicitacao tipo;
  private final StatusSolicitacao status;
  private final PrioridadeSolicitacao prioridade;
  private final UUID modeloId;
  private final UUID abertaPorUsuarioId;
  private final String comentarioFinal;
  private final Instant criadaEm;
  private final Instant atualizadaEm;
  private final Instant concluidaEm;
  private final Instant canceladaEm;

  public Solicitacao(
      final UUID id,
      final String titulo,
      final String descricao,
      final TipoSolicitacao tipo,
      final StatusSolicitacao status,
      final PrioridadeSolicitacao prioridade,
      final UUID modeloId,
      final UUID abertaPorUsuarioId,
      final String comentarioFinal,
      final Instant criadaEm,
      final Instant atualizadaEm,
      final Instant concluidaEm,
      final Instant canceladaEm) {
    this.id = requireNonNull(id, "id");
    this.titulo = requireNonBlank(titulo, "titulo");
    this.descricao = requireNonBlank(descricao, "descricao");
    this.tipo = requireNonNull(tipo, "tipo");
    this.status = requireNonNull(status, "status");
    this.prioridade = prioridade;
    this.modeloId = requireNonNull(modeloId, "modeloId");
    this.abertaPorUsuarioId = requireNonNull(abertaPorUsuarioId, "abertaPorUsuarioId");
    this.comentarioFinal = optionalTrimToNull(comentarioFinal);
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");
    this.atualizadaEm = requireNonNull(atualizadaEm, "atualizadaEm");
    this.concluidaEm = concluidaEm;
    this.canceladaEm = canceladaEm;

    validateInvariants();
  }

  /** UC-02: Abre uma nova solicitacao em A_FAZER. */
  public static Solicitacao abrir(
      final String titulo,
      final String descricao,
      final TipoSolicitacao tipo,
      final UUID modeloId,
      final UUID abertaPorUsuarioId,
      final Instant agora) {
    return new Solicitacao(
        UUID.randomUUID(),
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.A_FAZER,
        null,
        modeloId,
        abertaPorUsuarioId,
        null,
        agora,
        agora,
        null,
        null);
  }

  /**
   * UC-03: Triar e atribuir (A_FAZER -> EM_ANDAMENTO). Requer prioridade. Validacao de atribuicoes
   * feita externamente (use case).
   */
  public Solicitacao triar(final PrioridadeSolicitacao novaPrioridade, final Instant agora) {
    requireNonNull(novaPrioridade, "prioridade");
    validarTransicao(StatusSolicitacao.EM_ANDAMENTO);
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.EM_ANDAMENTO,
        novaPrioridade,
        modeloId,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null);
  }

  /** UC-05: Enviar para validacao (EM_ANDAMENTO -> EM_VALIDACAO). */
  public Solicitacao enviarParaValidacao(final Instant agora) {
    validarTransicao(StatusSolicitacao.EM_VALIDACAO);
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.EM_VALIDACAO,
        prioridade,
        modeloId,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null);
  }

  /** UC-06: Devolver para correcao (EM_VALIDACAO -> EM_ANDAMENTO). */
  public Solicitacao devolver(final PrioridadeSolicitacao novaPrioridade, final Instant agora) {
    validarTransicao(StatusSolicitacao.EM_ANDAMENTO);
    final PrioridadeSolicitacao prioridadeFinal =
        novaPrioridade != null ? novaPrioridade : this.prioridade;
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.EM_ANDAMENTO,
        prioridadeFinal,
        modeloId,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null);
  }

  /** Editar titulo, descricao e tipo (somente em status nao-terminal). */
  public Solicitacao editar(
      final String novoTitulo,
      final String novaDescricao,
      final TipoSolicitacao novoTipo,
      final Instant agora) {
    if (!status.isNaoTerminal()) {
      throw new BusinessRuleException("Nao e possivel editar solicitacao em status terminal");
    }
    return new Solicitacao(
        id,
        requireNonBlank(novoTitulo, "titulo"),
        requireNonBlank(novaDescricao, "descricao"),
        requireNonNull(novoTipo, "tipo"),
        status,
        prioridade,
        modeloId,
        abertaPorUsuarioId,
        comentarioFinal,
        criadaEm,
        agora,
        concluidaEm,
        canceladaEm);
  }

  /** UC-07: Concluir solicitacao (EM_VALIDACAO -> CONCLUIDA). */
  public Solicitacao concluir(final String novoComentarioFinal, final Instant agora) {
    requireNonBlank(novoComentarioFinal, "comentarioFinal");
    validarTransicao(StatusSolicitacao.CONCLUIDA);
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.CONCLUIDA,
        prioridade,
        modeloId,
        abertaPorUsuarioId,
        novoComentarioFinal,
        criadaEm,
        agora,
        agora,
        null);
  }

  /** UC-07: Cancelar solicitacao (status permitido -> CANCELADA). */
  public Solicitacao cancelar(final String novoComentarioFinal, final Instant agora) {
    requireNonBlank(novoComentarioFinal, "comentarioFinal");
    validarTransicao(StatusSolicitacao.CANCELADA);
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.CANCELADA,
        prioridade,
        modeloId,
        abertaPorUsuarioId,
        novoComentarioFinal,
        criadaEm,
        agora,
        null,
        agora);
  }

  /**
   * UC-04: Valida se o usuario pode mover esta solicitacao. OPERADOR: so move se atribuido e
   * transicao permitida ao perfil. GESTOR/ADMINISTRADOR: pode mover qualquer solicitacao.
   */
  public static void validarAutorizacaoMover(
      final PerfilUsuario perfil,
      final StatusSolicitacao de,
      final StatusSolicitacao para,
      final boolean estaAtribuido) {
    requireNonNull(perfil, "perfil");
    requireNonNull(de, "statusAtual");
    requireNonNull(para, "novoStatus");

    if (perfil == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode mover solicitacoes");
    }

    if (perfil.podeMoverQualquer()) {
      return;
    }

    if (perfil == PerfilUsuario.OPERADOR) {
      if (!estaAtribuido) {
        throw new NaoAutorizadoException("Operador so pode mover solicitacoes atribuidas a ele");
      }
      if (de != StatusSolicitacao.EM_ANDAMENTO || para != StatusSolicitacao.EM_VALIDACAO) {
        throw new NaoAutorizadoException(
            "Operador so pode mover de EM_ANDAMENTO para EM_VALIDACAO");
      }
    }
  }

  /** Valida que o perfil de um responsavel permite atribuicao. */
  public static void validarPerfilAtribuivel(final PerfilUsuario perfil) {
    requireNonNull(perfil, "perfil");
    if (!perfil.isAtribuivel()) {
      throw new BusinessRuleException(
          "Perfil " + perfil.name() + " nao pode ser atribuido como responsavel");
    }
  }

  private void validarTransicao(final StatusSolicitacao novoStatus) {
    if (!status.canTransitionTo(novoStatus)) {
      throw new TransicaoStatusInvalidaException(status, novoStatus);
    }
  }

  private void validateInvariants() {
    if (status.exigePrioridade() && prioridade == null) {
      throw new ValidationException("prioridade e obrigatoria a partir de EM_ANDAMENTO");
    }

    if (status == StatusSolicitacao.CONCLUIDA) {
      if (concluidaEm == null) {
        throw new ValidationException("concluidaEm e obrigatorio quando status=CONCLUIDA");
      }
      if (canceladaEm != null) {
        throw new ValidationException("canceladaEm deve ser nulo quando status=CONCLUIDA");
      }
      if (comentarioFinal == null) {
        throw new ValidationException("comentarioFinal e obrigatorio ao concluir");
      }
    }

    if (status == StatusSolicitacao.CANCELADA) {
      if (canceladaEm == null) {
        throw new ValidationException("canceladaEm e obrigatorio quando status=CANCELADA");
      }
      if (concluidaEm != null) {
        throw new ValidationException("concluidaEm deve ser nulo quando status=CANCELADA");
      }
      if (comentarioFinal == null) {
        throw new ValidationException("comentarioFinal e obrigatorio ao cancelar");
      }
    }

    if (status.isNaoTerminal()) {
      if (concluidaEm != null || canceladaEm != null) {
        throw new ValidationException("datas terminais devem ser nulas em status nao-terminal");
      }
    }
  }

  public UUID getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescricao() {
    return descricao;
  }

  public TipoSolicitacao getTipo() {
    return tipo;
  }

  public StatusSolicitacao getStatus() {
    return status;
  }

  public PrioridadeSolicitacao getPrioridade() {
    return prioridade;
  }

  public UUID getModeloId() {
    return modeloId;
  }

  public UUID getAbertaPorUsuarioId() {
    return abertaPorUsuarioId;
  }

  public String getComentarioFinal() {
    return comentarioFinal;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }

  public Instant getAtualizadaEm() {
    return atualizadaEm;
  }

  public Instant getConcluidaEm() {
    return concluidaEm;
  }

  public Instant getCanceladaEm() {
    return canceladaEm;
  }
}
