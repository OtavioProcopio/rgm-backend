package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record GerenciarResponsaveisRequest(@NotEmpty List<UUID> responsavelIds) {}
