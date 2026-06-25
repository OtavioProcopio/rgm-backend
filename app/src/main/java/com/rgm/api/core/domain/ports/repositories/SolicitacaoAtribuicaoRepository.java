package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import java.util.List;
import java.util.UUID;

public interface SolicitacaoAtribuicaoRepository {

  SolicitacaoAtribuicao save(SolicitacaoAtribuicao atribuicao);

  List<SolicitacaoAtribuicao> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(UUID solicitacaoId, UUID usuarioId);

  boolean existsByUsuarioIdAndRemovidoEmIsNull(UUID usuarioId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
