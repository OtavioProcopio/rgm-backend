package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.ConflitoException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.validation.LimitesTexto;
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
  private final String instrucao;
  private final Instant criadaEm;
  private final Instant atualizadaEm;
  private final Instant concluidaEm;
  private final Instant canceladaEm;
  private final Long version;

  private Solicitacao(
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
      final String instrucao,
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
    this.instrucao = optionalTrimToNull(instrucao);
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");
    this.atualizadaEm = requireNonNull(atualizadaEm, "atualizadaEm");
    this.concluidaEm = concluidaEm;
    this.canceladaEm = canceladaEm;
    this.version = version;

    validateInvariants();
  }

  /** Ponto unico de montagem: nomeia cada campo e roda as mesmas invariantes do construtor. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Copia desta solicitacao para uma transicao: quem chama diz so o que muda. Um campo novo do
   * agregado entra aqui e em {@link Builder}, e nas transicoes nao precisa ser repetido.
   */
  private Builder toBuilder() {
    return new Builder()
        .id(id)
        .titulo(titulo)
        .descricao(descricao)
        .tipo(tipo)
        .status(status)
        .prioridade(prioridade)
        .modeloId(modeloId)
        .modeloCodigo(modeloCodigo)
        .modeloMaquina(modeloMaquina)
        .modeloObservacoes(modeloObservacoes)
        .abertaPorUsuarioId(abertaPorUsuarioId)
        .comentarioFinal(comentarioFinal)
        .instrucao(instrucao)
        .criadaEm(criadaEm)
        .atualizadaEm(atualizadaEm)
        .concluidaEm(concluidaEm)
        .canceladaEm(canceladaEm)
        .version(version);
  }

  /** Montagem nomeada de {@link Solicitacao}; {@link #build()} valida as invariantes. */
  public static final class Builder {
    private UUID id;
    private String titulo;
    private String descricao;
    private TipoSolicitacao tipo;
    private StatusSolicitacao status;
    private PrioridadeSolicitacao prioridade;
    private UUID modeloId;
    private String modeloCodigo;
    private String modeloMaquina;
    private String modeloObservacoes;
    private UUID abertaPorUsuarioId;
    private String comentarioFinal;
    private String instrucao;
    private Instant criadaEm;
    private Instant atualizadaEm;
    private Instant concluidaEm;
    private Instant canceladaEm;
    private Long version;

    private Builder() {}

    public Builder id(final UUID valor) {
      this.id = valor;
      return this;
    }

    public Builder titulo(final String valor) {
      this.titulo = valor;
      return this;
    }

    public Builder descricao(final String valor) {
      this.descricao = valor;
      return this;
    }

    public Builder tipo(final TipoSolicitacao valor) {
      this.tipo = valor;
      return this;
    }

    public Builder status(final StatusSolicitacao valor) {
      this.status = valor;
      return this;
    }

    public Builder prioridade(final PrioridadeSolicitacao valor) {
      this.prioridade = valor;
      return this;
    }

    public Builder modeloId(final UUID valor) {
      this.modeloId = valor;
      return this;
    }

    public Builder modeloCodigo(final String valor) {
      this.modeloCodigo = valor;
      return this;
    }

    public Builder modeloMaquina(final String valor) {
      this.modeloMaquina = valor;
      return this;
    }

    public Builder modeloObservacoes(final String valor) {
      this.modeloObservacoes = valor;
      return this;
    }

    public Builder abertaPorUsuarioId(final UUID valor) {
      this.abertaPorUsuarioId = valor;
      return this;
    }

    public Builder comentarioFinal(final String valor) {
      this.comentarioFinal = valor;
      return this;
    }

    public Builder instrucao(final String valor) {
      this.instrucao = valor;
      return this;
    }

    public Builder criadaEm(final Instant valor) {
      this.criadaEm = valor;
      return this;
    }

    public Builder atualizadaEm(final Instant valor) {
      this.atualizadaEm = valor;
      return this;
    }

    public Builder concluidaEm(final Instant valor) {
      this.concluidaEm = valor;
      return this;
    }

    public Builder canceladaEm(final Instant valor) {
      this.canceladaEm = valor;
      return this;
    }

    /** Controle de versao do lock otimista: nulo enquanto a solicitacao nao foi persistida. */
    public Builder version(final Long valor) {
      this.version = valor;
      return this;
    }

    public Solicitacao build() {
      return new Solicitacao(
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
          instrucao,
          criadaEm,
          atualizadaEm,
          concluidaEm,
          canceladaEm,
          version);
    }
  }

  /** UC-02: Abre uma nova solicitacao em A_FAZER, vinculada a um modelo existente. */
  public static Solicitacao abrir(
      final String titulo,
      final String descricao,
      final TipoSolicitacao tipo,
      final UUID modeloId,
      final UUID abertaPorUsuarioId,
      final Instant agora) {
    return builder()
        .id(UUID.randomUUID())
        .titulo(titulo)
        .descricao(descricao)
        .tipo(tipo)
        .status(StatusSolicitacao.A_FAZER)
        .modeloId(modeloId)
        .abertaPorUsuarioId(abertaPorUsuarioId)
        .criadaEm(agora)
        .atualizadaEm(agora)
        .build();
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
    return builder()
        .id(UUID.randomUUID())
        .titulo(titulo)
        .descricao(descricao)
        .tipo(TipoSolicitacao.CRIACAO)
        .status(StatusSolicitacao.A_FAZER)
        .modeloCodigo(modeloCodigo)
        .modeloMaquina(modeloMaquina)
        .modeloObservacoes(modeloObservacoes)
        .abertaPorUsuarioId(abertaPorUsuarioId)
        .criadaEm(agora)
        .atualizadaEm(agora)
        .build();
  }

  /**
   * UC-03: Triar e atribuir (A_FAZER -> EM_ANDAMENTO). Requer prioridade e a instrucao do que deve
   * ser feito, que fica gravada na propria solicitacao. So se tria a partir de A_FAZER. Validacao
   * de atribuicoes feita externamente (use case).
   */
  public Solicitacao triar(
      final PrioridadeSolicitacao novaPrioridade, final String novaInstrucao, final Instant agora) {
    requireNonNull(novaPrioridade, "prioridade");
    if (status != StatusSolicitacao.A_FAZER) {
      throw new TransicaoStatusInvalidaException(status, StatusSolicitacao.EM_ANDAMENTO);
    }
    validarTransicao(StatusSolicitacao.EM_ANDAMENTO);
    return toBuilder()
        .status(StatusSolicitacao.EM_ANDAMENTO)
        .prioridade(novaPrioridade)
        .instrucao(validarInstrucao(novaInstrucao))
        .comentarioFinal(null)
        .atualizadaEm(agora)
        .concluidaEm(null)
        .canceladaEm(null)
        .build();
  }

  /**
   * Corrige a instrucao do que fazer. So com a solicitacao EM_ANDAMENTO: antes dela a instrucao
   * nasce na triagem e, depois, o responsavel ja enviou para validacao. Texto igual ao atual nao
   * muda nada e devolve a propria instancia.
   */
  public Solicitacao editarInstrucao(final String novaInstrucao, final Instant agora) {
    if (status != StatusSolicitacao.EM_ANDAMENTO) {
      throw new ConflitoException(
          "A instrucao so pode ser editada com a solicitacao em andamento (status atual: "
              + status
              + ")");
    }
    final String valida = validarInstrucao(novaInstrucao);
    if (valida.equals(instrucao)) {
      return this;
    }
    return toBuilder().instrucao(valida).atualizadaEm(requireNonNull(agora, "agora")).build();
  }

  /** UC-05: Enviar para validacao (EM_ANDAMENTO -> EM_VALIDACAO). */
  public Solicitacao enviarParaValidacao(final Instant agora) {
    validarTransicao(StatusSolicitacao.EM_VALIDACAO);
    return toBuilder()
        .status(StatusSolicitacao.EM_VALIDACAO)
        .comentarioFinal(null)
        .atualizadaEm(agora)
        .concluidaEm(null)
        .canceladaEm(null)
        .build();
  }

  /** UC-06: Devolver para correcao (EM_VALIDACAO -> EM_ANDAMENTO). */
  public Solicitacao devolver(final PrioridadeSolicitacao novaPrioridade, final Instant agora) {
    validarTransicao(StatusSolicitacao.EM_ANDAMENTO);
    final PrioridadeSolicitacao prioridadeFinal =
        novaPrioridade != null ? novaPrioridade : this.prioridade;
    return toBuilder()
        .status(StatusSolicitacao.EM_ANDAMENTO)
        .prioridade(prioridadeFinal)
        .comentarioFinal(null)
        .atualizadaEm(agora)
        .concluidaEm(null)
        .canceladaEm(null)
        .build();
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
    return toBuilder()
        .titulo(requireNonBlank(novoTitulo, "titulo"))
        .descricao(requireNonBlank(novaDescricao, "descricao"))
        .atualizadaEm(agora)
        .build();
  }

  /**
   * Marca a solicitacao como alterada quando seus responsaveis mudam: atualiza a data e, ao salvar,
   * o controle de versao, para que duas trocas simultaneas nao se sobreponham.
   */
  public Solicitacao registrarAlteracaoDeResponsaveis(final Instant agora) {
    return toBuilder().atualizadaEm(requireNonNull(agora, "agora")).build();
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
    return toBuilder()
        .status(StatusSolicitacao.CONCLUIDA)
        .comentarioFinal(novoComentarioFinal)
        .atualizadaEm(agora)
        .concluidaEm(agora)
        .canceladaEm(null)
        .build();
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
    return toBuilder()
        .status(StatusSolicitacao.CONCLUIDA)
        .modeloId(modeloIdCriado)
        .comentarioFinal(novoComentarioFinal)
        .atualizadaEm(agora)
        .concluidaEm(agora)
        .canceladaEm(null)
        .build();
  }

  /** UC-07: Cancelar solicitacao (status permitido -> CANCELADA). */
  public Solicitacao cancelar(final String novoComentarioFinal, final Instant agora) {
    requireNonBlank(novoComentarioFinal, "comentarioFinal");
    validarTransicao(StatusSolicitacao.CANCELADA);
    return toBuilder()
        .status(StatusSolicitacao.CANCELADA)
        .comentarioFinal(novoComentarioFinal)
        .atualizadaEm(agora)
        .concluidaEm(null)
        .canceladaEm(agora)
        .build();
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

  private static String validarInstrucao(final String texto) {
    if (texto == null || texto.isBlank()) {
      throw new ValidationException("instrucao e obrigatoria");
    }
    final String aparada = texto.trim();
    if (aparada.length() > LimitesTexto.SOLICITACAO_INSTRUCAO) {
      throw new ValidationException(
          "instrucao deve ter no maximo " + LimitesTexto.SOLICITACAO_INSTRUCAO + " caracteres");
    }
    return aparada;
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

    if (status == StatusSolicitacao.A_FAZER && instrucao != null) {
      throw new ValidationException("instrucao so existe depois da triagem");
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

  public String getInstrucao() {
    return instrucao;
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
