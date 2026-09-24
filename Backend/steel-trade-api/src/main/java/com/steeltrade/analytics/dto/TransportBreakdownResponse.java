package com.steeltrade.analytics.dto;

import java.math.BigDecimal;

/** Volume e valor por via de transporte (o ângulo logístico do dashboard). */
public record TransportBreakdownResponse(
        String via,
        BigDecimal kgLiquido,
        BigDecimal valorFobUsd
) {
}
