package com.rgm.api.adapter.in.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarUsuarioRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
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
  @MockitoBean private GerenciarMaquinasUseCase gerenciarMaquinasUseCase;
  @MockitoBean private ExcluirRegistroUseCase excluirRegistroUseCase;
  @MockitoBean private ListarUsuariosUseCase listarUsuariosUseCase;
  @MockitoBean private ListarMaquinasUseCase listarMaquinasUseCase;

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
    when(listarUsuariosUseCase.execute(anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(u), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/admin/usuarios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nome").value("Admin"))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void listarMaquinas() throws Exception {
    final Instant agora = Instant.now();
    final Maquina maq =
        new Maquina(UUID.randomUUID(), "CNC-01", "CNC01", "Torno CNC", true, agora, agora);
    when(listarMaquinasUseCase.execute(anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(maq), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/admin/maquinas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nome").value("CNC-01"))
        .andExpect(jsonPath("$.totalElements").value(1));
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
  void criarMaquina() throws Exception {
    final Instant agora = Instant.now();
    final Maquina maq =
        new Maquina(UUID.randomUUID(), "CNC-02", "CNC02", "Fresadora", true, agora, agora);
    when(gerenciarMaquinasUseCase.criar(any())).thenReturn(maq);

    mockMvc
        .perform(
            post("/api/admin/maquinas")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CriarMaquinaRequest("CNC-02", "CNC02", "Fresadora"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("CNC-02"));
  }
}
