package com.steeltrade.ingestion.core;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.ComexStatIngestionService;
import com.steeltrade.ingestion.comexstat.ComexStatIngestionServiceImpl;
import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.shared.config.ComexStatProperties;
import com.steeltrade.shared.exception.ExternalSourceException;
import com.steeltrade.warehouse.fact.Fluxo;
import com.steeltrade.warehouse.loader.TradeFactLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestionRunnerImpl implements IngestionRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionRunnerImpl.class);

    private final ComexStatIngestionService ingestionService;
    private final TradeFactLoader tradeFactLoader;
    private final IngestionLogRepository ingestionLogRepository;
    private final ComexStatProperties properties;
    private final Clock clock;

    public IngestionRunnerImpl(ComexStatIngestionService ingestionService,
                               TradeFactLoader tradeFactLoader,
                               IngestionLogRepository ingestionLogRepository,
                               ComexStatProperties properties,
                               Clock clock) {
        this.ingestionService = ingestionService;
        this.tradeFactLoader = tradeFactLoader;
        this.ingestionLogRepository = ingestionLogRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public PipelineRunResponse executarComexStat(Fluxo fluxo, YearMonth de, YearMonth ate, int capitulo) {
        String periodo = fluxo + " cap" + capitulo + " " + de + ".." + ate;
        var execucao = ingestionLogRepository.save(IngestionLog.iniciar(
                ComexStatIngestionServiceImpl.FONTE, periodo, OffsetDateTime.now(clock)));

        int coletados = 0;
        int comErro = 0;
        try {
            // mês a mês, com pausa: janelas longas são truncadas pela fonte
            // e o rate limit é ~1 req/10s
            for (YearMonth mes = de; !mes.isAfter(ate); mes = mes.plusMonths(1)) {
                try {
                    var coleta = ingestionService.coletar(fluxo, mes, mes, capitulo);
                    if (coleta.status() == StatusProcessamento.PENDENTE) {
                        coletados++;
                    } else {
                        comErro++;
                    }
                } catch (ExternalSourceException ex) {
                    comErro++;
                    log.warn("evento=coleta_mes_falhou fonte=COMEXSTAT mes={} motivo={}", mes, ex.getMessage());
                }
                if (mes.isBefore(ate)) {
                    pausar();
                }
            }

            var carga = coletados > 0
                    ? tradeFactLoader.processarPendentes()
                    : new TradeFactLoader.LoadResult(0, 0, 0);

            var statusFinal = statusFinal(coletados, comErro, carga);
            execucao.finalizar(statusFinal, carga.registrosCarregados(), carga.registrosCarregados(),
                    comErro > 0 ? comErro + " mes(es) com falha de coleta" : null,
                    OffsetDateTime.now(clock));
            ingestionLogRepository.save(execucao);

            log.info("evento=execucao_concluida fonte={} execucaoId={} status={} meses={} registros={}",
                    execucao.getFonte(), execucao.getId(), statusFinal, coletados, carga.registrosCarregados());
            return new PipelineRunResponse(execucao.getId(), statusFinal, coletados, comErro, carga);
        } catch (RuntimeException ex) {
            execucao.finalizar(StatusExecucao.FALHA, null, null, ex.getMessage(), OffsetDateTime.now(clock));
            ingestionLogRepository.save(execucao);
            log.error("evento=execucao_falhou fonte={} execucaoId={}", execucao.getFonte(), execucao.getId(), ex);
            throw ex;
        }
    }

    private StatusExecucao statusFinal(int coletados, int comErro, TradeFactLoader.LoadResult carga) {
        if (coletados == 0) {
            return StatusExecucao.FALHA;
        }
        return comErro > 0 || carga.stagingComErro() > 0 ? StatusExecucao.PARCIAL : StatusExecucao.SUCESSO;
    }

    private void pausar() {
        try {
            Thread.sleep(properties.pausaEntreMesesMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Coleta interrompida durante a pausa entre meses", ex);
        }
    }
}
