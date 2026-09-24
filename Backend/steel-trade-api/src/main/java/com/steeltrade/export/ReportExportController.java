package com.steeltrade.export;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import com.steeltrade.warehouse.fact.Fluxo;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/export")
public class ReportExportController {

    private final ReportExportService reportExportService;

    public ReportExportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @GetMapping(value = "/trade.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarSerieTemporal(
            @RequestParam(defaultValue = "EXPORT") Fluxo flow,
            @RequestParam(name = "ncmChapter", defaultValue = "72") int capituloNcm,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        String csv = reportExportService.serieTemporalCsv(flow, capituloNcm, from, to);
        String nomeArquivo = "steel-trade_%s_cap%d.csv".formatted(flow.name().toLowerCase(), capituloNcm);
        // BOM UTF-8: sem ele o Excel abre acentos quebrados
        byte[] corpo = ("﻿" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(corpo);
    }
}
