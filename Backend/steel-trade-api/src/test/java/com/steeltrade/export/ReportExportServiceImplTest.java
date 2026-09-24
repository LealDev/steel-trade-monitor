package com.steeltrade.export;

import java.math.BigDecimal;
import java.util.List;

import com.steeltrade.analytics.TradeAnalyticsService;
import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.warehouse.fact.Fluxo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceImplTest {

    @Mock
    private TradeAnalyticsService tradeAnalyticsService;

    @Test
    void geraCsvComPrecoDerivadoEVirgulaDecimal() {
        when(tradeAnalyticsService.serieTemporal(any(), anyInt(), any(), any())).thenReturn(List.of(
                // 2.000 t a US$ 800/t
                new TimeSeriesPointResponse("2025-06",
                        new BigDecimal("2000000.000"), new BigDecimal("1600000.00"))));
        var service = new ReportExportServiceImpl(tradeAnalyticsService);

        String csv = service.serieTemporalCsv(Fluxo.EXPORT, 72, null, null);

        var linhas = csv.split("\r\n");
        assertThat(linhas[0]).isEqualTo("periodo;fluxo;capitulo_ncm;kg_liquido;valor_fob_usd;preco_medio_usd_t");
        assertThat(linhas[1]).isEqualTo("2025-06;EXPORT;72;2000000,000;1600000,00;800,00");
    }

    @Test
    void mesSemVolumeNaoDivideProZero() {
        when(tradeAnalyticsService.serieTemporal(any(), anyInt(), any(), any())).thenReturn(List.of(
                new TimeSeriesPointResponse("2025-06", BigDecimal.ZERO, BigDecimal.TEN)));
        var service = new ReportExportServiceImpl(tradeAnalyticsService);

        String csv = service.serieTemporalCsv(Fluxo.IMPORT, 73, null, null);

        assertThat(csv).contains("2025-06;IMPORT;73;0;10;0");
    }
}
