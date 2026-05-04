package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.adapter.in.web.dto.request.AbrirSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.ComentarioRequest;
import com.rgm.api.adapter.in.web.dto.request.DevolverSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.EncerrarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.request.TriarSolicitacaoRequest;
import com.rgm.api.adapter.in.web.dto.response.AtividadeResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.SolicitacaoResponse;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
  private final RegistrarComentarioUseCase comentarioUseCase;
  private final ListarSolicitacoesUseCase listarUseCase;
  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public SolicitacaoController(
      final AbrirSolicitacaoUseCase abrirUseCase,
      final TriarSolicitacaoUseCase triarUseCase,
      final EnviarParaValidacaoUseCase enviarUseCase,
      final DevolverSolicitacaoUseCase devolverUseCase,
      final EncerrarSolicitacaoUseCase encerrarUseCase,
      final RegistrarComentarioUseCase comentarioUseCase,
      final ListarSolicitacoesUseCase listarUseCase,
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.abrirUseCase = abrirUseCase;
    this.triarUseCase = triarUseCase;
    this.enviarUseCase = enviarUseCase;
    this.devolverUseCase = devolverUseCase;
    this.encerrarUseCase = encerrarUseCase;
    this.comentarioUseCase = comentarioUseCase;
    this.listarUseCase = listarUseCase;
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
  }

  @GetMapping
  public ResponseEntity<PageResponse<SolicitacaoResponse>> listar(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      @RequestParam(required = false) final String status) {
    final StatusSolicitacao statusFilter =
        status != null ? StatusSolicitacao.valueOf(status) : null;
    final var result =
        listarUseCase.execute(new ListarSolicitacoesUseCase.Input(statusFilter, page, size));
    return ResponseEntity.ok(PageResponse.from(result, SolicitacaoResponse::from));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SolicitacaoResponse> buscarPorId(@PathVariable final UUID id) {
    log.info("SolicitacaoController.buscarPorId id={}", id);
    final var solicitacao =
        solicitacaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    return ResponseEntity.ok(SolicitacaoResponse.from(solicitacao));
  }

  @GetMapping("/{id}/atividades")
  public ResponseEntity<List<AtividadeResponse>> listarAtividades(@PathVariable final UUID id) {
    log.info("SolicitacaoController.listarAtividades id={}", id);
    solicitacaoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    final var atividades =
        atividadeRepository.findBySolicitacaoId(id).stream()
            .map(AtividadeResponse::from)
            .toList();
    return ResponseEntity.ok(atividades);
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
}
