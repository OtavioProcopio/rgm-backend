package com.rgm.api.adapter.in.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.config.RateLimitFilter;
import com.rgm.api.adapter.config.SecurityConfig;
import com.rgm.api.adapter.in.web.solicitacao.SolicitacaoEventPublisher;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RateLimitFilter.class,
  GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class AdminControllerSegurancaTest {

  private static final String ROTA_USUARIOS = "/api/admin/usuarios";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GerenciarUsuariosUseCase gerenciarUsuariosUseCase;
  @MockitoBean private CadastrarPrestadorExternoUseCase cadastrarExternoUseCase;
  @MockitoBean private ExcluirRegistroUseCase excluirRegistroUseCase;
  @MockitoBean private ListarUsuariosUseCase listarUsuariosUseCase;
  @MockitoBean private UsuarioRepository usuarioRepository;
  @MockitoBean private SolicitacaoEventPublisher eventPublisher;
  @MockitoBean private AutenticarAcessoUseCase autenticarAcessoUseCase;

  private static Usuario criarUsuario(final PerfilUsuario perfil) {
    return new Usuario(
        UUID.randomUUID(),
        "Usuario",
        "usuario@rgm.com",
        "senhaHash",
        perfil,
        true,
        Instant.now(),
        Instant.now(),
        1);
  }

  private void autenticarComo(final String token, final PerfilUsuario perfil) {
    when(autenticarAcessoUseCase.execute(token)).thenReturn(criarUsuario(perfil));
  }

  @Test
  void shouldResponder403WhenOperadorListaUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        get(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(listarUsuariosUseCase);
  }

  @Test
  void shouldRetornarCorpoDeErroWhenOperadorListaUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        get(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"));
  }

  @Test
  void shouldResponder200WhenGestorListaUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-gestor", PerfilUsuario.GESTOR);
    when(listarUsuariosUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    final var requisicao =
        get(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-gestor");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
    verificarListagemPadraoComToken("token-gestor");
  }

  @Test
  void shouldResponder200WhenAdministradorListaUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-admin", PerfilUsuario.ADMINISTRADOR);
    when(listarUsuariosUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    final var requisicao =
        get(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-admin");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isOk());
    verificarListagemPadraoComToken("token-admin");
  }

  @Test
  void shouldResponder403WhenOperadorUsaHeadEmUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        head(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-operador");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(listarUsuariosUseCase);
  }

  @Test
  void shouldResponder403WhenGestorUsaHeadEmUsuarios() throws Exception {
    // Arrange
    autenticarComo("token-gestor", PerfilUsuario.GESTOR);
    final var requisicao =
        head(ROTA_USUARIOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-gestor");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(listarUsuariosUseCase);
  }

  private void verificarListagemPadraoComToken(final String token) {
    final var esperado = new ListarUsuariosUseCase.Input(null, null, 0, 20);
    verify(listarUsuariosUseCase, times(1)).execute(esperado);
    verifyNoMoreInteractions(listarUsuariosUseCase);
    verify(autenticarAcessoUseCase, times(1)).execute(token);
    verifyNoMoreInteractions(autenticarAcessoUseCase);
  }

  @Test
  void shouldResponder401WhenListaUsuariosSemCredencial() throws Exception {
    // Arrange
    final var requisicao = get(ROTA_USUARIOS);

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isUnauthorized());
    verifyNoInteractions(listarUsuariosUseCase);
  }

  @Test
  void shouldResponder403WhenOperadorCriaUsuario() throws Exception {
    // Arrange
    autenticarComo("token-operador", PerfilUsuario.OPERADOR);
    final var requisicao =
        post(ROTA_USUARIOS)
            .header(HttpHeaders.AUTHORIZATION, "Bearer token-operador")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}");

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isForbidden());
    verifyNoInteractions(gerenciarUsuariosUseCase);
  }
}
