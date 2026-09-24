package com.steeltrade.ingestion.comexstat;

import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.IngestionRunResponse;
import com.steeltrade.warehouse.fact.Fluxo;

public interface ComexStatIngestionService {

    /**
     * Busca um período na fonte externa e grava o payload cru na staging.
     * Resposta HTTP de erro (429, 500...) também vira staging, marcada como
     * ERRO — dead letter para diagnóstico, sem quebrar o fluxo.
     */
    IngestionRunResponse coletar(Fluxo fluxo, YearMonth de, YearMonth ate, int capitulo);
}
