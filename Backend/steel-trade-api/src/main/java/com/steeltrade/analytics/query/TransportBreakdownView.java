package com.steeltrade.analytics.query;

import java.math.BigDecimal;

public interface TransportBreakdownView {

    String getVia();

    BigDecimal getKgLiquido();

    BigDecimal getValorFobUsd();
}
