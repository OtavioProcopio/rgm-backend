package com.rgm.api.adapter.in.web.solicitacao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.solicitacao.ListarResponsaveisDisponiveisUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ResponsaveisDisponiveisController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class))
@Import({WebMvcTestConfig.class, GlobalExceptionHandler.class})
class ResponsaveisDisponiveisControllerTest {

  private static final String ROTA = "/api/solicitacoes/responsaveis-disponiveis";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ListarResponsaveisDisponiveisUseCase listarUseCase;

  @Test
  void shouldReturnPageWithIdNameAndProfileWhenListing() throws Exception {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final Usuario zelia =
        Usuario.criarInterno("Zélia", "z@rgm.com", "hash", PerfilUsuario.OPERADOR, Instant.now());
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(zelia), 0, 20, 1, 1));

    // Act and Assert
    mockMvc
        .perform(get(ROTA).param("busca", "zelia").with(user(usuarioId.toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(zelia.getId().toString()))
        .andExpect(jsonPath("$.content[0].nome").value("Zélia"))
        .andExpect(jsonPath("$.content[0].perfil").value("OPERADOR"))
        .andExpect(jsonPath("$.totalElements").value(1));
    verify(listarUseCase, times(1))
        .execute(new ListarResponsaveisDisponiveisUseCase.Input("zelia", 0, 20, usuarioId));
  }

  @Test
  void shouldRespond403WhenUseCaseRefusesTheProfile() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenThrow(new NaoAutorizadoException("sem permissao"));

    // Act and Assert
    mockMvc
        .perform(get(ROTA).with(user(UUID.randomUUID().toString())))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRespond400WhenSizeIsOverTheLimit() throws Exception {
    // Arrange
    final String usuario = UUID.randomUUID().toString();

    // Act and Assert
    mockMvc
        .perform(get(ROTA).param("size", "101").with(user(usuario)))
        .andExpect(status().isBadRequest());
    verify(listarUseCase, never()).execute(any());
  }

  @Test
  void shouldRespond400WhenBuscaHasOneCharacter() throws Exception {
    // Arrange
    final String usuario = UUID.randomUUID().toString();

    // Act and Assert
    mockMvc
        .perform(get(ROTA).param("busca", "a").with(user(usuario)))
        .andExpect(status().isBadRequest());
    verify(listarUseCase, never()).execute(any());
  }
}
