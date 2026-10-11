package com.rgm.api.adapter.in.web.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SolicitacaoSseControllerTest {

  private static final String TOKEN = "token-de-acesso";

  private SolicitacaoEventPublisher publisher;
  private AutenticarAcessoUseCase autenticarAcessoUseCase;
  private SolicitacaoSseController controller;

  @BeforeEach
  void setUp() {
    publisher = mock(SolicitacaoEventPublisher.class);
    autenticarAcessoUseCase = mock(AutenticarAcessoUseCase.class);
    controller = new SolicitacaoSseController(publisher, autenticarAcessoUseCase);
  }

  private static Usuario usuario() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Usuario",
        "usuario@rgm.test",
        "hash",
        PerfilUsuario.OPERADOR,
        true,
        agora,
        agora);
  }

  @Test
  void shouldRegisterConnectionForTheUserWhenCredentialIsAccepted() {
    // Arrange
    final Usuario usuario = usuario();
    when(autenticarAcessoUseCase.execute(TOKEN)).thenReturn(usuario);

    // Act
    final SseEmitter emitter = controller.subscribe(TOKEN);

    // Assert
    assertEquals(30L * 60L * 1000L, emitter.getTimeout());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verify(publisher, times(1)).addEmitter(usuario.getId(), emitter);
    verifyNoMoreInteractions(autenticarAcessoUseCase, publisher);
  }

  @Test
  void shouldReturnTheEmitterWhenConnectionIsClosedBeforeTheFirstEvent() {
    // Arrange
    final Usuario usuario = usuario();
    when(autenticarAcessoUseCase.execute(TOKEN)).thenReturn(usuario);
    doAnswer(
            chamada -> {
              chamada.<SseEmitter>getArgument(1).complete();
              return null;
            })
        .when(publisher)
        .addEmitter(eq(usuario.getId()), any(SseEmitter.class));

    // Act
    final SseEmitter emitter = controller.subscribe(TOKEN);

    // Assert
    assertEquals(30L * 60L * 1000L, emitter.getTimeout());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verify(publisher, times(1)).addEmitter(usuario.getId(), emitter);
    verifyNoMoreInteractions(autenticarAcessoUseCase, publisher);
  }

  @Test
  void shouldRefuseConnectionWhenCredentialIsRefused() {
    // Arrange
    when(autenticarAcessoUseCase.execute(TOKEN))
        .thenThrow(new NaoAutorizadoException("Credencial emitida antes da troca de senha"));

    // Act
    final ResponseStatusException erro =
        assertThrows(ResponseStatusException.class, () -> controller.subscribe(TOKEN));

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    verify(autenticarAcessoUseCase, times(1)).execute(TOKEN);
    verifyNoMoreInteractions(autenticarAcessoUseCase, publisher);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void shouldRefuseConnectionWhenTokenIsMissing(final String token) {
    // Arrange
    final HttpStatus esperado = HttpStatus.UNAUTHORIZED;

    // Act
    final ResponseStatusException erro =
        assertThrows(ResponseStatusException.class, () -> controller.subscribe(token));

    // Assert
    assertEquals(esperado, erro.getStatusCode());
    verifyNoMoreInteractions(autenticarAcessoUseCase, publisher);
  }
}
