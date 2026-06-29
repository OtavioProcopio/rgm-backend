package com.rgm.api.adapter.in.web.modelo;

import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.FotoCapaUploadRequest;
import com.rgm.api.adapter.in.web.dto.response.EventoModeloResponse;
import com.rgm.api.adapter.in.web.dto.response.ModeloResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.out.report.ModeloPdfService;
import com.rgm.api.core.application.usecases.modelo.AtualizarFotoCapaUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/modelos")
public class ModeloController {
  private static final Logger log = LoggerFactory.getLogger(ModeloController.class);

  private final GerenciarModelosUseCase gerenciarUseCase;
  private final AtualizarFotoCapaUseCase fotoCapaUseCase;
  private final ListarModelosUseCase listarUseCase;
  private final ModeloRepository modeloRepository;
  private final EventoModeloRepository eventoModeloRepository;
  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final ModeloPdfService modeloPdfService;
  private final UsuarioRepository usuarioRepository;

  public ModeloController(
      final GerenciarModelosUseCase gerenciarUseCase,
      final AtualizarFotoCapaUseCase fotoCapaUseCase,
      final ListarModelosUseCase listarUseCase,
      final ModeloRepository modeloRepository,
      final EventoModeloRepository eventoModeloRepository,
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final ModeloPdfService modeloPdfService,
      final UsuarioRepository usuarioRepository) {
    this.gerenciarUseCase = gerenciarUseCase;
    this.fotoCapaUseCase = fotoCapaUseCase;
    this.listarUseCase = listarUseCase;
    this.modeloRepository = modeloRepository;
    this.eventoModeloRepository = eventoModeloRepository;
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.modeloPdfService = modeloPdfService;
    this.usuarioRepository = usuarioRepository;
  }

  @GetMapping
  public ResponseEntity<PageResponse<ModeloResponse>> listar(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      @RequestParam(required = false) final Boolean ativo,
      @RequestParam(required = false) final String codigo,
      @RequestParam(required = false) final String maquina,
      @RequestParam(required = false) final String descricao) {
    final var result =
        listarUseCase.execute(
            new ListarModelosUseCase.Input(ativo, codigo, maquina, descricao, page, size));
    return ResponseEntity.ok(PageResponse.from(result, ModeloResponse::from));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ModeloResponse> buscarPorId(@PathVariable final UUID id) {
    log.info("ModeloController.buscarPorId id={}", id);
    final var modelo =
        modeloRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
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

  @PostMapping(value = "/{id}/foto-capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ModeloResponse> uploadFotoCapa(
      @PathVariable final UUID id,
      @RequestParam("file") final MultipartFile file,
      final Authentication authentication) {
    try {
      final UUID gestorId = UUID.fromString(authentication.getName());
      final var input =
          new AtualizarFotoCapaUseCase.UploadInput(
              id,
              file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
              file.getContentType() != null ? file.getContentType() : "application/octet-stream",
              file.getSize(),
              file.getInputStream(),
              gestorId);

      final String publicUrl = fotoCapaUseCase.uploadFile(input);
      final Modelo modelo = fotoCapaUseCase.persistUpload(input, publicUrl);
      return ResponseEntity.ok(ModeloResponse.from(modelo));
    } catch (final java.io.IOException e) {
      throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
    }
  }

  @GetMapping("/relatorio")
  public ResponseEntity<byte[]> gerarRelatorioLista(
      @RequestParam(required = false) final Boolean ativo,
      @RequestParam(required = false) final String codigo,
      @RequestParam(required = false) final String maquina,
      @RequestParam(required = false) final String descricao,
      final Authentication authentication) {
    final var result =
        listarUseCase.execute(
            new ListarModelosUseCase.Input(ativo, codigo, maquina, descricao, 0, 1000));
    final String geradoPorNome = resolveNome(authentication);
    final byte[] pdf = modeloPdfService.gerarLista(result.content(), geradoPorNome);
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "relatorio-modelos.pdf");
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  @GetMapping("/{id}/relatorio")
  public ResponseEntity<byte[]> gerarFichaModelo(
      @PathVariable final UUID id, final Authentication authentication) {
    final var modelo =
        modeloRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));
    final var eventos = eventoModeloRepository.findByModeloId(id);
    final var solicitacoes = solicitacaoRepository.findByModeloId(id);
    final var atividadesPorSolicitacao =
        solicitacoes.stream()
            .collect(
                Collectors.toMap(
                    s -> s.getId(), s -> atividadeRepository.findBySolicitacaoId(s.getId())));

    final var userIds =
        solicitacoes.stream()
            .flatMap(
                s -> {
                  final var ids = new java.util.ArrayList<UUID>();
                  if (s.getAbertaPorUsuarioId() != null) ids.add(s.getAbertaPorUsuarioId());
                  return ids.stream();
                })
            .distinct()
            .collect(Collectors.toList());
    final Map<UUID, String> nomesPorUsuario =
        usuarioRepository.findAllByIdIn(userIds).stream()
            .collect(Collectors.toMap(u -> u.getId(), u -> u.getNome()));

    final String geradoPorNome = resolveNome(authentication);
    final byte[] pdf =
        modeloPdfService.gerarFicha(
            modelo,
            eventos,
            solicitacoes,
            atividadesPorSolicitacao,
            geradoPorNome,
            nomesPorUsuario);
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    final String filename =
        "ficha-modelo-" + modelo.getCodigo().replaceAll("[^a-zA-Z0-9]", "-") + ".pdf";
    headers.setContentDispositionFormData("attachment", filename);
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
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

  @Transactional
  @PatchMapping("/{id}/foto-capa")
  public ResponseEntity<ModeloResponse> usarEvidenciaComoFotoCapa(
      @PathVariable final UUID id,
      @RequestBody final FotoCapaUploadRequest request,
      final Authentication authentication) {
    log.info("ModeloController.usarEvidenciaComoFotoCapa iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        fotoCapaUseCase.executeEvidenciaExistente(
            new AtualizarFotoCapaUseCase.EvidenciaExistenteInput(
                id, request.evidenciaId(), gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }
}
