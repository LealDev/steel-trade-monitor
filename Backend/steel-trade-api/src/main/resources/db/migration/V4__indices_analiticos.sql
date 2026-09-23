-- V4: índices para as consultas analíticas mais frequentes.

-- série temporal (a consulta mais frequente)
CREATE INDEX idx_fato_tempo_ncm ON fato_comercio_exterior (dim_tempo_id, dim_ncm_id);

-- ranking de destinos
CREATE INDEX idx_fato_pais_tempo ON fato_comercio_exterior (dim_pais_id, dim_tempo_id);
