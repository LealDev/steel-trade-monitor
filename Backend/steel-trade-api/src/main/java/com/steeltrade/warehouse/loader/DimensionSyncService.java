package com.steeltrade.warehouse.loader;

import com.steeltrade.ingestion.comexstat.dto.RegistroComercioExterior;

/**
 * Garante que as dimensões referenciadas por um registro existem antes do
 * upsert no fato (as FKs exigem isso) e devolve os ids resolvidos.
 */
public interface DimensionSyncService {

    DimensionKeys sincronizar(RegistroComercioExterior registro);

    record DimensionKeys(Long tempoId, Long ncmId, Long paisId, Long ufId, Long viaId) {
    }
}
