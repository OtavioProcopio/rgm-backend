package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.adapter.in.web.dto.response.SolicitacaoResponse;

public record SolicitacaoEvent(String tipo, SolicitacaoResponse solicitacao) {}
