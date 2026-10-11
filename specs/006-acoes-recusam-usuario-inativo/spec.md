# Especificação — Usuário desativado perde o acesso imediatamente

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: decisão do usuário em 2026-10-05, ao esclarecer a feature 005 (rgm-backend#90). Não
há issue própria no GitHub. A pasta foi criada com o nome "ações recusam usuário inativo";
o escopo foi ampliado na pergunta seguinte para o sistema inteiro.

## Problema

Quando um administrador desativa um usuário, o sistema só impede o próximo login e a
próxima renovação de sessão. A sessão que o usuário já tem aberta continua valendo até o
fim da validade do acesso, que é de até 24 horas. Nesse intervalo o usuário desativado
ainda abre, tria, edita, devolve, encerra e cancela solicitações, consulta dados e recebe
os avisos em tempo real.

Desativar um usuário costuma acontecer por desligamento ou por suspeita de mau uso. Nos
dois casos, até um dia de acesso residual é um risco.

## Objetivo

A partir do momento em que um usuário é desativado, toda chamada autenticada dele é
recusada, em qualquer parte do sistema, sem esperar o acesso vencer.

## Fora de escopo

- Encerrar conexões de tempo real que o usuário já tinha abertas antes de ser desativado
  (elas terminam sozinhas ao atingir o tempo limite da conexão).
- Refletir na sessão aberta uma mudança de perfil do usuário.
- Reduzir a validade do acesso ou mudar o mecanismo de sessão (#93).
- Lista de sessões ativas ou "sair de todos os dispositivos".
- Mudar as regras de login e de renovação, que já recusam usuário inativo.

## Personas e cenários de uso

- **Administrador:** desativa um usuário e espera que ele pare de conseguir usar o sistema
  na mesma hora.
- **Usuário desativado com sessão aberta:** na próxima ação, é tratado como não autenticado
  e volta para a tela de login, onde o login é recusado.
- **Usuário ativo:** não percebe diferença.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve tratar como não autenticada toda chamada feita com acesso válido de um usuário que está inativo. | obrigatório |
| RF-02 | O sistema deve tratar como não autenticada toda chamada feita com acesso válido de um usuário que não existe mais. | obrigatório |
| RF-03 | O sistema deve continuar autenticando normalmente a chamada feita com acesso válido de um usuário ativo. | obrigatório |
| RF-04 | O sistema deve recusar a abertura de conexão de tempo real feita com acesso válido de um usuário inativo ou inexistente. | obrigatório |
| RF-05 | O sistema deve manter as respostas atuais para acesso ausente, vencido, adulterado ou de tipo errado. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Tempo até o bloqueio | 0 chamadas autenticadas aceitas depois da desativação (antes: até 24 horas de acesso residual) |
| RNF-02 | Custo por chamada autenticada | no máximo 1 consulta adicional ao banco por chamada |
| RNF-03 | Compatibilidade do contrato | 0 campos, endpoints ou códigos de resposta alterados para usuários ativos |
| RNF-04 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado que entra na medição |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Usuário desativado perde o acesso imediatamente

  Cenário: Usuário ativo é autenticado (RF-03)
    Dado um acesso válido do usuário ativo "joao"
    Quando "joao" faz uma chamada autenticada
    Então a chamada segue autenticada como "joao"

  Cenário: Usuário inativo não é autenticado (RF-01)
    Dado um acesso válido do usuário "joao"
    E que o usuário "joao" foi desativado
    Quando "joao" faz uma chamada autenticada
    Então a chamada segue sem autenticação
    Mas a chamada não é interrompida pelo filtro

  Cenário: Usuário inexistente não é autenticado (RF-02)
    Dado um acesso válido de um usuário que foi removido
    Quando é feita uma chamada autenticada com esse acesso
    Então a chamada segue sem autenticação

  Cenário: Usuário ativo abre conexão de tempo real (RF-03, RF-04)
    Dado um acesso válido do usuário ativo "joao"
    Quando "joao" abre a conexão de tempo real
    Então a conexão é aberta

  Cenário: Usuário inativo não abre conexão de tempo real (RF-04)
    Dado um acesso válido do usuário "joao"
    E que o usuário "joao" foi desativado
    Quando "joao" abre a conexão de tempo real
    Então a abertura é recusada com resposta 401

  Cenário: Usuário inexistente não abre conexão de tempo real (RF-04)
    Dado um acesso válido de um usuário que foi removido
    Quando é aberta a conexão de tempo real com esse acesso
    Então a abertura é recusada com resposta 401
```

RF-05 é verificado pelos testes existentes do filtro de autenticação (acesso ausente,
vencido, de tipo errado e com assinatura inválida), que não mudam de resultado.

## Ambiguidades

Nenhuma em aberto. Resolvidas em `/bu:clarify` em 2026-10-05:

| Pergunta | Decisão |
|---|---|
| Barrar o usuário inativo só nas ações da solicitação ou em tudo? | Em tudo, na autenticação. Respondido pelo usuário. |
| Código de resposta para o usuário desativado | 401 (não autenticado), o mesmo de um acesso vencido. Decisão técnica: o frontend já trata 401 tentando renovar a sessão, a renovação recusa usuário inativo e a tela volta ao login. |

## Métricas de sucesso

- Em produção, depois de desativar um usuário, o log de acesso não registra nenhuma
  resposta 2xx para chamadas dele.
- Nenhum registro de atividade em solicitação com autor desativado em data posterior à
  desativação.
