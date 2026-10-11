package com.rgm.api.adapter.out.report;

import java.util.ArrayList;
import java.util.List;

/**
 * O que o relatorio precisa dizer sobre si mesmo: os filtros aplicados (ja escritos de forma
 * legivel), o total que a tela mostraria e quantas linhas o PDF traz. Se o PDF traz menos que o
 * total, o aviso aparece no cabecalho.
 */
public record ContextoDoRelatorio(List<String> filtros, long total, int exibidas) {

  /** Contexto de um relatorio completo, sem filtros. */
  public static ContextoDoRelatorio semFiltros(final int linhas) {
    return new ContextoDoRelatorio(List.of(), linhas, linhas);
  }

  public String filtrosEmTexto() {
    return filtros.isEmpty() ? "Sem filtros" : "Filtros: " + String.join(" · ", filtros);
  }

  /** Aviso de corte, ou {@code null} quando o PDF traz todas as linhas. */
  public String aviso() {
    return total > exibidas
        ? "Exibindo " + exibidas + " de " + total + " — refine os filtros para ver o restante."
        : null;
  }

  /** Monta a lista de filtros, ignorando os que nao foram informados. */
  public static final class Filtros {
    private final List<String> itens = new ArrayList<>();

    public Filtros com(final String rotulo, final Object valor) {
      if (valor != null && !valor.toString().isBlank()) {
        itens.add(rotulo + ": " + valor);
      }
      return this;
    }

    /** Valor de enumeração escrito de forma legível: EM_ANDAMENTO vira "Em andamento". */
    public Filtros comEnum(final String rotulo, final Object valor) {
      if (valor == null || valor.toString().isBlank()) {
        return this;
      }
      final String texto = valor.toString().replace('_', ' ').toLowerCase();
      return com(rotulo, Character.toUpperCase(texto.charAt(0)) + texto.substring(1));
    }

    public List<String> build() {
      return List.copyOf(itens);
    }
  }
}
