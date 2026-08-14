package com.rgm.api.adapter.in.web.modelo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.EditarFotoGaleriaRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.modelo.AdicionarFotoGaleriaUseCase;
import com.rgm.api.core.application.usecases.modelo.EditarFotoGaleriaUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarGaleriaModeloUseCase;
import com.rgm.api.core.application.usecases.modelo.RemoverFotoGaleriaUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = GaleriaModeloController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class GaleriaModeloControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private AdicionarFotoGaleriaUseCase adicionarUseCase;
  @MockitoBean private ListarGaleriaModeloUseCase listarUseCase;
  @MockitoBean private EditarFotoGaleriaUseCase editarUseCase;
  @MockitoBean private RemoverFotoGaleriaUseCase removerUseCase;

  private FotoGaleriaModelo criarFoto(final UUID modeloId) {
    return FotoGaleriaModelo.criar(
        modeloId, "http://minio/foto.jpg", "Parte 1", true, UUID.randomUUID(), Instant.now());
  }

  @Test
  void listarGaleria() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    when(listarUseCase.execute(modeloId)).thenReturn(List.of(criarFoto(modeloId)));

    mockMvc
        .perform(get("/api/modelos/{modeloId}/galeria", modeloId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].identificacao").value("Parte 1"))
        .andExpect(jsonPath("$[0].principal").value(true));
  }

  @Test
  void listarGaleria_modeloInexistenteRetorna404() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    when(listarUseCase.execute(modeloId))
        .thenThrow(new RecursoNaoEncontradoException("Modelo nao encontrado"));

    mockMvc
        .perform(get("/api/modelos/{modeloId}/galeria", modeloId))
        .andExpect(status().isNotFound());
  }

  @Test
  void adicionarFoto() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final FotoGaleriaModelo foto = criarFoto(modeloId);
    when(adicionarUseCase.upload(any())).thenReturn("http://minio/foto.jpg");
    when(adicionarUseCase.persist(any(), any())).thenReturn(foto);

    final MockMultipartFile arquivo =
        new MockMultipartFile("file", "foto.jpg", "image/jpeg", "fake-content".getBytes());

    mockMvc
        .perform(
            multipart("/api/modelos/{modeloId}/galeria", modeloId)
                .file(arquivo)
                .param("identificacao", "Parte 1")
                .with(user(userId.toString())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.identificacao").value("Parte 1"));
  }

  @Test
  void adicionarFoto_semPermissaoRetorna403() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    when(adicionarUseCase.upload(any())).thenThrow(new NaoAutorizadoException("sem permissao"));

    final MockMultipartFile arquivo =
        new MockMultipartFile("file", "foto.jpg", "image/jpeg", "fake-content".getBytes());

    mockMvc
        .perform(
            multipart("/api/modelos/{modeloId}/galeria", modeloId)
                .file(arquivo)
                .param("identificacao", "Parte 1")
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isForbidden());
  }

  @Test
  void editarFoto() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    final UUID fotoId = UUID.randomUUID();
    final FotoGaleriaModelo foto = criarFoto(modeloId).comIdentificacao("Contra-macho");
    when(editarUseCase.execute(any())).thenReturn(foto);

    mockMvc
        .perform(
            patch("/api/modelos/{modeloId}/galeria/{fotoId}", modeloId, fotoId)
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new EditarFotoGaleriaRequest("Contra-macho", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.identificacao").value("Contra-macho"));
  }

  @Test
  void removerFoto() throws Exception {
    final UUID modeloId = UUID.randomUUID();
    final UUID fotoId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/api/modelos/{modeloId}/galeria/{fotoId}", modeloId, fotoId)
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isNoContent());
  }
}
