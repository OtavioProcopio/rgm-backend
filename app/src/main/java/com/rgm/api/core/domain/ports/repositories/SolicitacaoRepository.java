package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository {

  Optional<Solicitacao> findById(UUID id);

  Solicitacao save(Solicitacao solicitacao);

  void deleteById(UUID id);

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  List<Solicitacao> findByModeloId(UUID modeloId);

  PageResult<Solicitacao> findAll(int page, int size);

  PageResult<Solicitacao> findByStatus(StatusSolicitacao status, int page, int size);

  PageResult<Solicitacao> findByFilters(
      StatusSolicitacao status,
      UUID modeloId,
      TipoSolicitacao tipo,
      PrioridadeSolicitacao prioridade,
      int page,
      int size);
}
