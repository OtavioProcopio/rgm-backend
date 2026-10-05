## ADDED Requirements

### Requirement: Evento gerado automaticamente ao nascer um modelo
O sistema SHALL registrar automaticamente um evento do tipo CADASTRO no
historico de um Modelo sempre que ele for criado com sucesso, independente
do caminho: cadastro direto (tela "Novo modelo") ou conclusao de uma
solicitacao do tipo CRIACAO (ver capacidade `solicitacoes-kanban`). O autor
do evento e o usuario responsavel pela criacao (quem cadastrou diretamente,
ou quem concluiu a solicitacao). O evento CADASTRO nunca referencia uma
solicitacao quando originado do cadastro direto; quando originado da
conclusao de uma solicitacao CRIACAO, referencia essa solicitacao.

#### Scenario: Cadastro direto gera evento de CADASTRO
- **WHEN** um gestor cadastra um novo modelo diretamente pela tela "Novo
  modelo"
- **THEN** um evento de tipo CADASTRO e criado no historico daquele modelo,
  com autor igual ao gestor, sem referencia a nenhuma solicitacao

#### Scenario: Conclusao de solicitacao CRIACAO gera evento de CADASTRO
- **WHEN** uma solicitacao do tipo CRIACAO e concluida e o Modelo
  correspondente e criado
- **THEN** um evento de tipo CADASTRO e criado no historico do modelo
  recem-criado, com autor igual a quem concluiu a solicitacao, referenciando
  a solicitacao de origem

#### Scenario: Evento de cadastro aparece como primeira entrada da timeline
- **WHEN** um usuario autenticado consulta o historico de eventos de um
  modelo recem-criado, sem nenhuma outra solicitacao concluida ainda
- **THEN** o evento de tipo CADASTRO aparece como a unica (e portanto
  primeira) entrada da timeline
