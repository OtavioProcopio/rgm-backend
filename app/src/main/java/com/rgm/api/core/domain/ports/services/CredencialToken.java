package com.rgm.api.core.domain.ports.services;

import java.util.UUID;

/** O que um token valido diz sobre quem o apresenta. */
public record CredencialToken(UUID usuarioId, int versaoCredencial) {}
