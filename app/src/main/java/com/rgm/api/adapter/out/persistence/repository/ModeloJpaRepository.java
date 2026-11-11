package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModeloJpaRepository extends JpaRepository<ModeloJpaEntity, UUID> {

  /**
   * Filtros do catalogo, todos opcionais e somados. {@code q} (ja com %, _ e \\ escapados) casa,
   * sem caixa e sem acento, com o codigo ou a descricao; {@code maquinaExata} troca "contem" por
   * igualdade na maquina; {@code codigo}, {@code descricao} e a maquina "contem" seguem como na
   * listagem de sempre.
   */
  String CATALOGO_FILTROS =
      "(CAST(:ativo AS boolean) IS NULL OR m.ativo = :ativo) AND "
          + "(CAST(:codigo AS text) IS NULL OR LOWER(m.codigo) LIKE LOWER('%' || CAST(:codigo AS text) || '%')) AND "
          + "(CAST(:descricao AS text) IS NULL OR LOWER(m.descricao) LIKE LOWER('%' || CAST(:descricao AS text) || '%')) AND "
          + "(CAST(:maquina AS text) IS NULL OR "
          + "(:maquinaExata = true AND rgm_sem_acento(m.maquina) = rgm_sem_acento(CAST(:maquina AS text))) OR "
          + "(:maquinaExata = false AND LOWER(m.maquina) LIKE LOWER('%' || CAST(:maquina AS text) || '%'))) AND "
          + "(CAST(:tipo AS text) IS NULL OR LOWER(m.tipo) = LOWER(CAST(:tipo AS text))) AND "
          + "(CAST(:q AS text) IS NULL OR "
          + "rgm_sem_acento(m.codigo) LIKE '%' || rgm_sem_acento(CAST(:q AS text)) || '%' OR "
          + "rgm_sem_acento(m.descricao) LIKE '%' || rgm_sem_acento(CAST(:q AS text)) || '%')";

  @Query(
      value =
          "SELECT m.* FROM modelos m WHERE "
              + CATALOGO_FILTROS
              + " ORDER BY m.codigo ASC, m.versao DESC, m.id ASC",
      countQuery = "SELECT COUNT(*) FROM modelos m WHERE " + CATALOGO_FILTROS,
      nativeQuery = true)
  Page<ModeloJpaEntity> catalogoPorCodigo(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q,
      Pageable pageable);

  @Query(
      value =
          "SELECT m.* FROM modelos m WHERE "
              + CATALOGO_FILTROS
              + " ORDER BY m.criado_em DESC, m.id ASC",
      countQuery = "SELECT COUNT(*) FROM modelos m WHERE " + CATALOGO_FILTROS,
      nativeQuery = true)
  Page<ModeloJpaEntity> catalogoPorCriacao(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q,
      Pageable pageable);

  @Query(value = "SELECT COUNT(*) FROM modelos m WHERE " + CATALOGO_FILTROS, nativeQuery = true)
  long contarNoCatalogo(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q);

  @Query(
      value =
          "SELECT m.tipo, COUNT(*) FROM modelos m WHERE "
              + CATALOGO_FILTROS
              + " AND m.tipo IS NOT NULL GROUP BY m.tipo ORDER BY COUNT(*) DESC, m.tipo ASC",
      nativeQuery = true)
  List<Object[]> contarNoCatalogoPorTipo(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q);

  @Query(
      value =
          "SELECT m.maquina, COUNT(*) FROM modelos m WHERE "
              + CATALOGO_FILTROS
              + " GROUP BY m.maquina ORDER BY COUNT(*) DESC, m.maquina ASC",
      nativeQuery = true)
  List<Object[]> contarNoCatalogoPorMaquina(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q);

  /** Quantos modelos do filtro vem antes do alvo, em ordem de codigo, versao maior e id. */
  @Query(
      value =
          "SELECT COUNT(*) FROM modelos m WHERE "
              + CATALOGO_FILTROS
              + " AND (m.codigo < :codigoAlvo OR (m.codigo = :codigoAlvo AND "
              + "(m.versao > :versaoAlvo OR (m.versao = :versaoAlvo AND m.id < :idAlvo))))",
      nativeQuery = true)
  long contarAntesDe(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q,
      @Param("codigoAlvo") String codigoAlvo,
      @Param("versaoAlvo") int versaoAlvo,
      @Param("idAlvo") UUID idAlvo);

  /** 1 se o modelo atende ao filtro, 0 se nao. */
  @Query(
      value = "SELECT COUNT(*) FROM modelos m WHERE " + CATALOGO_FILTROS + " AND m.id = :idAlvo",
      nativeQuery = true)
  long contarModelo(
      @Param("ativo") Boolean ativo,
      @Param("codigo") String codigo,
      @Param("maquina") String maquina,
      @Param("maquinaExata") boolean maquinaExata,
      @Param("descricao") String descricao,
      @Param("tipo") String tipo,
      @Param("q") String q,
      @Param("idAlvo") UUID idAlvo);

  @Query(
      "SELECT COALESCE(MAX(m.versao), 0) FROM ModeloJpaEntity m "
          + "WHERE m.maquina = :maquina AND m.codigo = :codigo")
  int maiorVersao(
      @org.springframework.data.repository.query.Param("maquina") String maquina,
      @org.springframework.data.repository.query.Param("codigo") String codigo);

  @org.springframework.data.jpa.repository.Modifying
  @Query("UPDATE ModeloJpaEntity m SET m.maquina = :para WHERE m.maquina = :de")
  int trocarMaquina(
      @org.springframework.data.repository.query.Param("de") String de,
      @org.springframework.data.repository.query.Param("para") String para);

  @org.springframework.data.jpa.repository.Modifying
  @Query("UPDATE ModeloJpaEntity m SET m.tipo = :para WHERE m.tipo = :de")
  int trocarTipo(
      @org.springframework.data.repository.query.Param("de") String de,
      @org.springframework.data.repository.query.Param("para") String para);

  boolean existsByMaquinaAndCodigoAndVersaoAndIdNot(
      String maquina, String codigo, int versao, UUID id);

  @Query(
      "SELECT m FROM ModeloJpaEntity m WHERE "
          + "(:ativo IS NULL OR m.ativo = :ativo) AND "
          + "(CAST(:codigo AS string) IS NULL OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:codigo AS string), '%'))) AND "
          + "(CAST(:maquina AS string) IS NULL OR LOWER(m.maquina) LIKE LOWER(CONCAT('%', CAST(:maquina AS string), '%'))) AND "
          + "(CAST(:descricao AS string) IS NULL OR LOWER(m.descricao) LIKE LOWER(CONCAT('%', CAST(:descricao AS string), '%')))")
  Page<ModeloJpaEntity> findByFilters(
      Boolean ativo, String codigo, String maquina, String descricao, Pageable pageable);

  /** Uma linha: total, ativos e com pendencia aberta (ativos e inativos). */
  @Query(
      "SELECT COUNT(m), "
          + "COALESCE(SUM(CASE WHEN m.ativo = true THEN 1 ELSE 0 END), 0), "
          + "COALESCE(SUM(CASE WHEN m.temPendenciaAberta = true THEN 1 ELSE 0 END), 0) "
          + "FROM ModeloJpaEntity m")
  List<Object[]> resumirContagens();

  /** Quantidade de modelos por maquina, da maior para a menor; empate em ordem alfabetica. */
  @Query(
      "SELECT m.maquina, COUNT(m) FROM ModeloJpaEntity m "
          + "GROUP BY m.maquina ORDER BY COUNT(m) DESC, m.maquina ASC")
  List<Object[]> contarPorMaquina();
}
