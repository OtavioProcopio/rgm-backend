package com.rgm.api.adapter.in.web.maquina;

import static org.mockito.ArgumentMatchers.any;
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
}
