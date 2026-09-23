package com.steeltrade.status.dto;

import java.time.OffsetDateTime;

import com.steeltrade.ingestion.core.StatusExecucao;

/** Situação operacional de uma fonte: última execução e último sucesso. */
public record SourceFreshnessResponse(
        String fonte,
        StatusExecucao status,
        OffsetDateTime iniciadoEm,
        OffsetDateTime finalizadoEm,
        String periodoReferencia,
        Integer registrosGravados,
        String mensagem,
        OffsetDateTime ultimoSucessoEm
) {
}
