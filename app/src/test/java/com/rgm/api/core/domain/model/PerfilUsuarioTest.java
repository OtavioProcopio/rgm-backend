package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import org.junit.jupiter.api.Test;

class PerfilUsuarioTest {

  @Test
  void administradorNaoDeveSerAtribuivel() {
    assertFalse(PerfilUsuario.ADMINISTRADOR.isAtribuivel());
    assertTrue(PerfilUsuario.OPERADOR.isAtribuivel());
    assertTrue(PerfilUsuario.GESTOR.isAtribuivel());
    assertTrue(PerfilUsuario.EXTERNO.isAtribuivel());
  }

  @Test
  void gestorEAdminPodemMoverQualquer() {
    assertTrue(PerfilUsuario.GESTOR.podeMoverQualquer());
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeMoverQualquer());
    assertFalse(PerfilUsuario.OPERADOR.podeMoverQualquer());
    assertFalse(PerfilUsuario.EXTERNO.podeMoverQualquer());
  }

  @Test
  void apenasAdminPodeExcluir() {
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeExcluir());
    assertFalse(PerfilUsuario.GESTOR.podeExcluir());
    assertFalse(PerfilUsuario.OPERADOR.podeExcluir());
    assertFalse(PerfilUsuario.EXTERNO.podeExcluir());
  }

  @Test
  void apenasAdminPodeGerenciarUsuariosEMaquinas() {
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeGerenciarUsuariosEMaquinas());
    assertFalse(PerfilUsuario.GESTOR.podeGerenciarUsuariosEMaquinas());
    assertFalse(PerfilUsuario.OPERADOR.podeGerenciarUsuariosEMaquinas());
  }

  @Test
  void gestorEAdminPodemGerenciarModelos() {
    assertTrue(PerfilUsuario.GESTOR.podeGerenciarModelos());
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeGerenciarModelos());
    assertFalse(PerfilUsuario.OPERADOR.podeGerenciarModelos());
    assertFalse(PerfilUsuario.EXTERNO.podeGerenciarModelos());
  }

  @Test
  void externoNaoFazLogin() {
    assertFalse(PerfilUsuario.EXTERNO.fazLogin());
    assertTrue(PerfilUsuario.OPERADOR.fazLogin());
    assertTrue(PerfilUsuario.GESTOR.fazLogin());
    assertTrue(PerfilUsuario.ADMINISTRADOR.fazLogin());
  }

  @Test
  void gestorEAdminPodemTriar() {
    assertTrue(PerfilUsuario.GESTOR.podeTriar());
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeTriar());
    assertFalse(PerfilUsuario.OPERADOR.podeTriar());
    assertFalse(PerfilUsuario.EXTERNO.podeTriar());
  }

  @Test
  void gestorEAdminPodemEncerrar() {
    assertTrue(PerfilUsuario.GESTOR.podeEncerrar());
    assertTrue(PerfilUsuario.ADMINISTRADOR.podeEncerrar());
    assertFalse(PerfilUsuario.OPERADOR.podeEncerrar());
    assertFalse(PerfilUsuario.EXTERNO.podeEncerrar());
  }
}
