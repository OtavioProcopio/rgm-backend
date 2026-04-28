package com.rgm.api.core.domain.ports.repositories;

import java.util.List;

/**
 * Representacao generica de uma pagina de resultados. Pertence ao dominio para nao acoplar use
 * cases ao Spring Data Page.
 */
public record PageResult<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
