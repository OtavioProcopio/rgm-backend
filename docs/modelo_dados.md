# Modelo de dados

Este documento descreve as entidades, enums e relacionamentos de alto nível.

## Convenções de tipos

- `UUID`: identificador único.
- `datetime`: data/hora (UTC).
- `string`: texto.
- `int`: inteiro.
- `boolean`: verdadeiro/falso.

## Enums

- `PerfilUsuario`: `OPERADOR` | `GESTOR` | `ADMINISTRADOR` | `EXTERNO`
- `StatusSolicitacao`: `A_FAZER` | `EM_ANDAMENTO` | `EM_VALIDACAO` | `CONCLUIDA` | `CANCELADA`
- `PrioridadeSolicitacao`: `BAIXA` | `MEDIA` | `ALTA` | `URGENTE`
- `TipoSolicitacao`: `REPARO` | `INSPECAO` | `REENGENHARIA`
- `TipoAtividadeSolicitacao`: `ABERTURA` | `ATRIBUICAO` | `MUDANCA_STATUS` | `COMENTARIO` | `EVIDENCIA_ADICIONADA`
- `TipoEventoModelo`: `MODIFICACAO` | `INSPECAO` | `REPARO` | `AJUSTE` | `MANUTENCAO` | `OUTRO`

## Entidades

### Usuario

- `id: UUID`
- `nome: string`
- `email: string` (opcional para `perfil=EXTERNO`)
- `senhaHash: string` (opcional para `perfil=EXTERNO` — externo não faz login)
- `perfil: PerfilUsuario`
- `ativo: boolean`
- `criadoEm: datetime`
- `atualizadoEm: datetime`

### Maquina

- `id: UUID`
- `nome: string` (ex: "Injetora 03")
- `codigo: string` (identificador interno)
- `descricao: string` (opcional)
- `ativa: boolean`
- `criadaEm: datetime`
- `atualizadaEm: datetime`

### Modelo

- `id: UUID`
- `codigo: string` (código do ferramental)
- `versao: int` (controle de versão por máquina + código)
- `descricao: string`
- `observacoes: string` (opcional; texto livre para anotações técnicas/operacionais — ex.: cavidades, particularidades, avisos)
- `fotoUrl: string` (opcional; URL persistente/sem expiração do objeto no bucket público para a foto capa/estado atual; não usar presigned URLs)
- `fotoAtualizadaEm: datetime` (opcional)
- `estadoAtualDescricao: string` (opcional; texto livre do estado/condição atual)
- `estadoAtualAtualizadoEm: datetime` (opcional)
- `ativo: boolean`
- `maquinaId: UUID`
- `temPendenciaAberta: boolean` (derivado/cache)
- `criadoEm: datetime`
- `atualizadoEm: datetime`

Relação (conceitual):
- `historico: EventoModelo[]` (lista/timeline de eventos do modelo)

### Solicitacao

- `id: UUID`
- `titulo: string`
- `descricao: string`
- `tipo: TipoSolicitacao`
- `status: StatusSolicitacao`
- `prioridade: PrioridadeSolicitacao` (opcional em `A_FAZER`; obrigatória a partir de `EM_ANDAMENTO`)
- `modeloId: UUID`
- `abertaPorUsuarioId: UUID`
- `comentarioFinal: string` (opcional)
- `criadaEm: datetime`
- `atualizadaEm: datetime`
- `concluidaEm: datetime` (opcional)
- `canceladaEm: datetime` (opcional)

### SolicitacaoAtribuicao

Permite atribuir a solicitação a **múltiplos usuários** e manter histórico.

- `id: UUID`
- `solicitacaoId: UUID`
- `usuarioId: UUID` (referencia um `Usuario` com perfil `OPERADOR`, `GESTOR` ou `EXTERNO`; `ADMINISTRADOR` não é atribuível)
- `atribuidoPorUsuarioId: UUID` (Gestor ou Administrador)
- `atribuidoEm: datetime`
- `removidoEm: datetime` (opcional)

### AtividadeSolicitacao

Histórico/auditoria do card.

- `id: UUID`
- `solicitacaoId: UUID`
- `tipo: TipoAtividadeSolicitacao`
- `deStatus: StatusSolicitacao` (opcional)
- `paraStatus: StatusSolicitacao` (opcional)
- `comentario: string` (opcional)
- `autorUsuarioId: UUID`
- `criadaEm: datetime`

> Importante: `autorUsuarioId` é sempre o usuário real que interagiu com o sistema (Operador/Gestor/Administrador). Para atribuições a `EXTERNO`, o Gestor/Administrador atua como procurador e permanece como autor.

### Evidencia

Anexos de fotos/documentos.

- `id: UUID`
- `publicUrl: string` (URL persistente/sem expiração do objeto no bucket público; não usar presigned URLs)
- `mimeType: string`
- `nomeArquivo: string`
- `tamanhoBytes: int` (opcional)
- `enviadaPorUsuarioId: UUID`
- `criadaEm: datetime`

> Observação: `enviadaPorUsuarioId` segue a mesma regra do autor (Operador/Gestor/Administrador). `EXTERNO` não faz upload direto.

> Importante: persistir a URL pública e persistente do bucket no banco de dados.

### SolicitacaoEvidencia (N:N)

- `solicitacaoId: UUID`
- `evidenciaId: UUID`

### EventoModelo

Timeline / prontuário do modelo.

Cada item do histórico (`EventoModelo`) pode ter **1 ou mais evidências** anexadas por meio da relação N:N `EventoModeloEvidencia`.
Se vocês quiserem **obrigar** que todo evento tenha pelo menos 1 evidência, isso deve virar uma regra de negócio/validação (o modelo suporta 0..N).

- `id: UUID`
- `modeloId: UUID`
- `tipo: TipoEventoModelo`
- `titulo: string` (resumo curto)
- `descricao: string`
- `estadoModeloDescricao: string` (opcional; estado/condição resultante)
- `defineFotoCapa: boolean` (quando verdadeiro, uma evidência do evento vira a foto capa)
- `executadoPorUsuarioId: UUID`
- `criadoEm: datetime`
- `solicitacaoRelacionadaId: UUID` (opcional)

### EventoModeloEvidencia (N:N)

- `eventoModeloId: UUID`
- `evidenciaId: UUID`

## Relacionamentos (resumo)

- Máquina 1 → N Modelos
- Modelo 1 → N Solicitações
- Solicitação 1 → N Atribuições
- Solicitação 1 → N Atividades
- Solicitação N ↔ N Evidências
- Modelo 1 → N Eventos do modelo
- Evento do modelo N ↔ N Evidências

O diagrama de classes completo está em [diagramas.md](diagramas.md).
