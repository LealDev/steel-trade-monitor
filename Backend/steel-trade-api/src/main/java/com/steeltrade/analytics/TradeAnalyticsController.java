package com.steeltrade.analytics;

import java.time.YearMonth;
import java.util.List;

import com.steeltrade.analytics.dto.CountryRankingResponse;
import com.steeltrade.analytics.dto.TimeSeriesPointResponse;
import com.steeltrade.analytics.dto.TransportBreakdownResponse;
import com.steeltrade.warehouse.fact.Fluxo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
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

    @GetMapping("/top-countries")
    public List<CountryRankingResponse> rankingDePaises(
            @RequestParam(defaultValue = "EXPORT") Fluxo flow,
            @RequestParam(name = "ncmChapter", defaultValue = "72") int capituloNcm,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return tradeAnalyticsService.rankingDePaises(flow, capituloNcm, from, to, limit);
    }

    @GetMapping("/by-transport")
    public List<TransportBreakdownResponse> recortePorVia(
            @RequestParam(defaultValue = "EXPORT") Fluxo flow,
            @RequestParam(name = "ncmChapter", defaultValue = "72") int capituloNcm,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return tradeAnalyticsService.recortePorVia(flow, capituloNcm, from, to);
    }
}
