package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.application.usecases.auth.AlterarSenhaPropriaUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.time.Instant;
import java.util.UUID;

/**
 * Resposta da troca da propria senha: os campos de {@link UsuarioResponse} e as credenciais novas,
 * que mantem aberta a sessao que fez a troca.
 */
public record SenhaAlteradaResponse(
    UUID id,
    String nome,
    String email,
    String perfil,
    boolean ativo,
    Instant criadoEm,
    Instant atualizadoEm,
    String token,
    String refreshToken) {

  public static SenhaAlteradaResponse from(final AlterarSenhaPropriaUseCase.Output output) {
    final Usuario u = output.usuario();
    return new SenhaAlteradaResponse(
        u.getId(),
        u.getNome(),
        u.getEmail(),
        u.getPerfil().name(),
        u.isAtivo(),
        u.getCriadoEm(),
        u.getAtualizadoEm(),
        output.token(),
        output.refreshToken());
  }
}
