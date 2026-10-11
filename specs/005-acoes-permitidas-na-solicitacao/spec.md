# Especificação — Ações permitidas na solicitação

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#90 (prioridade baixa). Relacionada:
OtavioProcopio/rgm-frontend#115.

> **Registro de processo:** esta especificação foi escrita em 2026-10-05 **depois** da
> implementação, para regularizar a feature no padrão de specs. A ordem correta era
> especificar antes. Os pontos que a issue não determinava estão em **Ambiguidades**,
> marcados para confirmação.

## Problema

A tela decide por conta própria quais botões mostrar em uma solicitação, recriando as
regras de permissão que a API aplica. Quando uma regra muda na API, a tela fica mostrando
botões que terminam em recusa, ou escondendo ações que seriam aceitas. A correção da #86
(feature 001) é um exemplo: a regra de quem pode anexar evidência mudou, e a tela não sabe.

## Objetivo

Ao consultar uma solicitação, o usuário recebe a lista das ações que ele pode executar
nela, calculada pela API com as mesmas regras que ela aplica ao executar cada ação.

## Fora de escopo

- Trocar as regras locais do frontend por esse campo (rgm-frontend#115).
- Calcular as ações em listagens, em avisos de tempo real e nas respostas das ações.
- Mudar qualquer regra de permissão existente.
- Indicar pré-condições de dados de uma ação (por exemplo, a evidência de serviço
  realizado exigida para enviar à validação).
- Indicar quais tipos de evidência o usuário pode anexar.

## Personas e cenários de uso

- **Operador que abriu o chamado:** abre o detalhe e vê só os botões das ações que pode
  executar naquele status.
- **Operador responsável:** vê o botão de enviar para validação quando o chamado está em
  andamento.
- **Gestor ou administrador:** vê as ações de triagem, devolução, encerramento e
  cancelamento conforme o status.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | A consulta de uma solicitação pelo identificador deve devolver o campo `acoesPermitidas`, com as ações que o usuário autenticado pode executar nela. | obrigatório |
| RF-02 | `TRIAR` é permitida a GESTOR e ADMINISTRADOR quando a solicitação está em A_FAZER. | obrigatório |
| RF-03 | `ENVIAR_VALIDACAO` é permitida quando a solicitação está em EM_ANDAMENTO, a GESTOR, ADMINISTRADOR ou responsável atribuído. | obrigatório |
| RF-04 | `DEVOLVER` e `ENCERRAR` são permitidas a GESTOR e ADMINISTRADOR quando a solicitação está em EM_VALIDACAO. | obrigatório |
| RF-05 | `CANCELAR` é permitida a GESTOR e ADMINISTRADOR em solicitação não encerrada; e a quem abriu a solicitação, só em A_FAZER e sem responsável atribuído. | obrigatório |
| RF-06 | `EDITAR` é permitida em solicitação não encerrada, a GESTOR, ADMINISTRADOR ou quem a abriu. | obrigatório |
| RF-07 | `COMENTAR` é permitida a GESTOR e ADMINISTRADOR em qualquer status; e a quem abriu ou é responsável, enquanto a solicitação não está encerrada. | obrigatório |
| RF-08 | `ALTERAR_RESPONSAVEIS` é permitida a GESTOR e ADMINISTRADOR em solicitação não encerrada. | obrigatório |
| RF-09 | `ANEXAR_EVIDENCIA` é permitida em solicitação não encerrada, a quem tem acesso às evidências dela (GESTOR, ADMINISTRADOR, responsável ou quem a abriu). | obrigatório |
| RF-10 | Usuário inativo e usuário de perfil EXTERNO não têm ação permitida. | obrigatório |
| RF-11 | Fora da consulta pelo identificador (listagens, avisos de tempo real e respostas de ação), o campo `acoesPermitidas` deve vir nulo, significando "não calculado". | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Compatibilidade do contrato | 0 campos removidos ou renomeados na resposta de solicitação em relação à v1.5.0 |
| RNF-02 | Custo da consulta pelo identificador | no máximo 1 consulta adicional ao banco em relação à v1.5.0 |
| RNF-03 | Custo das listagens | 0 consultas adicionais ao banco em relação à v1.5.0 |
| RNF-04 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Ações permitidas na solicitação

  Cenário: Gestor em solicitação A_FAZER (RF-02, RF-05, RF-06, RF-07, RF-08, RF-09)
    Dado um GESTOR ativo e uma solicitação em A_FAZER aberta por outro usuário
    Quando as ações permitidas são calculadas
    Então o resultado é TRIAR, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS e ANEXAR_EVIDENCIA

  Cenário: Gestor em solicitação EM_ANDAMENTO (RF-03)
    Dado um GESTOR ativo e uma solicitação em EM_ANDAMENTO
    Quando as ações permitidas são calculadas
    Então o resultado é ENVIAR_VALIDACAO, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS e ANEXAR_EVIDENCIA

  Cenário: Gestor em solicitação EM_VALIDACAO (RF-04)
    Dado um GESTOR ativo e uma solicitação em EM_VALIDACAO
    Quando as ações permitidas são calculadas
    Então o resultado é DEVOLVER, ENCERRAR, CANCELAR, EDITAR, COMENTAR, ALTERAR_RESPONSAVEIS e ANEXAR_EVIDENCIA

  Cenário: Gestor em solicitação encerrada (RF-07)
    Dado um GESTOR ativo e uma solicitação CONCLUIDA ou CANCELADA
    Quando as ações permitidas são calculadas
    Então o resultado é COMENTAR

  Cenário: Autor em solicitação A_FAZER sem responsável (RF-05, RF-06, RF-07, RF-09)
    Dado um OPERADOR ativo que abriu uma solicitação em A_FAZER sem responsável
    Quando as ações permitidas são calculadas
    Então o resultado é CANCELAR, EDITAR, COMENTAR e ANEXAR_EVIDENCIA

  Cenário: Autor não cancela solicitação com responsável (RF-05)
    Dado um OPERADOR ativo que abriu uma solicitação em A_FAZER que já tem responsável
    Quando as ações permitidas são calculadas
    Então o resultado é EDITAR, COMENTAR e ANEXAR_EVIDENCIA

  Cenário: Autor não cancela solicitação em andamento (RF-05)
    Dado um OPERADOR ativo que abriu uma solicitação em EM_ANDAMENTO e não é responsável
    Quando as ações permitidas são calculadas
    Então o resultado é EDITAR, COMENTAR e ANEXAR_EVIDENCIA

  Cenário: Responsável em solicitação EM_ANDAMENTO (RF-03)
    Dado um OPERADOR ativo responsável por uma solicitação em EM_ANDAMENTO que ele não abriu
    Quando as ações permitidas são calculadas
    Então o resultado é ENVIAR_VALIDACAO, COMENTAR e ANEXAR_EVIDENCIA

  Cenário: Responsável em solicitação EM_VALIDACAO (RF-03)
    Dado um OPERADOR ativo responsável por uma solicitação em EM_VALIDACAO que ele não abriu
    Quando as ações permitidas são calculadas
    Então o resultado é COMENTAR e ANEXAR_EVIDENCIA

  Cenário: Operador sem relação (RF-03, RF-06, RF-07, RF-09)
    Dado um OPERADOR ativo que não abriu nem é responsável por uma solicitação em EM_ANDAMENTO
    Quando as ações permitidas são calculadas
    Então o resultado é vazio

  Cenário: Autor em solicitação encerrada (RF-06, RF-07, RF-09)
    Dado um OPERADOR ativo que abriu uma solicitação CONCLUIDA
    Quando as ações permitidas são calculadas
    Então o resultado é vazio

  Cenário: Usuário inativo (RF-10)
    Dado um GESTOR inativo e uma solicitação em A_FAZER
    Quando as ações permitidas são calculadas
    Então o resultado é vazio

  Cenário: Usuário EXTERNO (RF-10)
    Dado um usuário de perfil EXTERNO e uma solicitação em EM_ANDAMENTO
    Quando as ações permitidas são calculadas
    Então o resultado é vazio

  Cenário: Consulta pelo identificador devolve as ações (RF-01)
    Dado que as ações calculadas para o usuário são EDITAR e COMENTAR
    Quando o usuário consulta a solicitação pelo identificador
    Então a resposta é 200
    E o campo "acoesPermitidas" contém "EDITAR" e "COMENTAR", nessa ordem

  Cenário: Listagem não calcula ações (RF-11)
    Quando um usuário lista as solicitações
    Então a resposta é 200
    E o campo "acoesPermitidas" de cada item é nulo
```

## Ambiguidades

Nenhuma em aberto. Resolvidas em `/bu:clarify` em 2026-10-05:

| Pergunta | Decisão |
|---|---|
| Calcular as ações também na listagem? | Não: só na consulta pelo identificador (RF-11). Respondido pelo usuário. |
| `ENCERRAR` e `CANCELAR` separadas? | Sim: `ENCERRAR` é concluir, só em EM_VALIDACAO; o restante é `CANCELAR` (RF-04, RF-05). Respondido pelo usuário. |
| Esconder `ENVIAR_VALIDACAO` enquanto falta a evidência de serviço? | Não: a ação aparece por permissão e status, e a API explica o que falta ao recusar. Respondido pelo usuário. |
| Usuário inativo recebe lista vazia; e as ações que não verificam se o usuário está ativo? | Lista vazia mantida (RF-10). As ações passam a recusar usuário inativo na feature 006, nesta mesma branch. Respondido pelo usuário. |
| Campo nulo nos avisos de tempo real | Mantido nulo. O usuário não opinou sobre este ponto; é decisão técnica: o aviso é um só para todos os usuários, então não há como calcular por usuário. Registrar na rgm-frontend#113 que o frontend deve preservar o valor que já tinha. |

## Métricas de sucesso

- Depois da rgm-frontend#115, nenhum botão de ação exibido no detalhe termina em 403.
- Nenhuma regra de permissão de solicitação duplicada no frontend.
