package com.rgm.api.adapter.in.web.modelo;

import com.rgm.api.adapter.in.web.ParametrosDeConsulta;
import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarModeloRequest;
import com.rgm.api.adapter.in.web.dto.response.EventoModeloResponse;
import com.rgm.api.adapter.in.web.dto.response.ModeloResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.ResumoModelosResponse;
import com.rgm.api.adapter.in.web.dto.response.ResumoSolicitacoesModeloResponse;
import com.rgm.api.adapter.out.report.ContextoDoRelatorio;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterFichaDoModeloUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ObterResumoSolicitacoesModeloUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modelos")
public class ModeloController {
  private static final Logger log = LoggerFactory.getLogger(ModeloController.class);

  private final GerenciarModelosUseCase gerenciarUseCase;
  private final ListarModelosUseCase listarUseCase;
  private final ModeloRepository modeloRepository;
  private final EventoModeloRepository eventoModeloRepository;
  private final ObterFichaDoModeloUseCase obterFichaUseCase;
  private final ModeloPdfService modeloPdfService;
  private final UsuarioRepository usuarioRepository;
  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private final ObterResumoModelosUseCase resumoModelosUseCase;
  private final ObterResumoSolicitacoesModeloUseCase resumoSolicitacoesUseCase;

  public ModeloController(
      final GerenciarModelosUseCase gerenciarUseCase,
      final ListarModelosUseCase listarUseCase,
      final ModeloRepository modeloRepository,
      final EventoModeloRepository eventoModeloRepository,
      final ObterFichaDoModeloUseCase obterFichaUseCase,
      final ModeloPdfService modeloPdfService,
      final UsuarioRepository usuarioRepository,
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository,
      final ObterResumoModelosUseCase resumoModelosUseCase,
      final ObterResumoSolicitacoesModeloUseCase resumoSolicitacoesUseCase) {
    this.gerenciarUseCase = gerenciarUseCase;
    this.listarUseCase = listarUseCase;
    this.modeloRepository = modeloRepository;
    this.eventoModeloRepository = eventoModeloRepository;
    this.obterFichaUseCase = obterFichaUseCase;
    this.modeloPdfService = modeloPdfService;
    this.usuarioRepository = usuarioRepository;
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
    this.resumoModelosUseCase = resumoModelosUseCase;
    this.resumoSolicitacoesUseCase = resumoSolicitacoesUseCase;
  }

  @GetMapping
  public ResponseEntity<PageResponse<ModeloResponse>> listar(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      @RequestParam(required = false) final Boolean ativo,
      @RequestParam(required = false) final String codigo,
      @RequestParam(required = false) final String maquina,
      @RequestParam(required = false) final String descricao) {
    ParametrosDeConsulta.pagina(page, size);
    final var result =
        listarUseCase.execute(
            new ListarModelosUseCase.Input(ativo, codigo, maquina, descricao, page, size));
    final Map<UUID, String> fotosCapa =
        fotoGaleriaModeloRepository.findPrincipalUrlsByModeloIds(
            result.content().stream().map(Modelo::getId).toList());
    return ResponseEntity.ok(
        PageResponse.from(result, m -> ModeloResponse.from(m, fotosCapa.get(m.getId()))));
  }

  @GetMapping("/resumo")
  public ResponseEntity<ResumoModelosResponse> resumir() {
    return ResponseEntity.ok(ResumoModelosResponse.from(resumoModelosUseCase.execute()));
  }

