package com.steeltrade.ingestion.core;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.ComexStatIngestionService;
import com.steeltrade.ingestion.comexstat.ComexStatIngestionServiceImpl;
import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.ingestion.staging.StatusProcessamento;
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
    private final Clock clock;

    public IngestionRunnerImpl(ComexStatIngestionService ingestionService,
                               TradeFactLoader tradeFactLoader,
                               IngestionLogRepository ingestionLogRepository,
                               Clock clock) {
        this.ingestionService = ingestionService;
        this.tradeFactLoader = tradeFactLoader;
        this.ingestionLogRepository = ingestionLogRepository;
        this.clock = clock;
    }

    @Override
    public PipelineRunResponse executarComexStat(YearMonth de, YearMonth ate, int capitulo) {
        var execucao = ingestionLogRepository.save(IngestionLog.iniciar(
                ComexStatIngestionServiceImpl.FONTE, de + ".." + ate, OffsetDateTime.now(clock)));
        try {
            var coleta = ingestionService.coletarExportacoes(de, ate, capitulo);

            var carga = coleta.status() == StatusProcessamento.PENDENTE
                    ? tradeFactLoader.processarPendentes()
                    : new TradeFactLoader.LoadResult(0, 0, 0);

            var statusFinal = statusFinal(coleta.status(), carga);
            execucao.finalizar(statusFinal, carga.registrosCarregados(), carga.registrosCarregados(),
                    coleta.mensagemErro(), OffsetDateTime.now(clock));
            ingestionLogRepository.save(execucao);

            log.info("evento=execucao_concluida fonte={} execucaoId={} status={} registros={}",
                    execucao.getFonte(), execucao.getId(), statusFinal, carga.registrosCarregados());
            return new PipelineRunResponse(execucao.getId(), statusFinal, coleta, carga);
        } catch (RuntimeException ex) {
            execucao.finalizar(StatusExecucao.FALHA, null, null, ex.getMessage(), OffsetDateTime.now(clock));
            ingestionLogRepository.save(execucao);
            log.error("evento=execucao_falhou fonte={} execucaoId={}", execucao.getFonte(), execucao.getId(), ex);
            throw ex;
        }
    }

    private StatusExecucao statusFinal(StatusProcessamento statusColeta, TradeFactLoader.LoadResult carga) {
        if (statusColeta == StatusProcessamento.ERRO) {
            return StatusExecucao.FALHA;
        }
        return carga.stagingComErro() > 0 ? StatusExecucao.PARCIAL : StatusExecucao.SUCESSO;
    }
}
