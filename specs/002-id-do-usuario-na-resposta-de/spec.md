# Especificação — Id do usuário na resposta de login

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#92 (prioridade baixa, `good first issue`).

## Problema

A resposta de login devolve o token de acesso, o token de renovação, o nome e o perfil do
usuário, mas não o identificador dele. A tela precisa desse identificador para decidir o
que mostrar ("abri esta solicitação", "sou responsável por ela"). Sem ele, o frontend faz
uma segunda chamada à API logo depois de cada login só para descobrir quem é o usuário.

O custo é uma chamada a mais em todo login e uma janela, entre as duas chamadas, em que a
tela já tem sessão mas ainda não sabe aplicar as regras de autoria.

## Objetivo

Um login bem-sucedido devolve, na mesma resposta, o identificador do usuário autenticado.

## Fora de escopo

- Incluir o identificador na resposta de renovação de token.
- Remover ou alterar a consulta dos dados do próprio usuário, que continua existindo.
- Alterar o frontend para deixar de fazer a segunda chamada (trabalho do `rgm-frontend`).
- Incluir outros dados do usuário na resposta (e-mail, situação, datas).
- Mudar as regras de quem pode fazer login e as respostas de falha.

## Personas e cenários de uso

- **Usuário que faz login** (OPERADOR, GESTOR ou ADMINISTRADOR): entra no sistema e espera
  que as telas já apliquem as regras de autoria e responsabilidade, sem espera adicional.
- **Cliente da API já publicado** (frontend da v1.5.0): continua funcionando sem alteração,
  ignorando o campo novo.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve incluir na resposta de login bem-sucedido o identificador do usuário autenticado, no campo `id`. | obrigatório |
| RF-02 | O sistema deve manter na resposta de login os campos existentes (`token`, `refreshToken`, `nome`, `perfil`), com os mesmos nomes e significados. | obrigatório |
| RF-03 | O sistema deve manter as respostas de falha de login sem o identificador de nenhum usuário. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Compatibilidade do contrato da API | 0 campos removidos ou renomeados na resposta de login em relação à v1.5.0 |
| RNF-02 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado |
| RNF-03 | Custo do login | 0 consultas adicionais ao banco por login em relação à v1.5.0 |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Id do usuário na resposta de login

  Contexto:
    Dado que existe o usuário ativo "joao@rgm.test" com perfil OPERADOR e senha "senha-correta"

  Cenário: Login bem-sucedido devolve o identificador do usuário (RF-01)
    Quando "joao@rgm.test" faz login com a senha "senha-correta"
    Então a resposta é 200
    E o campo "id" da resposta é o identificador do usuário "joao@rgm.test"

  Cenário: Login bem-sucedido mantém os campos existentes (RF-02)
    Quando "joao@rgm.test" faz login com a senha "senha-correta"
    Então a resposta é 200
    E a resposta contém os campos "token", "refreshToken", "nome" e "perfil"

  Cenário: Login com senha errada não devolve identificador (RF-03)
    Quando "joao@rgm.test" faz login com a senha "senha-errada"
    Então a resposta é 401
    Mas a resposta não contém o campo "id"
```

## Ambiguidades

Nenhuma. A issue determina o nome do campo (`id`), onde ele entra (resposta de login) e que
a mudança é aditiva.

## Métricas de sucesso

- Toda resposta 200 de login em produção traz o campo `id` preenchido.
- Depois que o frontend adotar o campo, o número de consultas aos dados do próprio usuário
  logo após o login cai a zero.
