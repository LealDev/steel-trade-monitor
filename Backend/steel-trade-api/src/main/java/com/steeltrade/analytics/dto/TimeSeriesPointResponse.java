package com.steeltrade.analytics.dto;

import java.math.BigDecimal;

/** Um ponto da série temporal agregada (um mês). */
public record TimeSeriesPointResponse(
        String period,
        BigDecimal kgLiquido,
        BigDecimal valorFobUsd
) {
}
