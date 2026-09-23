-- V2: dimensões do modelo estrela.
-- Chaves substitutas (id) como PK; chave natural com UNIQUE — é ela que o upsert usa.

CREATE TABLE dim_tempo (
    id           BIGSERIAL PRIMARY KEY,
    ano          INT        NOT NULL,
    mes          INT        NOT NULL CHECK (mes BETWEEN 1 AND 12),
    trimestre    INT        NOT NULL CHECK (trimestre BETWEEN 1 AND 4),
    ano_mes      VARCHAR(7) NOT NULL,
    primeiro_dia DATE       NOT NULL,
    CONSTRAINT uk_dim_tempo_ano_mes UNIQUE (ano, mes)
);

CREATE TABLE dim_ncm (
    id         BIGSERIAL PRIMARY KEY,
    codigo_ncm VARCHAR(8) NOT NULL,
    descricao  TEXT       NOT NULL,
    sh4        VARCHAR(4) NOT NULL,
    sh6        VARCHAR(6) NOT NULL,
    capitulo   INT        NOT NULL,
    CONSTRAINT uk_dim_ncm_codigo UNIQUE (codigo_ncm)
);

-- O /general do Comex Stat devolve só o nome do país (sem código);
-- a chave natural prática é nome_pt. codigo/iso ficam para enriquecimento futuro.
CREATE TABLE dim_pais (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(3),
    nome_pt         VARCHAR(120) NOT NULL,
    nome_en         VARCHAR(120),
    bloco_economico VARCHAR(60),
    iso_alpha3      VARCHAR(3),
    CONSTRAINT uk_dim_pais_nome UNIQUE (nome_pt)
);

CREATE TABLE dim_uf (
    id     BIGSERIAL PRIMARY KEY,
    sigla  VARCHAR(2),
    nome   VARCHAR(60) NOT NULL,
    regiao VARCHAR(20),
    CONSTRAINT uk_dim_uf_nome UNIQUE (nome)
);

CREATE TABLE dim_via_transporte (
    id        BIGSERIAL PRIMARY KEY,
    codigo    VARCHAR(10),
    descricao VARCHAR(60) NOT NULL,
    CONSTRAINT uk_dim_via_descricao UNIQUE (descricao)
);
