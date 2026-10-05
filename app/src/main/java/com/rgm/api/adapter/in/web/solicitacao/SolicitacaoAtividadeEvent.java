package com.rgm.api.adapter.in.web.solicitacao;

import java.util.UUID;

/** Aviso de que a linha do tempo de uma solicitacao mudou (comentario ou evidencia). */
public record SolicitacaoAtividadeEvent(String tipo, UUID solicitacaoId) {}
