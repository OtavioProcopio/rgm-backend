package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import java.util.List;
import java.util.UUID;

public interface SolicitacaoEvidenciaRepository {

  SolicitacaoEvidencia save(SolicitacaoEvidencia solicitacaoEvidencia);

  List<SolicitacaoEvidencia> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsBySolicitacaoIdAndEvidenciaId(UUID solicitacaoId, UUID evidenciaId);

  void deleteBySolicitacaoId(UUID solicitacaoId);

  void deleteByEvidenciaId(UUID evidenciaId);
}
