package com.steeltrade.warehouse.fact;

import java.math.BigDecimal;

/**
 * Escrita idempotente no fato pela chave natural.
 * Interface própria porque o upsert é SQL nativo (ON CONFLICT), fora do
 * vocabulário do Spring Data.
 */
public interface TradeFactUpsertRepository {

    void upsert(UpsertCommand comando);

    record UpsertCommand(
            Fluxo fluxo,
            Long tempoId,
            Long ncmId,
            Long paisId,
            Long ufId,
            Long viaId,
            BigDecimal kgLiquido,
            BigDecimal valorFobUsd
    ) {
    }
}
