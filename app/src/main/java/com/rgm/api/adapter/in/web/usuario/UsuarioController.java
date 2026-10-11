package com.rgm.api.adapter.in.web.usuario;

import com.rgm.api.adapter.in.web.dto.request.AlterarSenhaRequest;
import com.rgm.api.adapter.in.web.dto.response.SenhaAlteradaResponse;
import com.rgm.api.adapter.in.web.dto.response.UsuarioResponse;
import com.rgm.api.adapter.in.web.solicitacao.SolicitacaoEventPublisher;
import com.rgm.api.core.application.usecases.auth.AlterarSenhaPropriaUseCase;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
  private final SolicitacaoEventPublisher eventPublisher;

  public UsuarioController(
      final AlterarSenhaPropriaUseCase alterarSenhaUseCase,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoEventPublisher eventPublisher) {
    this.alterarSenhaUseCase = alterarSenhaUseCase;
    this.usuarioRepository = usuarioRepository;
    this.eventPublisher = eventPublisher;
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

  // Sem transacao no controller: as conexoes de tempo real so podem cair depois que a senha
  // nova estiver gravada, senao o token antigo reconecta antes do commit.
  @PatchMapping("/me/senha")
  public ResponseEntity<SenhaAlteradaResponse> alterarSenha(
      @Valid @RequestBody final AlterarSenhaRequest request, final Authentication authentication) {
    log.info("UsuarioController.alterarSenha iniciado");
    final UUID usuarioId = UUID.fromString(authentication.getName());
    final var output =
        alterarSenhaUseCase.execute(
            new AlterarSenhaPropriaUseCase.Input(
                usuarioId, request.senhaAtual(), request.novaSenha()));
    eventPublisher.encerrarConexoes(usuarioId);
    return ResponseEntity.ok(SenhaAlteradaResponse.from(output));
  }
}
