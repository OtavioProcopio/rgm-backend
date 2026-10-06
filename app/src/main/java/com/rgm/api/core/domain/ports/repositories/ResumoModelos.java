package com.rgm.api.core.domain.ports.repositories;

import java.util.List;

/**
 * Contagens do cadastro de modelos. `comPendenciaAberta` e `porMaquina` contam modelos ativos e
 * inativos; `porMaquina` vem da maior para a menor quantidade, empate em ordem alfabetica.
 */
public record ResumoModelos(
    long total,
    long ativos,
    long inativos,
    long comPendenciaAberta,
    List<QuantidadePorMaquina> porMaquina) {}
