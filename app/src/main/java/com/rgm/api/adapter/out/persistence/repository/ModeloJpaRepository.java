package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModeloJpaRepository extends JpaRepository<ModeloJpaEntity, UUID> {

  int countByMaquinaAndCodigo(String maquina, String codigo);

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
