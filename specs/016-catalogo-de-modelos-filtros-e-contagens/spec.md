# Especificação — Catálogo de modelos: filtros por tipo e máquina, busca única e contagens

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: pedido do usuário em 2026-11-11 (sem issue): o catálogo de modelos passa a ter botões
de **tipo** e de **máquina**, uma busca única e o modelo em destaque, carregando por blocos.
Consumidor: `rgm-frontend`, spec 019. Base: `develop` na `v1.7.0`.

## Problema

1. **A listagem não filtra por tipo.** `GET /api/modelos` aceita `ativo`, `codigo`, `maquina` e
   `descricao`; o tipo do modelo só volta na resposta. Filtrar por tipo exigiria baixar o
   catálogo inteiro, o que não combina com carga por blocos.
2. **Não existe busca única.** `codigo` e `descricao` são filtros separados que se somam: digitar
   o mesmo texto nos dois só acha modelo cujo código **e** descrição contenham o texto. Além
   disso a comparação é só por caixa, não por acento.
3. **O filtro de máquina é "contém".** "LINHA 2" também acha "LINHA 20". Botão de máquina precisa
   de igualdade.
4. **Não há como saber quantos modelos cada botão traria.** Sem a contagem, o botão "Resina"
   pode levar a uma tela vazia.
5. **A ordem é "mais recente primeiro".** Para achar um modelo no catálogo, o código em ordem é
   previsível; hoje o "primeiro" do filtro depende da data de criação.

**Custo de não resolver:** o catálogo teria de carregar todos os modelos no navegador para filtrar
e contar, ou mostrar botões que levam a resultado vazio.

## Objetivo

- A listagem aceita filtrar por tipo, por máquina exata e por uma busca única (código ou
  descrição, sem acento), e escolher a ordem.
- Uma consulta devolve quantos modelos cada tipo e cada máquina trariam, dados os outros filtros.
- Nada muda para quem não usa os parâmetros novos.

## Fora de escopo

- Filtrar ou contar por situação (ativo), por pendência ou por versão.
- Mudar o formato da resposta da listagem ou da galeria.
- Índice de texto, cache ou busca por similaridade; o catálogo é pequeno (centenas de modelos).
- Qualquer mudança no frontend (spec 019).
- Mudar o filtro `maquina` ("contém") nem o `descricao` e o `codigo` existentes: a tela de
  administração continua como está.

## Personas e cenários de uso

- **Operador, gestor e administrador na tela de catálogo:** escolhem tipo e máquina por botão,
  digitam parte do código ou da descrição e navegam pelos resultados.
- **Tela de administração de modelos (existente):** continua usando os filtros de hoje, sem mudança.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | `GET /api/modelos` aceita `tipo`: devolve só os modelos desse tipo (nome exato do tipo do catálogo, sem diferenciar caixa) | obrigatório |
| RF-02 | `GET /api/modelos` aceita `q`: devolve os modelos cujo código **ou** descrição contêm o texto, sem diferenciar caixa nem acento; texto com menos de 2 caracteres (depois de aparado) responde 400 | obrigatório |
| RF-03 | `GET /api/modelos` aceita `maquinaExata=true`: com ele, `maquina` compara por igualdade (sem diferenciar caixa nem acento) em vez de "contém"; sem ele, `maquina` segue "contém" | obrigatório |
| RF-04 | `GET /api/modelos` aceita `ordem`: `CRIACAO` (padrão, a de hoje: mais recente primeiro) ou `CODIGO` (código em ordem alfabética e, no mesmo código, versão maior primeiro, depois `id` para desempate); outro valor responde 400 | obrigatório |
| RF-05 | Todos os filtros se somam (`ativo`, `codigo`, `maquina`, `descricao`, `tipo`, `q`); dentro de `q`, código e descrição são alternativas | obrigatório |
| RF-06 | `GET /api/modelos/contagens` aceita os mesmos filtros e devolve `total` (todos os filtros aplicados), `porTipo` (cada tipo com a contagem que teria sem o filtro de tipo, mantidos os demais) e `porMaquina` (idem, sem o filtro de máquina); só entram valores com contagem maior que zero | obrigatório |
| RF-07 | `GET /api/modelos/contagens` não pagina nem ordena por pedido: `porTipo` e `porMaquina` saem da maior contagem para a menor, com empate em ordem alfabética | obrigatório |
| RF-08 | Sem nenhum parâmetro novo, a listagem devolve exatamente o que devolve hoje (conteúdo, ordem, paginação e erros) | obrigatório |
| RF-09 | A paginação continua valendo com os filtros novos: `total` de elementos e de páginas refletem o filtro | obrigatório |
| RF-10 | As rotas novas e os parâmetros novos têm a mesma regra de acesso da listagem atual | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Contagens sem carregar modelos em memória | `contagens` faz no máximo 3 consultas agregadas no banco, independentes do número de modelos |
| RNF-02 | Foto de capa sem N+1 | A listagem continua buscando as capas da página em 1 consulta |
| RNF-03 | Cobertura dos arquivos tocados | ≥ 95% de linhas por arquivo (Princípio 10), sem teste escrito só para cobrir linha |
| RNF-04 | Sem migração de esquema | 0 arquivos novos em `db/migration`; a busca reaproveita `rgm_sem_acento` da V11 |
| RNF-05 | Verificação | `make validate` com 0 falhas e lint limpo |

