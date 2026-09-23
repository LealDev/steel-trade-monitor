package com.steeltrade.ingestion.comexstat.dto;

import java.math.BigDecimal;

/**
 * Registro de comércio exterior já traduzido para o vocabulário interno.
 * É a saída da camada anticorrupção: nomes externos (coNcm, metricFOB, state)
 * não existem daqui para dentro.
 */
public record RegistroComercioExterior(
        String codigoNcm,
        String descricaoNcm,
        int ano,
        int mes,
        String nomePais,
        String nomeUf,
        String descricaoVia,
        BigDecimal kgLiquido,
        BigDecimal valorFobUsd
) {

    public int capitulo() {
        return Integer.parseInt(codigoNcm.substring(0, 2));
    }

    public String sh4() {
        return codigoNcm.substring(0, 4);
    }

    public String sh6() {
        return codigoNcm.substring(0, 6);
    }
}
