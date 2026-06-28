# Diagramas

Este arquivo reúne os diagramas principais (casos de uso, classes e sequência) e está alinhado com:

- RBAC com distinção entre **mover** (controle) e **executar** (atribuição)
- Perfil **EXTERNO** para prestadores de serviço (apenas para atribuição e histórico)
- Evidências em bucket público (MinIO/S3) usando URLs persistentes (sem expiração)
- Recalculo de `Modelo.temPendenciaAberta` via evento/listener ao atingir estado terminal
- Foto capa do Modelo (`fotoUrl`) atualizável por Gestor (e Administrador por herança) (upload novo ou reaproveitar evidência existente)
- Prestador externo representado como um Usuário no sistema (Gestor atua como seu procurador para movimentações)

## Casos de uso (completo)

```mermaid
flowchart LR
    %% Atores
    Op(["Operador"])
    Ge(["Gestor"])
    Ad(["Administrador"])
    Sys(["Sistema"])

    %% Relações de herança
    Ge -- extends --> Op
    Ad -- extends --> Ge

    subgraph RGM [Sistema RGM]
        direction TB

        %% Autenticação
        UC_Login([Logar no sistema])

        %% Solicitações (Kanban)
        UC_AbrirSol([Abrir solicitacao A_FAZER])
        UC_VerKanban([Ver Kanban de solicitacoes])
        UC_VerDetalhesSol([Ver detalhes e historico da solicitacao])
        UC_TriarAtribuir([Triar e atribuir A_FAZER -> EM_ANDAMENTO])
        UC_MoverKanban([Movimentar solicitacao no Kanban])
        UC_EnviarValidacao([Enviar para validacao EM_ANDAMENTO -> EM_VALIDACAO])
        UC_Devolver([Devolver para correcao EM_VALIDACAO -> EM_ANDAMENTO])
        UC_Encerrar([Encerrar EM_VALIDACAO -> CONCLUIDA ou CANCELADA])

        %% Evidencias (bucket público)
        UC_AnexarEvidencia([Anexar evidencias])
        UC_VisualizarEvidencia([Visualizar evidencias via URL persistente])

        %% Modelos
        UC_VerModelos([Ver lista e detalhes de Modelos])
        UC_GerenciarModelos([Gerenciar Modelos cadastrar editar desativar])
        UC_AtualizarFotoCapa([Atualizar foto capa do Modelo])

        %% Prestador externo
        UC_CadastrarPrestador([Cadastrar prestador externo])
        UC_Terceirizar([Atribuir a prestador e movimentar como procurador])

        %% Consistencia
        UC_RecalcularPendencia([Recalcular temPendenciaAberta do Modelo])

        %% Administracao
        UC_GerenciarUsuarios([Gerenciar usuarios])
        UC_ExcluirQualquer([Excluir permanentemente cards/registros])
    end

    %% Ligações Operador
    Op --> UC_Login
    Op --> UC_AbrirSol
    Op --> UC_VerKanban
    Op --> UC_VerDetalhesSol
    Op --> UC_EnviarValidacao
    Op --> UC_AnexarEvidencia
    Op --> UC_VisualizarEvidencia
    Op --> UC_VerModelos

    %% Ligações Gestor
    Ge --> UC_TriarAtribuir
    Ge --> UC_MoverKanban
    Ge --> UC_Devolver
    Ge --> UC_Encerrar
    Ge --> UC_GerenciarModelos
    Ge --> UC_AtualizarFotoCapa
    Ge --> UC_Terceirizar

    %% Ligações Administrador
    Ad --> UC_GerenciarUsuarios
    Ad --> UC_CadastrarPrestador
    Ad --> UC_ExcluirQualquer

    %% Sistema
    Sys --> UC_RecalcularPendencia
```

## Diagrama de classes (completo)

