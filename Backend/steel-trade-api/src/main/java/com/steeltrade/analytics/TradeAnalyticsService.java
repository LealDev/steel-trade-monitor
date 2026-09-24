package com.steeltrade.analytics;

import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.dto.CountryRankingResponse;
import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.analytics.dto.TransportBreakdownResponse;
import com.steeltrade.warehouse.fact.Fluxo;

public interface TradeAnalyticsService {

    /**
     * Série temporal mensal agregada. {@code de} e {@code ate} são opcionais:
     * ausentes, a série cobre todo o histórico carregado.
     */
    List<TimeSeriesPointResponse> serieTemporal(Fluxo fluxo, int capituloNcm, YearMonth de, YearMonth ate);

    /** Top países por valor FOB no período. */
    List<CountryRankingResponse> rankingDePaises(Fluxo fluxo, int capituloNcm,
                                                 YearMonth de, YearMonth ate, int limite);

    /** Volume e valor por via de transporte no período. */
    List<TransportBreakdownResponse> recortePorVia(Fluxo fluxo, int capituloNcm,
                                                   YearMonth de, YearMonth ate);
}
