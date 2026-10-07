package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevolverSolicitacaoRequest(
    @NotBlank @Size(max = LimitesTexto.COMENTARIO, message = LimitesTexto.MENSAGEM) String motivo,
    String prioridade) {}
