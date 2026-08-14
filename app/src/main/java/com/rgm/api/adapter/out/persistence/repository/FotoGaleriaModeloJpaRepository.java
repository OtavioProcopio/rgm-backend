package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.FotoGaleriaModeloJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FotoGaleriaModeloJpaRepository
    extends JpaRepository<FotoGaleriaModeloJpaEntity, UUID> {

  List<FotoGaleriaModeloJpaEntity> findByModeloIdOrderByCriadoEmAsc(UUID modeloId);

  List<FotoGaleriaModeloJpaEntity> findByModeloIdInAndPrincipalTrue(Collection<UUID> modeloIds);

  @Modifying
  @Query(
      "UPDATE FotoGaleriaModeloJpaEntity f SET f.principal = false "
          + "WHERE f.modeloId = :modeloId AND f.principal = true")
  void limparPrincipal(UUID modeloId);
}
