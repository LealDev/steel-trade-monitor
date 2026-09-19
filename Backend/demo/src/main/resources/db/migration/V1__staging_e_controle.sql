-- V1: camada de staging e controle de ingestão.
-- stg_coleta_bruta guarda o payload cru como veio da fonte externa (ELT):
-- a partir daqui o reprocessamento não depende mais da internet nem gasta cota.

CREATE TABLE stg_coleta_bruta (
    id                    BIGSERIAL PRIMARY KEY,
    fonte                 VARCHAR(30)  NOT NULL,
    endpoint              VARCHAR(255) NOT NULL,
    parametros            JSONB,
    payload               JSONB        NOT NULL,
    status_http           INT          NOT NULL,
    status_processamento  VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    mensagem_erro         TEXT,
    coletado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processado_em         TIMESTAMPTZ,
    CONSTRAINT chk_stg_status CHECK (status_processamento IN ('PENDENTE', 'PROCESSADO', 'ERRO'))
);

-- busca do que falta processar: "me dê os PENDENTES da fonte X"
CREATE INDEX idx_stg_status_fonte ON stg_coleta_bruta (status_processamento, fonte);

-- ctl_execucao_ingestao: uma linha por rodada de job (watermark + volumetria).
-- Alimenta a tela de "última atualização por fonte".
CREATE TABLE ctl_execucao_ingestao (
    id                  BIGSERIAL PRIMARY KEY,
    fonte               VARCHAR(30) NOT NULL,
    iniciado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_em       TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
    registros_lidos     INT,
    registros_gravados  INT,
    periodo_referencia  VARCHAR(20),
    mensagem            TEXT,
    CONSTRAINT chk_ctl_status CHECK (status IN ('EM_ANDAMENTO', 'SUCESSO', 'FALHA', 'PARCIAL'))
);

-- última execução de cada fonte (freshness)
CREATE INDEX idx_ctl_fonte_iniciado ON ctl_execucao_ingestao (fonte, iniciado_em DESC);
