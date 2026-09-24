package com.steeltrade.ingestion.comexstat.dto;

import java.time.YearMonth;
import java.util.List;

import com.steeltrade.warehouse.fact.Fluxo;

/**
 * Formato do body aceito pelo POST /general do Comex Stat.
 * Contrato validado em 2026-09: o detail de via de transporte chama-se "via";
 * period usa "YYYY-MM"; métricas metricFOB/metricKG.
 */
public record ComexStatQueryRequest(
        String flow,
        boolean monthDetail,
        Period period,
        List<Filter> filters,
        List<String> details,
        List<String> metrics
) {

    public record Period(String from, String to) {
    }

    public record Filter(String filter, List<Integer> values) {
    }

    public static ComexStatQueryRequest porCapitulo(Fluxo fluxo, YearMonth de, YearMonth ate, int capitulo) {
        return new ComexStatQueryRequest(
                fluxo == Fluxo.IMPORT ? "import" : "export",
                true,
                new Period(de.toString(), ate.toString()),
                List.of(new Filter("chapter", List.of(capitulo))),
                List.of("country", "state", "via", "ncm"),
                List.of("metricFOB", "metricKG"));
    }
}
