package com.steeltrade.analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.dto.CountryRankingResponse;
import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.analytics.dto.TransportBreakdownResponse;
import com.steeltrade.analytics.query.TradeQueryRepository;
import com.steeltrade.warehouse.fact.Fluxo;

import org.springframework.data.domain.PageRequest;
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
        return tradeQueryRepository.serieTemporal(fluxo, capituloNcm, inicio(de), fim(ate)).stream()
                .map(ponto -> new TimeSeriesPointResponse(
                        ponto.getPeriod(), ponto.getKgLiquido(), ponto.getValorFobUsd()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CountryRankingResponse> rankingDePaises(Fluxo fluxo, int capituloNcm,
                                                        YearMonth de, YearMonth ate, int limite) {
        return tradeQueryRepository
                .rankingDePaises(fluxo, capituloNcm, inicio(de), fim(ate), PageRequest.of(0, limite))
                .stream()
                .map(pos -> new CountryRankingResponse(
                        pos.getNomePais(), pos.getKgLiquido(), pos.getValorFobUsd()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportBreakdownResponse> recortePorVia(Fluxo fluxo, int capituloNcm,
                                                          YearMonth de, YearMonth ate) {
        return tradeQueryRepository.recortePorVia(fluxo, capituloNcm, inicio(de), fim(ate)).stream()
                .map(via -> new TransportBreakdownResponse(
                        via.getVia(), via.getKgLiquido(), via.getValorFobUsd()))
                .toList();
    }

    private LocalDate inicio(YearMonth de) {
        return de != null ? de.atDay(1) : INICIO_HISTORICO;
    }

    private LocalDate fim(YearMonth ate) {
        return ate != null ? ate.atDay(1) : FIM_HISTORICO;
    }
}
