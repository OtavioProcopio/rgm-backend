# Especificação — Autor da solicitação anexa e vê evidências

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#86 (prioridade alta). Relacionada:
OtavioProcopio/rgm-frontend#112.

## Problema

Quem abre uma solicitação não tem acesso às evidências dela. Hoje só GESTOR, ADMINISTRADOR
ou usuário atribuído como responsável consegue anexar ou listar evidências.

Consequências para o OPERADOR que abre o chamado:

- A tela de nova solicitação envia a foto de abertura logo depois de criar a solicitação.
  Nesse momento o operador ainda não é responsável por nada, então o envio é sempre
  recusado por falta de permissão. A tela não mostra o erro, e a foto se perde sem aviso.
- O mesmo operador não consegue listar as evidências da solicitação que abriu, embora já
  veja a solicitação e já possa comentar nela.

O custo é o chamado chegar à triagem sem a foto do defeito, que é a informação que o gestor
usa para decidir prioridade e responsável.

## Objetivo

O usuário que abriu uma solicitação anexa evidências a ela e lista as evidências dela, com
as mesmas restrições que já valem para os responsáveis. Usuário sem relação com a
solicitação continua sem acesso.

## Fora de escopo

- Mostrar ao usuário os erros de upload na tela (OtavioProcopio/rgm-frontend#112).
- Enviar o anexo no mesmo pedido da abertura, triagem, encerramento ou devolução (#91).
- Expor as ações permitidas ao usuário na resposta da solicitação (#90).
- Mudar a regra de exclusão de evidências, que já libera quem enviou a evidência.
- Mudar quem pode ver ou listar solicitações.
- Recuperar fotos de abertura perdidas em solicitações já criadas.
- Mudar as regras de GESTOR, ADMINISTRADOR e responsável atribuído, exceto a recusa de
  usuário inativo (RF-08).

## Personas e cenários de uso

- **Operador que abre o chamado:** identifica um defeito no modelo, abre a solicitação pelo
  celular ou pelo computador e anexa a foto do defeito no mesmo fluxo. Espera que a foto
  apareça no detalhe da solicitação e depois acompanha as fotos que os responsáveis anexam.
- **Gestor que tria:** abre a solicitação recém-criada e espera encontrar a foto de abertura
  para decidir prioridade e responsável.
- **Operador sem relação com o chamado:** não abriu nem é responsável; não deve ver nem
  anexar evidências.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve permitir que o usuário que abriu a solicitação anexe evidência a ela, mesmo sem ser responsável atribuído. | obrigatório |
| RF-02 | O sistema deve permitir que o usuário que abriu a solicitação liste as evidências dela, mesmo sem ser responsável atribuído. | obrigatório |
| RF-03 | O sistema deve continuar recusando, por falta de permissão, o anexo e a listagem de evidências para OPERADOR que não abriu a solicitação nem é responsável atribuído. | obrigatório |
| RF-04 | O sistema deve continuar recusando o anexo de evidência em solicitação encerrada (concluída ou cancelada), inclusive para quem a abriu. | obrigatório |
| RF-05 | O sistema deve manter o acesso atual de GESTOR, ADMINISTRADOR e responsável atribuído, quando ativos, ao anexo de qualquer tipo e à listagem de evidências. | obrigatório |
| RF-06 | O sistema deve aplicar a mesma regra de acesso ao anexo e à listagem, de modo que quem pode anexar em uma solicitação também pode listar as evidências dela. | obrigatório |
| RF-07 | O sistema deve limitar aos tipos ABERTURA e GERAL o anexo feito por quem abriu a solicitação e não é responsável atribuído, GESTOR nem ADMINISTRADOR; os demais tipos são recusados por falta de permissão. | obrigatório |
| RF-08 | O sistema deve recusar, por falta de permissão, o anexo e a listagem de evidências feitos por usuário inativo, qualquer que seja o perfil. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Compatibilidade do contrato da API | 0 campos, endpoints ou códigos de resposta removidos ou renomeados em relação à v1.5.0 |
| RNF-02 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado |
| RNF-03 | Custo da verificação de acesso | no máximo 1 consulta adicional ao banco por pedido de anexo ou de listagem, em relação à v1.5.0 |

## Critérios de aceite

Escritos em DADO / QUANDO / ENTÃO / MAS. Cada critério vira um cenário em
`app/tests/bdd/` sem tradução no meio.

```gherkin
# language: pt
Funcionalidade: Autor da solicitação anexa e vê evidências

  Contexto:
    Dado que existe o usuário "ana" com perfil OPERADOR
    E que existe o usuário "bruno" com perfil OPERADOR
    E que existe o usuário "carla" com perfil GESTOR
    E que "ana" abriu a solicitação "S1", que está em A_FAZER e sem responsáveis

  Cenário: Autor anexa a foto de abertura (RF-01)
    Quando "ana" anexa uma imagem do tipo ABERTURA à solicitação "S1"
    Então o anexo é aceito com resposta 201
    E a solicitação "S1" passa a ter 1 evidência do tipo ABERTURA enviada por "ana"

  Cenário: Autor lista as evidências da própria solicitação (RF-02)
    Dado que "carla" anexou uma imagem do tipo GERAL à solicitação "S1"
    Quando "ana" lista as evidências da solicitação "S1"
    Então a listagem responde 200
    E a listagem contém 1 evidência

  Cenário: Operador sem relação não anexa (RF-03)
    Quando "bruno" anexa uma imagem do tipo GERAL à solicitação "S1"
    Então o anexo é recusado com resposta 403
    Mas a solicitação "S1" continua com 0 evidências

  Cenário: Operador sem relação não lista (RF-03)
    Quando "bruno" lista as evidências da solicitação "S1"
    Então a listagem é recusada com resposta 403

  Cenário: Autor não anexa em solicitação encerrada (RF-04)
    Dado que a solicitação "S1" foi cancelada
    Quando "ana" anexa uma imagem do tipo GERAL à solicitação "S1"
    Então o anexo é recusado por regra de negócio
    Mas a solicitação "S1" continua com 0 evidências

  Cenário: Autor ainda lista evidências de solicitação encerrada (RF-02)
    Dado que "carla" anexou uma imagem do tipo GERAL à solicitação "S1"
    E que a solicitação "S1" foi cancelada
    Quando "ana" lista as evidências da solicitação "S1"
    Então a listagem responde 200
    E a listagem contém 1 evidência

  Cenário: Responsável atribuído mantém o acesso (RF-05)
    Dado que "carla" triou a solicitação "S1" e atribuiu "bruno" como responsável
    Quando "bruno" anexa uma imagem do tipo GERAL à solicitação "S1"
    Então o anexo é aceito com resposta 201

  Cenário: Gestor mantém o acesso sem ser responsável (RF-05)
    Quando "carla" lista as evidências da solicitação "S1"
    Então a listagem responde 200

  Cenário: Autor continua anexando depois da triagem (RF-01)
    Dado que "carla" triou a solicitação "S1" e atribuiu "bruno" como responsável
    Quando "ana" anexa uma imagem do tipo GERAL à solicitação "S1"
    Então o anexo é aceito com resposta 201

  Esquema do Cenário: Autor não responsável só anexa ABERTURA e GERAL (RF-07)
    Quando "ana" anexa uma imagem do tipo <tipo> à solicitação "S1"
    Então o anexo é recusado com resposta 403
    Mas a solicitação "S1" continua com 0 evidências

    Exemplos:
      | tipo              |
      | INSTRUCAO_SERVICO |
      | SERVICO_REALIZADO |
      | CONCLUSAO         |
      | DEVOLUCAO         |

  Cenário: Autor que também é responsável anexa qualquer tipo (RF-07)
    Dado que "carla" triou a solicitação "S1" e atribuiu "ana" como responsável
    Quando "ana" anexa uma imagem do tipo SERVICO_REALIZADO à solicitação "S1"
    Então o anexo é aceito com resposta 201

  Cenário: Autor inativo não anexa (RF-08)
    Dado que o usuário "ana" foi desativado
    Quando "ana" anexa uma imagem do tipo ABERTURA à solicitação "S1"
    Então o anexo é recusado com resposta 403
    Mas a solicitação "S1" continua com 0 evidências

  Cenário: Usuário inativo não lista (RF-08)
    Dado que o usuário "ana" foi desativado
    Quando "ana" lista as evidências da solicitação "S1"
    Então a listagem é recusada com resposta 403

  Cenário: Quem anexa também lista (RF-06)
    Dado que "ana" anexou uma imagem do tipo ABERTURA à solicitação "S1"
    Quando "ana" lista as evidências da solicitação "S1"
    Então a listagem contém 1 evidência do tipo ABERTURA
```

## Ambiguidades

Nenhuma em aberto. Resolvidas em `/bu:clarify` em 2026-10-05:

| Pergunta | Decisão |
|---|---|
| Que tipos de evidência o autor não responsável pode anexar? | Só ABERTURA e GERAL (RF-07). |
| O autor mantém o acesso depois da triagem? | Sim: anexa enquanto a solicitação não está encerrada e lista sempre (RF-01, RF-02, RF-04). |
| Usuário inativo mantém acesso às evidências? | Não: anexo e listagem recusam usuário inativo, em qualquer perfil (RF-08). |

## Métricas de sucesso

- Em produção, nenhum pedido de anexo do tipo ABERTURA feito pelo autor da solicitação
  termina em 403 (hoje, todo pedido desse tipo feito por OPERADOR termina em 403).
- Solicitações abertas por OPERADOR com foto passam a chegar à triagem com 1 evidência do
  tipo ABERTURA.
- Nenhum chamado de suporte sobre "foto de abertura não aparece" depois da publicação.
