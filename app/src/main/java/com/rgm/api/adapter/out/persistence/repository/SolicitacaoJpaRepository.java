package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SolicitacaoJpaRepository extends JpaRepository<SolicitacaoJpaEntity, UUID> {

  /** SOL-007: prazo-limite = criada_em + horas de SLA da prioridade atual. */
  String PRAZO_LIMITE_EXPR =
      "(s.criada_em + (CASE s.prioridade "
          + "WHEN 'URGENTE' THEN INTERVAL '4 hours' "
          + "WHEN 'ALTA' THEN INTERVAL '24 hours' "
          + "WHEN 'MEDIA' THEN INTERVAL '72 hours' "
          + "WHEN 'BAIXA' THEN INTERVAL '168 hours' END))";

  /** issue #81: filtro por atrasada — espelha Solicitacao#isAtrasada em SQL. */
  String ATRASADA_FILTRO =
      "(CAST(:atrasada AS boolean) IS NULL OR ("
          + "s.prioridade IS NOT NULL AND ("
          + "CASE s.status "
          + "WHEN 'CANCELADA' THEN false "
          + "WHEN 'CONCLUIDA' THEN s.concluida_em > "
          + PRAZO_LIMITE_EXPR
          + " "
          + "ELSE now() > "
          + PRAZO_LIMITE_EXPR
          + " "
          + "END) = :atrasada))";

  /** Data de encerramento: conclusao das concluidas, cancelamento das canceladas. */
  String ENCERRADA_EM_EXPR = "COALESCE(s.concluida_em, s.cancelada_em)";

  /** Status em aberto: espelha StatusSolicitacao#isNaoTerminal. */
  String STATUS_EM_ABERTO = "s.status IN ('A_FAZER', 'EM_ANDAMENTO', 'EM_VALIDACAO')";

  /** "Em aberto"; desligado nao filtra. */
  String EM_ABERTO_FILTRO = "(:emAberto = false OR " + STATUS_EM_ABERTO + ")";

  /** Filtro opcional por modelo nas consultas do painel (nulo nao filtra). */
  String PAINEL_MODELO_FILTRO = "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId)";

  /**
   * Visibilidade do operador: nulo nao restringe; preenchido traz so o que o usuario abriu ou de
   * que e responsavel ativo. Espelha AcessoSolicitacao#podeVer.
   */
  String VISIVEL_PARA_FILTRO =
      "(CAST(:visivelPara AS uuid) IS NULL OR s.aberta_por_usuario_id = :visivelPara OR EXISTS ("
          + "SELECT 1 FROM solicitacao_atribuicoes v WHERE v.solicitacao_id = s.id "
          + "AND v.usuario_id = :visivelPara AND v.removido_em IS NULL))";

  /**
   * Busca textual: o termo (ja com %, _ e \\ escapados) casa, sem caixa e sem acento, com o titulo,
   * a descricao e o codigo do modelo (o do cadastro ou o pretendido na solicitacao de criacao).
   */
  String BUSCA_FILTRO =
      "(CAST(:busca AS text) IS NULL OR "
          + "rgm_sem_acento(s.titulo) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%' OR "
          + "rgm_sem_acento(COALESCE(s.descricao, '')) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%' OR "
          + "rgm_sem_acento(COALESCE(mo.codigo, '')) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%' OR "
          + "rgm_sem_acento(COALESCE(s.modelo_codigo, '')) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%')";

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  boolean existsByModeloId(UUID modeloId);

  boolean existsByAbertaPorUsuarioId(UUID abertaPorUsuarioId);

  List<SolicitacaoJpaEntity> findByModeloId(UUID modeloId);

  /** Origem e filtros da listagem; compartilhados pela consulta e pela contagem. */
  String LISTAGEM_FROM_WHERE =
      "FROM solicitacoes s "
          + "LEFT JOIN modelos mo ON mo.id = s.modelo_id WHERE "
          + "(CAST(:status AS text) IS NULL OR s.status = :status) AND "
          + "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) AND "
          + "(CAST(:tipo AS text) IS NULL OR s.tipo = :tipo) AND "
          + "(CAST(:prioridade AS text) IS NULL OR s.prioridade = :prioridade) AND "
          + "(CAST(:criadaEmInicio AS timestamptz) IS NULL OR s.criada_em >= :criadaEmInicio) AND "
          + "(CAST(:criadaEmFim AS timestamptz) IS NULL OR s.criada_em <= :criadaEmFim) AND "
          + "(CAST(:concluidaEmInicio AS timestamptz) IS NULL OR "
          + ENCERRADA_EM_EXPR
          + " >= :concluidaEmInicio) AND "
          + "(CAST(:concluidaEmFim AS timestamptz) IS NULL OR "
          + ENCERRADA_EM_EXPR
          + " <= :concluidaEmFim) AND "
          + "(CAST(:abertaPorUsuarioId AS uuid) IS NULL OR s.aberta_por_usuario_id = :abertaPorUsuarioId) AND "
          + "(CAST(:responsavelId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM solicitacao_atribuicoes a WHERE a.solicitacao_id = s.id AND a.usuario_id = :responsavelId AND a.removido_em IS NULL)) AND "
          + "(CAST(:maquina AS text) IS NULL OR mo.maquina = :maquina) AND "
          + ATRASADA_FILTRO
          + " AND "
          + EM_ABERTO_FILTRO
          + " AND "
          + VISIVEL_PARA_FILTRO
          + " AND "
          + BUSCA_FILTRO;

  /** Urgencia da prioridade atual: 1 (URGENTE) a 4 (BAIXA); sem prioridade vira nulo. */
  String PRIORIDADE_URGENCIA_EXPR =
      "(CASE s.prioridade WHEN 'URGENTE' THEN 1 WHEN 'ALTA' THEN 2 "
          + "WHEN 'MEDIA' THEN 3 WHEN 'BAIXA' THEN 4 END)";

  /**
   * Ordenacao da listagem. O criterio e o sentido chegam como parametros (nomes de enum, nunca
   * coluna vinda do cliente); so a expressao do criterio escolhido produz valor, as demais viram
   * nulo e nao ordenam. NULLS LAST nos dois sentidos; desempate por criada_em e id.
   */
  String LISTAGEM_ORDER_BY =
      "ORDER BY "
          + "CASE WHEN CAST(:criterio AS text) = 'PRAZO_LIMITE' AND CAST(:ascendente AS boolean) "
          + "THEN "
          + PRAZO_LIMITE_EXPR
          + " END ASC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'PRAZO_LIMITE' AND NOT CAST(:ascendente AS boolean) "
          + "THEN "
          + PRAZO_LIMITE_EXPR
          + " END DESC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'PRIORIDADE' AND CAST(:ascendente AS boolean) "
          + "THEN "
          + PRIORIDADE_URGENCIA_EXPR
          + " END ASC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'PRIORIDADE' AND NOT CAST(:ascendente AS boolean) "
          + "THEN "
          + PRIORIDADE_URGENCIA_EXPR
          + " END DESC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'ENCERRADA_EM' AND CAST(:ascendente AS boolean) "
          + "THEN "
          + ENCERRADA_EM_EXPR
          + " END ASC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'ENCERRADA_EM' AND NOT CAST(:ascendente AS boolean) "
          + "THEN "
          + ENCERRADA_EM_EXPR
          + " END DESC NULLS LAST, "
          + "CASE WHEN CAST(:criterio AS text) = 'CRIADA_EM' AND NOT CAST(:ascendente AS boolean) "
          + "THEN s.criada_em END DESC, "
          + "s.criada_em ASC, s.id ASC";

  @Query(
      value = "SELECT s.* " + LISTAGEM_FROM_WHERE + " " + LISTAGEM_ORDER_BY,
      countQuery = "SELECT COUNT(s.*) " + LISTAGEM_FROM_WHERE,
      nativeQuery = true)
  Page<SolicitacaoJpaEntity> findByFilters(
      @org.springframework.data.repository.query.Param("status") String status,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("tipo") String tipo,
      @org.springframework.data.repository.query.Param("prioridade") String prioridade,
      @org.springframework.data.repository.query.Param("criadaEmInicio") Instant criadaEmInicio,
      @org.springframework.data.repository.query.Param("criadaEmFim") Instant criadaEmFim,
      @org.springframework.data.repository.query.Param("concluidaEmInicio")
          Instant concluidaEmInicio,
      @org.springframework.data.repository.query.Param("concluidaEmFim") Instant concluidaEmFim,
      @org.springframework.data.repository.query.Param("abertaPorUsuarioId")
          UUID abertaPorUsuarioId,
      @org.springframework.data.repository.query.Param("responsavelId") UUID responsavelId,
      @org.springframework.data.repository.query.Param("maquina") String maquina,
      @org.springframework.data.repository.query.Param("atrasada") Boolean atrasada,
      @org.springframework.data.repository.query.Param("emAberto") boolean emAberto,
      @org.springframework.data.repository.query.Param("visivelPara") UUID visivelParaUsuarioId,
      @org.springframework.data.repository.query.Param("busca") String busca,
      @org.springframework.data.repository.query.Param("criterio") String criterio,
      @org.springframework.data.repository.query.Param("ascendente") boolean ascendente,
      Pageable pageable);

  @Query("SELECT s.modeloId, COUNT(s) FROM SolicitacaoJpaEntity s GROUP BY s.modeloId")
  List<Object[]> countGroupByModeloId();

  /** Quantidade em aberto por tipo (A_FAZER, EM_ANDAMENTO e EM_VALIDACAO). */
  @Query(
      "SELECT s.tipo, COUNT(s) FROM SolicitacaoJpaEntity s "
          + "WHERE s.status NOT IN (com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.CANCELADA) GROUP BY s.tipo")
  List<Object[]> countEmAbertoPorTipo();

  /** Quantidade em aberto por prioridade; a prioridade nula (nao triada) vira um grupo proprio. */
  @Query(
      "SELECT s.prioridade, COUNT(s) FROM SolicitacaoJpaEntity s "
          + "WHERE s.status NOT IN (com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.CANCELADA) GROUP BY s.prioridade")
  List<Object[]> countEmAbertoPorPrioridade();

  long countByStatus(StatusSolicitacao status);

  /**
   * Serie por data de criacao em [inicio, fim): dia local ou segunda-feira da semana no fuso,
   * total, abertas, concluidas, canceladas e media em segundos (floor por solicitacao, como o
   * historico sempre fez) das concluidas. {@code unidade} e 'day' ou 'week' (valor de enum, nunca
   * do cliente).
   */
  @Query(
      value =
          "SELECT CAST(date_trunc(CAST(:unidade AS text), s.criada_em AT TIME ZONE CAST(:fuso AS text)) AS date) AS periodo, "
              + "COUNT(*) AS total, "
              + "COUNT(*) FILTER (WHERE s.status NOT IN ('CONCLUIDA', 'CANCELADA')) AS abertas, "
              + "COUNT(*) FILTER (WHERE s.status = 'CONCLUIDA') AS concluidas, "
              + "COUNT(*) FILTER (WHERE s.status = 'CANCELADA') AS canceladas, "
              + "AVG(FLOOR(EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em)))) "
              + "FILTER (WHERE s.status = 'CONCLUIDA' AND s.concluida_em IS NOT NULL) AS tempo_medio "
              + "FROM solicitacoes s "
              + "WHERE s.criada_em >= :inicio AND s.criada_em < :fim "
              + "AND (CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) "
              + "GROUP BY 1 ORDER BY 1",
      nativeQuery = true)
  List<Object[]> agregarSerie(
      @org.springframework.data.repository.query.Param("inicio") Instant inicio,
      @org.springframework.data.repository.query.Param("fim") Instant fim,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("unidade") String unidade,
      @org.springframework.data.repository.query.Param("fuso") String fuso);

  /**
   * Totais do painel em [inicio, fim): criadas pela criacao, concluidas pela conclusao, canceladas
   * pelo cancelamento e media, em segundos sem arredondar, das concluidas na janela (nula sem
   * elas). Uma linha.
   */
  @Query(
      value =
          "SELECT "
              + "COUNT(*) FILTER (WHERE s.criada_em >= :inicio AND s.criada_em < :fim), "
              + "COUNT(*) FILTER (WHERE s.status = 'CONCLUIDA' "
              + "AND s.concluida_em >= :inicio AND s.concluida_em < :fim), "
              + "COUNT(*) FILTER (WHERE s.status = 'CANCELADA' "
              + "AND s.cancelada_em >= :inicio AND s.cancelada_em < :fim), "
              + "AVG(EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))) FILTER (WHERE "
              + "s.status = 'CONCLUIDA' AND s.concluida_em >= :inicio AND s.concluida_em < :fim) "
              + "FROM solicitacoes s WHERE "
              + PAINEL_MODELO_FILTRO,
      nativeQuery = true)
  List<Object[]> painelTotais(
      @org.springframework.data.repository.query.Param("inicio") Instant inicio,
      @org.springframework.data.repository.query.Param("fim") Instant fim,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId);

  /**
   * Serie do painel sobre os dias locais [inicioDia, fimDia): um ponto por dia ou por semana
   * (segunda-feira), mesmo sem solicitacao. Criadas, concluidas e canceladas contam so dentro da
   * janela; abertas e atrasadas sao as do instante de fim do ponto (ultimo dia dele na janela).
   * Aberta: criada antes do fim e sem encerramento ate ele. Atrasada: aberta, com prioridade, e
   * prazo anterior ao menor entre o ultimo instante do ponto e {@code agora}. {@code unidade} e
   * 'day' ou 'week' (valor de enum, nunca do cliente).
   */
  @Query(
      value =
          "WITH pontos AS ("
              + "SELECT CAST(date_trunc(CAST(:unidade AS text), g) AS date) AS periodo "
              + "FROM generate_series(CAST(:inicioDia AS timestamp), "
              + "CAST(:fimDia AS timestamp) - INTERVAL '1 day', INTERVAL '1 day') g GROUP BY 1"
              + "), limites AS ("
              + "SELECT p.periodo, "
              + "CAST(GREATEST(p.periodo, CAST(:inicioDia AS date)) AS timestamp) "
              + "AT TIME ZONE CAST(:fuso AS text) AS inicio_ponto, "
              + "CAST(LEAST(p.periodo + CASE WHEN CAST(:unidade AS text) = 'week' THEN 7 ELSE 1 END, "
              + "CAST(:fimDia AS date)) AS timestamp) AT TIME ZONE CAST(:fuso AS text) AS fim_ponto "
              + "FROM pontos p"
              + "), base AS MATERIALIZED ("
              + "SELECT s.criada_em, s.status, s.concluida_em, s.cancelada_em, "
              + ENCERRADA_EM_EXPR
              + " AS encerrada_em, "
              + "CASE WHEN s.prioridade IS NOT NULL THEN "
              + PRAZO_LIMITE_EXPR
              + " END AS prazo "
              + "FROM solicitacoes s WHERE "
              + PAINEL_MODELO_FILTRO
              + "), eventos AS ("
              // t, criadas, concluidas, canceladas, delta de abertas, delta de atrasadas
              + "SELECT criada_em AS t, 1 AS c, 0 AS k, 0 AS x, 1 AS a, 0 AS z FROM base "
              + "UNION ALL SELECT concluida_em, 0, 1, 0, 0, 0 FROM base "
              + "WHERE status = 'CONCLUIDA' AND concluida_em IS NOT NULL "
              + "UNION ALL SELECT cancelada_em, 0, 0, 1, 0, 0 FROM base "
              + "WHERE status = 'CANCELADA' AND cancelada_em IS NOT NULL "
              + "UNION ALL SELECT GREATEST(encerrada_em, criada_em), 0, 0, 0, -1, 0 FROM base "
              + "WHERE encerrada_em IS NOT NULL "
              + "UNION ALL SELECT prazo + INTERVAL '1 microsecond', 0, 0, 0, 0, 1 FROM base "
              + "WHERE prazo < CAST(:agora AS timestamptz) "
              + "UNION ALL SELECT GREATEST(encerrada_em, prazo + INTERVAL '1 microsecond'), "
              + "0, 0, 0, 0, -1 FROM base "
              + "WHERE prazo < CAST(:agora AS timestamptz) AND encerrada_em IS NOT NULL"
              + "), baldes AS ("
              // evento antes da janela cai em -infinity; depois da janela nao conta em ponto algum
              + "SELECT CASE WHEN e.t < j.lo THEN DATE '-infinity' "
              + "ELSE CAST(date_trunc(CAST(:unidade AS text), "
              + "e.t AT TIME ZONE CAST(:fuso AS text)) AS date) END AS periodo, "
              + "e.t >= j.lo AS na_janela, "
              + "SUM(e.c) AS c, SUM(e.k) AS k, SUM(e.x) AS x, SUM(e.a) AS a, SUM(e.z) AS z "
              + "FROM eventos e CROSS JOIN (SELECT MIN(inicio_ponto) AS lo, "
              + "MAX(fim_ponto) AS hi FROM limites) j "
              + "WHERE e.t < j.hi GROUP BY 1, 2"
              + ") "
              + "SELECT l.periodo, "
              + "COALESCE((SELECT SUM(b.c) FROM baldes b WHERE b.periodo = l.periodo "
              + "AND b.na_janela), 0) AS criadas, "
              + "COALESCE((SELECT SUM(b.k) FROM baldes b WHERE b.periodo = l.periodo "
              + "AND b.na_janela), 0) AS concluidas, "
              + "COALESCE((SELECT SUM(b.x) FROM baldes b WHERE b.periodo = l.periodo "
              + "AND b.na_janela), 0) AS canceladas, "
              + "COALESCE((SELECT SUM(b.a) FROM baldes b WHERE b.periodo <= l.periodo), 0) AS abertas, "
              + "COALESCE((SELECT SUM(b.z) FROM baldes b WHERE b.periodo <= l.periodo), 0) "
              + "AS atrasadas "
              + "FROM limites l ORDER BY l.periodo",
      nativeQuery = true)
  List<Object[]> painelSerie(
      @org.springframework.data.repository.query.Param("inicioDia") LocalDate inicioDia,
      @org.springframework.data.repository.query.Param("fimDia") LocalDate fimDia,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("unidade") String unidade,
      @org.springframework.data.repository.query.Param("fuso") String fuso,
      @org.springframework.data.repository.query.Param("agora") Instant agora);

  /**
   * Em aberto e em atraso em {@code agora} (uma linha): espelha o filtro de atrasada da listagem,
   * restrito as solicitacoes em aberto.
   */
  @Query(
      value =
          "SELECT COUNT(*), "
              + "COUNT(*) FILTER (WHERE s.prioridade IS NOT NULL AND "
              + "CAST(:agora AS timestamptz) > "
              + PRAZO_LIMITE_EXPR
              + ") "
              + "FROM solicitacoes s WHERE "
              + STATUS_EM_ABERTO
              + " AND "
              + PAINEL_MODELO_FILTRO,
      nativeQuery = true)
  List<Object[]> painelAgora(
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("agora") Instant agora);

  @Query(
      value =
          "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))), 0) FROM solicitacoes s WHERE s.status = 'CONCLUIDA'",
      nativeQuery = true)
  double getTempoMedioResolucaoSegundos();

  @Query(
      value =
          "WITH intervalos AS ("
              + "  SELECT modelo_id, "
              + "    EXTRACT(EPOCH FROM (criada_em - LAG(criada_em) OVER (PARTITION BY modelo_id ORDER BY criada_em))) AS intervalo_segundos "
              + "  FROM solicitacoes WHERE tipo <> 'CRIACAO'"
              + "), resolucao AS ("
              + "  SELECT modelo_id, AVG(EXTRACT(EPOCH FROM (concluida_em - criada_em))) AS tempo_medio_resolucao "
              + "  FROM solicitacoes WHERE status = 'CONCLUIDA' AND tipo <> 'CRIACAO' GROUP BY modelo_id"
              + "), intervalo_medio AS ("
              + "  SELECT modelo_id, AVG(intervalo_segundos) AS intervalo_medio "
              + "  FROM intervalos WHERE intervalo_segundos IS NOT NULL GROUP BY modelo_id"
              + ") "
              + "SELECT m.id AS modelo_id, m.codigo AS codigo, "
              + "  r.tempo_medio_resolucao AS tempo_medio_resolucao, i.intervalo_medio AS intervalo_medio "
              + "FROM modelos m "
              + "JOIN resolucao r ON r.modelo_id = m.id "
              + "LEFT JOIN intervalo_medio i ON i.modelo_id = m.id",
      countQuery =
          "WITH resolucao AS ("
              + "  SELECT modelo_id FROM solicitacoes "
              + "  WHERE status = 'CONCLUIDA' AND tipo <> 'CRIACAO' GROUP BY modelo_id"
              + ") "
              + "SELECT COUNT(*) FROM modelos m JOIN resolucao r ON r.modelo_id = m.id",
      nativeQuery = true)
  Page<Object[]> findMetricasPorModelo(Pageable pageable);

  /**
   * Uma linha: total (inclui criacao), em aberto, concluidas, canceladas, primeira e ultima
   * abertura de manutencao, media em segundos do tempo de resolucao das manutencoes concluidas
   * (nulo sem elas) e a contagem de manutencoes (solicitacoes que nao sao criacao).
   */
  @Query(
      "SELECT COUNT(s), "
          + "COALESCE(SUM(CASE WHEN s.status IN (com.rgm.api.core.domain.model.enums.StatusSolicitacao.A_FAZER, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.EM_ANDAMENTO, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.EM_VALIDACAO) THEN 1 ELSE 0 END), 0), "
          + "COALESCE(SUM(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA THEN 1 ELSE 0 END), 0), "
          + "COALESCE(SUM(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CANCELADA THEN 1 ELSE 0 END), 0), "
          + "MIN(CASE WHEN s.tipo <> com.rgm.api.core.domain.model.enums.TipoSolicitacao.CRIACAO "
          + "THEN s.criadaEm END), "
          + "MAX(CASE WHEN s.tipo <> com.rgm.api.core.domain.model.enums.TipoSolicitacao.CRIACAO "
          + "THEN s.criadaEm END), "
          + "AVG(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA "
          + "AND s.tipo <> com.rgm.api.core.domain.model.enums.TipoSolicitacao.CRIACAO "
          + "THEN ((s.concluidaEm - s.criadaEm) BY SECOND) END), "
          + "COALESCE(SUM(CASE WHEN s.tipo <> com.rgm.api.core.domain.model.enums.TipoSolicitacao.CRIACAO "
          + "THEN 1 ELSE 0 END), 0) "
          + "FROM SolicitacaoJpaEntity s WHERE s.modeloId = :modeloId")
  List<Object[]> resumirPorModelo(
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId);
}
