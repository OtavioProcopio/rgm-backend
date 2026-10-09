package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase.ResponsavelNome;
import java.util.UUID;

/** Responsavel pela solicitacao com o nome resolvido (nulo se o usuario nao for encontrado). */
public record ResponsavelResponse(UUID id, String nome) {

  public static ResponsavelResponse from(final ResponsavelNome responsavel) {
    return new ResponsavelResponse(responsavel.id(), responsavel.nome());
  }
}
