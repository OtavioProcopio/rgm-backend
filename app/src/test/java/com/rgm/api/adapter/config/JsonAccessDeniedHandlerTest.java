package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JsonAccessDeniedHandlerTest {

  private static final String MENSAGEM = "Acesso negado: permissões insuficientes";

  private ObjectMapper objectMapper;
  private JsonAccessDeniedHandler handler;
  private MockHttpServletRequest request;
  private AccessDeniedException exception;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    handler = new JsonAccessDeniedHandler(objectMapper);
    request = new MockHttpServletRequest();
    exception = new AccessDeniedException("negado");
  }

  @Test
  void shouldReturnStatus403WhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertEquals(403, response.getStatus());
  }

  @Test
  void shouldSetJsonUtf8ContentTypeWhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertEquals("application/json;charset=UTF-8", response.getHeader("Content-Type"));
  }

  @Test
  void shouldWriteStatusInBodyWhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertEquals(403, corpoDe(response).get("status").asInt());
  }

  @Test
  void shouldWriteErrorTypeInBodyWhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertEquals("Forbidden", corpoDe(response).get("error").asText());
  }

  @Test
  void shouldWriteMessageInPortugueseInBodyWhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertEquals(MENSAGEM, corpoDe(response).get("message").asText());
  }

  @Test
  void shouldWriteTimestampInBodyWhenAccessDenied() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    handler.handle(request, response, exception);

    // Assert
    assertTrue(corpoDe(response).hasNonNull("timestamp"));
  }

  @Test
  void shouldNeverCallSendErrorWhenAccessDenied() throws IOException {
    // Arrange
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream())
        .thenReturn(new DelegatingServletOutputStream(new ByteArrayOutputStream()));

    // Act
    handler.handle(request, response, exception);

    // Assert
    verify(response, never()).sendError(anyInt());
    verify(response, never()).sendError(anyInt(), anyString());
  }

  private JsonNode corpoDe(final MockHttpServletResponse response) throws IOException {
    return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }
}
