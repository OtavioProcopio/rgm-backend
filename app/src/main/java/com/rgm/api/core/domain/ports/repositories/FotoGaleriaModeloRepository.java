package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FotoGaleriaModeloRepository {

  Optional<FotoGaleriaModelo> findById(UUID id);

  /** Lista as fotos do modelo, mais antigas primeiro. */
  List<FotoGaleriaModelo> findByModeloId(UUID modeloId);

  FotoGaleriaModelo save(FotoGaleriaModelo foto);

  void deleteById(UUID id);

  /** Desmarca qualquer foto principal existente do modelo (no maximo uma por vez). */
  void limparPrincipal(UUID modeloId);

  /** Resolve a URL da foto principal de cada modelo, para uso em listagens sem N+1. */
  Map<UUID, String> findPrincipalUrlsByModeloIds(Collection<UUID> modeloIds);
}
