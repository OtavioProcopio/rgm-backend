package com.rgm.api.core.domain.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import org.junit.jupiter.api.Test;

class DomainExceptionsTest {

  @Test
  void domainExceptionComMensagem() {
    final DomainException ex = new DomainException("erro");
    assertEquals("erro", ex.getMessage());
    assertNull(ex.getCause());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void domainExceptionComCausa() {
    final Throwable causa = new IllegalStateException("raiz");
    final DomainException ex = new DomainException("erro", causa);
    assertEquals("erro", ex.getMessage());
    assertSame(causa, ex.getCause());
  }

  @Test
  void recursoNaoEncontradoExceptionEhDomainException() {
    final RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("nao encontrado");
    assertEquals("nao encontrado", ex.getMessage());
    assertInstanceOf(DomainException.class, ex);
  }

  @Test
  void businessRuleExceptionEhDomainException() {
    final BusinessRuleException ex = new BusinessRuleException("regra violada");
    assertEquals("regra violada", ex.getMessage());
    assertInstanceOf(DomainException.class, ex);
  }

  @Test
  void validationExceptionEhDomainException() {
    final ValidationException ex = new ValidationException("invalido");
    assertEquals("invalido", ex.getMessage());
    assertInstanceOf(DomainException.class, ex);
  }

  @Test
  void naoAutorizadoExceptionEhDomainException() {
    final NaoAutorizadoException ex = new NaoAutorizadoException("sem permissao");
    assertEquals("sem permissao", ex.getMessage());
    assertInstanceOf(DomainException.class, ex);
  }

  @Test
  void transicaoStatusInvalidaExceptionMontaMensagemEGuardaCampos() {
    final TransicaoStatusInvalidaException ex =
        new TransicaoStatusInvalidaException(
            StatusSolicitacao.CONCLUIDA, StatusSolicitacao.EM_ANDAMENTO);

    assertEquals("Transicao de status invalida: CONCLUIDA -> EM_ANDAMENTO", ex.getMessage());
    assertEquals(StatusSolicitacao.CONCLUIDA, ex.getDe());
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, ex.getPara());
    assertInstanceOf(DomainException.class, ex);
  }
}
