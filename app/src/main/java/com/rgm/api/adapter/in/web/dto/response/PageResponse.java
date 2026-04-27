package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <D, R> PageResponse<R> from(
      final PageResult<D> pageResult, final Function<D, R> mapper) {
    return new PageResponse<>(
        pageResult.content().stream().map(mapper).toList(),
        pageResult.page(),
        pageResult.size(),
        pageResult.totalElements(),
        pageResult.totalPages());
  }
}
