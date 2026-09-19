# Steel Trade Monitor — Arquitetura e Estruturação do Projeto

> Documento de referência para construção manual do projeto.
> Objetivo duplo: gerar um item forte de portfólio **e** consolidar o vocabulário
> técnico por trás de coisas que você já faz na prática.

---

## Sumário

1. [O que é o projeto e o que ele prova](#1-o-que-é-o-projeto-e-o-que-ele-prova)
2. [Visão geral da arquitetura](#2-visão-geral-da-arquitetura)
3. [Glossário — os termos, sem enrolação](#3-glossário--os-termos-sem-enrolação)
4. [Estrutura de pastas do backend](#4-estrutura-de-pastas-do-backend)
5. [Estrutura de pastas do frontend](#5-estrutura-de-pastas-do-frontend)
6. [Modelo de dados](#6-modelo-de-dados)
7. [Fluxo de ponta a ponta](#7-fluxo-de-ponta-a-ponta)
8. [Decisões de arquitetura (ADRs)](#8-decisões-de-arquitetura-adrs)
9. [Roadmap por fases](#9-roadmap-por-fases)
10. [Armadilhas conhecidas](#10-armadilhas-conhecidas)
11. [Checklist "eu sei explicar isso?"](#11-checklist-eu-sei-explicar-isso)

---

## 1. O que é o projeto e o que ele prova

**O que é:** um sistema que coleta dados públicos de comércio exterior de aço,
logística ferroviária/portuária e preços de commodity, armazena esses dados em
banco próprio, e expõe relatórios analíticos via API consumida por um dashboard
Angular.

**O que ele prova para quem contrata:**

| Competência | Onde aparece no projeto |
|---|---|
| Integração com sistemas externos instáveis | Clients HTTP, retry, circuit breaker |
| Modelagem de dados analítica | Modelo dimensional (fato/dimensão) |
| Processamento em lote (batch) | Jobs agendados de ingestão |
| Idempotência e integridade | Upsert por chave natural, tabela de auditoria |
| API REST bem desenhada | Contrato versionado, paginação, OpenAPI |
| Frontend de dados | Dashboard com gráficos e filtros |
| Operação | Health check, logs estruturados, Docker |

Isso é bem mais do que um CRUD prova. E é o mesmo conjunto de problemas que você
resolve em sistema de logística industrial, só que numa versão pública que você
pode mostrar.

---

## 2. Visão geral da arquitetura

### 2.1 O desenho

```
┌──────────────────────────────────────────────────────────────┐
│  FONTES EXTERNAS (fora do seu controle)                      │
│  Comex Stat · ANTT · ANTAQ · brapi · AwesomeAPI · BCB        │
└───────────────────────────┬──────────────────────────────────┘
                            │  HTTP (JSON / CSV)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  BACKEND — Spring Boot                                       │
│                                                              │
│  ┌────────────────┐   agendado (@Scheduled)                  │
│  │ INGESTÃO       │   1 client por fonte                     │
│  │ (integration)  │   retry · circuit breaker · rate limit   │
│  └───────┬────────┘                                          │
│          │ grava payload cru                                 │
│          ▼                                                   │
│  ┌────────────────┐                                          │
│  │ STAGING        │   tabela de payload bruto (JSONB)        │
│  └───────┬────────┘                                          │
│          │ transforma / normaliza                            │
│          ▼                                                   │
│  ┌────────────────┐                                          │
│  │ MODELO         │   fatos + dimensões                      │
│  │ ANALÍTICO      │   upsert idempotente                     │
│  └───────┬────────┘                                          │
│          │ consultas agregadas                               │
│          ▼                                                   │
│  ┌────────────────┐                                          │
│  │ API REST       │   /api/v1/...  · DTOs · paginação        │
│  └───────┬────────┘                                          │
└──────────┼───────────────────────────────────────────────────┘
           │ HTTP/JSON
           ▼
┌──────────────────────────────────────────────────────────────┐
│  FRONTEND — Angular                                          │
│  services (HttpClient) → state → componentes → gráficos      │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 A regra de ouro

**O navegador nunca fala com a fonte externa.** Sempre passa pelo seu backend.

Três motivos, e todos os três são resposta de entrevista:

1. **CORS** — a maioria das APIs de governo não libera chamada de navegador.
2. **Cota** — se cada usuário chama a API externa direto, sua cota gratuita
   evapora. Com o backend no meio, mil usuários geram uma chamada só.
3. **Transformação** — o dado bruto do Comex Stat não é o dado que a tela quer.
   Alguém tem que traduzir, e esse alguém tem banco e CPU.

### 2.3 Por que três camadas de dados (bruto → staging → analítico)

Muita gente vai direto da API externa para a tabela final. Funciona até o dia em
que o número não bate e não há como saber se o erro foi da fonte ou da sua
transformação. Guardando o payload cru você consegue:

- **Reprocessar** sem gastar cota externa.
- **Auditar** — "o MDIC mandou isso, eu transformei naquilo".
- **Corrigir bug de transformação retroativamente**, sem perder histórico.

---

## 3. Glossário — os termos, sem enrolação

Organizado por área. A definição vem primeiro, o exemplo dentro deste projeto
vem em seguida — é o exemplo que fixa.

### 3.1 Dados e processamento

**ETL / ELT**
*Extract, Transform, Load.* O processo de tirar dado de um lugar, ajustar o
formato e gravar em outro. ELT inverte a ordem: carrega cru primeiro,
transforma depois, dentro do banco.
→ *Aqui:* você faz ELT. Baixa o JSON do Comex Stat (Extract), grava cru
(Load), depois transforma em fato/dimensão (Transform).

**Staging (área de estágio)**
Zona de pouso do dado cru, antes de qualquer tratamento. Não tem regra de
negócio, não tem validação bonita. É o "cheguei, tô aqui".
→ *Aqui:* tabela `stg_coleta_bruta` com o JSON como veio.

**Batch (lote)**
Processamento de um conjunto grande de registros de uma vez, em horário
definido, em oposição a processar um por um conforme chegam.
→ *Aqui:* o job diário do Comex Stat processa milhares de linhas de uma vez.

**Idempotência**
Propriedade de uma operação que pode ser executada várias vezes produzindo
sempre o mesmo resultado final. Rodar duas vezes = rodar uma vez.
→ *Aqui:* é a propriedade mais importante do projeto. Se o job rodar duas vezes
no mesmo dia, os números não podem dobrar. Sem idempotência, todo pipeline de
dados vira mentira.

**Upsert**
*Update + Insert.* "Se já existe, atualiza; se não existe, insere." Em SQL
padrão é o comando `MERGE`; no PostgreSQL, `INSERT ... ON CONFLICT DO UPDATE`.
→ *Aqui:* é o mecanismo que garante a idempotência acima.

**Chave natural × chave substituta (surrogate key)**
Chave natural é a combinação de campos que já identifica o registro no mundo
real. Chave substituta é um `id` sequencial que você inventou, sem significado.
→ *Aqui:* a chave natural do fato de comércio exterior é
`(ano, mês, ncm, país, uf, via)`. A chave substituta é o `id` da tabela. Você
usa a substituta como PK e a natural como índice único — é ela que o upsert usa
para decidir se atualiza ou insere.

**Carga incremental × carga full**
Full recarrega tudo, sempre. Incremental traz só o que mudou desde a última vez.
→ *Aqui:* o Comex Stat revisa meses passados, então "só o mês novo" é
armadilha. O padrão sensato é recarregar uma janela móvel — os últimos 6 meses,
por exemplo — e confiar no upsert para reconciliar.

**Watermark (marca d'água)**
O ponteiro que registra até onde você já processou. É o que torna a carga
incremental possível.
→ *Aqui:* campo na tabela de controle guardando o último período carregado.

**Backfill**
Carga histórica inicial, para preencher o passado antes de o sistema entrar em
regime.
→ *Aqui:* trazer 2019–2026 de uma vez, na primeira execução. Faça isso como
comando manual separado, nunca no job automático.

**Grão (grain)**
O nível de detalhe de uma linha da tabela de fato. Definir o grão é a primeira
decisão de qualquer modelagem analítica, e mudar depois é caro.
→ *Aqui:* "uma linha = um mês, um NCM, um país de destino, uma UF de origem,
uma via de transporte". Escreva isso no README. É a frase que mostra que você
entende modelagem.

**Tabela fato × tabela dimensão**
Fato guarda medições numéricas (o que se soma: peso, valor). Dimensão guarda
descrição (país, produto, tempo — o "por quê" e o "onde" da medição).
→ *Aqui:* `fato_comercio_exterior` tem kg e dólares; `dim_pais`,
`dim_ncm`, `dim_tempo` têm os nomes e classificações.

**Modelo estrela (star schema)**
Desenho onde uma tabela de fato central se liga direto a várias dimensões, cada
uma a uma tabela só. É o oposto da normalização máxima, e é proposital: menos
joins, consulta analítica mais rápida.
→ *Aqui:* é o modelo que você vai usar. O nome vem do desenho: o fato no meio,
as dimensões em volta, parecendo uma estrela.

**Normalização × desnormalização**
Normalizar é eliminar redundância (cada dado em um lugar só) — ótimo para
sistema transacional, que escreve muito. Desnormalizar é aceitar repetição para
ler mais rápido — ótimo para análise, que lê muito e escreve pouco.
→ *Aqui:* o banco transacional que você conhece do trabalho é normalizado. Este
é desnormalizado de propósito, e saber justificar essa escolha vale muito.

**Cardinalidade**
Quantidade de valores distintos em uma coluna. Coluna de alta cardinalidade
(muitos valores diferentes, como NCM) se beneficia de índice; coluna de baixa
cardinalidade (como um flag sim/não) geralmente não.
→ *Aqui:* orienta quais índices criar.

### 3.2 Arquitetura de aplicação

**Arquitetura em camadas**
Separação vertical: controller → service → repository. Cada camada só conhece a
de baixo. É o padrão que você já usa em Spring Boot.

**Arquitetura hexagonal / ports and adapters**
Evolução da anterior. O núcleo (regra de negócio) não conhece nada de fora. Tudo
que é externo — banco, API de terceiro, fila — entra por uma **porta**
(interface definida pelo núcleo) implementada por um **adaptador** (a classe
concreta que fala HTTP ou JDBC).
→ *Aqui:* `ComexStatGateway` é a porta (interface). `ComexStatHttpClient` é o
adaptador. O service depende da interface, não do client. Trocar a fonte de
dados não toca no service.

**Inversão de dependência (o D do SOLID)**
Módulo de alto nível não depende de módulo de baixo nível; ambos dependem de
abstração. É o princípio que justifica o parágrafo acima.
→ *Aqui:* na prática, é `@Autowired` de uma interface e não de uma classe
concreta. Você provavelmente já faz isso — o nome do que você faz é esse.

**Anti-corruption layer (camada anticorrupção)**
Camada de tradução que impede que o modelo esquisito de um sistema externo
contamine o seu modelo interno.
→ *Aqui:* o Comex Stat devolve campos como `coAno`, `vlFob`, `noPaispt`. Esses
nomes **não** podem vazar para dentro do seu domínio. O mapper traduz para
`ano`, `valorFob`, `nomePais` na fronteira. Quando a API externa mudar (e vai),
você conserta um arquivo só.

**DTO (Data Transfer Object)**
Objeto cuja única função é carregar dados entre camadas ou pela rede. Sem
comportamento, sem regra.
→ *Aqui:* você vai ter três famílias distintas, e misturá-las é erro clássico:
- `*ExternalResponse` — o formato da API externa
- `*Entity` — o formato do banco (JPA)
- `*Response` / `*Request` — o formato do seu contrato REST

**Entity**
Classe mapeada para uma tabela do banco, gerenciada pelo ORM (JPA/Hibernate).

**Mapper**
Classe que converte entre as famílias acima. Pode ser manual ou via MapStruct.
Comece manual: é mais código, mas você enxerga o que está acontecendo.

**Repository**
Abstração de acesso a dados. Esconde o SQL de quem chama.

**Service**
Onde mora a regra de negócio e a orquestração. Se seu controller tem `if`, a
regra está no lugar errado.

**Contrato de API**
A promessa pública que sua API faz: rotas, parâmetros, formato de resposta,
códigos de erro. Uma vez publicado, quebrar isso quebra o cliente.
→ *Aqui:* daí o prefixo `/api/v1/`. Quando precisar mudar de forma incompatível,
nasce o `/v2` e o `/v1` continua vivo por um tempo.

**OpenAPI / Swagger**
Especificação padronizada que descreve o contrato da API, em formato que gera
documentação navegável automaticamente.
→ *Aqui:* springdoc-openapi. Um Swagger público é o que faz um recrutador
conseguir testar seu projeto sem clonar nada.

### 3.3 Resiliência e operação

**Retry com backoff exponencial**
Tentar de novo depois de uma falha, esperando cada vez mais entre tentativas
(1s, 2s, 4s, 8s...). O crescimento evita que sua aplicação martele um serviço
que já está sofrendo.

**Jitter**
Aleatoriedade somada ao tempo de espera do retry. Sem jitter, se cem clientes
falharem juntos, todos tentam de novo no mesmo instante e derrubam o serviço de
novo. Com jitter, eles se espalham.

**Circuit breaker (disjuntor)**
Componente que "abre o circuito" após N falhas seguidas e passa a rejeitar
chamadas imediatamente, sem nem tentar a rede, por um tempo. Depois entra em
estado *half-open*, deixa passar uma chamada de teste, e fecha se der certo.
→ *Aqui:* se o Comex Stat cair, seu job falha rápido em vez de travar
segurando threads. Biblioteca: Resilience4j.

**Fallback**
O que fazer quando a chamada falha ou o circuito está aberto.
→ *Aqui:* servir o último dado do banco e marcar como defasado na tela. Isso é
**degradação graciosa** — o sistema piora sem morrer.

**Rate limit**
Teto de requisições por período imposto pelo provedor. Estourar geralmente
devolve HTTP 429.
→ *Aqui:* determina a frequência dos seus jobs. Documente o limite de cada
fonte na config.

**Cache com TTL**
Guardar a resposta em memória por um tempo (*time to live*), servindo cópias em
vez de refazer o trabalho.
→ *Aqui:* Caffeine, com TTL de 60s no câmbio e de horas nos agregados pesados.

**Dead letter**
Registro que falhou o processamento e foi para um canto separado, para análise
posterior, em vez de travar a fila ou ser perdido silenciosamente.
→ *Aqui:* linha de staging marcada com `status = ERRO` e a mensagem da exceção.

**Observabilidade**
Capacidade de entender o que está acontecendo dentro do sistema olhando de fora.
Três pilares: **logs** (o que aconteceu), **métricas** (quanto/quantas vezes),
**traces** (o caminho de uma requisição).
→ *Aqui:* Spring Boot Actuator dá health check e métricas de graça.

**Log estruturado**
Log em formato de dados (JSON) com campos nomeados, em vez de frase solta. Dá
para filtrar e agregar.
→ *Ruim:* `Carregou 5000 registros`
→ *Bom:* `{"evento":"ingestao_concluida","fonte":"comexstat","registros":5000,"duracaoMs":8200}`

**Health check**
Endpoint que responde se a aplicação está viva e se suas dependências estão OK.
Plataformas de deploy usam isso para decidir se reiniciam seu container.

**Migration**
Script versionado que altera o esquema do banco, aplicado em ordem e registrado
numa tabela de controle. Você já faz isso na mão no trabalho — Flyway é a
ferramenta que automatiza e garante que a ordem nunca se perca.

### 3.4 Frontend e transporte

**Polling**
O cliente pergunta repetidamente "mudou alguma coisa?". Simples, funciona em
qualquer lugar, desperdiça requisição.

**SSE (Server-Sent Events)**
Conexão HTTP que fica aberta e pela qual o **servidor empurra** mensagens para o
cliente. Uma via só (servidor → cliente), e é exatamente o que um dashboard
precisa. Muito mais simples que WebSocket.

**WebSocket**
Canal bidirecional persistente. Necessário quando o cliente também precisa
empurrar dados (chat, edição colaborativa). Para dashboard, é canhão para matar
mosca — mas saber **por que** você não usou é tão valioso quanto usar.

**Standalone component (Angular)**
Componente que declara suas próprias dependências e dispensa `NgModule`. É o
padrão moderno do Angular; se você aprendeu na era dos módulos, é a maior
mudança de hábito.

**Lazy loading**
Carregar o código de uma rota só quando o usuário navega até ela, em vez de
tudo no primeiro acesso. Deixa o carregamento inicial leve.

**Interceptor (Angular)**
Função que intercepta toda requisição HTTP que sai e toda resposta que volta.
Lugar certo para header comum, spinner global e tratamento centralizado de erro.

**Signals × RxJS**
Signals são o modelo reativo novo do Angular, baseado em valor: você lê o valor
direto e a tela reage à mudança. RxJS é baseado em fluxo de eventos ao longo do
tempo, e continua sendo o certo para HTTP, debounce e cancelamento.
→ *Aqui:* HTTP e filtros com debounce em RxJS; estado da tela em signals.

**Debounce**
Esperar o usuário parar de digitar/mexer antes de disparar a ação. Sem isso,
cada tecla no filtro vira uma requisição.

**Barrel file**
Um `index.ts` que reexporta vários arquivos de uma pasta, para encurtar imports.
Útil, mas use com moderação: em excesso cria dependência circular.

---

## 4. Estrutura de pastas do backend

Organização **por feature** (vertical), não por tipo técnico (horizontal).
Ou seja: `comexstat/` contendo controller, service e repository juntos, em vez
de `controllers/` com todos os controllers do sistema. Feature-first é mais
fácil de navegar e revela melhor os limites de cada domínio.

```
steel-trade-monitor-api/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/br/com/emanuel/steeltrade/
    │   │   │
    │   │   ├── SteelTradeApplication.java
    │   │   │
    │   │   ├── shared/                     ← infra transversal
    │   │   │   ├── config/
    │   │   │   │   ├── WebClientConfig.java
    │   │   │   │   ├── CacheConfig.java
    │   │   │   │   ├── SchedulingConfig.java
    │   │   │   │   ├── CorsConfig.java
    │   │   │   │   └── OpenApiConfig.java
    │   │   │   ├── exception/
    │   │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   ├── ExternalSourceException.java
    │   │   │   │   └── ApiErrorResponse.java
    │   │   │   ├── pagination/
    │   │   │   └── util/
    │   │   │
    │   │   ├── ingestion/                  ← tudo que fala com o mundo externo
    │   │   │   ├── core/
    │   │   │   │   ├── IngestionJob.java           (interface comum)
    │   │   │   │   ├── IngestionRunner.java        (orquestra + registra execução)
    │   │   │   │   ├── IngestionLog.java           (entity de controle)
    │   │   │   │   └── IngestionLogRepository.java
    │   │   │   ├── staging/
    │   │   │   │   ├── RawPayload.java             (entity de staging)
    │   │   │   │   └── RawPayloadRepository.java
    │   │   │   ├── comexstat/
    │   │   │   │   ├── ComexStatGateway.java       ← PORTA (interface)
    │   │   │   │   ├── ComexStatHttpClient.java    ← ADAPTADOR
    │   │   │   │   ├── ComexStatIngestionJob.java
    │   │   │   │   ├── ComexStatMapper.java        ← anti-corruption layer
    │   │   │   │   └── dto/
    │   │   │   │       ├── ComexStatQueryRequest.java
    │   │   │   │       └── ComexStatExternalResponse.java
    │   │   │   ├── exchange/                       (câmbio)
    │   │   │   ├── quotes/                         (cotações B3)
    │   │   │   └── rail/                           (ANTT)
    │   │   │
    │   │   ├── warehouse/                  ← modelo analítico
    │   │   │   ├── dimension/
    │   │   │   │   ├── Country.java
    │   │   │   │   ├── Ncm.java
    │   │   │   │   ├── FederativeUnit.java
    │   │   │   │   ├── TransportMode.java
    │   │   │   │   └── repository/
    │   │   │   ├── fact/
    │   │   │   │   ├── TradeFact.java
    │   │   │   │   ├── TradeFactRepository.java
    │   │   │   │   └── TradeFactUpsertRepository.java   (SQL nativo)
    │   │   │   └── loader/
    │   │   │       ├── TradeFactLoader.java        (staging → fato)
    │   │   │       └── DimensionSyncService.java
    │   │   │
    │   │   ├── analytics/                  ← consultas de negócio
    │   │   │   ├── TradeAnalyticsController.java
    │   │   │   ├── TradeAnalyticsService.java
    │   │   │   ├── query/
    │   │   │   │   ├── TradeQueryRepository.java
    │   │   │   │   └── projection/                 (interfaces de projeção)
    │   │   │   └── dto/
    │   │   │       ├── TradeFilterRequest.java
    │   │   │       ├── TimeSeriesPointResponse.java
    │   │   │       ├── CountryRankingResponse.java
    │   │   │       └── UnitPriceResponse.java
    │   │   │
    │   │   ├── market/                     ← câmbio e cotações (dado "vivo")
    │   │   │   ├── MarketController.java
    │   │   │   ├── MarketService.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── export/                     ← geração de CSV/XLSX
    │   │   │   ├── ReportExportController.java
    │   │   │   └── CsvWriterService.java
    │   │   │
    │   │   └── status/                     ← "quando atualizou pela última vez"
    │   │       ├── DataFreshnessController.java
    │   │       └── DataFreshnessService.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__staging_e_controle.sql
    │           ├── V2__dimensoes.sql
    │           ├── V3__fato_comercio_exterior.sql
    │           ├── V4__indices_analiticos.sql
    │           └── V5__seed_dimensoes_estaticas.sql
    │
    └── test/
        └── java/br/com/emanuel/steeltrade/
            ├── ingestion/comexstat/
            │   ├── ComexStatHttpClientTest.java     (com MockWebServer)
            │   └── ComexStatMapperTest.java
            ├── warehouse/loader/
            │   └── TradeFactLoaderIdempotencyTest.java   ← o teste mais importante
            └── analytics/
                └── TradeAnalyticsServiceTest.java
```

### Por que `ingestion` e `warehouse` são separados

Porque são responsabilidades com ritmos diferentes. A ingestão muda quando a
API externa muda. O warehouse muda quando a pergunta de negócio muda. Se
estiverem no mesmo pacote, toda alteração da API externa te obriga a mexer no
modelo analítico — e é aí que projetos apodrecem.

O ponto de contato entre os dois é a tabela de staging. Um escreve, o outro lê.
Essa fronteira é o coração do desenho.

---

## 5. Estrutura de pastas do frontend

```
steel-trade-monitor-web/
├── angular.json
├── package.json
├── Dockerfile
└── src/
    ├── environments/
    │   ├── environment.ts
    │   └── environment.prod.ts
    ├── styles/
    │   ├── _tokens.scss          (cores, espaçamentos, tipografia)
    │   └── styles.scss
    └── app/
        ├── app.config.ts
        ├── app.routes.ts
        │
        ├── core/                 ← singletons: uma instância na aplicação toda
        │   ├── http/
        │   │   ├── api-base.service.ts
        │   │   ├── error.interceptor.ts
        │   │   └── loading.interceptor.ts
        │   ├── services/
        │   │   ├── trade-analytics.service.ts
        │   │   ├── market.service.ts
        │   │   └── data-freshness.service.ts
        │   └── models/           ← interfaces espelhando os DTOs do backend
        │       ├── trade-filter.model.ts
        │       ├── time-series-point.model.ts
        │       └── country-ranking.model.ts
        │
        ├── shared/               ← reutilizável e burro (sem regra de negócio)
        │   ├── components/
        │   │   ├── kpi-card/
        │   │   ├── data-table/
        │   │   ├── empty-state/
        │   │   ├── loading-skeleton/
        │   │   └── freshness-badge/
        │   ├── pipes/
        │   │   ├── compact-number.pipe.ts
        │   │   └── tonnage.pipe.ts
        │   └── directives/
        │
        └── features/             ← uma pasta por tela, com lazy loading
            ├── dashboard/
            │   ├── dashboard.routes.ts
            │   ├── dashboard.page.ts
            │   ├── components/
            │   │   ├── market-ticker/
            │   │   ├── trade-volume-chart/
            │   │   └── top-destinations/
            │   └── state/
            │       └── dashboard.store.ts        (signals)
            ├── trade-explorer/
            │   ├── trade-explorer.page.ts
            │   ├── components/
            │   │   ├── filter-panel/
            │   │   └── result-grid/
            │   └── state/
            └── reports/
                ├── reports.page.ts
                └── components/
```

### A divisão core / shared / features

- **core** — instanciado uma vez, injetado em qualquer lugar: serviços HTTP,
  interceptors, configuração. Nunca contém componente visual.
- **shared** — peças reutilizáveis e sem opinião de negócio. Um `kpi-card` não
  sabe se está mostrando tonelada ou dólar; recebe tudo por `@Input`.
- **features** — uma pasta por tela/domínio, carregada sob demanda. Aqui mora a
  regra específica.

Regra de dependência: `features` pode importar de `core` e `shared`;
`shared` pode importar de `core`; **`core` não importa de ninguém**. Se você se
pegar importando de `features` dentro de `shared`, a peça não era compartilhada.

---

## 6. Modelo de dados

### 6.1 Camada de staging e controle

**`stg_coleta_bruta`** — o payload como veio, sem tratamento.

| Coluna | Tipo | Papel |
|---|---|---|
| `id` | BIGSERIAL | PK substituta |
| `fonte` | VARCHAR | COMEXSTAT, ANTT, BRAPI... |
| `endpoint` | VARCHAR | qual rota foi chamada |
| `parametros` | JSONB | o filtro usado na chamada |
| `payload` | JSONB | resposta crua |
| `status_http` | INT | 200, 429, 500... |
| `status_processamento` | VARCHAR | PENDENTE, PROCESSADO, ERRO |
| `mensagem_erro` | TEXT | preenchido quando falha (dead letter) |
| `coletado_em` | TIMESTAMPTZ | quando chegou |
| `processado_em` | TIMESTAMPTZ | quando virou fato |

**`ctl_execucao_ingestao`** — histórico de cada rodada de job.

| Coluna | Tipo | Papel |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `fonte` | VARCHAR | qual job |
| `iniciado_em` / `finalizado_em` | TIMESTAMPTZ | duração |
| `status` | VARCHAR | SUCESSO, FALHA, PARCIAL |
| `registros_lidos` / `registros_gravados` | INT | volumetria |
| `periodo_referencia` | VARCHAR | a janela carregada (watermark) |
| `mensagem` | TEXT | erro, se houve |

Essa segunda tabela alimenta a tela de "última atualização por fonte". Poucas
linhas de código, e é o detalhe que faz o projeto parecer sistema de verdade.

### 6.2 Dimensões

**`dim_tempo`** — `id`, `ano`, `mes`, `trimestre`, `ano_mes` (`2026-03`),
`primeiro_dia`.
Parece exagero criar tabela para isso, mas é padrão em modelo dimensional:
permite agrupar por trimestre ou semestre sem espalhar função de data por todo
canto do SQL.

**`dim_ncm`** — `id`, `codigo_ncm` (8 dígitos), `descricao`, `sh4`, `sh6`,
`capitulo`, `unidade_estatistica`.
Capítulo 72 = ferro fundido, ferro e aço. Capítulo 73 = obras de ferro/aço.
É a dimensão que define o recorte do projeto.

**`dim_pais`** — `id`, `codigo`, `nome_pt`, `nome_en`, `bloco_economico`,
`iso_alpha3`.
Guarde o ISO alpha-3: é o que os mapas de biblioteca de gráfico esperam.

**`dim_uf`** — `id`, `sigla`, `nome`, `regiao`.

**`dim_via_transporte`** — `id`, `codigo`, `descricao` (marítima, ferroviária,
rodoviária, aérea...).
Dimensão pequena, mas é ela que dá o ângulo logístico do dashboard.

### 6.3 Tabela fato

**`fato_comercio_exterior`**

| Coluna | Tipo | Papel |
|---|---|---|
| `id` | BIGSERIAL | PK substituta |
| `fluxo` | VARCHAR(6) | EXPORT / IMPORT |
| `dim_tempo_id` | BIGINT | FK |
| `dim_ncm_id` | BIGINT | FK |
| `dim_pais_id` | BIGINT | FK |
| `dim_uf_id` | BIGINT | FK |
| `dim_via_id` | BIGINT | FK |
| `kg_liquido` | NUMERIC(18,3) | **medida** |
| `valor_fob_usd` | NUMERIC(18,2) | **medida** |
| `quantidade_estatistica` | NUMERIC(18,3) | **medida** |
| `carregado_em` | TIMESTAMPTZ | trilha |

**Restrição única sobre a chave natural** — `(fluxo, dim_tempo_id, dim_ncm_id,
dim_pais_id, dim_uf_id, dim_via_id)`. Essa constraint é o alicerce do upsert e
o que impede duplicação em reprocessamento. É a linha de SQL mais importante do
projeto inteiro.

**Medida derivada:** preço médio por tonelada = `valor_fob_usd / (kg_liquido / 1000)`.
Não armazene esse valor — calcule na consulta. Guardar valor derivado te obriga
a recalcular tudo quando a fórmula mudar, e cria risco de o armazenado
divergir do real.

### 6.4 Fato de mercado

**`fato_cotacao`** — `id`, `ativo`, `tipo` (ACAO, CAMBIO, COMMODITY),
`preco`, `moeda`, `variacao_percentual`, `coletado_em`.
Grão diferente do outro fato: aqui é um ponto no tempo, não um mês agregado.
Fatos com grãos diferentes **não** vivem na mesma tabela — misturar é o erro
mais comum em modelagem dimensional.

### 6.5 Índices

Além das PKs e da constraint única:

- índice em `(dim_tempo_id, dim_ncm_id)` — série temporal, a consulta mais frequente
- índice em `(dim_pais_id, dim_tempo_id)` — ranking de destinos
- índice em `stg_coleta_bruta(status_processamento, fonte)` — busca do que falta processar
- índice em `fato_cotacao(ativo, coletado_em DESC)` — última cotação de um ativo

Não crie índice por precaução. Todo índice acelera leitura e atrasa escrita.
Crie quando o `EXPLAIN` mostrar varredura sequencial em tabela grande — e
guarde o antes/depois para o README.

---

## 7. Fluxo de ponta a ponta

Acompanhe um dado do MDIC até o pixel na tela:

```
1.  @Scheduled dispara ComexStatIngestionJob às 03:00
         ↓
2.  IngestionRunner abre registro em ctl_execucao_ingestao (status=EM_ANDAMENTO)
         ↓
3.  ComexStatHttpClient monta o POST com a janela móvel (últimos 6 meses,
    capítulos 72 e 73) e chama a API
    → protegido por retry + circuit breaker
    → se falhar de vez: status=FALHA, log estruturado, job encerra sem quebrar nada
         ↓
4.  Resposta crua vira linha em stg_coleta_bruta (status=PENDENTE)
    → a partir daqui, a fonte externa não é mais necessária
         ↓
5.  TradeFactLoader lê o que está PENDENTE
         ↓
6.  DimensionSyncService garante que país/NCM/UF/via existem
    (upsert nas dimensões primeiro — FK exige isso)
         ↓
7.  ComexStatMapper traduz nomes externos → modelo interno
    (anti-corruption layer: coAno → ano, vlFob → valorFob)
         ↓
8.  Upsert em lote no fato, pela chave natural
    → rodar de novo aqui não duplica nada (idempotência)
         ↓
9.  Staging marcado como PROCESSADO; execução fechada com volumetria
         ↓
10. Cache dos agregados é invalidado
         ↓
    ——— usuário abre a tela ———
         ↓
11. Angular chama GET /api/v1/trade/time-series?ncmChapter=72&from=2024-01
         ↓
12. TradeAnalyticsService consulta agregada (GROUP BY sobre o fato + joins)
    → resposta vem do cache se estiver quente
         ↓
13. DTO de resposta → JSON → service Angular → signal → gráfico
```

O passo 4 é a fronteira que sustenta tudo. Antes dele, você está à mercê da
internet. Depois dele, é só o seu banco.

### Esboço do contrato REST

```
GET  /api/v1/trade/time-series      ?flow&ncmChapter&countryId&from&to&groupBy
GET  /api/v1/trade/top-countries    ?flow&period&limit
GET  /api/v1/trade/unit-price       ?ncmId&countryId&from&to
GET  /api/v1/trade/by-transport     ?flow&period
GET  /api/v1/market/summary
GET  /api/v1/status/freshness
GET  /api/v1/export/trade.csv       ?<mesmos filtros>
```

Convenções que valem seguir: substantivo no plural, filtro em query param,
paginação com `page`/`size` retornando também o total, e erro sempre no mesmo
formato (`timestamp`, `status`, `code`, `message`, `path`) via
`@RestControllerAdvice`.

---

## 8. Decisões de arquitetura (ADRs)

**ADR** = *Architecture Decision Record*. É um registro curto de uma decisão
técnica: o contexto, a escolha e as consequências. A prática vale por si só —
em entrevista, "está documentado no ADR-003" é uma resposta muito forte.
Crie uma pasta `docs/adr/` com um arquivo por decisão.

**ADR-001 — PostgreSQL em vez de Oracle**
*Contexto:* domínio de Oracle no trabalho, mas hospedagem gratuita inviável.
*Decisão:* PostgreSQL.
*Consequências:* JSONB facilita o staging; SQL analítico é transferível; perde-se
a demonstração direta de Oracle, compensada por citar a experiência no README.

**ADR-002 — Staging antes do modelo analítico**
*Contexto:* cota limitada e fonte que revisa dados retroativamente.
*Decisão:* persistir payload cru antes de transformar.
*Consequências:* mais espaço em disco e uma etapa a mais; em troca, reprocessamento
sem custo externo e auditabilidade completa.

**ADR-003 — Modelo dimensional em vez de normalizado**
*Contexto:* carga é batch, leitura é analítica e frequente.
*Decisão:* star schema.
*Consequências:* redundância controlada nas dimensões; consultas com menos joins.

**ADR-004 — SSE em vez de WebSocket**
*Contexto:* fluxo é unidirecional (servidor → cliente).
*Decisão:* Server-Sent Events para a camada de mercado.
*Consequências:* implementação bem mais simples; se um dia houver interação
bidirecional, migra-se para WebSocket.

**ADR-005 — Janela móvel de recarga em vez de carga incremental estrita**
*Contexto:* o Comex Stat revisa meses já publicados.
*Decisão:* recarregar sempre os últimos N meses, apoiado em upsert.
*Consequências:* processa dado repetido; em troca, os números ficam sempre
corretos sem lógica de detecção de mudança.

Escreva cada um em 5 linhas. Se ficar maior que isso, não é ADR, é documentação.

---

## 9. Roadmap por fases

Regra: **cada fase termina com algo que roda**. Nada de "fase de modelagem" que
não produz tela.

**Fase 0 — Esqueleto**
Projeto Spring Boot + Postgres no Docker Compose + Flyway com uma migration boba
+ Actuator respondendo. Angular criado com uma rota. Objetivo: `docker compose up`
e ver as duas coisas de pé.

**Fase 1 — Uma fonte, ponta a ponta (a fase mais importante)**
Só Comex Stat, só capítulo 72, só exportação. Client → staging → dimensões →
fato → um endpoint de série temporal → um gráfico de linha na tela.
Feio, mas completo. Não avance sem isso fechado.

**Fase 2 — Confiabilidade**
Retry, circuit breaker, tabela de execução, tratamento global de erro, teste de
idempotência (rode o loader duas vezes e prove que o total não muda).

**Fase 3 — Profundidade analítica**
Importação também, capítulo 73, ranking de países, preço médio por tonelada,
recorte por via de transporte, filtros combinados no front.

**Fase 4 — Camada viva**
Câmbio e cotações, cache com TTL, SSE, ticker no topo do dashboard, badge de
frescor do dado.

**Fase 5 — Acabamento**
Export CSV/XLSX, Swagger publicado, README com print e GIF, CI no GitHub Actions
rodando os testes, deploy em Render ou Fly.io.

**Fase 6 — Opcional, se a energia durar**
ANTT (ferroviário) como segunda fonte. Aqui o desenho de `ingestion` prova seu
valor: se você fez direito, a fonte nova não toca em nada do que já existe.

---

## 10. Armadilhas conhecidas

**Duplicar dados no reprocessamento.** Sem a constraint única + upsert, a
segunda execução dobra tudo. Escreva o teste de idempotência cedo.

**Deixar o nome de campo externo vazar.** No dia em que o MDIC renomear um
campo, ou você conserta um mapper, ou caça `coAno` em quarenta arquivos.

**Fuso horário.** Use `TIMESTAMPTZ` e trabalhe em UTC no banco, convertendo só
na exibição. Job agendado com fuso ambíguo é bug de madrugada.

**Precisão numérica.** `NUMERIC`, nunca `DOUBLE`, para valor monetário e peso.
Ponto flutuante binário não representa decimal exatamente, e o total vai
divergir do oficial em centavos — justo o tipo de erro que destrói a
credibilidade de um dashboard.

**Encoding.** Fonte de governo brasileiro adora Latin-1. Se aparecer "Ã§" na
tela, é isso. Defina o charset no client HTTP explicitamente.

**Buscar tudo e agregar em Java.** Faça o `GROUP BY` no banco. Trazer um milhão
de linhas para somar na aplicação é o erro de performance clássico — o mesmo
raciocínio de query pesada que você já enfrenta no trabalho.

**Gráfico sem estado vazio nem de carregamento.** Tela em branco parece bug.
Skeleton e mensagem de "sem dados para este filtro" custam pouco e mudam a
percepção de qualidade.

**Cota estourada em desenvolvimento.** Enquanto desenvolve o front, sirva de um
`stg_coleta_bruta` já populado ou de fixture, não da API real.

**Escopo infinito.** Toda fonte nova parece barata e nenhuma é. Feche a Fase 5
antes de pensar na 6.

---

## 11. Checklist "eu sei explicar isso?"

Se você conseguir responder cada item em voz alta, sem consultar nada, o projeto
cumpriu o objetivo de aprendizado — e você está pronto para qualquer entrevista
técnica sobre ele.

**Dados**
- [ ] Qual é o grão da minha tabela fato, e por que escolhi esse?
- [ ] Por que separei staging do modelo analítico?
- [ ] O que acontece se o job rodar duas vezes? Por quê?
- [ ] Por que desnormalizei, se aprendi que normalizar é o certo?
- [ ] Por que o preço por tonelada não está armazenado?

**Arquitetura**
- [ ] O que é a porta e o que é o adaptador no meu código?
- [ ] Onde exatamente está minha camada anticorrupção?
- [ ] Por que o Angular não chama a API externa direto? (três motivos)
- [ ] O que acontece com o dashboard se o Comex Stat cair agora?

**Resiliência**
- [ ] Qual a diferença entre retry e circuit breaker?
- [ ] O que meu circuit breaker faz quando abre? E depois?
- [ ] Por que backoff exponencial e não intervalo fixo?

**Frontend**
- [ ] Por que SSE e não WebSocket?
- [ ] Qual a regra de dependência entre core, shared e features?
- [ ] O que ganho com lazy loading, na prática?

**Operação**
- [ ] Como sei que a última carga funcionou, sem abrir o banco?
- [ ] Como reprocesso um mês específico sem chamar a API externa?
- [ ] O que meus logs me permitem responder que um `println` não permitiria?

---

## Referências das fontes

| Fonte | Endereço | Frequência | Chave |
|---|---|---|---|
| Comex Stat (API) | `api-comexstat.mdic.gov.br/docs` | mensal | não |
| Dados Abertos ANTT (CKAN) | `dados.antt.gov.br` | mensal/anual | não |
| ANTAQ — estatístico aquaviário | portal gov.br/antaq | mensal | não |
| Banco Central — SGS/PTAX | API pública do BCB | diária | não |
| IBGE / SIDRA | API do SIDRA | mensal | não |
| AwesomeAPI — câmbio | `economia.awesomeapi.com.br` | minutos | não |
| brapi — B3 | `brapi.dev/docs` | intradiária | parcial |
| Metals-API / Commodities-API | freemium | intradiária | sim |

Confirme o limite gratuito de cada uma antes de desenhar a tela que depende
dela — free tier muda sem aviso, e descobrir isso na véspera de publicar o
portfólio é frustrante.
