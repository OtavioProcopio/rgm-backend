package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.UUID;

/** Renomeia a identificacao e/ou marca uma foto como principal na galeria do Modelo. */
public final class EditarFotoGaleriaUseCase {

  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private final UsuarioRepository usuarioRepository;

  public EditarFotoGaleriaUseCase(
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository,
      final UsuarioRepository usuarioRepository) {
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(
      UUID modeloId, UUID fotoId, String identificacao, Boolean principal, UUID gestorId) {}

  public FotoGaleriaModelo execute(final Input input) {
    validarPermissao(input.gestorId());

    final FotoGaleriaModelo foto = buscarNoModelo(input.modeloId(), input.fotoId());

    FotoGaleriaModelo atualizada = foto;

    if (input.identificacao() != null) {
      if (input.identificacao().isBlank()) {
        throw new ValidationException("Identificacao da foto nao pode ficar vazia");
      }
      atualizada = atualizada.comIdentificacao(input.identificacao().trim());
    }

    if (Boolean.TRUE.equals(input.principal())) {
      fotoGaleriaModeloRepository.limparPrincipal(input.modeloId());
      atualizada = atualizada.comPrincipal(true);
    } else if (Boolean.FALSE.equals(input.principal())) {
      atualizada = atualizada.comPrincipal(false);
    }

    return fotoGaleriaModeloRepository.save(atualizada);
  }

  private FotoGaleriaModelo buscarNoModelo(final UUID modeloId, final UUID fotoId) {
    final FotoGaleriaModelo foto =
        fotoGaleriaModeloRepository
            .findById(fotoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Foto nao encontrada"));

    if (!foto.getModeloId().equals(modeloId)) {
      throw new BusinessRuleException("Foto nao pertence a galeria deste Modelo");
    }

    return foto;
  }

  private void validarPermissao(final UUID gestorId) {
    final Usuario gestor =
        usuarioRepository
            .findById(gestorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeGerenciarGaleriaModelo()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar galeria do Modelo");
    }
  }
}
