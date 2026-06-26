package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-13 (Modelos): Cadastrar, editar e desativar Modelos. Ator: Gestor (Admin herda). */
public final class GerenciarModelosUseCase {

  private final ModeloRepository modeloRepository;
  private final UsuarioRepository usuarioRepository;

  public GerenciarModelosUseCase(
      final ModeloRepository modeloRepository, final UsuarioRepository usuarioRepository) {
    this.modeloRepository = modeloRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record CriarInput(
      String codigo, String descricao, String observacoes, String maquina, UUID gestorId) {}

  public record EditarInput(
      UUID modeloId,
      String codigo,
      String descricao,
      String observacoes,
      String maquina,
      UUID gestorId) {}

  public record DesativarInput(UUID modeloId, UUID gestorId) {}

  public record ReativarInput(UUID modeloId, UUID gestorId) {}

  public Modelo criar(final CriarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());

    final int versao =
        modeloRepository.countByMaquinaAndCodigo(input.maquina(), input.codigo()) + 1;

    final Modelo modelo =
        Modelo.criar(
            input.codigo(), input.descricao(), input.observacoes(), input.maquina(), versao, agora);

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
        modelo.editar(
            input.codigo(), input.descricao(), input.observacoes(), input.maquina(), agora);

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

  public Modelo reativar(final ReativarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Modelo ativado = modelo.ativar(agora);
    return modeloRepository.save(ativado);
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
