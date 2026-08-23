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
import java.time.Duration;
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
  private final String modeloCodigo;
  private final String modeloMaquina;
  private final String modeloObservacoes;
  private final UUID abertaPorUsuarioId;
  private final String comentarioFinal;
  private final Instant criadaEm;
  private final Instant atualizadaEm;
  private final Instant concluidaEm;
  private final Instant canceladaEm;
  private final Long version;

  public Solicitacao(
      final UUID id,
      final String titulo,
      final String descricao,
      final TipoSolicitacao tipo,
      final StatusSolicitacao status,
      final PrioridadeSolicitacao prioridade,
      final UUID modeloId,
      final String modeloCodigo,
      final String modeloMaquina,
      final String modeloObservacoes,
      final UUID abertaPorUsuarioId,
      final String comentarioFinal,
      final Instant criadaEm,
      final Instant atualizadaEm,
      final Instant concluidaEm,
      final Instant canceladaEm) {
    this(
        id,
        titulo,
        descricao,
        tipo,
        status,
        prioridade,
        modeloId,
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        comentarioFinal,
        criadaEm,
        atualizadaEm,
        concluidaEm,
        canceladaEm,
        null);
  }

  /**
   * Construtor completo, incluindo o {@code version} usado para lock otimista na persistencia
   * (issue #80) — preservado, nao recalculado, a cada transicao. O construtor publico de 16
   * argumentos cobre o caso comum (version nulo, ainda nao persistido).
   */
  public Solicitacao(
      final UUID id,
      final String titulo,
      final String descricao,
      final TipoSolicitacao tipo,
      final StatusSolicitacao status,
      final PrioridadeSolicitacao prioridade,
      final UUID modeloId,
      final String modeloCodigo,
      final String modeloMaquina,
      final String modeloObservacoes,
      final UUID abertaPorUsuarioId,
      final String comentarioFinal,
      final Instant criadaEm,
      final Instant atualizadaEm,
      final Instant concluidaEm,
      final Instant canceladaEm,
      final Long version) {
    this.id = requireNonNull(id, "id");
    this.titulo = requireNonBlank(titulo, "titulo");
    this.descricao = requireNonBlank(descricao, "descricao");
    this.tipo = requireNonNull(tipo, "tipo");
    this.status = requireNonNull(status, "status");
    this.prioridade = prioridade;
    this.modeloId = modeloId;
    this.modeloCodigo = optionalTrimToNull(modeloCodigo);
    this.modeloMaquina = optionalTrimToNull(modeloMaquina);
    this.modeloObservacoes = optionalTrimToNull(modeloObservacoes);
    this.abertaPorUsuarioId = requireNonNull(abertaPorUsuarioId, "abertaPorUsuarioId");
    this.comentarioFinal = optionalTrimToNull(comentarioFinal);
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");
    this.atualizadaEm = requireNonNull(atualizadaEm, "atualizadaEm");
    this.concluidaEm = concluidaEm;
    this.canceladaEm = canceladaEm;
    this.version = version;

    validateInvariants();
  }

  /** UC-02: Abre uma nova solicitacao em A_FAZER, vinculada a um modelo existente. */
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
        null,
        null,
        null,
        abertaPorUsuarioId,
        null,
        agora,
        agora,
        null,
        null);
  }

  /**
   * Abre uma nova solicitacao do tipo CRIACAO em A_FAZER — sem modelo vinculado, carregando os
   * dados do modelo a ser criado quando a solicitacao for concluida.
   */
  public static Solicitacao abrirCriacao(
      final String titulo,
      final String descricao,
      final String modeloCodigo,
      final String modeloMaquina,
      final String modeloObservacoes,
      final UUID abertaPorUsuarioId,
      final Instant agora) {
    return new Solicitacao(
        UUID.randomUUID(),
        titulo,
        descricao,
        TipoSolicitacao.CRIACAO,
        StatusSolicitacao.A_FAZER,
        null,
        null,
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
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
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null,
        version);
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
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null,
        version);
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
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        null,
        criadaEm,
        agora,
        null,
        null,
        version);
  }

  /**
   * Editar titulo e descricao (somente em status nao-terminal). O tipo e imutavel apos a abertura:
   * {@code novoTipo} so e aceito se igual ao tipo atual, e e ignorado (mantido) quando nulo —
   * qualquer tentativa de mudanca e rejeitada.
   */
  public Solicitacao editar(
      final String novoTitulo,
      final String novaDescricao,
      final TipoSolicitacao novoTipo,
      final Instant agora) {
    if (!status.isNaoTerminal()) {
      throw new BusinessRuleException("Nao e possivel editar solicitacao em status terminal");
    }
    if (novoTipo != null && novoTipo != tipo) {
      throw new BusinessRuleException("Tipo da solicitacao e imutavel apos a abertura");
    }
    return new Solicitacao(
        id,
        requireNonBlank(novoTitulo, "titulo"),
        requireNonBlank(novaDescricao, "descricao"),
        tipo,
        status,
        prioridade,
        modeloId,
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        comentarioFinal,
        criadaEm,
        agora,
        concluidaEm,
        canceladaEm,
        version);
  }

  /** UC-07: Concluir solicitacao (EM_VALIDACAO -> CONCLUIDA). Nao se aplica ao tipo CRIACAO. */
  public Solicitacao concluir(final String novoComentarioFinal, final Instant agora) {
    if (tipo == TipoSolicitacao.CRIACAO) {
      throw new BusinessRuleException(
          "Solicitacao do tipo CRIACAO deve ser concluida via concluirCriacao(), que cria o"
              + " Modelo e vincula o modeloId");
    }
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
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        novoComentarioFinal,
        criadaEm,
        agora,
        agora,
        null,
        version);
  }

  /**
   * UC-07 (CRIACAO): Concluir uma solicitacao do tipo CRIACAO, vinculando o {@code Modelo}
   * recem-criado a partir dos dados carregados por ela.
   */
  public Solicitacao concluirCriacao(
      final String novoComentarioFinal, final UUID modeloIdCriado, final Instant agora) {
    if (tipo != TipoSolicitacao.CRIACAO) {
      throw new BusinessRuleException(
          "concluirCriacao() so se aplica a solicitacoes do tipo CRIACAO");
    }
    requireNonBlank(novoComentarioFinal, "comentarioFinal");
    requireNonNull(modeloIdCriado, "modeloId");
    validarTransicao(StatusSolicitacao.CONCLUIDA);
    return new Solicitacao(
        id,
        titulo,
        descricao,
        tipo,
        StatusSolicitacao.CONCLUIDA,
        prioridade,
        modeloIdCriado,
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        novoComentarioFinal,
        criadaEm,
        agora,
        agora,
        null,
        version);
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
        modeloCodigo,
        modeloMaquina,
        modeloObservacoes,
        abertaPorUsuarioId,
        novoComentarioFinal,
        criadaEm,
        agora,
        null,
        agora,
        version);
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

  /**
   * Valida se o perfil pode abrir uma solicitacao do tipo CRIACAO — restrito a GESTOR e
   * ADMINISTRADOR, diferente dos demais tipos (abertos por qualquer usuario interno).
   */
  public static void validarAutorizacaoAbrirCriacao(final PerfilUsuario perfil) {
    requireNonNull(perfil, "perfil");
    if (!perfil.podeGerenciarModelos()) {
      throw new NaoAutorizadoException(
          "Apenas GESTOR/ADMINISTRADOR podem abrir solicitacao do tipo CRIACAO");
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

    if (tipo == TipoSolicitacao.CRIACAO) {
      if (modeloCodigo == null) {
        throw new ValidationException(
            "modeloCodigo e obrigatorio para solicitacao do tipo CRIACAO");
      }
      if (modeloMaquina == null) {
        throw new ValidationException(
            "modeloMaquina e obrigatorio para solicitacao do tipo CRIACAO");
      }
      if (status == StatusSolicitacao.CONCLUIDA) {
        if (modeloId == null) {
          throw new ValidationException(
              "modeloId e obrigatorio quando a solicitacao CRIACAO esta concluida");
        }
      } else if (modeloId != null) {
        throw new ValidationException(
            "modeloId deve ser nulo ate a solicitacao CRIACAO ser concluida");
      }
    } else {
      if (modeloId == null) {
        throw new ValidationException("modeloId e obrigatorio para este tipo de solicitacao");
      }
      if (modeloCodigo != null || modeloMaquina != null || modeloObservacoes != null) {
        throw new ValidationException(
            "dados de modelo em preparacao nao se aplicam a este tipo de solicitacao");
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

  public String getModeloCodigo() {
    return modeloCodigo;
  }

  public String getModeloMaquina() {
    return modeloMaquina;
  }

  public String getModeloObservacoes() {
    return modeloObservacoes;
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

  /** Version usada para lock otimista na persistencia (issue #80). Nulo se ainda nao persistida. */
  public Long getVersion() {
    return version;
  }

  /**
   * SOL-007: data-limite de SLA, contada a partir da abertura ({@code criadaEm}) mais as horas de
   * prazo da prioridade atual. {@code null} enquanto a solicitacao nao foi triada (sem prioridade).
   */
  public Instant getPrazoLimite() {
    if (prioridade == null) {
      return null;
    }
    return criadaEm.plusSeconds(prioridade.slaHoras() * 3600L);
  }

  /**
   * SOL-007: tempo restante ate o prazo de SLA, em segundos (negativo se ja vencido). {@code null}
   * se a solicitacao nao tem prioridade ainda ou ja esta em status terminal.
   */
  public Long getTempoRestanteSegundos(final Instant agora) {
    final Instant prazo = getPrazoLimite();
    if (prazo == null || !status.isNaoTerminal()) {
      return null;
    }
    return Duration.between(agora, prazo).getSeconds();
  }

  /**
   * SOL-007: indica se a solicitacao esta ou ficou atrasada em relacao ao SLA. Para status
   * nao-terminal, compara com o instante atual; para CONCLUIDA, compara com {@code concluidaEm};
   * CANCELADA nunca conta como atrasada.
   */
  public boolean isAtrasada(final Instant agora) {
    final Instant prazo = getPrazoLimite();
    if (prazo == null || status == StatusSolicitacao.CANCELADA) {
      return false;
    }
    if (status == StatusSolicitacao.CONCLUIDA) {
      return concluidaEm.isAfter(prazo);
    }
    return agora.isAfter(prazo);
  }

  /**
   * SOL-007: tempo total de resolucao (abertura ate conclusao), em segundos. {@code null} se a
   * solicitacao ainda nao foi concluida.
   */
  public Long getTempoResolucaoSegundos() {
    if (status != StatusSolicitacao.CONCLUIDA) {
      return null;
    }
    return Duration.between(criadaEm, concluidaEm).getSeconds();
  }
}
