package com.rgm.api.adapter.in.web.tipomodelo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.admin.TipoDeModeloAdminController;
import com.rgm.api.adapter.in.web.dto.request.CriarTipoDeModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarTipoDeModeloRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.GerenciarTiposDeModeloUseCase;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.validation.LimitesTexto;
import java.time.Instant;
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
    controllers = TipoDeModeloAdminController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class TipoDeModeloAdminControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private GerenciarTiposDeModeloUseCase gerenciarTiposDeModeloUseCase;

  private TipoDeModelo tipoDeModelo() {
    final Instant agora = Instant.now();
    return new TipoDeModelo(UUID.randomUUID(), "VICK", true, agora, agora);
  }

  @Test
  void criar() throws Exception {
    when(gerenciarTiposDeModeloUseCase.criar(any())).thenReturn(tipoDeModelo());

    mockMvc
        .perform(
            post("/api/admin/tipos-modelo")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CriarTipoDeModeloRequest("VICK"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("VICK"))
        .andExpect(jsonPath("$.ativo").value(true));
  }

  @Test
  void renomear() throws Exception {
    when(gerenciarTiposDeModeloUseCase.renomear(any())).thenReturn(tipoDeModelo());

    mockMvc
        .perform(
            put("/api/admin/tipos-modelo/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EditarTipoDeModeloRequest("VICK"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("VICK"));
  }

  @Test
  void desativar() throws Exception {
    when(gerenciarTiposDeModeloUseCase.desativar(any())).thenReturn(tipoDeModelo());

    mockMvc
        .perform(
            patch("/api/admin/tipos-modelo/{id}/desativar", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  @Test
  void ativar() throws Exception {
    when(gerenciarTiposDeModeloUseCase.ativar(any())).thenReturn(tipoDeModelo());

    mockMvc
        .perform(
            patch("/api/admin/tipos-modelo/{id}/ativar", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  private static final String NOME_LONGO = "x".repeat(LimitesTexto.TIPO_DE_MODELO_NOME + 1);
  private static final String MENSAGEM_LIMITE =
      "nome: deve ter no máximo " + LimitesTexto.TIPO_DE_MODELO_NOME + " caracteres";

  @Test
  void shouldAnswerBadRequestWhenCreatingTipoDeModeloWithNameAboveLimit() throws Exception {
    // Arrange
    final String corpo = objectMapper.writeValueAsString(new CriarTipoDeModeloRequest(NOME_LONGO));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/admin/tipos-modelo")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(MENSAGEM_LIMITE));
    verifyNoMoreInteractions(gerenciarTiposDeModeloUseCase);
  }

  @Test
  void shouldAnswerBadRequestWhenRenamingTipoDeModeloWithNameAboveLimit() throws Exception {
    // Arrange
    final String corpo = objectMapper.writeValueAsString(new EditarTipoDeModeloRequest(NOME_LONGO));

    // Act
    final var resposta =
        mockMvc.perform(
            put("/api/admin/tipos-modelo/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(MENSAGEM_LIMITE));
    verifyNoMoreInteractions(gerenciarTiposDeModeloUseCase);
  }
}
