package com.steeltrade.ingestion.comexstat.dto;

import java.time.OffsetDateTime;

import com.steeltrade.ingestion.staging.StatusProcessamento;

/** Resultado da etapa de coleta: o que entrou na staging. */
public record IngestionRunResponse(
        Long stagingId,
        int statusHttp,
        StatusProcessamento status,
        String mensagemErro,
        OffsetDateTime coletadoEm
) {
}
