package com.steeltrade.ingestion.comexstat.dto;

import java.time.YearMonth;

import com.steeltrade.warehouse.fact.Fluxo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Disparo manual de uma coleta do Comex Stat. flow ausente = EXPORT. */
public record IngestionRunRequest(
        @NotNull YearMonth from,
        @NotNull YearMonth to,
        @NotNull @Min(1) @Max(99) Integer chapter,
        Fluxo flow
) {

    public Fluxo fluxoOuPadrao() {
        return flow != null ? flow : Fluxo.EXPORT;
    }

    @AssertTrue(message = "from deve ser anterior ou igual a to")
    public boolean isPeriodoValido() {
        return from == null || to == null || !from.isAfter(to);
    }
}
