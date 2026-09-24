package com.steeltrade.ingestion.comexstat;

import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.warehouse.fact.Fluxo;

/**
 * Porta (arquitetura hexagonal): o núcleo depende desta interface,
 * nunca do client HTTP concreto. Trocar a fonte não toca em quem consome.
 */
public interface ComexStatGateway {

    ComexStatRawResponse buscar(Fluxo fluxo, YearMonth de, YearMonth ate, int capitulo);
}
