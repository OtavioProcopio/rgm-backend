package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.entity.FotoGaleriaModeloJpaEntity;
import com.rgm.api.adapter.out.persistence.mapper.FotoGaleriaModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.FotoGaleriaModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class FotoGaleriaModeloRepositoryAdapter implements FotoGaleriaModeloRepository {

  private final FotoGaleriaModeloJpaRepository jpa;

  public FotoGaleriaModeloRepositoryAdapter(final FotoGaleriaModeloJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<FotoGaleriaModelo> findById(final UUID id) {
    return jpa.findById(id).map(FotoGaleriaModeloMapper::toDomain);
  }

  @Override
  public List<FotoGaleriaModelo> findByModeloId(final UUID modeloId) {
    return jpa.findByModeloIdOrderByCriadoEmAsc(modeloId).stream()
        .map(FotoGaleriaModeloMapper::toDomain)
        .toList();
  }

  @Override
  public FotoGaleriaModelo save(final FotoGaleriaModelo foto) {
    return FotoGaleriaModeloMapper.toDomain(jpa.save(FotoGaleriaModeloMapper.toJpa(foto)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public void limparPrincipal(final UUID modeloId) {
    jpa.limparPrincipal(modeloId);
  }

  @Override
  public Map<UUID, String> findPrincipalUrlsByModeloIds(final Collection<UUID> modeloIds) {
    if (modeloIds == null || modeloIds.isEmpty()) {
      return Map.of();
    }
    return jpa.findByModeloIdInAndPrincipalTrue(modeloIds).stream()
        .collect(
            Collectors.toMap(
                FotoGaleriaModeloJpaEntity::getModeloId, FotoGaleriaModeloJpaEntity::getPublicUrl));
  }
}