```mermaid
classDiagram
    direction LR

    class Usuario {
        +UUID id
        +string nome
        +string email
        +string senhaHash
        +PerfilUsuario perfil
        +boolean ativo
        +datetime criadoEm
        +datetime atualizadoEm
    }

    class Modelo {
        +UUID id
        +string codigo
        +int versao
        +string descricao
        +string observacoes
        +string fotoUrl
        +datetime fotoAtualizadaEm
        +string estadoAtualDescricao
        +datetime estadoAtualAtualizadoEm
        +boolean ativo
        +string maquina
        +boolean temPendenciaAberta
        +datetime criadoEm
        +datetime atualizadoEm
    }

    class Solicitacao {
        +UUID id
        +string titulo
        +string descricao
        +TipoSolicitacao tipo
        +StatusSolicitacao status
        +PrioridadeSolicitacao prioridade
        +UUID modeloId
        +UUID abertaPorUsuarioId
        +string comentarioFinal
        +datetime criadaEm
        +datetime atualizadaEm
        +datetime concluidaEm
        +datetime canceladaEm
    }

    class SolicitacaoAtribuicao {
        +UUID id
        +UUID solicitacaoId
        +UUID usuarioId
        +UUID atribuidoPorUsuarioId
        +datetime atribuidoEm
        +datetime removidoEm
    }

    class AtividadeSolicitacao {
        +UUID id
        +UUID solicitacaoId
        +TipoAtividadeSolicitacao tipo
        +StatusSolicitacao deStatus
        +StatusSolicitacao paraStatus
        +string comentario
        +UUID autorUsuarioId
        +datetime criadaEm
    }

    class Evidencia {
        +UUID id
        +string publicUrl
        +string mimeType
        +string nomeArquivo
        +int tamanhoBytes
        +UUID enviadaPorUsuarioId
        +datetime criadaEm
    }

    class SolicitacaoEvidencia {
        +UUID solicitacaoId
        +UUID evidenciaId
    }

    class EventoModelo {
        +UUID id
        +UUID modeloId
        +TipoEventoModelo tipo
        +string titulo
        +string descricao
        +string estadoModeloDescricao
        +boolean defineFotoCapa
        +UUID executadoPorUsuarioId
        +UUID solicitacaoRelacionadaId
        +datetime criadoEm
    }

    class EventoModeloEvidencia {
        +UUID eventoModeloId
        +UUID evidenciaId
    }

    class PerfilUsuario {
        <<enumeration>>
        OPERADOR
        GESTOR
        ADMINISTRADOR
        EXTERNO
    }

    class StatusSolicitacao {
        <<enumeration>>
        A_FAZER
        EM_ANDAMENTO
        EM_VALIDACAO
        CONCLUIDA
        CANCELADA
    }

    class PrioridadeSolicitacao {
        <<enumeration>>
        BAIXA
        MEDIA
        ALTA
        URGENTE
    }

    class TipoSolicitacao {
        <<enumeration>>
        REPARO
        INSPECAO
        REENGENHARIA
    }

    class TipoAtividadeSolicitacao {
        <<enumeration>>
        ABERTURA
        ATRIBUICAO
        MUDANCA_STATUS
        COMENTARIO
        EVIDENCIA_ADICIONADA
    }

    class TipoEventoModelo {
        <<enumeration>>
        MODIFICACAO
        INSPECAO
        REPARO
        AJUSTE
        MANUTENCAO
        OUTRO
    }

    Modelo "1" --> "0..*" Solicitacao : tem
    Solicitacao "1" --> "0..*" SolicitacaoAtribuicao : atribuicoes
    Solicitacao "1" --> "0..*" AtividadeSolicitacao : atividades
    Usuario "1" --> "0..*" Solicitacao : abre
    Usuario "1" --> "0..*" AtividadeSolicitacao : executa

    Solicitacao "1" --> "0..*" SolicitacaoEvidencia : anexos
    Evidencia "1" --> "0..*" SolicitacaoEvidencia : emSolicitacoes

    Modelo "1" --> "0..*" EventoModelo : timeline
    EventoModelo "1" --> "0..*" EventoModeloEvidencia : anexos
    Evidencia "1" --> "0..*" EventoModeloEvidencia : emEventos
```

## Sequencia 1: Solicitacao e evidencias (MinIO) com consistencia de pendencia

Este diagrama cobre RBAC (mover vs executar), evidencias via bucket público (URL persistente) e recalculo de `temPendenciaAberta` via evento.

