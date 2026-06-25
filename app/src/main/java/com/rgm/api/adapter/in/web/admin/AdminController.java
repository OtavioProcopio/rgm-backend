package com.rgm.api.adapter.in.web.admin;

import com.rgm.api.adapter.in.web.dto.request.AlterarPerfilRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.ExcluirRegistroRequest;
import com.rgm.api.adapter.in.web.dto.request.RedefinirSenhaRequest;
import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.UsuarioResponse;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin")
public class AdminController {
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  private final GerenciarUsuariosUseCase gerenciarUsuariosUseCase;
  private final CadastrarPrestadorExternoUseCase cadastrarExternoUseCase;
  private final GerenciarMaquinasUseCase gerenciarMaquinasUseCase;
  private final ExcluirRegistroUseCase excluirRegistroUseCase;
  private final ListarUsuariosUseCase listarUsuariosUseCase;
  private final ListarMaquinasUseCase listarMaquinasUseCase;
  private final UsuarioRepository usuarioRepository;
  private final MaquinaRepository maquinaRepository;

  public AdminController(
      final GerenciarUsuariosUseCase gerenciarUsuariosUseCase,
      final CadastrarPrestadorExternoUseCase cadastrarExternoUseCase,
      final GerenciarMaquinasUseCase gerenciarMaquinasUseCase,
      final ExcluirRegistroUseCase excluirRegistroUseCase,
      final ListarUsuariosUseCase listarUsuariosUseCase,
      final ListarMaquinasUseCase listarMaquinasUseCase,
      final UsuarioRepository usuarioRepository,
      final MaquinaRepository maquinaRepository) {
    this.gerenciarUsuariosUseCase = gerenciarUsuariosUseCase;
    this.cadastrarExternoUseCase = cadastrarExternoUseCase;
    this.gerenciarMaquinasUseCase = gerenciarMaquinasUseCase;
    this.excluirRegistroUseCase = excluirRegistroUseCase;
    this.listarUsuariosUseCase = listarUsuariosUseCase;
    this.listarMaquinasUseCase = listarMaquinasUseCase;
    this.usuarioRepository = usuarioRepository;
    this.maquinaRepository = maquinaRepository;
  }

  @GetMapping("/usuarios")
  public ResponseEntity<PageResponse<UsuarioResponse>> listarUsuarios(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      @RequestParam(required = false) final String perfil,
      @RequestParam(required = false) final Boolean ativo) {
    final PerfilUsuario perfilFilter = perfil != null ? PerfilUsuario.valueOf(perfil) : null;
    final var result =
        listarUsuariosUseCase.execute(
            new ListarUsuariosUseCase.Input(perfilFilter, ativo, page, size));
    return ResponseEntity.ok(PageResponse.from(result, UsuarioResponse::from));
  }

