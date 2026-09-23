package com.steeltrade.analytics;

import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.warehouse.fact.Fluxo;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/trade")
public class TradeAnalyticsController {

    private final TradeAnalyticsService tradeAnalyticsService;

    public TradeAnalyticsController(TradeAnalyticsService tradeAnalyticsService) {
        this.tradeAnalyticsService = tradeAnalyticsService;
    }

    @GetMapping("/time-series")
    public List<TimeSeriesPointResponse> serieTemporal(
            @RequestParam(defaultValue = "EXPORT") Fluxo flow,
            @RequestParam(name = "ncmChapter", defaultValue = "72") int capituloNcm,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return tradeAnalyticsService.serieTemporal(flow, capituloNcm, from, to);
    }
}
