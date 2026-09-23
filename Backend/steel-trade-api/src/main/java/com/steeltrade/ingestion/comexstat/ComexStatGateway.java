package com.steeltrade.ingestion.comexstat;

import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;

/**
 * Porta (arquitetura hexagonal): o núcleo depende desta interface,
 * nunca do client HTTP concreto. Trocar a fonte não toca em quem consome.
 */
public interface ComexStatGateway {

    ComexStatRawResponse buscarExportacoes(YearMonth de, YearMonth ate, int capitulo);
}
