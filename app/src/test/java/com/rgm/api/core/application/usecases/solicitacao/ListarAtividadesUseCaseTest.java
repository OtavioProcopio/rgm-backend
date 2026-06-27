package com.rgm.api.core.application.usecases.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarAtividadesUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private AtividadeSolicitacaoRepository atividadeRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @InjectMocks private ListarAtividadesUseCase useCase;

  @Test
  void execute_retornaAtividadesComNomeDoAutor() {
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.abertura(solId, autorId, Instant.now());
    final Usuario autor =
        new Usuario(
            autorId,
            "Alice",
            "a@x.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            Instant.now(),
            Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atividade));
    when(usuarioRepository.findAllByIdIn(List.of(autorId))).thenReturn(List.of(autor));

    final var result = useCase.execute(solId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).autorNome()).isEqualTo("Alice");
    assertThat(result.get(0).atividade()).isEqualTo(atividade);
  }

  @Test
  void execute_usaNomePadraoSeAutorNaoEncontrado() {
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.abertura(solId, autorId, Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atividadeRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atividade));
    when(usuarioRepository.findAllByIdIn(List.of(autorId))).thenReturn(List.of());

    final var result = useCase.execute(solId);

    assertThat(result.get(0).autorNome()).isEqualTo("Usuário");
  }

  @Test
  void execute_lancaExcecaoSeNaoEncontrado() {
    final UUID solId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(solId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
