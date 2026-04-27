package com.rgm.api.adapter.in.web.dto.response;

import java.time.Instant;

public record ErrorResponse(int status, String error, String message, Instant timestamp) {

  public static ErrorResponse of(final int status, final String error, final String message) {
    return new ErrorResponse(status, error, message, Instant.now());
  }
}
