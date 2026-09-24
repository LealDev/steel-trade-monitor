package com.steeltrade.ingestion.comexstat.dto;

import com.steeltrade.ingestion.core.StatusExecucao;
import com.steeltrade.warehouse.loader.TradeFactLoader;

/**
 * Resultado do pipeline (coleta mês a mês + carga), com a execução
 * registrada em ctl_execucao_ingestao.
 */
public record PipelineRunResponse(
        Long execucaoId,
        StatusExecucao statusExecucao,
        int mesesColetados,
        int mesesComErro,
        TradeFactLoader.LoadResult carga
) {
}
