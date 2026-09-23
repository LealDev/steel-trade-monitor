package com.steeltrade.warehouse.loader;

/**
 * Transforma staging PENDENTE em linhas do fato (a etapa T do ELT).
 * Reprocessar não duplica nada: a escrita é upsert pela chave natural.
 */
public interface TradeFactLoader {

    LoadResult processarPendentes();

    record LoadResult(int stagingProcessados, int registrosCarregados, int stagingComErro) {
    }
}
