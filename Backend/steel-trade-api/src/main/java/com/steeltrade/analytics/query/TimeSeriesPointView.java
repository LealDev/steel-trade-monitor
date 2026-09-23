package com.steeltrade.analytics.query;

import java.math.BigDecimal;

/** Projeção da consulta agregada — o Spring Data materializa por nome. */
public interface TimeSeriesPointView {

    String getPeriod();

    BigDecimal getKgLiquido();

    BigDecimal getValorFobUsd();
}
