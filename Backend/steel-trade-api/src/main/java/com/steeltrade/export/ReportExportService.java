package com.steeltrade.export;

import java.time.YearMonth;

import com.steeltrade.warehouse.fact.Fluxo;

public interface ReportExportService {

    /**
     * Série mensal do recorte em CSV (separador ';' e vírgula decimal,
     * amigável ao Excel pt-BR). O preço US$/t é calculado na hora — medida
     * derivada nunca é armazenada.
     */
    String serieTemporalCsv(Fluxo fluxo, int capituloNcm, YearMonth de, YearMonth ate);
}