  @GetMapping("/{id}/solicitacoes/resumo")
  public ResponseEntity<ResumoSolicitacoesModeloResponse> resumirSolicitacoes(
      @PathVariable final UUID id) {
    return ResponseEntity.ok(
        ResumoSolicitacoesModeloResponse.from(resumoSolicitacoesUseCase.execute(id)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ModeloResponse> buscarPorId(@PathVariable final UUID id) {
    log.info("ModeloController.buscarPorId id={}", id);
    final var modelo =
        modeloRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));
    final Map<UUID, String> fotosCapa =
        fotoGaleriaModeloRepository.findPrincipalUrlsByModeloIds(List.of(id));
    return ResponseEntity.ok(ModeloResponse.from(modelo, fotosCapa.get(id)));
  }

  @GetMapping("/{id}/eventos")
  public ResponseEntity<List<EventoModeloResponse>> listarEventos(@PathVariable final UUID id) {
    log.info("ModeloController.listarEventos id={}", id);
    modeloRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));
    final var eventos =
        eventoModeloRepository.findByModeloId(id).stream().map(EventoModeloResponse::from).toList();
    return ResponseEntity.ok(eventos);
  }

  @Transactional
  @PostMapping
  public ResponseEntity<ModeloResponse> criar(
      @Valid @RequestBody final CriarModeloRequest request, final Authentication authentication) {
    log.info("ModeloController.criar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                request.codigo(),
                request.descricao(),
                request.observacoes(),
                request.maquina(),
                parseTipo(request.tipo()),
                gestorId));
    return ResponseEntity.status(HttpStatus.CREATED).body(ModeloResponse.from(modelo));
  }

  @Transactional
  @PutMapping("/{id}")
  public ResponseEntity<ModeloResponse> editar(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarModeloRequest request,
      final Authentication authentication) {
    log.info("ModeloController.editar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.editar(
            new GerenciarModelosUseCase.EditarInput(
                id,
                request.codigo(),
                request.descricao(),
                request.observacoes(),
                request.maquina(),
                parseTipo(request.tipo()),
                gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }

  @Transactional
  @PatchMapping("/{id}/desativar")
  public ResponseEntity<ModeloResponse> desativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("ModeloController.desativar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.desativar(new GerenciarModelosUseCase.DesativarInput(id, gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }

  @Transactional
  @PatchMapping("/{id}/ativar")
  public ResponseEntity<ModeloResponse> ativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("ModeloController.ativar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.reativar(new GerenciarModelosUseCase.ReativarInput(id, gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }

  @GetMapping("/relatorio")
  public ResponseEntity<byte[]> gerarRelatorioLista(
      @RequestParam(required = false) final Boolean ativo,
      @RequestParam(required = false) final String codigo,
      @RequestParam(required = false) final String maquina,
      @RequestParam(required = false) final String descricao,
      final Authentication authentication) {
    final var pagina =
        listarUseCase.execute(
            new ListarModelosUseCase.Input(
                ativo,
                codigo,
                maquina,
                descricao,
                0,
                ParametrosDeConsulta.LIMITE_DE_LINHAS_DO_RELATORIO));
    final var contexto =
        new ContextoDoRelatorio(
            new ContextoDoRelatorio.Filtros()
                .com("Situação", ativo == null ? null : ativo ? "ativos" : "inativos")
                .com("Código", codigo)
                .com("Máquina", maquina)
                .com("Descrição", descricao)
                .build(),
            pagina.totalElements(),
            pagina.content().size());
    final String geradoPorNome = resolveNome(authentication);
    final byte[] pdf = modeloPdfService.gerarLista(pagina.content(), geradoPorNome, contexto);
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "relatorio-modelos.pdf");
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  @GetMapping("/{id}/relatorio")
  public ResponseEntity<byte[]> gerarFichaModelo(
      @PathVariable final UUID id, final Authentication authentication) {
    final var ficha = obterFichaUseCase.execute(id);
    final byte[] pdf = modeloPdfService.gerarFicha(ficha, resolveNome(authentication));
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    final String filename =
        "ficha-modelo-" + ficha.modelo().getCodigo().replaceAll("[^a-zA-Z0-9]", "-") + ".pdf";
    headers.setContentDispositionFormData("attachment", filename);
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  private String parseTipo(final String tipo) {
    return tipo == null || tipo.isBlank() ? null : tipo.trim();
  }

  private String resolveNome(final Authentication authentication) {
    if (authentication == null) return "Sistema";
    try {
      return usuarioRepository
          .findById(UUID.fromString(authentication.getName()))
          .map(u -> u.getNome())
          .orElse("Sistema");
    } catch (final IllegalArgumentException e) {
      return "Sistema";
    }
  }
}
