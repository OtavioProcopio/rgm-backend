# Maquinas Catalogo Specification

## Purpose

Manter um catalogo administravel de nomes de maquinas, usado para validar o
campo `maquina` de um Modelo. Um Modelo so pode ser criado/editado referenciando
uma maquina que exista e esteja ativa neste catalogo.

## Requirements

### Requirement: Listar maquinas do catalogo
O sistema SHALL expor a lista completa de maquinas cadastradas (id, nome,
ativo, criadoEm, atualizadoEm), sem paginacao, para uso em formularios e
filtros.

#### Scenario: Listagem
- **WHEN** qualquer usuario autenticado consulta o catalogo
- **THEN** o sistema retorna todas as maquinas cadastradas

### Requirement: Validacao de maquina ao criar/editar Modelo
O sistema SHALL validar que o valor de `maquina` informado ao criar ou editar
um Modelo corresponde exatamente (apos trim) ao nome de uma maquina ativa no
catalogo.

#### Scenario: Maquina valida
- **WHEN** o nome informado corresponde a uma maquina ativa
- **THEN** a criacao/edicao do Modelo prossegue

#### Scenario: Maquina inexistente ou inativa
- **WHEN** o nome informado nao corresponde a nenhuma maquina ativa
- **THEN** o sistema rejeita com "Maquina invalida ou inativa: <nome>"
