package com.steeltrade.ingestion.core;

import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;

/**
 * Orquestra o pipeline de uma fonte (coleta → carga) registrando a execução
 * em ctl_execucao_ingestao — inclusive quando algo estoura no meio.
 */
public interface IngestionRunner {

    PipelineRunResponse executarComexStat(YearMonth de, YearMonth ate, int capitulo);
}