```mermaid
sequenceDiagram
    autonumber
    actor OP as Operador
    actor GE as Gestor
    participant WEB as Frontend
    participant API as BackendAPI
    participant DB as PostgreSQL
    participant S3 as MinIO_S3
    participant EV as DomainEvents

    Note over OP,API: Abrir solicitacao A_FAZER
    OP->>WEB: Preenche dados
    WEB->>API: Criar solicitacao
    API->>API: Validar autenticacao
    API->>DB: INSERT Solicitacao status=A_FAZER
    API->>DB: INSERT Atividade ABERTURA
    API->>DB: UPDATE Modelo temPendenciaAberta=true
    API-->>WEB: 201

    Note over GE,API: Triar e atribuir A_FAZER para EM_ANDAMENTO
    GE->>WEB: Define prioridade e responsaveis
    WEB->>API: Atribuir solicitacao
    API->>API: Validar perfil Gestor
    alt Responsavel inclui Administrador
        API-->>WEB: 400 Regra de negocio
    else Sem responsaveis
        API-->>WEB: 400 Regra de negocio
    else OK
        API->>DB: UPDATE Solicitacao status=EM_ANDAMENTO prioridade
        API->>DB: INSERT SolicitacaoAtribuicao (1 ou mais)
        API->>DB: INSERT Atividades ATRIBUICAO e MUDANCA_STATUS
        API-->>WEB: 200
    end

    Note over OP,API: Anexar evidencia (upload)
    OP->>WEB: Seleciona foto
    WEB->>API: Upload evidencia
    API->>API: Validar acesso a solicitacao
    API->>API: Gerar publicUrl persistente
    API->>S3: PUT objeto
    alt S3 indisponivel
        API-->>WEB: 503
    else OK
        API->>DB: INSERT Evidencia publicUrl
        API->>DB: INSERT SolicitacaoEvidencia
        API->>DB: INSERT Atividade EVIDENCIA_ADICIONADA
        API-->>WEB: 201
    end

    Note over OP,API: Visualizar evidencias (URL persistente)
    WEB->>API: Buscar detalhes da solicitacao
    API->>API: Validar acesso
    API->>DB: SELECT Solicitacao e Evidencias (publicUrl)
    API-->>WEB: 200 (lista de URLs persistentes)

    Note over OP,API: Enviar para validacao EM_ANDAMENTO para EM_VALIDACAO
    WEB->>API: Enviar para validacao
    API->>API: Autorizar mover
    alt Perfil=OPERADOR e nao atribuido
        API-->>WEB: 403
    else Transicao invalida
        API-->>WEB: 409
    else OK
        API->>DB: UPDATE Solicitacao status=EM_VALIDACAO
        API->>DB: INSERT Atividade MUDANCA_STATUS
        API-->>WEB: 200
    end

    Note over GE,API: Validacao do Gestor (devolver ou encerrar)
    alt Devolver para correcao
        WEB->>API: Devolver com motivo
        API->>API: Validar perfil Gestor e motivo
        API->>DB: UPDATE Solicitacao status=EM_ANDAMENTO
        API->>DB: INSERT Atividade MUDANCA_STATUS e COMENTARIO
        API-->>WEB: 200
    else Encerrar
        WEB->>API: Encerrar com comentarioFinal
        API->>API: Validar perfil Gestor e comentario
        API->>DB: UPDATE Solicitacao status terminal e data
        API->>DB: INSERT Atividade MUDANCA_STATUS
        API->>EV: Publicar SolicitacaoFinalizadaEvent modeloId
        EV->>DB: Recalcular pendencia por query
        EV->>DB: UPDATE Modelo temPendenciaAberta
        API-->>WEB: 200
    end
```

## Sequencia 2: Atualizar foto capa do Modelo (upload ou evidencias existentes)

