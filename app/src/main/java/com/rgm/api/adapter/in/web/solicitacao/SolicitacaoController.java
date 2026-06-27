package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.adapter.in.web.dto.request.AbrirSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.CancelarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.ComentarioRequest;
import com.rgm.api.adapter.in.web.dto.request.DevolverSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.EncerrarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.TriarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.response.AtividadeResponse;
import com.rgm.api.adapter.in.web.dto.response.MetricasSolicitacaoResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.SolicitacaoResponse;
import com.rgm.api.adapter.out.report.SolicitacaoPdfService;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.CancelarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EditarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {
  private static final Logger log = LoggerFactory.getLogger(SolicitacaoController.class);

  private final AbrirSolicitacaoUseCase abrirUseCase;
  private final TriarSolicitacaoUseCase triarUseCase;
  private final EnviarParaValidacaoUseCase enviarUseCase;
  private final DevolverSolicitacaoUseCase devolverUseCase;
  private final EncerrarSolicitacaoUseCase encerrarUseCase;
  private final CancelarSolicitacaoUseCase cancelarUseCase;
  private final RegistrarComentarioUseCase comentarioUseCase;
  private final EditarSolicitacaoUseCase editarUseCase;
  private final ListarSolicitacoesUseCase listarUseCase;
  private final ObterMetricasSolicitacoesUseCase obterMetricasUseCase;
  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoPdfService pdfService;

  public SolicitacaoController(
      final AbrirSolicitacaoUseCase abrirUseCase,
      final TriarSolicitacaoUseCase triarUseCase,
      final EnviarParaValidacaoUseCase enviarUseCase,
      final DevolverSolicitacaoUseCase devolverUseCase,
      final EncerrarSolicitacaoUseCase encerrarUseCase,
      final CancelarSolicitacaoUseCase cancelarUseCase,
      final RegistrarComentarioUseCase comentarioUseCase,
      final EditarSolicitacaoUseCase editarUseCase,
      final ListarSolicitacoesUseCase listarUseCase,
      final ObterMetricasSolicitacoesUseCase obterMetricasUseCase,
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoPdfService pdfService) {
    this.abrirUseCase = abrirUseCase;
    this.triarUseCase = triarUseCase;
    this.enviarUseCase = enviarUseCase;
    this.devolverUseCase = devolverUseCase;
    this.encerrarUseCase = encerrarUseCase;
    this.cancelarUseCase = cancelarUseCase;
    this.comentarioUseCase = comentarioUseCase;
    this.editarUseCase = editarUseCase;
    this.listarUseCase = listarUseCase;
    this.obterMetricasUseCase = obterMetricasUseCase;
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.pdfService = pdfService;
  }

  @GetMapping("/metricas")
  public ResponseEntity<MetricasSolicitacaoResponse> obterMetricas() {
    log.info("SolicitacaoController.obterMetricas iniciado");
    final var output = obterMetricasUseCase.execute();
    return ResponseEntity.ok(MetricasSolicitacaoResponse.from(output));
  }

  @GetMapping("/relatorio")
  public ResponseEntity<byte[]> gerarRelatorio(
      @RequestParam(required = false) final String status,
      @RequestParam(required = false) final UUID modeloId,
      @RequestParam(required = false) final String tipo,
      @RequestParam(required = false) final String prioridade,
      @RequestParam(required = false) final String criadaEmInicio,
      @RequestParam(required = false) final String criadaEmFim,
      @RequestParam(required = false) final UUID abertaPorUsuarioId,
      @RequestParam(required = false) final UUID responsavelId) {
    log.info("SolicitacaoController.gerarRelatorio iniciado");
    final var input =
        buildInput(
            status,
            modeloId,
            tipo,
            prioridade,
            criadaEmInicio,
            criadaEmFim,
            abertaPorUsuarioId,
            responsavelId,
            0,
            Integer.MAX_VALUE);
    final var solicitacoes = listarUseCase.execute(input).content();
    final byte[] pdf = pdfService.gerar(solicitacoes);
    final var headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "relatorio-solicitacoes.pdf");
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<PageResponse<SolicitacaoResponse>> listar(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      @RequestParam(required = false) final String status,
      @RequestParam(required = false) final UUID modeloId,
      @RequestParam(required = false) final String tipo,
      @RequestParam(required = false) final String prioridade,
      @RequestParam(required = false) final String criadaEmInicio,
      @RequestParam(required = false) final String criadaEmFim,
      @RequestParam(required = false) final UUID abertaPorUsuarioId,
      @RequestParam(required = false) final UUID responsavelId) {
    final var result =
        listarUseCase.execute(
            buildInput(
                status,
                modeloId,
                tipo,
                prioridade,
                criadaEmInicio,
                criadaEmFim,
                abertaPorUsuarioId,
                responsavelId,
                page,
                size));
    return ResponseEntity.ok(PageResponse.from(result, SolicitacaoResponse::from));
  }

  private ListarSolicitacoesUseCase.Input buildInput(
      final String status,
      final UUID modeloId,
      final String tipo,
      final String prioridade,
      final String criadaEmInicio,
      final String criadaEmFim,
      final UUID abertaPorUsuarioId,
      final UUID responsavelId,
      final int page,
      final int size) {
    return new ListarSolicitacoesUseCase.Input(
        status != null ? StatusSolicitacao.valueOf(status) : null,
        modeloId,
        tipo != null ? TipoSolicitacao.valueOf(tipo) : null,
        prioridade != null ? PrioridadeSolicitacao.valueOf(prioridade) : null,
        criadaEmInicio != null ? Instant.parse(criadaEmInicio) : null,
        criadaEmFim != null ? Instant.parse(criadaEmFim) : null,
        abertaPorUsuarioId,
        responsavelId,
        page,
        size);
  }

  @Transactional
  @PutMapping("/{id}")
  public ResponseEntity<SolicitacaoResponse> editar(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.editar iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var salva =
        editarUseCase.execute(
            new EditarSolicitacaoUseCase.Input(
                id, request.titulo(), request.descricao(), usuarioId));
    return ResponseEntity.ok(SolicitacaoResponse.from(salva));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SolicitacaoResponse> buscarPorId(@PathVariable final UUID id) {
    log.info("SolicitacaoController.buscarPorId id={}", id);
    final var solicitacao =
        solicitacaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    final List<UUID> responsaveis =
        atribuicaoRepository.findBySolicitacaoId(id).stream()
            .filter(a -> a.getRemovidoEm() == null)
            .map(a -> a.getUsuarioId())
            .toList();
    return ResponseEntity.ok(SolicitacaoResponse.from(solicitacao, responsaveis));
  }

  @GetMapping("/{id}/atividades")
  public ResponseEntity<List<AtividadeResponse>> listarAtividades(@PathVariable final UUID id) {
    log.info("SolicitacaoController.listarAtividades id={}", id);
    solicitacaoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    final var atividades = atividadeRepository.findBySolicitacaoId(id);
    final List<UUID> autorIds =
        atividades.stream().map(a -> a.getAutorUsuarioId()).distinct().toList();
    final Map<UUID, String> nomesPorId =
        usuarioRepository.findAllByIdIn(autorIds).stream()
            .collect(Collectors.toMap(u -> u.getId(), u -> u.getNome()));
    final var response =
        atividades.stream()
            .map(
                a ->
                    AtividadeResponse.from(
                        a, nomesPorId.getOrDefault(a.getAutorUsuarioId(), "Usuário")))
            .toList();
    return ResponseEntity.ok(response);
  }

  @Transactional
  @PostMapping
  public ResponseEntity<SolicitacaoResponse> abrir(
      @Valid @RequestBody final AbrirSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.abrir iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var output =
        abrirUseCase.execute(
            new AbrirSolicitacaoUseCase.Input(
                request.titulo(),
                request.descricao(),
                TipoSolicitacao.valueOf(request.tipo()),
                request.modeloId(),
                usuarioId));
    return ResponseEntity.status(HttpStatus.CREATED).body(SolicitacaoResponse.from(output));
  }

  @Transactional
  @PatchMapping("/{id}/triar")
  public ResponseEntity<SolicitacaoResponse> triar(
      @PathVariable final UUID id,
      @Valid @RequestBody final TriarSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.triar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final var output =
        triarUseCase.execute(
            new TriarSolicitacaoUseCase.Input(
                id,
                PrioridadeSolicitacao.valueOf(request.prioridade()),
                request.responsavelIds(),
                gestorId));
    return ResponseEntity.ok(SolicitacaoResponse.from(output));
  }

  @Transactional
  @PatchMapping("/{id}/enviar-validacao")
  public ResponseEntity<SolicitacaoResponse> enviarParaValidacao(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("SolicitacaoController.enviarParaValidacao iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var output = enviarUseCase.execute(new EnviarParaValidacaoUseCase.Input(id, usuarioId));
    return ResponseEntity.ok(SolicitacaoResponse.from(output));
  }

  @Transactional
  @PatchMapping("/{id}/devolver")
  public ResponseEntity<SolicitacaoResponse> devolver(
      @PathVariable final UUID id,
      @Valid @RequestBody final DevolverSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.devolver iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final PrioridadeSolicitacao prioridade =
        request.prioridade() != null ? PrioridadeSolicitacao.valueOf(request.prioridade()) : null;
    final var output =
        devolverUseCase.execute(
            new DevolverSolicitacaoUseCase.Input(id, request.motivo(), prioridade, gestorId));
    return ResponseEntity.ok(SolicitacaoResponse.from(output));
  }

  @Transactional
  @PatchMapping("/{id}/encerrar")
  public ResponseEntity<SolicitacaoResponse> encerrar(
      @PathVariable final UUID id,
      @Valid @RequestBody final EncerrarSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.encerrar iniciado");
    final UUID gestorId = UUID.fromString(authentication.getName());
    final var output =
        encerrarUseCase.execute(
            new EncerrarSolicitacaoUseCase.Input(
                id, request.concluir(), request.comentario(), gestorId));
    return ResponseEntity.ok(SolicitacaoResponse.from(output));
  }

  @Transactional
  @PostMapping("/{id}/comentarios")
  public ResponseEntity<Void> comentar(
      @PathVariable final UUID id,
      @Valid @RequestBody final ComentarioRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.comentar iniciado");
    final UUID autorId = UUID.fromString(authentication.getName());
    comentarioUseCase.execute(
        new RegistrarComentarioUseCase.Input(id, request.comentario(), autorId));
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Transactional
  @PatchMapping("/{id}/cancelar")
  public ResponseEntity<SolicitacaoResponse> cancelar(
      @PathVariable final UUID id,
      @Valid @RequestBody final CancelarSolicitacaoRequest request,
      final Authentication authentication) {
    log.info("SolicitacaoController.cancelar iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var output =
        cancelarUseCase.execute(
            new CancelarSolicitacaoUseCase.Input(id, request.motivo(), usuarioId));
    return ResponseEntity.ok(SolicitacaoResponse.from(output));
  }
}