  @GetMapping("/usuarios/{id}")
  public ResponseEntity<UsuarioResponse> buscarUsuarioPorId(@PathVariable final UUID id) {
    log.info("AdminController.buscarUsuarioPorId id={}", id);
    final var usuario =
        usuarioRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PostMapping("/usuarios")
  public ResponseEntity<UsuarioResponse> criarUsuario(
      @Valid @RequestBody final CriarUsuarioRequest request, final Authentication authentication) {
    log.info("AdminController.criarUsuario iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final PerfilUsuario perfil = PerfilUsuario.valueOf(request.perfil());

    final Usuario usuario;
    if (perfil == PerfilUsuario.EXTERNO) {
      usuario =
          cadastrarExternoUseCase.execute(
              new CadastrarPrestadorExternoUseCase.Input(request.nome(), adminId));
    } else {
      usuario =
          gerenciarUsuariosUseCase.criar(
              new GerenciarUsuariosUseCase.CriarInput(
                  request.nome(), request.email(), request.senha(), perfil, adminId));
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PutMapping("/usuarios/{id}")
  public ResponseEntity<UsuarioResponse> editarUsuario(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarUsuarioRequest request,
      final Authentication authentication) {
    log.info("AdminController.editarUsuario iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Usuario usuario =
        gerenciarUsuariosUseCase.editar(
            new GerenciarUsuariosUseCase.EditarInput(id, request.nome(), request.email(), adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PatchMapping("/usuarios/{id}/desativar")
  public ResponseEntity<UsuarioResponse> desativarUsuario(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("AdminController.desativarUsuario iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Usuario usuario =
        gerenciarUsuariosUseCase.desativar(
            new GerenciarUsuariosUseCase.DesativarInput(id, adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PatchMapping("/usuarios/{id}/ativar")
  public ResponseEntity<UsuarioResponse> ativarUsuario(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("AdminController.ativarUsuario iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Usuario usuario =
        gerenciarUsuariosUseCase.ativar(new GerenciarUsuariosUseCase.AtivarInput(id, adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @GetMapping("/maquinas")
  public ResponseEntity<PageResponse<MaquinaResponse>> listarMaquinas(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size) {
    final var result = listarMaquinasUseCase.execute(page, size);
    return ResponseEntity.ok(PageResponse.from(result, MaquinaResponse::from));
  }

  @Transactional
  @DeleteMapping("/maquinas/{id}")
  public ResponseEntity<Void> excluirMaquina(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("AdminController.excluirMaquina iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    excluirRegistroUseCase.execute(
        new ExcluirRegistroUseCase.Input(ExcluirRegistroUseCase.TipoRecurso.MAQUINA, id, adminId));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/maquinas/{id}")
  public ResponseEntity<MaquinaResponse> buscarMaquinaPorId(@PathVariable final UUID id) {
    log.info("AdminController.buscarMaquinaPorId id={}", id);
    final var maquina =
        maquinaRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Maquina nao encontrada"));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }

  @Transactional
  @PostMapping("/maquinas")
  public ResponseEntity<MaquinaResponse> criarMaquina(
      @Valid @RequestBody final CriarMaquinaRequest request, final Authentication authentication) {
    log.info("AdminController.criarMaquina iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Maquina maquina =
        gerenciarMaquinasUseCase.criar(
            new GerenciarMaquinasUseCase.CriarInput(
                request.nome(), request.codigo(), request.descricao(), adminId));
    return ResponseEntity.status(HttpStatus.CREATED).body(MaquinaResponse.from(maquina));
  }

  @Transactional
  @PutMapping("/maquinas/{id}")
  public ResponseEntity<MaquinaResponse> editarMaquina(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarMaquinaRequest request,
      final Authentication authentication) {
    log.info("AdminController.editarMaquina iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Maquina maquina =
        gerenciarMaquinasUseCase.editar(
            new GerenciarMaquinasUseCase.EditarInput(
                id, request.nome(), request.codigo(), request.descricao(), adminId));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }

  @Transactional
  @PatchMapping("/maquinas/{id}/desativar")
  public ResponseEntity<MaquinaResponse> desativarMaquina(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("AdminController.desativarMaquina iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Maquina maquina =
        gerenciarMaquinasUseCase.desativar(
            new GerenciarMaquinasUseCase.DesativarInput(id, adminId));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }

  @Transactional
  @DeleteMapping("/registros")
  public ResponseEntity<Void> excluirRegistro(
      @Valid @RequestBody final ExcluirRegistroRequest request,
      final Authentication authentication) {
    log.info("AdminController.excluirRegistro iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    excluirRegistroUseCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.valueOf(request.tipoRecurso()),
            request.recursoId(),
            adminId));
    return ResponseEntity.noContent().build();
  }

  @Transactional
  @PatchMapping("/usuarios/{id}/senha")
  public ResponseEntity<UsuarioResponse> redefinirSenha(
      @PathVariable final UUID id,
      @Valid @RequestBody final RedefinirSenhaRequest request,
      final Authentication authentication) {
    log.info("AdminController.redefinirSenha iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final Usuario usuario =
        gerenciarUsuariosUseCase.redefinirSenha(
            new GerenciarUsuariosUseCase.RedefinirSenhaInput(id, request.novaSenha(), adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PatchMapping("/usuarios/{id}/perfil")
  public ResponseEntity<UsuarioResponse> alterarPerfil(
      @PathVariable final UUID id,
      @Valid @RequestBody final AlterarPerfilRequest request,
      final Authentication authentication) {
    log.info("AdminController.alterarPerfil iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final PerfilUsuario novoPerfil = PerfilUsuario.valueOf(request.perfil());
    final Usuario usuario =
        gerenciarUsuariosUseCase.alterarPerfil(
            new GerenciarUsuariosUseCase.AlterarPerfilInput(id, novoPerfil, adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }
}
