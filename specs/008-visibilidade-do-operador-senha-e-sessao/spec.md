# Especificação — Visibilidade do operador, senha e sessão seguras e limites de texto

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#96, #98 e #109. São as três issues do backend das quais o
frontend depende: a rgm-frontend#122 espera a #96, e a feature 007 do frontend (PR
rgm-frontend#135, em `develop` desde 2026-10-07) já aplica nos formulários a senha de 8
caracteres e os limites de texto que a API ainda não cobra. O usuário pediu as três numa
especificação só, em 2026-10-07.

## Problema

**Visibilidade do operador (#96).** A API tem duas regras opostas para o operador:

1. A listagem só devolve ao operador as solicitações das quais ele é responsável, mesmo
   quando ele pede as que abriu. Uma solicitação recém-aberta por ele, ainda sem
   responsável, some do quadro e da lista "abertas por mim"; as contagens de concluídas e
   canceladas abertas por ele ficam erradas; e a regra que deixa o operador cancelar a
   própria solicitação antes da triagem fica inalcançável pela tela.
2. O detalhe e o histórico não checam acesso: qualquer usuário autenticado lê qualquer
   solicitação pelo identificador. A listagem esconde o que o detalhe expõe.

**Senha e sessão (#98).**

3. A API aceita senha de 1 caractere na troca e na redefinição, e aceita criar usuário sem
   senha nenhuma num perfil que faz login. O frontend exige 8, mas quem chama a API direto
   não passa por ele.
4. Trocar ou redefinir a senha não encerra as sessões abertas: o acesso emitido antes
   continua valendo por até 24 horas e a renovação por até 7 dias. Depois de uma troca por
   suspeita de vazamento, quem tem a credencial antiga continua dentro.
5. O perfil do usuário viaja dentro da credencial de acesso. Rebaixar um administrador só
   vale por completo quando a credencial dele expira.

**Limites de texto (#109).**

6. A API não limita o tamanho dos textos. Um título de solicitação com 256 caracteres
   responde "Registro duplicado ou violação de integridade" (409), mandando o usuário
   procurar uma duplicata que não existe. Nos campos sem limite de armazenamento, um texto
   de qualquer tamanho é gravado.

## Objetivo

O operador enxerga as solicitações que abriu e as que estão com ele, e só essas, em qualquer
caminho da API. Senha fraca é recusada pela API, e trocar a senha ou o perfil vale de
imediato. Texto grande demais é recusado com uma mensagem que diz o limite.

## Fora de escopo

- Limitar o tamanho de página (rgm-backend#89): depende da paginação do frontend
  (rgm-frontend#114), que ainda não foi entregue.
- Mudar onde o frontend guarda as credenciais e como o tempo real se autentica
  (rgm-backend#93).
- Não revelar no login se a conta existe ou está inativa (rgm-backend#100).
- Restringir métricas, ranking e relatórios por perfil (rgm-backend#97).
- Tratamento genérico de erros de requisição que hoje viram 500 (rgm-backend#99).
- Regras de composição de senha (maiúscula, número, símbolo) e histórico de senhas.
- Mudar a regra de acesso às evidências, que já segue "abriu ou é responsável".
- Mudar o que gestor e administrador enxergam.
- Ajustes no frontend: texto do quadro vazio e marcação do card (rgm-frontend#122).
- Cortar ou migrar textos já gravados que passem dos limites novos.

## Personas e cenários de uso

- **Operador** abre uma solicitação e a encontra no quadro e em "abertas por mim" antes da
  triagem; consegue cancelá-la enquanto ninguém a assumiu.
- **Operador** não consegue ler, nem pelo endereço direto, uma solicitação que não abriu e
  que não está com ele.
- **Administrador** redefine a senha de um usuário por suspeita de vazamento e sabe que
  quem estava com a credencial antiga saiu.
- **Administrador** rebaixa um usuário de administrador para operador e a mudança vale na
  hora.
- **Qualquer usuário** que cola um texto longo demais recebe uma mensagem dizendo o limite,
  mesmo usando uma versão antiga da tela.

## Requisitos funcionais

### Visibilidade do operador

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | Um operador tem acesso a uma solicitação quando a abriu ou quando é responsável ativo por ela; gestor e administrador têm acesso a todas | obrigatório |
| RF-02 | A listagem de solicitações, para um operador, deve devolver exatamente as solicitações a que ele tem acesso (RF-01), com o total e a paginação contando só essas | obrigatório |
| RF-03 | Os filtros "aberta por" e "responsável" enviados por um operador devem restringir dentro do conjunto de RF-02, sem ampliá-lo | obrigatório |
| RF-04 | A consulta de uma solicitação pelo identificador deve recusar com "acesso negado" o operador que não tem acesso a ela | obrigatório |
| RF-05 | A consulta do histórico de uma solicitação deve seguir a mesma regra de RF-04 | obrigatório |
| RF-06 | O relatório em PDF de solicitações deve trazer, para um operador, só as solicitações a que ele tem acesso | obrigatório |
| RF-07 | A listagem, o detalhe e o histórico não devem mudar para gestor e administrador | obrigatório |

### Senha e sessão

| ID | Requisito | Prioridade |
|---|---|---|
| RF-08 | Criar usuário, trocar a própria senha e redefinir a senha de outro usuário devem recusar senha com menos de 8 caracteres, com mensagem que diz o mínimo | obrigatório |
| RF-09 | Criar usuário de perfil que faz login (operador, gestor, administrador) sem senha deve ser recusado; usuário externo continua podendo ser criado sem senha | obrigatório |
| RF-10 | Criar e editar usuário devem recusar e-mail informado que não tenha formato de e-mail | obrigatório |
| RF-11 | Depois que a senha de um usuário é trocada ou redefinida, toda credencial de acesso e de renovação emitida antes para ele deve ser recusada como "não autenticado" nas chamadas comuns, na abertura do tempo real e na renovação | obrigatório |
| RF-12 | A credencial emitida no login feito depois da troca ou da redefinição deve ser aceita | obrigatório |
| RF-13 | A permissão de cada chamada deve considerar o perfil atual do usuário, não o perfil que ele tinha quando a credencial foi emitida | obrigatório |

### Limites de texto

| ID | Requisito | Prioridade |
|---|---|---|
| RF-14 | A API deve recusar com "requisição inválida" todo texto acima do limite do campo, conforme a tabela de limites abaixo, com mensagem que diz o campo e o limite | obrigatório |
| RF-15 | Texto exatamente no limite deve ser aceito | obrigatório |
| RF-16 | Uma violação de unicidade real (código de modelo, e-mail ou nome de máquina repetido) deve continuar respondendo "conflito" | obrigatório |
| RF-17 | A mensagem "registro duplicado" não deve mais aparecer para texto grande demais | obrigatório |

Tabela de limites (em caracteres). São os mesmos que o frontend aplica desde a feature 007:

| Campo | Limite |
|---|---|
| Título da solicitação | 255 |
| Descrição da solicitação | 2000 |
| Comentário, motivo de cancelamento, motivo de devolução e comentário de encerramento | 2000 |
| Comentário do envio para validação | 1000 (já existe) |
| Código do modelo | 100 |
| Descrição do modelo | 255 |
| Máquina do modelo | 255 |
| Observações do modelo | 2000 |
| Código do modelo pretendido (solicitação de criação) | 50 |
| Máquina do modelo pretendido | 100 |
| Observações do modelo pretendido | 2000 |
| Nome da máquina | 255 |
| Nome do usuário | 255 |
| E-mail do usuário | 255 |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Custo da regra de visibilidade na listagem | 0 consultas a mais por item listado; no máximo 1 consulta adicional por chamada de listagem |
| RNF-02 | Custo da checagem de sessão | no máximo 1 leitura do usuário por chamada autenticada, a que já existe desde a feature 006 |
| RNF-03 | Compatibilidade do contrato | 0 campos, endpoints ou tipos de evento removidos ou renomeados |
| RNF-04 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado que entra na medição |
| RNF-05 | Fonte única dos limites | cada limite de texto e o mínimo da senha declarados em 1 lugar só no código de produção |
| RNF-06 | Mudança de perfil | vale em 0 chamadas de atraso: a primeira chamada depois da mudança já usa o perfil novo |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Visibilidade do operador, senha e sessão seguras e limites de texto

  # Visibilidade do operador

  Cenário: Operador vê a solicitação que abriu antes da triagem
    Dado que um operador abriu uma solicitação que está em "A Fazer" sem responsável
    Quando ele lista as solicitações
    Então a solicitação aparece na lista

  Cenário: Operador vê a solicitação da qual é responsável
    Dado que um gestor abriu uma solicitação e atribuiu um operador como responsável
    Quando esse operador lista as solicitações
    Então a solicitação aparece na lista

  Cenário: Operador não vê solicitação alheia na listagem
    Dado que existem 3 solicitações que um operador abriu, 2 das quais ele é responsável sem ter aberto e 4 sem relação com ele
    Quando ele lista as solicitações
    Então recebe 5 solicitações
    E o total informado é 5

  Cenário: Filtro "aberta por" restringe dentro do que o operador vê
    Dado que um operador abriu 3 solicitações e é responsável por outras 2
    Quando ele lista as solicitações abertas por ele mesmo
    Então recebe só as 3 que abriu

  Cenário: Filtro por outro usuário não amplia o que o operador vê
    Dado que outro usuário abriu 4 solicitações sem relação com um operador
    Quando o operador lista as solicitações abertas por esse outro usuário
    Então recebe uma lista vazia

  Cenário: Operador não lê solicitação alheia pelo identificador
    Dado uma solicitação que um operador não abriu e da qual não é responsável
    Quando ele consulta essa solicitação pelo identificador
    Então recebe "acesso negado"

  Cenário: Operador não lê o histórico de solicitação alheia
    Dado uma solicitação que um operador não abriu e da qual não é responsável
    Quando ele consulta o histórico dessa solicitação
    Então recebe "acesso negado"

  Cenário: Operador lê o detalhe e o histórico da solicitação que abriu
    Dado que um operador abriu uma solicitação
    Quando ele consulta a solicitação e o histórico dela
    Então recebe os dois

  Cenário: Relatório em PDF do operador
    Dado que existem solicitações que um operador abriu e solicitações sem relação com ele
    Quando ele gera o relatório em PDF de solicitações
    Então o relatório traz só as solicitações a que ele tem acesso

  Cenário: Gestor e administrador continuam vendo todas
    Dado que existem solicitações abertas por vários usuários
    Quando um gestor lista as solicitações e consulta qualquer uma pelo identificador
    Então recebe todas na lista
    E recebe o detalhe e o histórico de qualquer uma

  # Senha e sessão

  Esquema do Cenário: Senha curta é recusada
    Quando envio uma senha de 7 caracteres em "<operação>"
    Então recebo "requisição inválida"
    E a mensagem diz que o mínimo é de 8 caracteres

    Exemplos:
      | operação                    |
      | criar usuário               |
      | trocar a própria senha      |
      | redefinir a senha de outro  |

  Cenário: Senha de 8 caracteres é aceita
    Quando troco a minha senha por uma de 8 caracteres
    Então a senha é trocada

  Cenário: Usuário que faz login precisa de senha
    Quando crio um usuário de perfil "Operador" sem senha
    Então recebo "requisição inválida"

  Cenário: Usuário externo continua sem senha
    Quando crio um usuário de perfil "Externo" sem senha
    Então o usuário é criado

  Cenário: E-mail fora do formato é recusado
    Quando crio ou edito um usuário com o e-mail "fulano-sem-arroba"
    Então recebo "requisição inválida"

  Esquema do Cenário: Credencial antiga deixa de valer depois da troca de senha
    Dado que um usuário entrou e depois teve a senha "<mudança>"
    Quando a credencial emitida antes é usada em "<uso>"
    Então recebo "não autenticado"

    Exemplos:
      | mudança                         | uso                       |
      | trocada por ele                 | uma chamada comum         |
      | trocada por ele                 | a abertura do tempo real  |
      | trocada por ele                 | a renovação da credencial |
      | redefinida por um administrador | uma chamada comum         |
      | redefinida por um administrador | a abertura do tempo real  |
      | redefinida por um administrador | a renovação da credencial |

  Cenário: Login depois da troca funciona
    Dado que um usuário teve a senha redefinida
    Quando ele entra com a senha nova e faz uma chamada com a credencial recebida
    Então a chamada é aceita

  Cenário: Rebaixar o perfil vale na chamada seguinte
    Dado que um administrador entrou e depois teve o perfil alterado para "Operador"
    Quando ele chama uma operação de administração com a credencial que já tinha
    Então recebe "acesso negado"

  Cenário: Promover o perfil vale na chamada seguinte
    Dado que um operador entrou e depois teve o perfil alterado para "Gestor"
    Quando ele lista as solicitações com a credencial que já tinha
    Então recebe as solicitações de todos os usuários

  # Limites de texto

  Cenário: Título longo demais diz o limite
    Quando abro uma solicitação com título de 256 caracteres
    Então recebo "requisição inválida"
    E a mensagem cita o título e o limite de 255 caracteres
    Mas a mensagem não fala em registro duplicado

  Cenário: Título no limite é aceito
    Quando abro uma solicitação com título de 255 caracteres
    Então a solicitação é aberta

  Esquema do Cenário: Cada campo recusa um caractere acima do limite
    Quando envio "<campo>" com <tamanho> caracteres
    Então recebo "requisição inválida"
    E a mensagem cita o limite de <limite> caracteres

    Exemplos:
      | campo                                   | tamanho | limite |
      | descrição da solicitação                | 2001    | 2000   |
      | comentário                              | 2001    | 2000   |
      | motivo de cancelamento                  | 2001    | 2000   |
      | motivo de devolução                     | 2001    | 2000   |
      | comentário de encerramento              | 2001    | 2000   |
      | código do modelo                        | 101     | 100    |
      | descrição do modelo                     | 256     | 255    |
      | máquina do modelo                       | 256     | 255    |
      | observações do modelo                   | 2001    | 2000   |
      | código do modelo pretendido             | 51      | 50     |
      | máquina do modelo pretendido            | 101     | 100    |
      | observações do modelo pretendido        | 2001    | 2000   |
      | nome da máquina                         | 256     | 255    |
      | nome do usuário                         | 256     | 255    |
      | e-mail do usuário                       | 256     | 255    |

  Cenário: Duplicata de verdade continua sendo conflito
    Dado que existe um modelo com o código "MOD-001"
    Quando crio outro modelo com o código "MOD-001"
    Então recebo "conflito"
```

## Ambiguidades

1. **Regra de visibilidade (RF-01).** A #96 termina com "Decisão pendente: confirmar a regra
   acima antes de implementar".
   `[NECESSITA ESCLARECIMENTO: confirma que o operador vê a solicitação se a abriu ou se é responsável ativo, e que gestor e administrador veem todas?]`
2. **Resposta para solicitação alheia (RF-04 e RF-05).** A #96 pede "acesso negado" (403).
   Isso confirma a quem tenta que a solicitação existe; "não encontrado" (404) não confirma.
   `[NECESSITA ESCLARECIMENTO: solicitação alheia responde "acesso negado" ou "não encontrado"?]`
3. **Eventos em tempo real.** Hoje toda mudança de solicitação é enviada a todos os usuários
   conectados, operadores incluídos. Com a regra nova, o operador não lê a solicitação
   alheia, mas continua recebendo o aviso de que ela mudou.
   `[NECESSITA ESCLARECIMENTO: os eventos de tempo real entram nesta feature (operador só recebe evento de solicitação a que tem acesso) ou ficam para depois?]`
4. **Sessão de quem troca a própria senha (RF-11).** A regra derruba toda credencial emitida
   antes da troca, inclusive a da sessão em que o usuário trocou a senha: ele seria mandado
   de volta ao login logo depois de trocar.
   `[NECESSITA ESCLARECIMENTO: quem troca a própria senha continua na sessão atual ou entra de novo?]`
5. **Mudança de perfil e sessão (RF-13).** A #98 pede duas coisas: o perfil novo valer na
   chamada seguinte e as credenciais antigas serem recusadas na mudança de perfil. A primeira
   já resolve o rebaixamento; a segunda também desconecta o usuário a cada mudança de perfil.
   A spec está escrita só com a primeira.
   `[NECESSITA ESCLARECIMENTO: mudar o perfil de um usuário também deve desconectá-lo?]`

Decisões já tomadas, registradas aqui para não voltarem como dúvida:

- **Mínimo da senha: 8 caracteres.** Decisão do usuário em 2026-10-06, na feature 007 do
  frontend.
- **Limites de texto:** os da tabela, que o frontend já aplica desde o PR rgm-frontend#135.
  Onde o armazenamento não impõe limite, vale 2000.
- **Textos já gravados acima do limite** não são alterados. Consequência: editar uma
  solicitação ou um modelo antigo com texto acima do limite exige encurtar o texto para
  salvar.

## Métricas de sucesso

- A rgm-frontend#122 pode ser implementada: operador abre uma solicitação e a encontra no
  quadro e em "abertas por mim" antes da triagem.
- 0 respostas "registro duplicado" causadas por tamanho de texto.
- 0 senhas com menos de 8 caracteres aceitas pela API.
- Depois de uma redefinição de senha, 0 chamadas aceitas com credencial emitida antes dela.
