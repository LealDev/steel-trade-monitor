-- V6: periodo_referencia passou a carregar fluxo e capítulo além da janela
-- ("EXPORT cap72 2025-04..2025-09") — os 20 caracteres originais não bastam.

ALTER TABLE ctl_execucao_ingestao
    ALTER COLUMN periodo_referencia TYPE VARCHAR(60);
