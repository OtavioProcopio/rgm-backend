package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-13 (Modelos): Cadastrar, editar e desativar Modelos. Ator: Gestor (Admin herda). */
public final class GerenciarModelosUseCase {

  private final ModeloRepository modeloRepository;
  private final MaquinaRepository maquinaRepository;
  private final UsuarioRepository usuarioRepository;

  public GerenciarModelosUseCase(
      final ModeloRepository modeloRepository,
      final MaquinaRepository maquinaRepository,
      final UsuarioRepository usuarioRepository) {
    this.modeloRepository = modeloRepository;
    this.maquinaRepository = maquinaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record CriarInput(
      String codigo, String descricao, String observacoes, UUID maquinaId, UUID gestorId) {}

  public record EditarInput(
      UUID modeloId, String codigo, String descricao, String observacoes, UUID gestorId) {}

  public record DesativarInput(UUID modeloId, UUID gestorId) {}

  public Modelo criar(final CriarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());

    final Maquina maquina =
        maquinaRepository
            .findById(input.maquinaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Maquina nao encontrada"));

    if (!maquina.isAtiva()) {
      throw new ValidationException("Maquina inativa");
    }

    final int versao =
        modeloRepository.countByMaquinaIdAndCodigo(input.maquinaId(), input.codigo()) + 1;

    final Modelo modelo =
        Modelo.criar(
            input.codigo(),
            input.descricao(),
            input.observacoes(),
            input.maquinaId(),
            versao,
            agora);

    return modeloRepository.save(modelo);
  }

  public Modelo editar(final EditarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Modelo editado =
        modelo.editar(input.codigo(), input.descricao(), input.observacoes(), agora);

    return modeloRepository.save(editado);
  }

  public Modelo desativar(final DesativarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Modelo desativado = modelo.desativar(agora);
    return modeloRepository.save(desativado);
  }

  private void validarPermissaoModelo(final UUID gestorId) {
    final Usuario gestor =
        usuarioRepository
            .findById(gestorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (!gestor.getPerfil().podeGerenciarModelos()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar modelos");
    }
  }
}
