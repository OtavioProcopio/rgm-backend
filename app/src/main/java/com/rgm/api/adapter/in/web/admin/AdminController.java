package com.rgm.api.adapter.in.web.admin;

import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.ExcluirRegistroRequest;
import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.UsuarioResponse;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
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

  public AdminController(
      final GerenciarUsuariosUseCase gerenciarUsuariosUseCase,
      final CadastrarPrestadorExternoUseCase cadastrarExternoUseCase,
      final GerenciarMaquinasUseCase gerenciarMaquinasUseCase,
      final ExcluirRegistroUseCase excluirRegistroUseCase,
      final ListarUsuariosUseCase listarUsuariosUseCase,
      final ListarMaquinasUseCase listarMaquinasUseCase) {
    this.gerenciarUsuariosUseCase = gerenciarUsuariosUseCase;
    this.cadastrarExternoUseCase = cadastrarExternoUseCase;
    this.gerenciarMaquinasUseCase = gerenciarMaquinasUseCase;
    this.excluirRegistroUseCase = excluirRegistroUseCase;
    this.listarUsuariosUseCase = listarUsuariosUseCase;
    this.listarMaquinasUseCase = listarMaquinasUseCase;
  }

  @GetMapping("/usuarios")
  public ResponseEntity<PageResponse<UsuarioResponse>> listarUsuarios(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size) {
    final var result = listarUsuariosUseCase.execute(page, size);
    return ResponseEntity.ok(PageResponse.from(result, UsuarioResponse::from));
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

  @GetMapping("/maquinas")
  public ResponseEntity<PageResponse<MaquinaResponse>> listarMaquinas(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size) {
    final var result = listarMaquinasUseCase.execute(page, size);
    return ResponseEntity.ok(PageResponse.from(result, MaquinaResponse::from));
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
}
