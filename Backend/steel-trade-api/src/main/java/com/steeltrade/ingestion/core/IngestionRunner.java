package com.steeltrade.ingestion.core;

import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.warehouse.fact.Fluxo;

/**
 * Orquestra o pipeline de uma fonte registrando a execução em
 * ctl_execucao_ingestao — inclusive quando algo estoura no meio.
 * A coleta é SEMPRE mês a mês: a API do Comex Stat trunca silenciosamente
 * janelas longas (comprovado em 2026-09 — 15 meses devolveram 1/3 das
 * linhas, sem erro algum).
 */
public interface IngestionRunner {

    PipelineRunResponse executarComexStat(Fluxo fluxo, YearMonth de, YearMonth ate, int capitulo);
}
