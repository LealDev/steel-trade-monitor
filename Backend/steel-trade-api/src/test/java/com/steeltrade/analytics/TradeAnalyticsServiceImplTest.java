package com.steeltrade.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.query.TimeSeriesPointView;
import com.steeltrade.analytics.query.TradeQueryRepository;
import com.steeltrade.warehouse.fact.Fluxo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeAnalyticsServiceImplTest {

    @Mock
    private TradeQueryRepository repository;

    @Test
    void aplicaJanelaHistoricaCompletaQuandoPeriodoNaoInformado() {
        when(repository.serieTemporal(any(), anyInt(), any(), any())).thenReturn(List.of());
        var service = new TradeAnalyticsServiceImpl(repository);

        service.serieTemporal(Fluxo.EXPORT, 72, null, null);

        verify(repository).serieTemporal(Fluxo.EXPORT, 72,
                LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 1));
    }

    @Test
    void convertePeriodoInformadoParaPrimeiroDiaDoMes() {
        when(repository.serieTemporal(any(), anyInt(), any(), any()))
                .thenReturn(List.of(ponto("2025-06", "1000", "2000")));
        var service = new TradeAnalyticsServiceImpl(repository);

        var serie = service.serieTemporal(Fluxo.EXPORT, 72,
                YearMonth.of(2025, 1), YearMonth.of(2025, 6));

        verify(repository).serieTemporal(Fluxo.EXPORT, 72,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1));
        assertThat(serie).hasSize(1);
        assertThat(serie.getFirst().period()).isEqualTo("2025-06");
        assertThat(serie.getFirst().kgLiquido()).isEqualByComparingTo("1000");
        assertThat(serie.getFirst().valorFobUsd()).isEqualByComparingTo("2000");
    }

    private TimeSeriesPointView ponto(String period, String kg, String fob) {
        return new TimeSeriesPointView() {
            @Override
            public String getPeriod() {
                return period;
            }

            @Override
            public BigDecimal getKgLiquido() {
                return new BigDecimal(kg);
            }

            @Override
            public BigDecimal getValorFobUsd() {
                return new BigDecimal(fob);
            }
        };
    }
}
