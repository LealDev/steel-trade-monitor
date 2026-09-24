package com.steeltrade.status.dto;

import java.time.OffsetDateTime;

import com.steeltrade.ingestion.core.StatusExecucao;

/** Uma execução de ingestão no histórico operacional. */
public record ExecutionResponse(
        Long id,
        String fonte,
        StatusExecucao status,
        OffsetDateTime iniciadoEm,
        OffsetDateTime finalizadoEm,
        String periodoReferencia,
        Integer registrosGravados,
        String mensagem
) {
}
