package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import java.util.List;
import java.util.UUID;

public interface AtividadeSolicitacaoRepository {

  AtividadeSolicitacao save(AtividadeSolicitacao atividade);

  List<AtividadeSolicitacao> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsByAutorId(UUID autorId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