## Ordem de publicação (Princípio 9)

Parâmetros e rota novos são **aditivos**: nenhum campo, rota ou comportamento existente muda
(RF-08). O front atual continua funcionando. A spec 019 do `rgm-frontend` depende destes
parâmetros e é publicada **depois** do backend, sem comportamento de reserva.

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Catálogo de modelos — filtros e contagens

  Cenário: Filtrar por tipo
    Dado modelos dos tipos RESINA e COQUILHA_ACO
    Quando se lista com tipo=RESINA
    Então só modelos RESINA voltam

  Cenário: Busca única por código ou descrição
    Dado o modelo "FB-1001 — Carcaça de bomba" e o modelo "RS-4001 — Ponteira de válvula"
    Quando se lista com q=carcaca
    Então só o primeiro volta
    E com q=rs-40 só o segundo volta

  Cenário: Busca sem acento e sem caixa
    Dado o modelo "Suporte de câmbio"
    Quando se lista com q=CAMBIO
    Então o modelo volta

  Cenário: Busca curta demais
    Quando se lista com q=a
    Então a resposta é 400
    E com q="  a " também é 400

  Cenário: Máquina por igualdade
    Dado modelos das máquinas "LINHA 2" e "LINHA 20"
    Quando se lista com maquina=LINHA 2 e maquinaExata=true
    Então só os da "LINHA 2" voltam
    E sem maquinaExata voltam os dois (comportamento de hoje)

  Cenário: Filtros se somam
    Dado modelos de vários tipos, máquinas e situações
    Quando se lista com tipo, maquina exata, ativo=true e q
    Então só voltam os que atendem a todos

  Cenário: Ordem por código
    Dado os códigos "B-2" v1, "A-1" v1, "A-1" v2
    Quando se lista com ordem=CODIGO
    Então a ordem é "A-1" v2, "A-1" v1, "B-2" v1

  Cenário: Ordem inválida
    Quando se lista com ordem=ALEATORIA
    Então a resposta é 400

  Cenário: Sem parâmetros novos nada muda
    Dado o catálogo de hoje
    Quando se lista sem tipo, q, maquinaExata e ordem
    Então a resposta, a ordem e a paginação são as de antes

  Cenário: Paginação com filtro
    Dado 45 modelos RESINA e 10 de outros tipos
    Quando se lista com tipo=RESINA, size=20 e page=2
    Então vêm 5 modelos e totalElements=45

  Cenário: Contagens por tipo e por máquina
    Dado modelos de tipos e máquinas variados
    Quando se pede as contagens com maquina=FBOX e maquinaExata=true
    Então porTipo conta os modelos da FBOX por tipo
    E porMaquina conta por máquina sem aplicar o filtro de máquina, para cada botão de máquina saber o que traria
    E total é o número de modelos da FBOX

  Cenário: Contagem só traz valores com modelos
    Dado um tipo sem nenhum modelo no filtro atual
    Quando se pede as contagens
    Então esse tipo não aparece em porTipo

  Cenário: Mesmos filtros, mesma busca
    Quando se pede as contagens com q=carcaca
    Então os totais são os dos modelos que a listagem devolveria com q=carcaca
```

## Ambiguidades

- Nenhuma marca aberta.

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| Quantos modelos o catálogo tem? | Centenas; carga por blocos de 20 | 2026-11-11 |
| A máquina "contém" muda? | Não: `maquinaExata` é opt-in; a administração segue igual | 2026-11-11 |
| A ordem padrão muda? | Não: `CODIGO` é opt-in; o padrão continua "mais recente primeiro" | 2026-11-11 |

## Métricas de sucesso

- O catálogo monta botões e contagens com 1 requisição de contagens, sem baixar a lista inteira.
- 0 mudança de resposta para chamadas que não usam os parâmetros novos.
