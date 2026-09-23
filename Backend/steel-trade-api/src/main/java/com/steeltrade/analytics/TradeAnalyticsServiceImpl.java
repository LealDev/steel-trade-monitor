package com.steeltrade.analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.analytics.query.TradeQueryRepository;
import com.steeltrade.warehouse.fact.Fluxo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeAnalyticsServiceImpl implements TradeAnalyticsService {

    private static final LocalDate INICIO_HISTORICO = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIM_HISTORICO = LocalDate.of(2099, 12, 1);

    private final TradeQueryRepository tradeQueryRepository;

    public TradeAnalyticsServiceImpl(TradeQueryRepository tradeQueryRepository) {
        this.tradeQueryRepository = tradeQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPointResponse> serieTemporal(Fluxo fluxo, int capituloNcm,
                                                       YearMonth de, YearMonth ate) {
        LocalDate inicio = de != null ? de.atDay(1) : INICIO_HISTORICO;
        LocalDate fim = ate != null ? ate.atDay(1) : FIM_HISTORICO;
        return tradeQueryRepository.serieTemporal(fluxo, capituloNcm, inicio, fim).stream()
                .map(ponto -> new TimeSeriesPointResponse(
                        ponto.getPeriod(), ponto.getKgLiquido(), ponto.getValorFobUsd()))
                .toList();
    }
}
