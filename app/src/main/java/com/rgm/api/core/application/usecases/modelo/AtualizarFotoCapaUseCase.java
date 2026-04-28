package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.EventoModeloEvidencia;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.ports.repositories.EventoModeloEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/** UC-14: Atualizar foto capa do Modelo (upload novo ou evidencia existente). */
public final class AtualizarFotoCapaUseCase {

  private final ModeloRepository modeloRepository;
  private final UsuarioRepository usuarioRepository;
  private final EvidenciaRepository evidenciaRepository;
  private final EventoModeloRepository eventoModeloRepository;
  private final EventoModeloEvidenciaRepository eventoModeloEvidenciaRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final StorageService storageService;

  public AtualizarFotoCapaUseCase(
      final ModeloRepository modeloRepository,
      final UsuarioRepository usuarioRepository,
      final EvidenciaRepository evidenciaRepository,
      final EventoModeloRepository eventoModeloRepository,
      final EventoModeloEvidenciaRepository eventoModeloEvidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final StorageService storageService) {
    this.modeloRepository = modeloRepository;
    this.usuarioRepository = usuarioRepository;
    this.evidenciaRepository = evidenciaRepository;
    this.eventoModeloRepository = eventoModeloRepository;
    this.eventoModeloEvidenciaRepository = eventoModeloEvidenciaRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.storageService = storageService;
  }

  /** Input para upload de nova foto. */
  public record UploadInput(
      UUID modeloId,
      String nomeArquivo,
      String mimeType,
      long tamanhoBytes,
      InputStream conteudo,
      UUID gestorId) {}

  /** Input para reaproveitar evidencia existente. */
  public record EvidenciaExistenteInput(UUID modeloId, UUID evidenciaId, UUID gestorId) {}

  public String uploadFile(final UploadInput input) {
    validarPermissao(input.gestorId());

    modeloRepository
        .findById(input.modeloId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    return storageService.upload(
        input.nomeArquivo(), input.mimeType(), input.conteudo(), input.tamanhoBytes());
  }

  public Modelo persistUpload(final UploadInput input, final String publicUrl) {
    final Instant agora = Instant.now();

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Evidencia evidencia =
        Evidencia.criar(
            publicUrl,
            input.mimeType(),
            input.nomeArquivo(),
            input.tamanhoBytes() > 0 && input.tamanhoBytes() <= Integer.MAX_VALUE
                ? (int) input.tamanhoBytes()
                : null,
            input.gestorId(),
            agora);

    final Evidencia evidenciaSalva = evidenciaRepository.save(evidencia);

    final Modelo atualizado = modelo.withFotoUrl(publicUrl, agora);
    final Modelo salvo = modeloRepository.save(atualizado);

    registrarEvento(salvo.getId(), evidenciaSalva.getId(), input.gestorId(), agora);

    return salvo;
  }

  public Modelo executeEvidenciaExistente(final EvidenciaExistenteInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.gestorId());

    final Modelo modelo =
        modeloRepository
            .findById(input.modeloId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final Evidencia evidencia =
        evidenciaRepository
            .findById(input.evidenciaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Evidencia nao encontrada"));

    final boolean pertenceAoModelo =
        verificarEvidenciaPertenceAoModelo(input.evidenciaId(), input.modeloId());

    if (!pertenceAoModelo) {
      throw new BusinessRuleException("Evidencia nao pertence ao historico do Modelo");
    }

    final Modelo atualizado = modelo.withFotoUrl(evidencia.getPublicUrl(), agora);
    final Modelo salvo = modeloRepository.save(atualizado);

    registrarEvento(salvo.getId(), evidencia.getId(), input.gestorId(), agora);

    return salvo;
  }

  private void validarPermissao(final UUID gestorId) {
    final Usuario gestor =
        usuarioRepository
            .findById(gestorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeAtualizarFotoCapa()) {
      throw new NaoAutorizadoException("Perfil sem permissao para atualizar foto capa do Modelo");
    }
  }

  private boolean verificarEvidenciaPertenceAoModelo(final UUID evidenciaId, final UUID modeloId) {
    return eventoModeloEvidenciaRepository.existsByEvidenciaIdAndEventoModeloModeloId(
        evidenciaId, modeloId);
  }

  private void registrarEvento(
      final UUID modeloId, final UUID evidenciaId, final UUID gestorId, final Instant agora) {
    final EventoModelo evento =
        EventoModelo.criar(
            modeloId,
            TipoEventoModelo.MODIFICACAO,
            "Atualizacao de foto capa",
            "Foto capa do modelo atualizada",
            null,
            true,
            gestorId,
            null,
            agora);

    final EventoModelo eventoSalvo = eventoModeloRepository.save(evento);
    eventoModeloEvidenciaRepository.save(
        new EventoModeloEvidencia(eventoSalvo.getId(), evidenciaId));
  }
}
