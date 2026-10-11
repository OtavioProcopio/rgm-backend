package com.rgm.api.adapter.out.persistence.repository;

import static com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository.PRAZO_LIMITE_EXPR;
import static com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository.STATUS_EM_ABERTO;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consultas nativas dos indicadores de gestao. A entidade serve so para o Spring Data ligar a
 * interface; nenhuma consulta carrega solicitacoes. O prazo e o "em aberto" reusam as expressoes da
 * listagem para existir uma unica definicao de cada um.
 */
public interface IndicadoresDeGestaoJpaRepository
    extends JpaRepository<SolicitacaoJpaEntity, UUID> {

  String CONCLUIDA_NA_JANELA =
      "s.status = 'CONCLUIDA' AND s.concluida_em >= CAST(:inicio AS timestamptz) "
          + "AND s.concluida_em < CAST(:fim AS timestamptz)";

  String SEM_RESPONSAVEL =
      "NOT EXISTS (SELECT 1 FROM solicitacao_atribuicoes a "
          + "WHERE a.solicitacao_id = s.id AND a.removido_em IS NULL)";

  /** Prioridade, concluidas e quantas cumpriram o prazo (so concluidas com prioridade). */
  @Query(
      value =
          "SELECT s.prioridade, COUNT(*), "
              + "COUNT(*) FILTER (WHERE s.concluida_em <= "
              + PRAZO_LIMITE_EXPR
              + ") FROM solicitacoes s WHERE "
              + CONCLUIDA_NA_JANELA
              + " AND s.prioridade IS NOT NULL GROUP BY s.prioridade",
      nativeQuery = true)
  List<Object[]> prazoPorPrioridade(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

  @Query(
      value =
          "SELECT COUNT(*) FROM solicitacoes s WHERE "
              + CONCLUIDA_NA_JANELA
              + " AND s.prioridade IS NULL",
      nativeQuery = true)
  long concluidasSemPrioridade(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

  /** Uma linha: concluidas, mediana e p90 do tempo de resolucao, em segundos. */
  @Query(
      value =
          "SELECT COUNT(*), "
              + "percentile_cont(0.5) WITHIN GROUP (ORDER BY "
              + "EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))), "
              + "percentile_cont(0.9) WITHIN GROUP (ORDER BY "
              + "EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))) "
              + "FROM solicitacoes s WHERE "
              + CONCLUIDA_NA_JANELA,
      nativeQuery = true)
  List<Object[]> resolucao(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

  /** Uma linha: abertas, menos de 1 dia, de 1 a 3 dias, mais de 3 dias e sem responsavel. */
  @Query(
      value =
          "SELECT COUNT(*), "
              + "COUNT(*) FILTER (WHERE CAST(:agora AS timestamptz) - s.criada_em < INTERVAL '1 day'), "
              + "COUNT(*) FILTER (WHERE CAST(:agora AS timestamptz) - s.criada_em >= INTERVAL '1 day' "
              + "AND CAST(:agora AS timestamptz) - s.criada_em <= INTERVAL '3 days'), "
              + "COUNT(*) FILTER (WHERE CAST(:agora AS timestamptz) - s.criada_em > INTERVAL '3 days'), "
              + "COUNT(*) FILTER (WHERE "
              + SEM_RESPONSAVEL
              + ") FROM solicitacoes s WHERE "
              + STATUS_EM_ABERTO,
      nativeQuery = true)
  List<Object[]> backlog(@Param("agora") Instant agora);

  /**
   * Etapa, amostras, mediana e media em segundos. Marcos: a abertura (A Fazer) e cada mudanca de
   * status; um intervalo vale quando termina na janela e e encerrado pela transicao da etapa.
   */
  @Query(
      value =
          "WITH marcos AS ("
              + "SELECT s.id AS sid, 'A_FAZER' AS status, s.criada_em AS em, 0 AS ordem "
              + "FROM solicitacoes s "
              + "UNION ALL "
              + "SELECT a.solicitacao_id, a.para_status, a.criada_em, 1 "
              + "FROM atividades_solicitacao a "
              + "WHERE a.tipo = 'MUDANCA_STATUS' AND a.para_status IS NOT NULL), "
              + "intervalos AS ("
              + "SELECT sid, status, em AS inicio, LEAD(em) OVER w AS fim, "
              + "LEAD(status) OVER w AS proximo FROM marcos "
              + "WINDOW w AS (PARTITION BY sid ORDER BY em, ordem)), "
              + "por_solicitacao AS ("
              + "SELECT sid, status, SUM(EXTRACT(EPOCH FROM (fim - inicio))) AS segundos "
              + "FROM intervalos WHERE fim >= CAST(:inicio AS timestamptz) "
              + "AND fim < CAST(:fim AS timestamptz) AND ("
              + "(status = 'A_FAZER' AND proximo = 'EM_ANDAMENTO') OR "
              + "(status = 'EM_ANDAMENTO' AND proximo = 'EM_VALIDACAO') OR "
              + "(status = 'EM_VALIDACAO' AND proximo IN ('CONCLUIDA', 'EM_ANDAMENTO'))) "
              + "GROUP BY sid, status) "
              + "SELECT status, COUNT(*), "
              + "percentile_cont(0.5) WITHIN GROUP (ORDER BY segundos), AVG(segundos) "
              + "FROM por_solicitacao GROUP BY status",
      nativeQuery = true)
  List<Object[]> tempoPorEtapa(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

  /** Uma linha: enviadas para validacao e devolvidas na janela. */
  @Query(
      value =
          "SELECT COUNT(*) FILTER (WHERE a.para_status = 'EM_VALIDACAO'), "
              + "COUNT(*) FILTER (WHERE a.de_status = 'EM_VALIDACAO' "
              + "AND a.para_status = 'EM_ANDAMENTO') "
              + "FROM atividades_solicitacao a WHERE a.tipo = 'MUDANCA_STATUS' "
              + "AND a.criada_em >= CAST(:inicio AS timestamptz) "
              + "AND a.criada_em < CAST(:fim AS timestamptz)",
      nativeQuery = true)
  List<Object[]> devolucoes(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

  /** Maquina, abertas, em atraso e tempo medio das concluidas na janela (criacao fica fora). */
  @Query(
      value =
          "SELECT mo.maquina, "
              + "COUNT(*) FILTER (WHERE "
              + STATUS_EM_ABERTO
              + "), "
              + "COUNT(*) FILTER (WHERE "
              + STATUS_EM_ABERTO
              + " AND s.prioridade IS NOT NULL AND CAST(:agora AS timestamptz) > "
              + PRAZO_LIMITE_EXPR
              + "), "
              + "AVG(EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))) FILTER (WHERE "
              + CONCLUIDA_NA_JANELA
              + ") FROM solicitacoes s JOIN modelos mo ON mo.id = s.modelo_id "
              + "WHERE s.tipo <> 'CRIACAO' GROUP BY mo.maquina "
              + "HAVING COUNT(*) FILTER (WHERE "
              + STATUS_EM_ABERTO
              + ") > 0 OR COUNT(*) FILTER (WHERE "
              + CONCLUIDA_NA_JANELA
              + ") > 0 ORDER BY mo.maquina",
      nativeQuery = true)
  List<Object[]> porMaquina(
      @Param("inicio") Instant inicio, @Param("fim") Instant fim, @Param("agora") Instant agora);

  @Query(
      value =
          "SELECT mo.id, mo.codigo, mo.versao, mo.maquina, COUNT(*) "
              + "FROM solicitacoes s JOIN modelos mo ON mo.id = s.modelo_id "
              + "WHERE s.tipo = 'REPARO' AND s.criada_em >= CAST(:inicio AS timestamptz) "
              + "AND s.criada_em < CAST(:fim AS timestamptz) "
              + "GROUP BY mo.id, mo.codigo, mo.versao, mo.maquina "
              + "HAVING COUNT(*) >= :minimo ORDER BY COUNT(*) DESC, mo.codigo, mo.versao",
      nativeQuery = true)
  List<Object[]> reincidentes(
      @Param("inicio") Instant inicio, @Param("fim") Instant fim, @Param("minimo") int minimo);

  @Query(
      value =
          "SELECT COUNT(DISTINCT s.id) FROM solicitacoes s "
              + "JOIN solicitacao_atribuicoes a ON a.solicitacao_id = s.id "
              + "WHERE a.removido_em IS NULL AND "
              + STATUS_EM_ABERTO,
      nativeQuery = true)
  long abertasComResponsavel();

  @Query(
      value =
          "SELECT u.id, u.nome, COUNT(DISTINCT s.id) FROM solicitacoes s "
              + "JOIN solicitacao_atribuicoes a ON a.solicitacao_id = s.id "
              + "JOIN usuarios u ON u.id = a.usuario_id "
              + "WHERE a.removido_em IS NULL AND "
              + STATUS_EM_ABERTO
              + " GROUP BY u.id, u.nome ORDER BY COUNT(DISTINCT s.id) DESC, u.nome",
      nativeQuery = true)
  List<Object[]> cargaPorResponsavel();
}
