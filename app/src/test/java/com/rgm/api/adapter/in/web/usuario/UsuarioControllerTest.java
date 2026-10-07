package com.rgm.api.adapter.in.web.usuario;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.AlterarSenhaRequest;
import com.rgm.api.adapter.in.web.solicitacao.SolicitacaoEventPublisher;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.auth.AlterarSenhaPropriaUseCase;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UsuarioController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class UsuarioControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private AlterarSenhaPropriaUseCase alterarSenhaUseCase;
  @MockitoBean private UsuarioRepository usuarioRepository;
  @MockitoBean private SolicitacaoEventPublisher eventPublisher;

  @Test
  void obterPerfil() throws Exception {
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario u =
        new Usuario(userId, "Me", "me@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(u));

    mockMvc
        .perform(get("/api/usuarios/me").with(user(userId.toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Me"))
        .andExpect(jsonPath("$.email").value("me@t.com"));
  }

  @Test
  void shouldReturnUserFieldsAndNewCredentialsWhenPasswordChanges() throws Exception {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario u =
        new Usuario(
            userId, "Me", "me@t.com", "hashNovo", PerfilUsuario.OPERADOR, true, agora, agora);
    final AlterarSenhaPropriaUseCase.Input entrada =
        new AlterarSenhaPropriaUseCase.Input(userId, "senhaAtual", "senhaNova");
    when(alterarSenhaUseCase.execute(entrada))
        .thenReturn(new AlterarSenhaPropriaUseCase.Output(u, "novo-acesso", "nova-renovacao"));

    // Act
    final var resposta =
        mockMvc.perform(
            patch("/api/usuarios/me/senha")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AlterarSenhaRequest("senhaAtual", "senhaNova"))));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.nome").value(u.getNome()))
        .andExpect(jsonPath("$.email").value(u.getEmail()))
        .andExpect(jsonPath("$.perfil").value(u.getPerfil().name()))
        .andExpect(jsonPath("$.ativo").value(u.isAtivo()))
        .andExpect(jsonPath("$.criadoEm").exists())
        .andExpect(jsonPath("$.atualizadoEm").exists())
        .andExpect(jsonPath("$.token").value("novo-acesso"))
        .andExpect(jsonPath("$.refreshToken").value("nova-renovacao"));
    verify(alterarSenhaUseCase, times(1)).execute(entrada);
    verify(eventPublisher, times(1)).encerrarConexoes(userId);
    verifyNoMoreInteractions(alterarSenhaUseCase, eventPublisher);
  }

  @Test
  void shouldKeepLiveConnectionsWhenPasswordChangeIsRefused() throws Exception {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final AlterarSenhaPropriaUseCase.Input entrada =
        new AlterarSenhaPropriaUseCase.Input(userId, "senhaAtual", "curta");
    when(alterarSenhaUseCase.execute(entrada))
        .thenThrow(new ValidationException("Senha deve ter no minimo 8 caracteres"));

    // Act
    final var resposta =
        mockMvc.perform(
            patch("/api/usuarios/me/senha")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AlterarSenhaRequest("senhaAtual", "curta"))));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Senha deve ter no minimo 8 caracteres"));
    verify(alterarSenhaUseCase, times(1)).execute(entrada);
    verifyNoMoreInteractions(alterarSenhaUseCase, eventPublisher);
  }
}
