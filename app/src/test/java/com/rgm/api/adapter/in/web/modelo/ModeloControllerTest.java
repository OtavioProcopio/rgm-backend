package com.rgm.api.adapter.in.web.modelo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.config.GlobalExceptionHandler;
import com.rgm.api.adapter.in.web.WebMvcTestConfig;
import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarModeloRequest;
import com.rgm.api.adapter.out.report.ContextoDoRelatorio;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterFichaDoModeloUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoSolicitacoesModeloUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.QuantidadePorMaquina;
import com.rgm.api.core.domain.ports.repositories.ResumoModelos;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.validation.LimitesTexto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
  @MockitoBean private ListarModelosUseCase listarUseCase;
  @MockitoBean private ObterResumoModelosUseCase resumoModelosUseCase;
  @MockitoBean private ObterResumoSolicitacoesModeloUseCase resumoSolicitacoesUseCase;
  @MockitoBean private ModeloRepository modeloRepository;
  @MockitoBean private EventoModeloRepository eventoModeloRepository;
  @MockitoBean private ObterFichaDoModeloUseCase obterFichaUseCase;
  @MockitoBean private ModeloPdfService modeloPdfService;
  @MockitoBean private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;

  @MockitoBean
  private com.rgm.api.core.domain.ports.repositories.UsuarioRepository usuarioRepository;

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
        true,
        "FBOX",
        null,
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
                        new CriarModeloRequest("MOD-001", "Desc", "Obs", "FBOX", null))))
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
                        new EditarModeloRequest("MOD-001", "Nova Desc", "Obs", "FBOX", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("MOD-001"));
  }

  @Test
  void exportarRelatorioLista() throws Exception {
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    when(modeloPdfService.gerarLista(any(), any(), any())).thenReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(get("/api/modelos/relatorio").with(user(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  private ObterFichaDoModeloUseCase.Ficha fichaVazia(final Modelo modelo) {
    return new ObterFichaDoModeloUseCase.Ficha(
        modelo, List.of(), List.of(), Map.of(), Map.of(), null, null, List.of());
  }

  @Test
  void shouldRenderTheFichaFromTheUseCaseAndNameTheFileAfterTheModelWhenExportingFicha()
      throws Exception {
    // Arrange
    final Modelo modelo = criarModelo();
    final var ficha = fichaVazia(modelo);
    final byte[] pdf = {1, 2, 3};
    when(obterFichaUseCase.execute(modelo.getId())).thenReturn(ficha);
    when(modeloPdfService.gerarFicha(ficha, "Sistema")).thenReturn(pdf);

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/{id}/relatorio", modelo.getId()).with(user("nao-e-uuid")));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().bytes(pdf))
        .andExpect(
            header().string("Content-Disposition", containsString("ficha-modelo-MOD-001.pdf")));
    verify(obterFichaUseCase, times(1)).execute(modelo.getId());
    verify(modeloPdfService, times(1)).gerarFicha(ficha, "Sistema");
  }

  @Test
  void shouldResponderContagensEMaquinasWhenConsultaOResumoDeModelos() throws Exception {
    // Arrange
    final var resumo =
        new ResumoModelos(
            7,
            5,
            2,
            2,
            List.of(new QuantidadePorMaquina("DISA", 4), new QuantidadePorMaquina("FBOX", 3)));
    when(resumoModelosUseCase.execute()).thenReturn(resumo);

    // Act
    final var resposta =
        mockMvc.perform(get("/api/modelos/resumo").with(user(UUID.randomUUID().toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(resumo.total()))
        .andExpect(jsonPath("$.ativos").value(resumo.ativos()))
        .andExpect(jsonPath("$.inativos").value(resumo.inativos()))
        .andExpect(jsonPath("$.comPendenciaAberta").value(resumo.comPendenciaAberta()))
        .andExpect(jsonPath("$.porMaquina[0].maquina").value("DISA"))
        .andExpect(jsonPath("$.porMaquina[0].quantidade").value(4))
        .andExpect(jsonPath("$.porMaquina[1].maquina").value("FBOX"))
        .andExpect(jsonPath("$.porMaquina[1].quantidade").value(3));
    verify(resumoModelosUseCase, times(1)).execute();
  }

  @Test
  void shouldResponderContagensETemposWhenConsultaOResumoDasSolicitacoesDoModelo()
      throws Exception {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final var resumo = new ResumoSolicitacoesModelo(6, 2, 3, 1, 259200.0, 864000.0);
    when(resumoSolicitacoesUseCase.execute(modeloId)).thenReturn(resumo);

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/{id}/solicitacoes/resumo", modeloId)
                .with(user(UUID.randomUUID().toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(resumo.total()))
        .andExpect(jsonPath("$.emAberto").value(resumo.emAberto()))
        .andExpect(jsonPath("$.concluidas").value(resumo.concluidas()))
        .andExpect(jsonPath("$.canceladas").value(resumo.canceladas()))
        .andExpect(
            jsonPath("$.tempoMedioResolucaoSegundos").value(resumo.tempoMedioResolucaoSegundos()))
        .andExpect(jsonPath("$.intervaloMedioSegundos").value(resumo.intervaloMedioSegundos()));
    verify(resumoSolicitacoesUseCase, times(1)).execute(modeloId);
  }

  @Test
  void shouldResponderTemposNulosWhenResumoDoModeloNaoTemDado() throws Exception {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    when(resumoSolicitacoesUseCase.execute(modeloId))
        .thenReturn(new ResumoSolicitacoesModelo(1, 1, 0, 0, null, null));

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/{id}/solicitacoes/resumo", modeloId)
                .with(user(UUID.randomUUID().toString())));

    // Assert
    resposta
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tempoMedioResolucaoSegundos").value(nullValue()))
        .andExpect(jsonPath("$.intervaloMedioSegundos").value(nullValue()));
  }

  @Test
  void shouldResponderNaoEncontradoWhenResumoEDeModeloInexistente() throws Exception {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    when(resumoSolicitacoesUseCase.execute(modeloId))
        .thenThrow(new RecursoNaoEncontradoException("Modelo nao encontrado"));

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/{id}/solicitacoes/resumo", modeloId)
                .with(user(UUID.randomUUID().toString())));

    // Assert
    resposta.andExpect(status().isNotFound());
  }

  @Test
  void listarModelos_incluiFotoCapaDaGaleria() throws Exception {
    final Modelo modelo = criarModelo();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(modelo), 0, 20, 1, 1));
    when(fotoGaleriaModeloRepository.findPrincipalUrlsByModeloIds(any()))
        .thenReturn(java.util.Map.of(modelo.getId(), "http://minio/capa.jpg"));

    mockMvc
        .perform(get("/api/modelos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].fotoCapaUrl").value("http://minio/capa.jpg"));
  }

  private static String texto(final int tamanho) {
    return "x".repeat(tamanho);
  }

  private static Stream<Arguments> textosAcimaDoLimite() {
    final String codigo = texto(LimitesTexto.MODELO_CODIGO + 1);
    final String descricao = texto(LimitesTexto.MODELO_DESCRICAO + 1);
    final String observacoes = texto(LimitesTexto.MODELO_OBSERVACOES + 1);
    final String maquina = texto(LimitesTexto.MODELO_MAQUINA + 1);
    return Stream.of(
        Arguments.of(
            new CriarModeloRequest(codigo, "D", null, "FBOX", null),
            new EditarModeloRequest(codigo, "D", null, "FBOX", null),
            "codigo",
            LimitesTexto.MODELO_CODIGO),
        Arguments.of(
            new CriarModeloRequest("C", descricao, null, "FBOX", null),
            new EditarModeloRequest("C", descricao, null, "FBOX", null),
            "descricao",
            LimitesTexto.MODELO_DESCRICAO),
        Arguments.of(
            new CriarModeloRequest("C", "D", observacoes, "FBOX", null),
            new EditarModeloRequest("C", "D", observacoes, "FBOX", null),
            "observacoes",
            LimitesTexto.MODELO_OBSERVACOES),
        Arguments.of(
            new CriarModeloRequest("C", "D", null, maquina, null),
            new EditarModeloRequest("C", "D", null, maquina, null),
            "maquina",
            LimitesTexto.MODELO_MAQUINA));
  }

  @ParameterizedTest
  @MethodSource("textosAcimaDoLimite")
  void shouldAnswerBadRequestWhenCreatingModeloWithTextAboveLimit(
      final CriarModeloRequest criar,
      final EditarModeloRequest editar,
      final String campo,
      final int limite)
      throws Exception {
    // Arrange
    final String mensagem = campo + ": deve ter no máximo " + limite + " caracteres";

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criar)));

    // Assert
    resposta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensagem));
    verifyNoMoreInteractions(gerenciarUseCase);
  }

  @ParameterizedTest
  @MethodSource("textosAcimaDoLimite")
  void shouldAnswerBadRequestWhenEditingModeloWithTextAboveLimit(
      final CriarModeloRequest criar,
      final EditarModeloRequest editar,
      final String campo,
      final int limite)
      throws Exception {
    // Arrange
    final String mensagem = campo + ": deve ter no máximo " + limite + " caracteres";

    // Act
    final var resposta =
        mockMvc.perform(
            put("/api/modelos/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editar)));

    // Assert
    resposta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensagem));
    verifyNoMoreInteractions(gerenciarUseCase);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "101"})
  void shouldAnswerBadRequestWhenListingModelosWithSizeOutOfRange(final String size)
      throws Exception {
    // Arrange
    final var requisicao = get("/api/modelos").param("size", size);

    // Act
    final var resposta = mockMvc.perform(requisicao);

    // Assert
    resposta.andExpect(status().isBadRequest());
    verifyNoInteractions(listarUseCase);
  }

  @Test
  void shouldAnswerBadRequestWhenListingModelosWithNegativePage() throws Exception {
    // Arrange
    final String pageNegativa = "-1";

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos").param("page", pageNegativa));

    // Assert
    resposta.andExpect(status().isBadRequest());
    verifyNoInteractions(listarUseCase);
  }

  @Test
  void shouldRepassSizeToUseCaseWhenListingModelosWithMaximumSize() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 100, 0, 0));
    final ArgumentCaptor<ListarModelosUseCase.Input> captor =
        ArgumentCaptor.forClass(ListarModelosUseCase.Input.class);

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos").param("size", "100"));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1)).execute(captor.capture());
    verifyNoMoreInteractions(listarUseCase);
    assertEquals(100, captor.getValue().size());
  }

  @Test
  void shouldUseDefaultSizeWhenListingModelosWithoutSize() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    final ArgumentCaptor<ListarModelosUseCase.Input> captor =
        ArgumentCaptor.forClass(ListarModelosUseCase.Input.class);

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos"));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1)).execute(captor.capture());
    verifyNoMoreInteractions(listarUseCase);
    assertEquals(20, captor.getValue().size());
  }

  @Test
  void shouldInformStatusAndFieldInBodyWhenListingModelosWithInvalidSize() throws Exception {
    // Arrange
    final String sizeInvalido = "0";

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos").param("size", sizeInvalido));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message", containsString("size")));
    verifyNoInteractions(listarUseCase);
  }

  private Solicitacao criarSolicitacao(final UUID modeloId, final UUID abertaPorUsuarioId) {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.A_FAZER,
        null,
        modeloId,
        null,
        null,
        null,
        abertaPorUsuarioId,
        null,
        agora,
        agora,
        null,
        null);
  }

  private Usuario criarUsuario(final UUID id, final String nome) {
    final Instant agora = Instant.now();
    return new Usuario(id, nome, "u@rgm.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
  }

  @Test
  void shouldAnswerNotFoundWhenListingEventosOfUnknownModelo() throws Exception {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(modeloRepository.findById(id)).thenReturn(Optional.empty());

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos/{id}/eventos", id));

    // Assert
    resposta.andExpect(status().isNotFound());
    verify(modeloRepository, times(1)).findById(id);
    verifyNoMoreInteractions(modeloRepository);
    verifyNoInteractions(eventoModeloRepository);
  }

  @Test
  void shouldAnswerNotFoundWhenGeneratingFichaOfUnknownModelo() throws Exception {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(obterFichaUseCase.execute(id))
        .thenThrow(
            new com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException(
                "Modelo nao encontrado"));

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/{id}/relatorio", id).with(user(UUID.randomUUID().toString())));

    // Assert
    resposta.andExpect(status().isNotFound());
    verify(obterFichaUseCase, times(1)).execute(id);
    verifyNoInteractions(modeloPdfService);
  }

  @Test
  void shouldAnswerBadRequestWhenCreatingModeloWithUnknownTipo() throws Exception {
    // Arrange
    final var request = new CriarModeloRequest("MOD-001", "Desc", "Obs", "FBOX", "INEXISTENTE");
    when(gerenciarUseCase.criar(any()))
        .thenThrow(
            new com.rgm.api.core.domain.exceptions.ValidationException(
                "Tipo de modelo invalido ou inativo: INEXISTENTE"));

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Tipo de modelo invalido ou inativo: INEXISTENTE"));
  }

  @Test
  void shouldAnswerBadRequestWhenEditingModeloWithUnknownTipo() throws Exception {
    // Arrange
    final var request = new EditarModeloRequest("MOD-001", "Desc", "Obs", "FBOX", "INEXISTENTE");
    when(gerenciarUseCase.editar(any()))
        .thenThrow(
            new com.rgm.api.core.domain.exceptions.ValidationException(
                "Tipo de modelo invalido ou inativo: INEXISTENTE"));

    // Act
    final var resposta =
        mockMvc.perform(
            put("/api/modelos/{id}", UUID.randomUUID())
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Assert
    resposta
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Tipo de modelo invalido ou inativo: INEXISTENTE"));
  }

  @Test
  void shouldRepassTipoToUseCaseWhenCreatingModeloWithValidTipo() throws Exception {
    // Arrange
    when(gerenciarUseCase.criar(any())).thenReturn(criarModelo());
    final ArgumentCaptor<GerenciarModelosUseCase.CriarInput> captor =
        ArgumentCaptor.forClass(GerenciarModelosUseCase.CriarInput.class);
    final var request = new CriarModeloRequest("MOD-001", "Desc", "Obs", "FBOX", "RESINA");

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(gerenciarUseCase, times(1)).criar(captor.capture());
    verifyNoMoreInteractions(gerenciarUseCase);
    assertEquals("RESINA", captor.getValue().tipo());
  }

  @Test
  void shouldRepassNullTipoToUseCaseWhenCreatingModeloWithBlankTipo() throws Exception {
    // Arrange
    when(gerenciarUseCase.criar(any())).thenReturn(criarModelo());
    final ArgumentCaptor<GerenciarModelosUseCase.CriarInput> captor =
        ArgumentCaptor.forClass(GerenciarModelosUseCase.CriarInput.class);
    final var request = new CriarModeloRequest("MOD-001", "Desc", "Obs", "FBOX", "  ");

    // Act
    final var resposta =
        mockMvc.perform(
            post("/api/modelos")
                .with(user(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

    // Assert
    resposta.andExpect(status().isCreated());
    verify(gerenciarUseCase, times(1)).criar(captor.capture());
    verifyNoMoreInteractions(gerenciarUseCase);
    assertNull(captor.getValue().tipo());
  }

  @Test
  void shouldUseSistemaAsGeradoPorWhenGeneratingListaWithoutAuthentication() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 5000, 0, 0));
    when(modeloPdfService.gerarLista(any(), eq("Sistema"), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos/relatorio"));

    // Assert
    resposta.andExpect(status().isOk()).andExpect(content().bytes(new byte[] {1}));
    verify(listarUseCase, times(1))
        .execute(new ListarModelosUseCase.Input(null, null, null, null, 0, 5000));
    verify(modeloPdfService, times(1)).gerarLista(eq(List.of()), eq("Sistema"), any());
    verifyNoMoreInteractions(listarUseCase, modeloPdfService);
    verifyNoInteractions(usuarioRepository);
  }

  @Test
  void shouldUseSistemaAsGeradoPorWhenAuthenticatedUserIsNotFound() throws Exception {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 5000, 0, 0));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());
    when(modeloPdfService.gerarLista(any(), eq("Sistema"), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(get("/api/modelos/relatorio").with(user(usuarioId.toString())));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1))
        .execute(new ListarModelosUseCase.Input(null, null, null, null, 0, 5000));
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verify(modeloPdfService, times(1)).gerarLista(eq(List.of()), eq("Sistema"), any());
    verifyNoMoreInteractions(listarUseCase, usuarioRepository, modeloPdfService);
  }

  @Test
  void shouldUseSistemaAsGeradoPorWhenAuthenticatedNameIsNotAUuid() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 5000, 0, 0));
    when(modeloPdfService.gerarLista(any(), eq("Sistema"), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta = mockMvc.perform(get("/api/modelos/relatorio").with(user("nao-e-uuid")));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1))
        .execute(new ListarModelosUseCase.Input(null, null, null, null, 0, 5000));
    verify(modeloPdfService, times(1)).gerarLista(eq(List.of()), eq("Sistema"), any());
    verifyNoMoreInteractions(listarUseCase, modeloPdfService);
    verifyNoInteractions(usuarioRepository);
  }

  @Test
  void shouldUseUsuarioNomeAsGeradoPorWhenAuthenticatedUserIsFound() throws Exception {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 5000, 0, 0));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, "Maria")));
    when(modeloPdfService.gerarLista(any(), eq("Maria"), any())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(get("/api/modelos/relatorio").with(user(usuarioId.toString())));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1))
        .execute(new ListarModelosUseCase.Input(null, null, null, null, 0, 5000));
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verify(modeloPdfService, times(1)).gerarLista(eq(List.of()), eq("Maria"), any());
    verifyNoMoreInteractions(listarUseCase, usuarioRepository, modeloPdfService);
  }

  @Test
  void shouldPassTheAppliedFiltersAndTheTotalToThePdfWhenExportingTheList() throws Exception {
    // Arrange
    when(listarUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 5000, 7, 1));
    final var captor = ArgumentCaptor.forClass(ContextoDoRelatorio.class);
    when(modeloPdfService.gerarLista(any(), any(), captor.capture())).thenReturn(new byte[] {1});

    // Act
    final var resposta =
        mockMvc.perform(
            get("/api/modelos/relatorio")
                .param("ativo", "true")
                .param("maquina", "FBOX")
                .param("codigo", "MOD")
                .with(user("nao-e-uuid")));

    // Assert
    resposta.andExpect(status().isOk());
    verify(listarUseCase, times(1))
        .execute(new ListarModelosUseCase.Input(true, "MOD", "FBOX", null, 0, 5000));
    assertEquals(
        List.of("Situação: ativos", "Código: MOD", "Máquina: FBOX"), captor.getValue().filtros());
    assertEquals(7, captor.getValue().total());
    assertEquals(0, captor.getValue().exibidas());
  }
}
