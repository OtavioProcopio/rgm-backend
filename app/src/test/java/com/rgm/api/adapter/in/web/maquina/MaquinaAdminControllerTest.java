package com.rgm.api.adapter.in.web.maquina;

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
import com.rgm.api.adapter.in.web.admin.MaquinaAdminController;
import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarMaquinaRequest;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
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
    controllers = MaquinaAdminController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class MaquinaAdminControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private GerenciarMaquinasUseCase gerenciarMaquinasUseCase;

  private Maquina maquina() {
    final Instant agora = Instant.now();
    return new Maquina(UUID.randomUUID(), "VICK", true, agora, agora);
  }

  @Test
  void criar() throws Exception {
    when(gerenciarMaquinasUseCase.criar(any())).thenReturn(maquina());

    mockMvc
        .perform(
            post("/api/admin/maquinas")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CriarMaquinaRequest("VICK"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("VICK"))
        .andExpect(jsonPath("$.ativo").value(true));
  }

  @Test
  void renomear() throws Exception {
    when(gerenciarMaquinasUseCase.renomear(any())).thenReturn(maquina());

    mockMvc
        .perform(
            put("/api/admin/maquinas/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EditarMaquinaRequest("VICK"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("VICK"));
  }

  @Test
  void desativar() throws Exception {
    when(gerenciarMaquinasUseCase.desativar(any())).thenReturn(maquina());

    mockMvc
        .perform(
            patch("/api/admin/maquinas/{id}/desativar", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  @Test
  void ativar() throws Exception {
    when(gerenciarMaquinasUseCase.ativar(any())).thenReturn(maquina());

    mockMvc
        .perform(
            patch("/api/admin/maquinas/{id}/ativar", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  private static final String NOME_LONGO = "x".repeat(LimitesTexto.MAQUINA_NOME + 1);
  private static final String MENSAGEM_LIMITE =
      "nome: deve ter no máximo " + LimitesTexto.MAQUINA_NOME + " caracteres";

  @Test
  void shouldAnswerBadRequestWhenCreatingMaquinaWithNameAboveLimit() throws Exception {
    // Arrange
    final String corpo = objectMapper.writeValueAsString(new CriarMaquinaRequest(NOME_LONGO));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/admin/maquinas")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(MENSAGEM_LIMITE));
    verifyNoMoreInteractions(gerenciarMaquinasUseCase);
  }

  @Test
  void shouldAnswerBadRequestWhenRenamingMaquinaWithNameAboveLimit() throws Exception {
    // Arrange
    final String corpo = objectMapper.writeValueAsString(new EditarMaquinaRequest(NOME_LONGO));

    // Act
    final var resposta =
        mockMvc.perform(
            put("/api/admin/maquinas/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(MENSAGEM_LIMITE));
    verifyNoMoreInteractions(gerenciarMaquinasUseCase);
  }
}
