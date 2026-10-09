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

  /**
   * Grava a foto como principal do modelo, desmarcando a anterior na mesma operacao: se a gravacao
   * falhar, a principal anterior continua (no maximo uma por modelo).
   */
  FotoGaleriaModelo salvarComoPrincipal(FotoGaleriaModelo foto);

  /** Resolve a URL da foto principal de cada modelo, para uso em listagens sem N+1. */
  Map<UUID, String> findPrincipalUrlsByModeloIds(Collection<UUID> modeloIds);
}
