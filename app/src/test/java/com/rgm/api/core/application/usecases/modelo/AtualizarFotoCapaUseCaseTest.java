package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.EventoModeloEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtualizarFotoCapaUseCaseTest {

  private ModeloRepository modeloRepository;
  private UsuarioRepository usuarioRepository;
  private EvidenciaRepository evidenciaRepository;
  private EventoModeloRepository eventoModeloRepository;
  private EventoModeloEvidenciaRepository eventoModeloEvidenciaRepository;
  private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private StorageService storageService;
  private AtualizarFotoCapaUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    evidenciaRepository = mock(EvidenciaRepository.class);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    eventoModeloEvidenciaRepository = mock(EventoModeloEvidenciaRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new AtualizarFotoCapaUseCase(
            modeloRepository,
            usuarioRepository,
            evidenciaRepository,
            eventoModeloRepository,
            eventoModeloEvidenciaRepository,
            solicitacaoEvidenciaRepository,
            storageService);
  }

  private Usuario criarGestor() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Gestor",
        "g@test.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        agora,
        agora);
  }

  private Modelo criarModelo() {
    final Instant agora = Instant.now();
    return Modelo.criar("COD-01", "Desc", null, UUID.randomUUID(), 1, agora);
  }

  @Test
  void deveAtualizarFotoCapaViaUpload() {
    final Usuario gestor = criarGestor();
    final Modelo modelo = criarModelo();
    final String url = "http://minio:9000/images/nova.jpg";

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(eventoModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(eventoModeloEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.executeUpload(
            new AtualizarFotoCapaUseCase.UploadInput(
                modelo.getId(),
                "nova.jpg",
                "image/jpeg",
                512L,
                new ByteArrayInputStream(new byte[512]),
                gestor.getId()));

    assertEquals(url, resultado.getFotoUrl());
    verify(eventoModeloRepository).save(any());
  }

  @Test
  void deveAtualizarFotoCapaComEvidenciaExistente() {
    final Usuario gestor = criarGestor();
    final Modelo modelo = criarModelo();
    final Instant agora = Instant.now();
    final Evidencia evidencia =
        new Evidencia(
            UUID.randomUUID(),
            "http://minio:9000/images/existente.jpg",
            "image/jpeg",
            "existente.jpg",
            256,
            UUID.randomUUID(),
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(evidenciaRepository.findById(evidencia.getId())).thenReturn(Optional.of(evidencia));
    when(eventoModeloEvidenciaRepository.existsByEvidenciaIdAndEventoModeloModeloId(
            evidencia.getId(), modelo.getId()))
        .thenReturn(true);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(eventoModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(eventoModeloEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.executeEvidenciaExistente(
            new AtualizarFotoCapaUseCase.EvidenciaExistenteInput(
                modelo.getId(), evidencia.getId(), gestor.getId()));

    assertEquals("http://minio:9000/images/existente.jpg", resultado.getFotoUrl());
  }

  @Test
  void deveFalharComEvidenciaNaoPertenceAoModelo() {
    final Usuario gestor = criarGestor();
    final Modelo modelo = criarModelo();
    final Instant agora = Instant.now();
    final Evidencia evidencia =
        new Evidencia(
            UUID.randomUUID(), "http://url", "image/png", "x.png", 100, UUID.randomUUID(), agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(evidenciaRepository.findById(evidencia.getId())).thenReturn(Optional.of(evidencia));
    when(eventoModeloEvidenciaRepository.existsByEvidenciaIdAndEventoModeloModeloId(
            evidencia.getId(), modelo.getId()))
        .thenReturn(false);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.executeEvidenciaExistente(
                new AtualizarFotoCapaUseCase.EvidenciaExistenteInput(
                    modelo.getId(), evidencia.getId(), gestor.getId())));
  }

  @Test
  void operadorNaoDeveAtualizarFotoCapa() {
    final Instant agora = Instant.now();
    final Usuario operador =
        new Usuario(
            UUID.randomUUID(),
            "Op",
            "op@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.executeUpload(
                new AtualizarFotoCapaUseCase.UploadInput(
                    UUID.randomUUID(),
                    "f.jpg",
                    "image/jpeg",
                    100L,
                    new ByteArrayInputStream(new byte[0]),
                    operador.getId())));
  }
}
