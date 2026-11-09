package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record TriarSolicitacaoRequest(
    @NotNull String prioridade,
    @NotEmpty List<UUID> responsavelIds,
    @NotBlank @Size(max = LimitesTexto.COMENTARIO, message = LimitesTexto.MENSAGEM)
        String instrucao) {}
