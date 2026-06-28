package com.rgm.api.adapter.in.web.usuario;

import com.rgm.api.adapter.in.web.dto.request.AlterarSenhaRequest;
import com.rgm.api.adapter.in.web.dto.response.UsuarioResponse;
import com.rgm.api.core.application.usecases.auth.AlterarSenhaPropriaUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
  private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

  private final AlterarSenhaPropriaUseCase alterarSenhaUseCase;
  private final UsuarioRepository usuarioRepository;

  public UsuarioController(
      final AlterarSenhaPropriaUseCase alterarSenhaUseCase,
      final UsuarioRepository usuarioRepository) {
    this.alterarSenhaUseCase = alterarSenhaUseCase;
    this.usuarioRepository = usuarioRepository;
  }

  @GetMapping("/me")
  public ResponseEntity<UsuarioResponse> obterPerfil(final Authentication authentication) {
    log.info("UsuarioController.obterPerfil iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }

  @Transactional
  @PatchMapping("/me/senha")
  public ResponseEntity<UsuarioResponse> alterarSenha(
      @Valid @RequestBody final AlterarSenhaRequest request, final Authentication authentication) {
    log.info("UsuarioController.alterarSenha iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var usuario =
        alterarSenhaUseCase.execute(
            new AlterarSenhaPropriaUseCase.Input(
                usuarioId, request.senhaAtual(), request.novaSenha()));
    return ResponseEntity.ok(UsuarioResponse.from(usuario));
  }
}
