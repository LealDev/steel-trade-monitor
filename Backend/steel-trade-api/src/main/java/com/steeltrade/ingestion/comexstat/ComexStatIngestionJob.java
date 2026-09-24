package com.steeltrade.ingestion.comexstat;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

import com.steeltrade.ingestion.core.IngestionRunner;
import com.steeltrade.shared.config.ComexStatProperties;
import com.steeltrade.warehouse.fact.Fluxo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job agendado: recarrega a janela móvel (ADR-005) para cada combinação
 * fluxo × capítulo e processa a staging. A janela móvel — e não "só o mês
 * novo" — porque o Comex Stat revisa meses já publicados; o upsert reconcilia.
 */
@Component
@ConditionalOnProperty(name = "comexstat.agendamento-habilitado", havingValue = "true")
public class ComexStatIngestionJob {

    /** 72 = ferro fundido, ferro e aço; 73 = obras de ferro ou aço. */
    static final List<Integer> CAPITULOS = List.of(72, 73);

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

        for (Fluxo fluxo : Fluxo.values()) {
            for (int capitulo : CAPITULOS) {
                try {
                    ingestionRunner.executarComexStat(fluxo, de, ate, capitulo);
                } catch (RuntimeException ex) {
                    // job agendado nunca propaga: a falha ficou registrada em
                    // ctl_execucao_ingestao e as demais combinações continuam
                    log.error("evento=job_combinacao_falhou fluxo={} capitulo={}", fluxo, capitulo, ex);
                }
            }
        }
    }
}
