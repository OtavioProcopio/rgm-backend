package com.rgm.api.adapter.in.web.admin;

import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.CriarUsuarioRequest;
import com.rgm.api.adapter.in.web.dto.request.ExcluirRegistroRequest;
import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.adapter.in.web.dto.response.UsuarioResponse;
import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final GerenciarUsuariosUseCase gerenciarUsuariosUseCase;
  private final CadastrarPrestadorExternoUseCase cadastrarExternoUseCase;
  private final GerenciarMaquinasUseCase gerenciarMaquinasUseCase;
  private final ExcluirRegistroUseCase excluirRegistroUseCase;

  public AdminController(
      final GerenciarUsuariosUseCase gerenciarUsuariosUseCase,
      final CadastrarPrestadorExternoUseCase cadastrarExternoUseCase,
      final GerenciarMaquinasUseCase gerenciarMaquinasUseCase,
      final ExcluirRegistroUseCase excluirRegistroUseCase) {
    this.gerenciarUsuariosUseCase = gerenciarUsuariosUseCase;
    this.cadastrarExternoUseCase = cadastrarExternoUseCase;
    this.gerenciarMaquinasUseCase = gerenciarMaquinasUseCase;
    this.excluirRegistroUseCase = excluirRegistroUseCase;
  }

  @PostMapping("/usuarios")
  public ResponseEntity<UsuarioResponse> criarUsuario(
      @Valid @RequestBody final CriarUsuarioRequest request, final Authentication authentication) {
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

  @PatchMapping("/usuarios/{id}/desativar")
  public ResponseEntity<UsuarioResponse> desativarUsuario(
      @PathVariable final UUID id, final Authentication authentication) {
    final UUID adminId = UUID.fromString(authentication.getName());
    final Usuario usuario =
        gerenciarUsuariosUseCase.desativar(
            new GerenciarUsuariosUseCase.DesativarInput(id, adminId));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @PostMapping("/maquinas")
  public ResponseEntity<MaquinaResponse> criarMaquina(
      @Valid @RequestBody final CriarMaquinaRequest request, final Authentication authentication) {
    final UUID adminId = UUID.fromString(authentication.getName());
    final Maquina maquina =
        gerenciarMaquinasUseCase.criar(
            new GerenciarMaquinasUseCase.CriarInput(
                request.nome(), request.codigo(), request.descricao(), adminId));
    return ResponseEntity.status(HttpStatus.CREATED).body(MaquinaResponse.from(maquina));
  }

  @DeleteMapping("/registros")
  public ResponseEntity<Void> excluirRegistro(
      @Valid @RequestBody final ExcluirRegistroRequest request,
      final Authentication authentication) {
    final UUID adminId = UUID.fromString(authentication.getName());
    excluirRegistroUseCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.valueOf(request.tipoRecurso()),
            request.recursoId(),
            adminId));
    return ResponseEntity.noContent().build();
  }
}
