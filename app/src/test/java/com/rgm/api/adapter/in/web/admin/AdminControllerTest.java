package com.rgm.api.adapter.in.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.AlterarPerfilRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.ExcluirRegistroRequest;
import com.rgm.api.adapter.in.web.dto.request.RedefinirSenhaRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AdminController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class AdminControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private GerenciarUsuariosUseCase gerenciarUsuariosUseCase;
  @MockitoBean private CadastrarPrestadorExternoUseCase cadastrarExternoUseCase;
  @MockitoBean private ExcluirRegistroUseCase excluirRegistroUseCase;
  @MockitoBean private ListarUsuariosUseCase listarUsuariosUseCase;
  @MockitoBean private UsuarioRepository usuarioRepository;

  @Test
  void listarUsuarios() throws Exception {
    final Instant agora = Instant.now();
    final Usuario u =
        new Usuario(
            UUID.randomUUID(),
            "Admin",
            "a@t.com",
            "hash",
            PerfilUsuario.ADMINISTRADOR,
            true,
            agora,
            agora);
    when(listarUsuariosUseCase.execute(any()))
        .thenReturn(new PageResult<>(List.of(u), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/admin/usuarios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nome").value("Admin"))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void criarUsuario() throws Exception {
    final Instant agora = Instant.now();
    final Usuario u =
        new Usuario(
            UUID.randomUUID(),
            "Novo",
            "n@t.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);
    when(gerenciarUsuariosUseCase.criar(any())).thenReturn(u);

    mockMvc
        .perform(
            post("/api/admin/usuarios")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CriarUsuarioRequest("Novo", "n@t.com", "s3nha", "OPERADOR", true))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Novo"));
  }

  @Test
  void redefinirSenha() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final Usuario u =
        new Usuario(
            userId, "Alvo", "alvo@t.com", "hashed", PerfilUsuario.OPERADOR, true, agora, agora);
    when(gerenciarUsuariosUseCase.redefinirSenha(any())).thenReturn(u);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/admin/usuarios/{id}/senha", userId)
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new RedefinirSenhaRequest("senhaTemporaria"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Alvo"));
  }

  @Test
  void alterarPerfil() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final Usuario u =
        new Usuario(
            userId, "Alvo", "alvo@t.com", "hashed", PerfilUsuario.GESTOR, true, agora, agora);
    when(gerenciarUsuariosUseCase.alterarPerfil(any())).thenReturn(u);

    mockMvc
        .perform(
            patch("/api/admin/usuarios/{id}/perfil", userId)
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AlterarPerfilRequest("GESTOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("GESTOR"));
  }

  @Test
  void buscarUsuarioPorId() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final Usuario u =
        new Usuario(userId, "Op", "op@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
    when(usuarioRepository.findById(userId)).thenReturn(java.util.Optional.of(u));

    mockMvc
        .perform(get("/api/admin/usuarios/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Op"));
  }

  @Test
  void editarUsuario() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final UUID adminId = UUID.randomUUID();
    final Usuario u =
        new Usuario(
            userId, "Editado", "edit@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
    when(gerenciarUsuariosUseCase.editar(any())).thenReturn(u);

    mockMvc
        .perform(
            put("/api/admin/usuarios/{id}", userId)
                .with(user(adminId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EditarUsuarioRequest("Editado", "edit@t.com"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Editado"));
  }

  @Test
  void desativarUsuario() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final Usuario u =
        new Usuario(userId, "Op", "op@t.com", "hash", PerfilUsuario.OPERADOR, false, agora, agora);
    when(gerenciarUsuariosUseCase.desativar(any())).thenReturn(u);

    mockMvc
        .perform(
            patch("/api/admin/usuarios/{id}/desativar", userId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(false));
  }

  @Test
  void ativarUsuario() throws Exception {
    final Instant agora = Instant.now();
    final UUID userId = UUID.randomUUID();
    final Usuario u =
        new Usuario(userId, "Op", "op@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
    when(gerenciarUsuariosUseCase.ativar(any())).thenReturn(u);

    mockMvc
        .perform(
            patch("/api/admin/usuarios/{id}/ativar", userId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(true));
  }

  @Test
  void excluirRegistro() throws Exception {
    org.mockito.Mockito.doNothing().when(excluirRegistroUseCase).execute(any());

    mockMvc
        .perform(
            delete("/api/admin/registros")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ExcluirRegistroRequest("SOLICITACAO", UUID.randomUUID()))))
        .andExpect(status().isNoContent());
  }
}
