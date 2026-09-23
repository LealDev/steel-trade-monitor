package com.steeltrade.ingestion.comexstat.dto;

/**
 * Resultado cru de uma chamada ao Comex Stat, pronto para virar staging.
 * O corpo não é interpretado aqui — interpretação é papel do loader/mapper.
 */
public record ComexStatRawResponse(
        String endpoint,
        String parametrosJson,
        int statusHttp,
        String corpo
) {

    public boolean sucesso() {
        return statusHttp >= 200 && statusHttp < 300;
    }
}
