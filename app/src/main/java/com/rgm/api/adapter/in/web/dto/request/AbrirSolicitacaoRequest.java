package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code modeloId} e obrigatorio para todos os tipos exceto CRIACAO, que em vez disso usa {@code
 * modeloCodigo}/{@code modeloMaquina}/{@code modeloObservacoes} para carregar os dados do modelo a
 * ser criado ao concluir a solicitacao.
 */
public record AbrirSolicitacaoRequest(
    @NotBlank @Size(max = LimitesTexto.SOLICITACAO_TITULO, message = LimitesTexto.MENSAGEM)
        String titulo,
    @NotBlank @Size(max = LimitesTexto.SOLICITACAO_DESCRICAO, message = LimitesTexto.MENSAGEM)
        String descricao,
    @NotBlank String tipo,
    UUID modeloId,
    @Size(max = LimitesTexto.MODELO_PRETENDIDO_CODIGO, message = LimitesTexto.MENSAGEM)
        String modeloCodigo,
    @Size(max = LimitesTexto.MODELO_PRETENDIDO_MAQUINA, message = LimitesTexto.MENSAGEM)
        String modeloMaquina,
    @Size(max = LimitesTexto.MODELO_PRETENDIDO_OBSERVACOES, message = LimitesTexto.MENSAGEM)
        String modeloObservacoes) {}
