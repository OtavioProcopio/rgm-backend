# Checklist — Visibilidade do operador, senha e sessão seguras e limites de texto / Requisitos

> Avalia a **qualidade da especificação**, não do código. `[x]` significa "requisito
> aprovado por revisor humano". O agente não se autoaprova.

## Completude

- [ ] RF-01 a RF-22 têm, cada um, ao menos um cenário que os exercita
- [ ] RNF-01 a RNF-06 têm número e unidade
- [ ] A tabela de limites cobre todo campo de texto que os formulários do frontend limitam desde a feature 007
- [ ] "Fora de escopo" cita o limite de tamanho de página (#89), o armazenamento das credenciais (#93), o login que revela conta (#100), as métricas por perfil (#97) e os erros 500 (#99)
- [ ] "Fora de escopo" diz o que o frontend precisa fazer depois (rgm-frontend#122 e guardar as credenciais da troca de senha)

## Clareza

- [ ] Nenhuma marca `[NECESSITA ESCLARECIMENTO]` restante
- [ ] A spec diz o que é "ter acesso" a uma solicitação numa frase só (RF-01)
- [ ] A spec diz o que acontece quando o operador envia filtro por outro usuário (RF-03)
- [ ] A spec diz quais perfis precisam de senha e qual não precisa (RF-09)
- [ ] A spec diz o que acontece com a sessão de quem troca a própria senha e com as outras sessões dele (RF-11, RF-19)
- [ ] A spec diz que mudar o perfil não desconecta (RF-22)
- [ ] Nenhum requisito cita classe, método, consulta ou biblioteca

## Consistência

- [ ] RF-11 ("toda credencial emitida antes é recusada") e RF-19 ("a sessão que trocou continua") não se contradizem
- [ ] RF-13 e RF-22 não se contradizem: perfil novo vale na chamada seguinte, com a mesma credencial
- [ ] RF-18 e RF-20 não se contradizem: o operador removido recebe só o evento da remoção
- [ ] A regra de acesso é a mesma na listagem, no detalhe, no histórico, no PDF e no tempo real
- [ ] As sete linhas da tabela `Esclarecimentos` batem com os requisitos e com a seção `Ambiguidades`
- [ ] Os limites da tabela são os mesmos que a feature 007 do frontend aplica
- [ ] RNF-03 é compatível com o Princípio 9 da constituição

## Testabilidade

- [ ] Há cenário para operador que abriu, que é responsável, que é os dois e que não é nenhum
- [ ] Há cenário de "acesso negado" no detalhe e no histórico, e de acesso permitido nos dois
- [ ] Há cenário de tempo real para operador com acesso, sem acesso, removido e recém-atribuído, e para gestor
- [ ] Há cenário de senha com 7 e com 8 caracteres
- [ ] Há cenário de credencial antiga nos três usos (chamada comum, tempo real, renovação) e nas duas mudanças (troca e redefinição)
- [ ] Há cenário para um caractere acima do limite em cada campo da tabela, e para texto exatamente no limite
- [ ] Há cenário que prova que duplicata de verdade continua sendo "conflito"
- [ ] RNF-01 e RNF-02 podem ser verificados contando consultas num teste
