package com.rgm.api.adapter.in.web.modelo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.FotoCapaUploadRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.modelo.AtualizarFotoCapaUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
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
    controllers = ModeloController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class ModeloControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private GerenciarModelosUseCase gerenciarUseCase;
  @MockitoBean private AtualizarFotoCapaUseCase fotoCapaUseCase;
  @MockitoBean private ListarModelosUseCase listarUseCase;
  @MockitoBean private ModeloRepository modeloRepository;
  @MockitoBean private EventoModeloRepository eventoModeloRepository;
  @MockitoBean private SolicitacaoRepository solicitacaoRepository;
  @MockitoBean private AtividadeSolicitacaoRepository atividadeRepository;
  @MockitoBean private ModeloPdfService modeloPdfService;

  private Modelo criarModelo() {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        "MOD-001",
        1,
        "Desc",
        "Obs",
        null,
        null,
        null,
        null,
        true,
        "FBOX",
        false,
        agora,
        agora);
  }

  @Test
  void listarModelos() throws Exception {
    final Modelo modelo = criarModelo();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(modelo), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/modelos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigo").value("MOD-001"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void listarModelos_comFiltros() throws Exception {
    final Modelo modelo = criarModelo();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(modelo), 0, 20, 1, 1));

    mockMvc
        .perform(
            get("/api/modelos")
                .param("ativo", "true")
                .param("codigo", "MOD-001")
                .param("maquina", "FBOX")
                .param("descricao", "Desc")
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigo").value("MOD-001"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void deveCriarModelo() throws Exception {
    final Modelo modelo = criarModelo();
    when(gerenciarUseCase.criar(any())).thenReturn(modelo);

    mockMvc
        .perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CriarModeloRequest("MOD-001", "Desc", "Obs", "FBOX"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void deveDesativarModelo() throws Exception {
    final Modelo modelo = criarModelo();
    when(gerenciarUseCase.desativar(any())).thenReturn(modelo);

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/modelos/{id}/desativar", modelo.getId())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void deveAtivarModelo() throws Exception {
    final Modelo modelo = criarModelo();
    when(gerenciarUseCase.reativar(any())).thenReturn(modelo);

    mockMvc
        .perform(
            patch("/api/modelos/{id}/ativar", modelo.getId())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"))
        .andExpect(jsonPath("$.ativo").value(true));
  }

  @Test
  void buscarModeloPorId() throws Exception {
    final Modelo modelo = criarModelo();
    when(modeloRepository.findById(modelo.getId())).thenReturn(java.util.Optional.of(modelo));

    mockMvc
        .perform(get("/api/modelos/{id}", modelo.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void buscarModeloPorId_naoEncontrado() throws Exception {
    final UUID id = UUID.randomUUID();
    when(modeloRepository.findById(id)).thenReturn(java.util.Optional.empty());

    mockMvc.perform(get("/api/modelos/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void listarEventosModelo() throws Exception {
    final Modelo modelo = criarModelo();
    final EventoModelo evento =
        new EventoModelo(
            UUID.randomUUID(),
            modelo.getId(),
            TipoEventoModelo.INSPECAO,
            "Inspeção",
            "Tudo ok",
            "Bom",
            false,
            UUID.randomUUID(),
            null,
            java.time.Instant.now());
    when(modeloRepository.findById(modelo.getId())).thenReturn(java.util.Optional.of(modelo));
    when(eventoModeloRepository.findByModeloId(modelo.getId())).thenReturn(List.of(evento));

    mockMvc
        .perform(get("/api/modelos/{id}/eventos", modelo.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].titulo").value("Inspeção"));
  }

  @Test
  void editarModelo() throws Exception {
    final Modelo modelo = criarModelo();
    when(gerenciarUseCase.editar(any())).thenReturn(modelo);

    mockMvc
        .perform(
            put("/api/modelos/{id}", modelo.getId())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EditarModeloRequest("MOD-001", "Nova Desc", "Obs", "FBOX"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void uploadFotoCapa() throws Exception {
    final Modelo modelo = criarModelo();
    when(fotoCapaUseCase.uploadFile(any())).thenReturn("http://s3/file.png");
    when(fotoCapaUseCase.persistUpload(any(), any())).thenReturn(modelo);

    final org.springframework.mock.web.MockMultipartFile mockFile =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "avatar.png", "image/png", "some image".getBytes());

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                    "/api/modelos/{id}/foto-capa", modelo.getId())
                .file(mockFile)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void usarEvidenciaComoFotoCapa() throws Exception {
    final Modelo modelo = criarModelo();
    when(fotoCapaUseCase.executeEvidenciaExistente(any())).thenReturn(modelo);

    mockMvc
        .perform(
            patch("/api/modelos/{id}/foto-capa", modelo.getId())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new FotoCapaUploadRequest(UUID.randomUUID()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }
}
