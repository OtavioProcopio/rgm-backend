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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

class JsonAuthenticationEntryPointTest {

  private ObjectMapper objectMapper;
  private JsonAuthenticationEntryPoint entryPoint;
  private MockHttpServletRequest request;
  private AuthenticationException exception;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
    request = new MockHttpServletRequest();
    exception = new BadCredentialsException("sem credencial");
  }

  @Test
  void shouldRespondStatus401WhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertEquals(401, response.getStatus());
  }

  @Test
  void shouldRespondJsonUtf8WhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertEquals("application/json;charset=UTF-8", response.getHeader("Content-Type"));
  }

  @Test
  void shouldWriteStatusInBodyWhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertEquals(401, corpoDe(response).get("status").asInt());
  }

  @Test
  void shouldWriteErrorTypeInBodyWhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertEquals("Unauthorized", corpoDe(response).get("error").asText());
  }

  @Test
  void shouldWriteMessageInPortugueseInBodyWhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertEquals("Não autenticado", corpoDe(response).get("message").asText());
  }

  @Test
  void shouldWriteTimestampInBodyWhenCommence() throws IOException {
    // Arrange
    final MockHttpServletResponse response = new MockHttpServletResponse();

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    assertTrue(corpoDe(response).hasNonNull("timestamp"));
  }

  @Test
  void shouldNeverCallSendErrorWhenCommence() throws IOException {
    // Arrange
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream())
        .thenReturn(new DelegatingServletOutputStream(new ByteArrayOutputStream()));

    // Act
    entryPoint.commence(request, response, exception);

    // Assert
    verify(response, never()).sendError(anyInt());
    verify(response, never()).sendError(anyInt(), anyString());
  }

  private JsonNode corpoDe(final MockHttpServletResponse response) throws IOException {
    return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }
}
