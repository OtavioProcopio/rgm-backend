package com.rgm.api.core.domain.validation;

/** Tamanho maximo, em caracteres, de cada campo de texto que a API recebe. */
public final class LimitesTexto {

  public static final int SOLICITACAO_TITULO = 255;
  public static final int SOLICITACAO_DESCRICAO = 2000;

  /** Comentario, motivo de cancelamento, motivo de devolucao e comentario de encerramento. */
  public static final int COMENTARIO = 2000;

  public static final int MODELO_CODIGO = 100;
  public static final int MODELO_DESCRICAO = 255;
  public static final int MODELO_MAQUINA = 255;
  public static final int MODELO_OBSERVACOES = 2000;

  public static final int MODELO_PRETENDIDO_CODIGO = 50;
  public static final int MODELO_PRETENDIDO_MAQUINA = 100;
  public static final int MODELO_PRETENDIDO_OBSERVACOES = 2000;

  public static final int MAQUINA_NOME = 255;
  public static final int USUARIO_NOME = 255;
  public static final int USUARIO_EMAIL = 255;

  /** O limite entra no lugar de {max} quando a anotacao de tamanho e avaliada. */
  public static final String MENSAGEM = "deve ter no máximo {max} caracteres";

  private LimitesTexto() {}
}
