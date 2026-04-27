package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository {

  Optional<Solicitacao> findById(UUID id);

  Solicitacao save(Solicitacao solicitacao);

  void deleteById(UUID id);

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  List<Solicitacao> findByModeloId(UUID modeloId);
}
