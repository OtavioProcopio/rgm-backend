package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record TriarSolicitacaoRequest(
    @NotNull String prioridade, @NotEmpty List<UUID> responsavelIds) {}
