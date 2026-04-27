package com.rgm.api.core.domain.model.enums;

/** Perfil de acesso do usuario no sistema RGM. */
public enum PerfilUsuario {
  OPERADOR,
  GESTOR,
  ADMINISTRADOR,
  EXTERNO;

  /** Perfis que podem ser atribuidos como responsaveis em SolicitacaoAtribuicao. */
  public boolean isAtribuivel() {
    return this != ADMINISTRADOR;
  }

  /** Perfis com poder de controle sobre qualquer card do Kanban. */
  public boolean podeMoverQualquer() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem triar e atribuir (A_FAZER -> EM_ANDAMENTO). */
  public boolean podeTriar() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem encerrar solicitacoes (CONCLUIDA/CANCELADA). */
  public boolean podeEncerrar() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem devolver para correcao (EM_VALIDACAO -> EM_ANDAMENTO). */
  public boolean podeDevolver() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem gerenciar usuarios e maquinas. */
  public boolean podeGerenciarUsuariosEMaquinas() {
    return this == ADMINISTRADOR;
  }

  /** Perfis que podem cadastrar/editar/desativar Modelos. */
  public boolean podeGerenciarModelos() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem atualizar a foto capa do Modelo. */
  public boolean podeAtualizarFotoCapa() {
    return this == GESTOR || this == ADMINISTRADOR;
  }

  /** Perfis que podem realizar hard delete. */
  public boolean podeExcluir() {
    return this == ADMINISTRADOR;
  }

  /** Perfis que fazem login no sistema. */
  public boolean fazLogin() {
    return this != EXTERNO;
  }
}
