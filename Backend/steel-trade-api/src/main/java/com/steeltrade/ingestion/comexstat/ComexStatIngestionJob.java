package com.steeltrade.ingestion.comexstat;

import java.time.Clock;
import java.time.YearMonth;

import com.steeltrade.ingestion.core.IngestionRunner;
import com.steeltrade.shared.config.ComexStatProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job agendado: recarrega a janela móvel (ADR-005) e processa a staging.
 * A janela móvel — e não "só o mês novo" — porque o Comex Stat revisa meses
 * já publicados; o upsert reconcilia.
 */
@Component
@ConditionalOnProperty(name = "comexstat.agendamento-habilitado", havingValue = "true")
public class ComexStatIngestionJob {

    static final int CAPITULO_FERRO_ACO = 72;

    private static final Logger log = LoggerFactory.getLogger(ComexStatIngestionJob.class);

    private final IngestionRunner ingestionRunner;
    private final ComexStatProperties properties;
    private final Clock clock;

    public ComexStatIngestionJob(IngestionRunner ingestionRunner,
                                 ComexStatProperties properties,
                                 Clock clock) {
        this.ingestionRunner = ingestionRunner;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${comexstat.cron}")
    public void executar() {
        YearMonth ate = YearMonth.now(clock);
        YearMonth de = ate.minusMonths(properties.mesesJanelaMovel() - 1L);
        log.info("evento=job_iniciado fonte=COMEXSTAT janela={}..{}", de, ate);
        try {
            ingestionRunner.executarComexStat(de, ate, CAPITULO_FERRO_ACO);
        } catch (RuntimeException ex) {
            // job agendado nunca propaga: a falha já ficou registrada em
            // ctl_execucao_ingestao e a próxima janela tenta de novo
            log.error("evento=job_falhou fonte=COMEXSTAT", ex);
        }
    }
}
