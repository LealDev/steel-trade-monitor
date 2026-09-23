-- V3: tabela fato.
-- Grão: uma linha = um mês, um NCM, um país, uma UF, uma via, um fluxo.
-- A constraint única sobre a chave natural é o alicerce do upsert idempotente.

CREATE TABLE fato_comercio_exterior (
    id                     BIGSERIAL PRIMARY KEY,
    fluxo                  VARCHAR(6)     NOT NULL CHECK (fluxo IN ('EXPORT', 'IMPORT')),
    dim_tempo_id           BIGINT         NOT NULL REFERENCES dim_tempo (id),
    dim_ncm_id             BIGINT         NOT NULL REFERENCES dim_ncm (id),
    dim_pais_id            BIGINT         NOT NULL REFERENCES dim_pais (id),
    dim_uf_id              BIGINT         NOT NULL REFERENCES dim_uf (id),
    dim_via_id             BIGINT         NOT NULL REFERENCES dim_via_transporte (id),
    kg_liquido             NUMERIC(18, 3) NOT NULL DEFAULT 0,
    valor_fob_usd          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    quantidade_estatistica NUMERIC(18, 3),
    carregado_em           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_fato_chave_natural
        UNIQUE (fluxo, dim_tempo_id, dim_ncm_id, dim_pais_id, dim_uf_id, dim_via_id)
);
