package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.OrdemDeModelos;
import com.rgm.api.core.domain.ports.repositories.PageResult;

/** Listar modelos com paginacao e filtros opcionais. */
public final class ListarModelosUseCase {

  private final ModeloRepository modeloRepository;

  public ListarModelosUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  /**
   * Filtros e ordem da listagem. {@code tipo}, {@code q}, {@code maquinaExata} e {@code ordem} sao
   * do catalogo; sem eles a listagem e a de sempre.
   */
  public record Input(
      Boolean ativo,
      String codigo,
      String maquina,
      String descricao,
      String tipo,
      String q,
      boolean maquinaExata,
      OrdemDeModelos ordem,
      int page,
      int size) {

    /** Listagem de sempre: so os filtros antigos e a ordem por criacao. */
    public Input(
        final Boolean ativo,
        final String codigo,
        final String maquina,
        final String descricao,
        final int page,
        final int size) {
      this(
          ativo, codigo, maquina, descricao, null, null, false, OrdemDeModelos.CRIACAO, page, size);
    }

    boolean usaOCatalogo() {
      return tipo != null || q != null || maquinaExata || ordem != OrdemDeModelos.CRIACAO;
    }
  }

  public PageResult<Modelo> execute(final Input input) {
    if (input.usaOCatalogo()) {
      return modeloRepository.buscarCatalogo(
          new FiltrosDeModelos(
              input.ativo(),
              input.codigo(),
              input.maquina(),
              input.maquinaExata(),
              input.descricao(),
              input.tipo(),
              input.q()),
          input.ordem(),
          input.page(),
          input.size());
    }
    if (input.ativo() != null
        || input.codigo() != null
        || input.maquina() != null
        || input.descricao() != null) {
      return modeloRepository.findByFilters(
          input.ativo(),
          input.codigo(),
          input.maquina(),
          input.descricao(),
          input.page(),
          input.size());
    }
    return modeloRepository.findAll(input.page(), input.size());
  }
}
