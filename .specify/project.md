# rgm-backend

> Identidade e princípios **deste** projeto. Este arquivo **é versionado** — é o que o time
> compartilha. Os princípios da organização ficam em `.specify/memory/constitution.md`, que o
> `/bu:constitution` gera a cada clone e o `.gitignore` mantém fora do git.

## Identidade

- **Projeto**: rgm-backend
- **Tipo**: api
- **Stack**: Java 21, Spring Boot 3.5, PostgreSQL com Flyway, MinIO/S3, OpenPDF, Maven
- **Domínio**: API do sistema RGM, que gerencia modelos de fundição e seus chamados de manutenção em quadro Kanban, com evidências fotográficas, métricas e relatórios, para operadores, gestores e administradores da fábrica.
- **Cobertura mínima acordada**: 95%

## Princípios específicos deste projeto

> Entram aqui, e só aqui, as regras que valem para **este** repositório e que não estão nos
> princípios da organização: restrição regulatória, SLA, compatibilidade obrigatória, limite de
> dependência, janela de manutenção. Cada princípio declara o que **proíbe** — princípio que não
> proíbe nada não é portão.

### Princípio 7 — Mapa de camadas do legado

O projeto nasceu em Clean Architecture antes do padrão da organização e mantém os nomes de
pacote originais. Cada um responde por uma camada Byte Union:

| Pacote em `com.rgm.api` | Camada Byte Union |
|---|---|
| `core.domain.model`, `core.domain.validation`, `core.domain.events` | `core/domain` (entities, enums) |
| `core.domain.exceptions` | `core/domain/errors` |
| `core.domain.ports` | `interfaces/` |
| `core.application.usecases` | `core/application` |
| `adapter.in.web` | `adapters/controllers` |
| `adapter.out.persistence` | `adapters/repositories` |
| `adapter.out.storage` | `adapters/clients` |
| `adapter.out.security`, `adapter.out.report`, `adapter.out.event` | `infra/tools` |
| `adapter.config` | `infra/init` e `infra/api` |

**Proíbe:** criar pacote novo com a nomenclatura Byte Union ao lado dos existentes
(`adapters`, `interfaces`, `infra`) enquanto a migração não for decidida numa spec própria;
e proíbe código novo em `core` importar `org.springframework`, `jakarta` ou SDK externo. Os
10 casos de uso que hoje importam `@Transactional` são dívida registrada em
`.specify/memory/as-is.md`, não precedente.

### Princípio 8 — Linguagem ubíqua em português

O domínio é nomeado em português no código (`Solicitacao`, `Evidencia`, `PerfilUsuario`,
`podeTriar`) e no contrato da API, que o `rgm-frontend` consome.

**Proíbe:** traduzir ou renomear termo de domínio existente, e introduzir sinônimo em inglês
para conceito que já tem nome em português. Termos técnicos (`Repository`, `UseCase`,
`Controller`, `Request`, `Response`) seguem em inglês. Método de teste novo usa
`shouldXWhenY`.

### Princípio 9 — Compatibilidade com a versão em produção

A v1.5.0 está em produção e o `rgm-frontend` é publicado em separado.

**Proíbe:** remover ou renomear campo, endpoint ou tipo de evento SSE existente sem que a
spec declare a quebra e a ordem de publicação entre backend e frontend; e proíbe alterar
migração Flyway já publicada (`V1` a `V9`). Mudança de esquema entra em migração nova, de
número sequencial.

### Princípio 10 — Cobertura não regride

O `pom.xml` exige 95% de linha no conjunto, e a medição de 2026-10-05 registrou 95,60%.

**Proíbe:** baixar o mínimo do JaCoCo, acrescentar classe à lista de exclusões para
alcançar o limite, e entregar arquivo modificado com menos de 95% de linha.

### Princípio 11 — Padrão de teste vale para o que for tocado

A suíte existente (546 testes) segue um padrão anterior ao da organização.

**Proíbe:** teste novo ou alterado fora do Princípio 3 (AAA, um comportamento por teste,
`verifyNoMoreInteractions`). Não exige reescrever teste que a mudança não toca.

### Princípio 12 — Uma fonte de especificação

Funcionalidade nova é especificada em `specs/NNN-slug/`, pelo fluxo Byte Union. A pasta
`openspec/` fica congelada como registro histórico do comportamento até a v1.5.0: as três
mudanças feitas nela estão arquivadas e sincronizadas, e os comandos `/opsx:*` e as skills
`openspec-*` saíram de `.claude/`. A feature que altera comportamento descrito em
`openspec/specs/` acrescenta a própria linha à tabela de `openspec/README.md`.

**Proíbe:** abrir mudança em `openspec/changes/`, alterar `openspec/specs/` e reinstalar
comando ou skill de OpenSpec no repositório.

## Emendas

| Versão | Data | O que mudou |
|---|---|---|
| 1.0.0 | 2026-10-05 | ratificação inicial |
| 1.0.1 | 2026-10-05 | Princípio 12: `openspec/` congelado, mudanças arquivadas, ferramental OpenSpec removido |
