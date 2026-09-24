package com.steeltrade.analytics.dto;

import java.math.BigDecimal;

/** Uma posição do ranking de países (ordenado por valor FOB). */
public record CountryRankingResponse(
        String nomePais,
        BigDecimal kgLiquido,
        BigDecimal valorFobUsd
) {
}
