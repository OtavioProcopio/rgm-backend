package com.rgm.api.adapter.config;

import com.rgm.api.core.application.usecases.admin.CadastrarPrestadorExternoUseCase;
import com.rgm.api.core.application.usecases.admin.ExcluirRegistroUseCase;
import com.rgm.api.core.application.usecases.admin.GerenciarUsuariosUseCase;
import com.rgm.api.core.application.usecases.admin.ListarUsuariosUseCase;
import com.rgm.api.core.application.usecases.auth.AlterarSenhaPropriaUseCase;
import com.rgm.api.core.application.usecases.auth.LoginUseCase;
import com.rgm.api.core.application.usecases.auth.RefreshTokenUseCase;
import com.rgm.api.core.application.usecases.dashboard.ObterMetricasDashboardUseCase;
import com.rgm.api.core.application.usecases.evidencia.AnexarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.ExcluirEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.VisualizarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.modelo.AtualizarFotoCapaUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarModelosUseCase;
import com.rgm.api.core.application.usecases.modelo.RecalcularPendenciaUseCase;
import com.rgm.api.core.application.usecases.modelo.SolicitacaoFinalizadaListener;
import com.rgm.api.core.application.usecases.solicitacao.AbrirSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.CancelarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.DevolverSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EditarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EncerrarSolicitacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.EnviarParaValidacaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.GerenciarResponsaveisUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ListarSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import com.rgm.api.core.application.usecases.solicitacao.RegistrarComentarioUseCase;
import com.rgm.api.core.application.usecases.solicitacao.TriarSolicitacaoUseCase;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import com.rgm.api.core.domain.ports.services.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

  @Bean
  public LoginUseCase loginUseCase(
      final UsuarioRepository usuarioRepository,
      final PasswordHasher passwordHasher,
      final AccessTokenIssuer tokenIssuer) {
    return new LoginUseCase(usuarioRepository, passwordHasher, tokenIssuer);
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      final UsuarioRepository usuarioRepository, final AccessTokenIssuer tokenIssuer) {
    return new RefreshTokenUseCase(usuarioRepository, tokenIssuer);
  }

  @Bean
  public AlterarSenhaPropriaUseCase alterarSenhaPropriaUseCase(
      final UsuarioRepository usuarioRepository, final PasswordHasher passwordHasher) {
    return new AlterarSenhaPropriaUseCase(usuarioRepository, passwordHasher);
  }

  @Bean
  public AbrirSolicitacaoUseCase abrirSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository) {
    return new AbrirSolicitacaoUseCase(
        solicitacaoRepository, modeloRepository, atividadeRepository, usuarioRepository);
  }

  @Bean
  public TriarSolicitacaoUseCase triarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    return new TriarSolicitacaoUseCase(
        solicitacaoRepository, usuarioRepository, atribuicaoRepository, atividadeRepository);
  }

  @Bean
  public EnviarParaValidacaoUseCase enviarParaValidacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository) {
    return new EnviarParaValidacaoUseCase(
        solicitacaoRepository,
        usuarioRepository,
        atribuicaoRepository,
        atividadeRepository,
        solicitacaoEvidenciaRepository);
  }

  @Bean
  public DevolverSolicitacaoUseCase devolverSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    return new DevolverSolicitacaoUseCase(
        solicitacaoRepository, usuarioRepository, atividadeRepository);
  }

  @Bean
  public EncerrarSolicitacaoUseCase encerrarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final EventoModeloRepository eventoModeloRepository,
      final DomainEventPublisher eventPublisher) {
    return new EncerrarSolicitacaoUseCase(
        solicitacaoRepository,
        usuarioRepository,
        atividadeRepository,
        eventoModeloRepository,
        eventPublisher);
  }

  @Bean
  public CancelarSolicitacaoUseCase cancelarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final DomainEventPublisher eventPublisher) {
    return new CancelarSolicitacaoUseCase(
        solicitacaoRepository, usuarioRepository, atividadeRepository, eventPublisher);
  }

  @Bean
  public RegistrarComentarioUseCase registrarComentarioUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    return new RegistrarComentarioUseCase(
        solicitacaoRepository, atividadeRepository, usuarioRepository, atribuicaoRepository);
  }

  @Bean
  public EditarSolicitacaoUseCase editarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    return new EditarSolicitacaoUseCase(
        solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Bean
  public GerenciarResponsaveisUseCase gerenciarResponsaveisUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    return new GerenciarResponsaveisUseCase(
        solicitacaoRepository, usuarioRepository, atribuicaoRepository, atividadeRepository);
  }

  @Bean
  public AnexarEvidenciaUseCase anexarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final EvidenciaRepository evidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final StorageService storageService,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    return new AnexarEvidenciaUseCase(
        solicitacaoRepository,
        evidenciaRepository,
        solicitacaoEvidenciaRepository,
        atividadeRepository,
        storageService,
        usuarioRepository,
        atribuicaoRepository);
  }

  @Bean
  public ExcluirEvidenciaUseCase excluirEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final EvidenciaRepository evidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    return new ExcluirEvidenciaUseCase(
        solicitacaoRepository,
        evidenciaRepository,
        solicitacaoEvidenciaRepository,
        usuarioRepository,
        atribuicaoRepository);
  }

  @Bean
  public VisualizarEvidenciaUseCase visualizarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final EvidenciaRepository evidenciaRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    return new VisualizarEvidenciaUseCase(
        solicitacaoRepository,
        solicitacaoEvidenciaRepository,
        evidenciaRepository,
        usuarioRepository,
        atribuicaoRepository);
  }

  @Bean
  public RecalcularPendenciaUseCase recalcularPendenciaUseCase(
      final ModeloRepository modeloRepository, final SolicitacaoRepository solicitacaoRepository) {
    return new RecalcularPendenciaUseCase(modeloRepository, solicitacaoRepository);
  }

  @Bean
  public SolicitacaoFinalizadaListener solicitacaoFinalizadaListener(
      final RecalcularPendenciaUseCase recalcularPendenciaUseCase) {
    return new SolicitacaoFinalizadaListener(recalcularPendenciaUseCase);
  }

  @Bean
  public AtualizarFotoCapaUseCase atualizarFotoCapaUseCase(
      final ModeloRepository modeloRepository,
      final UsuarioRepository usuarioRepository,
      final EvidenciaRepository evidenciaRepository,
      final EventoModeloRepository eventoModeloRepository,
      final EventoModeloEvidenciaRepository eventoModeloEvidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final StorageService storageService) {
    return new AtualizarFotoCapaUseCase(
        modeloRepository,
        usuarioRepository,
        evidenciaRepository,
        eventoModeloRepository,
        eventoModeloEvidenciaRepository,
        solicitacaoEvidenciaRepository,
        storageService);
  }

  @Bean
  public GerenciarModelosUseCase gerenciarModelosUseCase(
      final ModeloRepository modeloRepository, final UsuarioRepository usuarioRepository) {
    return new GerenciarModelosUseCase(modeloRepository, usuarioRepository);
  }

  @Bean
  public CadastrarPrestadorExternoUseCase cadastrarPrestadorExternoUseCase(
      final UsuarioRepository usuarioRepository) {
    return new CadastrarPrestadorExternoUseCase(usuarioRepository);
  }

  @Bean
  public GerenciarUsuariosUseCase gerenciarUsuariosUseCase(
      final UsuarioRepository usuarioRepository, final PasswordHasher passwordHasher) {
    return new GerenciarUsuariosUseCase(usuarioRepository, passwordHasher);
  }

  @Bean
  public ExcluirRegistroUseCase excluirRegistroUseCase(
      final UsuarioRepository usuarioRepository,
      final SolicitacaoRepository solicitacaoRepository,
      final ModeloRepository modeloRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final EventoModeloRepository eventoModeloRepository) {
    return new ExcluirRegistroUseCase(
        usuarioRepository,
        solicitacaoRepository,
        modeloRepository,
        atribuicaoRepository,
        atividadeRepository,
        solicitacaoEvidenciaRepository,
        eventoModeloRepository);
  }

  @Bean
  public ListarSolicitacoesUseCase listarSolicitacoesUseCase(
      final SolicitacaoRepository solicitacaoRepository) {
    return new ListarSolicitacoesUseCase(solicitacaoRepository);
  }

  @Bean
  public ListarModelosUseCase listarModelosUseCase(final ModeloRepository modeloRepository) {
    return new ListarModelosUseCase(modeloRepository);
  }

  @Bean
  public ListarUsuariosUseCase listarUsuariosUseCase(final UsuarioRepository usuarioRepository) {
    return new ListarUsuariosUseCase(usuarioRepository);
  }

  @Bean
  public ObterMetricasSolicitacoesUseCase obterMetricasSolicitacoesUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository) {
    return new ObterMetricasSolicitacoesUseCase(
        solicitacaoRepository, usuarioRepository, modeloRepository);
  }

  @Bean
  public ObterMetricasDashboardUseCase obterMetricasDashboardUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    return new ObterMetricasDashboardUseCase(
        solicitacaoRepository, usuarioRepository, modeloRepository, atividadeRepository);
  }
}
