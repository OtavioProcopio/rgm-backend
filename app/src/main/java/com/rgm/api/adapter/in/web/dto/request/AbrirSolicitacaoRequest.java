package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * {@code modeloId} e obrigatorio para todos os tipos exceto CRIACAO, que em vez disso usa {@code
 * modeloCodigo}/{@code modeloMaquina}/{@code modeloObservacoes} para carregar os dados do modelo a
 * ser criado ao concluir a solicitacao.
 */
public record AbrirSolicitacaoRequest(
    @NotBlank String titulo,
    @NotBlank String descricao,
    @NotBlank String tipo,
    UUID modeloId,
    String modeloCodigo,
    String modeloMaquina,
    String modeloObservacoes) {}
