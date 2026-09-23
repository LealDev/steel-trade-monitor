package com.steeltrade.ingestion.comexstat.dto;

import com.steeltrade.ingestion.core.StatusExecucao;
import com.steeltrade.warehouse.loader.TradeFactLoader;

/** Resultado do pipeline completo (coleta + carga), com a execução registrada. */
public record PipelineRunResponse(
        Long execucaoId,
        StatusExecucao statusExecucao,
        IngestionRunResponse coleta,
        TradeFactLoader.LoadResult carga
) {
}
