package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.TipoModelo;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-13 (Modelos): Cadastrar, editar e desativar Modelos. Ator: Gestor (Admin herda). */
public final class GerenciarModelosUseCase {

  private final ModeloRepository modeloRepository;
  private final UsuarioRepository usuarioRepository;
  private final MaquinaRepository maquinaRepository;
  private final EventoModeloRepository eventoModeloRepository;

  public GerenciarModelosUseCase(
      final ModeloRepository modeloRepository,
      final UsuarioRepository usuarioRepository,
      final MaquinaRepository maquinaRepository,
      final EventoModeloRepository eventoModeloRepository) {
    this.modeloRepository = modeloRepository;
    this.usuarioRepository = usuarioRepository;
    this.maquinaRepository = maquinaRepository;
    this.eventoModeloRepository = eventoModeloRepository;
  }

  public record CriarInput(
      String codigo,
      String descricao,
      String observacoes,
      String maquina,
      TipoModelo tipo,
      UUID gestorId,
      UUID solicitacaoOrigemId) {

    /** Cadastro direto (fora de qualquer solicitacao) — construtor de conveniencia. */
    public CriarInput(
        final String codigo,
        final String descricao,
        final String observacoes,
        final String maquina,
        final TipoModelo tipo,
        final UUID gestorId) {
      this(codigo, descricao, observacoes, maquina, tipo, gestorId, null);
    }
  }

  public record EditarInput(
      UUID modeloId,
      String codigo,
      String descricao,
      String observacoes,
      String maquina,
      TipoModelo tipo,
      UUID gestorId) {}

  public record DesativarInput(UUID modeloId, UUID gestorId) {}

  public record ReativarInput(UUID modeloId, UUID gestorId) {}

  public Modelo criar(final CriarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());
    validarMaquina(input.maquina());

    final int versao =
        modeloRepository.countByMaquinaAndCodigo(input.maquina(), input.codigo()) + 1;

    final Modelo modelo =
        Modelo.criar(
            input.codigo(),
            input.descricao(),
            input.observacoes(),
            input.maquina(),
            input.tipo(),
            versao,
            agora);

    final Modelo salvo = modeloRepository.save(modelo);

    eventoModeloRepository.save(
        EventoModelo.criarCadastro(
            salvo.getId(),
            salvo.getDescricao(),
            input.gestorId(),
            input.solicitacaoOrigemId(),
            agora));

    return salvo;
  }

  public Modelo editar(final EditarInput input) {
    final Instant agora = Instant.now();
    validarPermissaoModelo(input.gestorId());
    validarMaquina(input.maquina());

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Modelo editado =
        modelo.editar(
            input.codigo(),
            input.descricao(),
            input.observacoes(),
            input.maquina(),
            input.tipo(),
            agora);

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

  private void validarMaquina(final String maquina) {
    if (maquina == null || !maquinaRepository.existsByNomeAndAtivoTrue(maquina.trim())) {
      throw new ValidationException("Maquina invalida ou inativa: " + maquina);
    }
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