```mermaid
sequenceDiagram
    autonumber
    actor GE as Gestor
    participant WEB as Frontend
    participant API as BackendAPI
    participant DB as PostgreSQL
    participant EV as DomainEvents
    participant EV as DomainEvents
    participant S3 as MinIO_S3

    Note over GE,API: Carregar contexto do Modelo
    WEB->>API: Buscar detalhes do Modelo
    API->>API: Validar perfil Gestor
    API->>DB: SELECT Modelo e historico
    API-->>WEB: 200

    Note over GE,API: Atualizar foto capa
    alt Opcao A - Upload de nova foto
        GE->>WEB: Seleciona arquivo
        WEB->>API: Upload foto capa
        API->>API: Validar perfil Gestor
        API->>API: Gerar publicUrl de capa
        API->>S3: PUT objeto
        alt S3 indisponivel
            API-->>WEB: 503
        else OK
            API->>DB: INSERT Evidencia publicUrl
            API->>DB: UPDATE Modelo fotoUrl e fotoAtualizadaEm
            API->>DB: INSERT EventoModelo (defineFotoCapa=true)
            API->>DB: INSERT EventoModeloEvidencia
            API-->>WEB: 200
        end
    else Opcao B - Usar evidencia existente
        GE->>WEB: Seleciona evidencia existente
        WEB->>API: Definir capa por evidenciaId
        API->>API: Validar perfil Gestor
        API->>DB: Verificar evidencia pertence ao Modelo
        alt Evidencia nao pertence ao Modelo
            API-->>WEB: 400 Regra de negocio
        else OK
            API->>DB: UPDATE Modelo fotoUrl e fotoAtualizadaEm
            API->>DB: INSERT EventoModelo (defineFotoCapa=true)
            API->>DB: INSERT EventoModeloEvidencia
            API-->>WEB: 200
        end
    end
```

## Sequencia 3: Terceirizar servico (prestador externo) e gestor como procurador

```mermaid
sequenceDiagram
    autonumber
    actor GE as Gestor
    participant WEB as Frontend
    participant API as BackendAPI
    participant DB as PostgreSQL

    Note over GE,API: Prestador externo já cadastrado por Administrador (UC-11)

    Note over GE,API: Atribuir solicitacao ao prestador
    WEB->>API: Atribuir solicitacao a prestador
    API->>API: Validar perfil Gestor
    API->>DB: INSERT SolicitacaoAtribuicao usuarioId=prestador
    API->>DB: INSERT Atividade ATRIBUICAO
    API-->>WEB: 200

    Note over GE,API: Gestor movimenta status como procurador
    GE->>WEB: Recebe feedback do prestador (email/tel/etc)
    WEB->>API: Registrar atividade/comentario
    API->>DB: INSERT Atividade COMENTARIO autor=Gestor
    API-->>WEB: 201

    WEB->>API: Enviar para validacao
    API->>API: Autorizar mover (Gestor pode mover cards atribuídos a EXTERNO)
    API->>DB: UPDATE Solicitacao status=EM_VALIDACAO
    API->>DB: INSERT Atividade MUDANCA_STATUS
    API-->>WEB: 200

    Note over GE,API: Encerrar e recalcular pendencia do Modelo
    WEB->>API: Encerrar com comentarioFinal
    API->>API: Validar perfil Gestor
    API->>DB: UPDATE Solicitacao status terminal e data
    API->>DB: INSERT Atividade MUDANCA_STATUS
    API->>EV: Publicar SolicitacaoFinalizadaEvent modeloId
    EV->>DB: Recalcular pendencia por query
    EV->>DB: UPDATE Modelo temPendenciaAberta
    API-->>WEB: 200
```

## Sequencia 4: Administracao (Geral e Deleção Avançada)

Este fluxo isola as funcoes exclusivas do Perfil Administrador (invisível em atribuicoes da aplicacao), incluindo exclusões completas para o painel administrativo.

```mermaid
sequenceDiagram
    autonumber
    actor AD as Administrador
    participant WEB as Frontend
    participant API as BackendAPI
    participant DB as PostgreSQL

    Note over AD,API: Cadastrar novo Operador ou Gestor
    AD->>WEB: Preenche dados do novo usuario
    WEB->>API: POST /usuarios
    API->>API: Validar perfil Administrador
    API->>API: Hash da senha
    API->>DB: INSERT Usuario (perfil=OPERADOR ou GESTOR)
    API-->>WEB: 201

    Note over AD,API: Cadastrar Prestador Externo
    AD->>WEB: Preenche dados empresariais do prestador
    WEB->>API: POST /usuarios/externo
    API->>API: Validar perfil Administrador
    API->>DB: INSERT Usuario (perfil=EXTERNO, ativo=true)
    API-->>WEB: 201

    Note over AD,API: Excluir qualquer registro (Hard Delete)
    AD->>WEB: Confirma exclusao de card/modelo/usuario
    WEB->>API: DELETE /{recurso}/{id}
    API->>API: Validar perfil Administrador e integridade
    API->>DB: Exclusao fisica/cascata (DELETE ON CASCADE)
    API-->>WEB: 204
```
