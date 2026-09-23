package com.steeltrade.ingestion.comexstat.dto;

import com.steeltrade.warehouse.loader.TradeFactLoader;

/** Resultado do pipeline completo disparado manualmente: coleta + carga. */
public record PipelineRunResponse(
        IngestionRunResponse coleta,
        TradeFactLoader.LoadResult carga
) {
}
