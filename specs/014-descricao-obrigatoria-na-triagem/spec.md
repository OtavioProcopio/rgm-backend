# Especificação — Descrição do que fazer, obrigatória na triagem

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: relato do usuário em 2026-10-31 (sem issue). Consumidor: `rgm-frontend`, spec 017.
Base: `develop` com a spec 013 mergeada.

> **Registro de processo (honestidade):** esta mudança foi **codificada e aberta como PR #139
> antes desta spec existir**, sem passar por `/bu:specify`, `/bu:plan` e `/bu:tasks`. O usuário
> apontou o desvio. Esta spec a absorve e a convergência a confronta com o código que já estava
> na branch.

## Problema

1. **A triagem não registra o que deve ser feito.** Triar é decidir prioridade e responsáveis.
   Quem tria sabe o que precisa ser feito, mas a API não recebe essa informação: o responsável
   atribuído vê a solicitação como o operador a abriu e não a instrução do gestor.
2. **Hoje a instrução só existe como foto ou comentário solto.** A foto é opcional e o
   comentário é outra ação, depois da triagem, que pode nunca acontecer. A instrução não é
   parte da decisão de triar, e uma triagem sem ela é aceita.

**Custo de não resolver:** solicitações entram em andamento sem dizer o que fazer, e o
responsável tem de perguntar fora do sistema.

## Objetivo

- Toda triagem carrega a descrição do que precisa ser feito, escrita por quem tria.
- A descrição fica registrada junto com a triagem, no histórico da solicitação, ou nenhuma das
  duas coisas acontece.

## Fora de escopo

- Campo próprio da solicitação para a instrução, tipo de atividade novo e migração de esquema
  (alternativas registradas no plano; ficam para outra spec se o usuário pedir).
- Alterar a descrição de uma triagem já feita; a edição da solicitação não muda.
- Exigir descrição em outras ações (devolver já exige motivo; encerrar já exige comentário).
- Preencher a descrição das triagens que já aconteceram.

## Personas e cenários de uso

- **Gestor:** tria a solicitação e diz o que o responsável deve fazer.
- **Responsável (operador):** abre a solicitação e lê, no histórico, o que o gestor pediu.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | A triagem exige a descrição do que precisa ser feito; sem ela, ou só com espaços, a triagem é recusada com erro de validação e a solicitação não muda | obrigatório |
| RF-02 | A descrição aceita até 2000 caracteres, o mesmo limite do comentário; acima disso a triagem é recusada | obrigatório |
| RF-03 | A descrição, sem os espaços das pontas, fica registrada no histórico da solicitação, atribuída a quem triou, na mesma operação que muda o status; se qualquer parte falhar, nada é gravado | obrigatório |
| RF-04 | Nenhuma outra regra da triagem muda: perfis permitidos, responsáveis atribuíveis e ativos, prioridade e status de origem | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Cobertura dos arquivos tocados | ≥ 95% de linhas por arquivo (Princípio 10) |
| RNF-02 | Sem migração de esquema | 0 arquivos novos em `db/migration` |
| RNF-03 | Verificação | `make validate` com 0 falhas e lint limpo |

## Ordem de publicação (Princípio 9)

Esta mudança **quebra** o contrato de `PATCH /api/solicitacoes/{id}/triar`: o campo novo
`descricao` é obrigatório, e um cliente antigo que não o envia recebe 400. Nenhum campo,
rota ou evento existente é removido ou renomeado. O `rgm-frontend` (spec 017) e este backend
são publicados **juntos, com o backend primeiro no mesmo deploy**. Publicar só o backend
impede triar pela tela antiga; publicar só o front faz o backend antigo ignorar `descricao`
sem avisar.

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Descrição do que fazer na triagem

  Cenário: Triar com descrição
    Dado uma solicitação A_FAZER e um gestor
    Quando o gestor tria com prioridade, um responsável ativo e a descrição "Trocar o gabarito"
    Então a solicitação fica EM_ANDAMENTO
    E o histórico traz a atribuição, a mudança de status e a descrição "Trocar o gabarito" do gestor

  Cenário: Triar sem descrição
    Dado uma solicitação A_FAZER
    Quando o gestor tria sem o campo descrição
    Então a resposta é 400
    E a solicitação continua A_FAZER, sem atribuição nem histórico novo

  Cenário: Descrição só com espaços
    Dado uma solicitação A_FAZER
    Quando o gestor tria com a descrição "   "
    Então a resposta é 400
    E a solicitação continua A_FAZER

  Cenário: Descrição acima do limite
    Dado uma solicitação A_FAZER
    Quando o gestor tria com uma descrição de 2001 caracteres
    Então a resposta é 400
    E a solicitação continua A_FAZER

  Cenário: Descrição no limite
    Dado uma solicitação A_FAZER
    Quando o gestor tria com uma descrição de 2000 caracteres
    Então a triagem é feita

  Cenário: Espaços das pontas não são gravados
    Dado a descrição "  Trocar o gabarito  "
    Quando o gestor tria
    Então o histórico traz "Trocar o gabarito"

  Cenário: Regras antigas continuam valendo
    Dado um operador, um responsável inativo ou um perfil não atribuível
    Quando a triagem é pedida com descrição válida
    Então ela é recusada como antes
```

## Ambiguidades

- Nenhuma marca aberta. A forma de registrar (atividade de comentário) é decisão de plano e
  está listada com as alternativas.

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| A descrição pode ser opcional? | Não: obrigatória. A foto continua opcional (no front) | 2026-10-31 |
| É um comentário? | Não: é parte da decisão de triar, gravada na triagem | 2026-10-31 |

## Métricas de sucesso

- 0 solicitações triadas, depois da publicação, sem descrição no histórico.
- 0 dos 4 caminhos de recusa (ausente, em branco, longa demais, regra antiga) deixam efeito parcial.
