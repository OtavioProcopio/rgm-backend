package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.util.UUID;

/** Remove uma foto da galeria do Modelo (arquivo fisico + registro). */
public final class RemoverFotoGaleriaUseCase {

  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private final UsuarioRepository usuarioRepository;
  private final StorageService storageService;

  public RemoverFotoGaleriaUseCase(
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository,
      final UsuarioRepository usuarioRepository,
      final StorageService storageService) {
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
    this.usuarioRepository = usuarioRepository;
    this.storageService = storageService;
  }

  public record Input(UUID modeloId, UUID fotoId, UUID gestorId) {}

  public void execute(final Input input) {
    validarPermissao(input.gestorId());

    final FotoGaleriaModelo foto =
        fotoGaleriaModeloRepository
            .findById(input.fotoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Foto nao encontrada"));

    if (!foto.getModeloId().equals(input.modeloId())) {
      throw new BusinessRuleException("Foto nao pertence a galeria deste Modelo");
    }

    storageService.delete(foto.getPublicUrl());
    fotoGaleriaModeloRepository.deleteById(foto.getId());
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
