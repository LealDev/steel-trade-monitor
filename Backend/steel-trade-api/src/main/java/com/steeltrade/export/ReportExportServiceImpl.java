package com.steeltrade.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

import com.steeltrade.analytics.TradeAnalyticsService;
import com.steeltrade.warehouse.fact.Fluxo;

import org.springframework.stereotype.Service;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    private static final BigDecimal MIL = BigDecimal.valueOf(1000);

    private final TradeAnalyticsService tradeAnalyticsService;

    public ReportExportServiceImpl(TradeAnalyticsService tradeAnalyticsService) {
        this.tradeAnalyticsService = tradeAnalyticsService;
    }

    @Override
    public String serieTemporalCsv(Fluxo fluxo, int capituloNcm, YearMonth de, YearMonth ate) {
        var serie = tradeAnalyticsService.serieTemporal(fluxo, capituloNcm, de, ate);

        var csv = new StringBuilder("periodo;fluxo;capitulo_ncm;kg_liquido;valor_fob_usd;preco_medio_usd_t\r\n");
        for (var ponto : serie) {
            var toneladas = ponto.kgLiquido().divide(MIL, 3, RoundingMode.HALF_UP);
            var preco = toneladas.signum() > 0
                    ? ponto.valorFobUsd().divide(toneladas, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            csv.append(ponto.period()).append(';')
                    .append(fluxo).append(';')
                    .append(capituloNcm).append(';')
                    .append(decimalPtBr(ponto.kgLiquido())).append(';')
                    .append(decimalPtBr(ponto.valorFobUsd())).append(';')
                    .append(decimalPtBr(preco)).append("\r\n");
        }
        return csv.toString();
    }

    private String decimalPtBr(BigDecimal valor) {
        return valor.toPlainString().replace('.', ',');
    }
}
