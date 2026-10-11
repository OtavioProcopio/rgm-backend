package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AtividadeSolicitacaoRepository {

  AtividadeSolicitacao save(AtividadeSolicitacao atividade);

  List<AtividadeSolicitacao> findBySolicitacaoId(UUID solicitacaoId);

  /** Atividades de varias solicitacoes numa so consulta, em ordem de criacao. */
  List<AtividadeSolicitacao> findBySolicitacaoIdIn(Collection<UUID> solicitacaoIds);

  boolean existsByAutorId(UUID autorId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
