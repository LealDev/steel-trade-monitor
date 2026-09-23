package com.steeltrade.warehouse.loader;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.steeltrade.ingestion.comexstat.ComexStatIngestionServiceImpl;
import com.steeltrade.ingestion.comexstat.ComexStatMapper;
import com.steeltrade.ingestion.staging.RawPayload;
import com.steeltrade.ingestion.staging.RawPayloadRepository;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.warehouse.fact.Fluxo;
import com.steeltrade.warehouse.fact.TradeFactUpsertRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TradeFactLoaderImpl implements TradeFactLoader {

    private static final Logger log = LoggerFactory.getLogger(TradeFactLoaderImpl.class);

    private final RawPayloadRepository rawPayloadRepository;
    private final ComexStatMapper mapper;
    private final DimensionSyncService dimensionSyncService;
    private final TradeFactUpsertRepository upsertRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public TradeFactLoaderImpl(RawPayloadRepository rawPayloadRepository,
                               ComexStatMapper mapper,
                               DimensionSyncService dimensionSyncService,
                               TradeFactUpsertRepository upsertRepository,
                               TransactionTemplate transactionTemplate,
                               Clock clock) {
        this.rawPayloadRepository = rawPayloadRepository;
        this.mapper = mapper;
        this.dimensionSyncService = dimensionSyncService;
        this.upsertRepository = upsertRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public LoadResult processarPendentes() {
        List<RawPayload> pendentes = rawPayloadRepository.findByStatusProcessamentoAndFonteOrderByIdAsc(
                StatusProcessamento.PENDENTE, ComexStatIngestionServiceImpl.FONTE);

        int processados = 0;
        int registrosCarregados = 0;
        int erros = 0;

        // Cada staging tem sua própria transação: uma falha não desfaz as
        // anteriores nem impede as próximas (a linha ruim vira dead letter).
        for (RawPayload staging : pendentes) {
            try {
                registrosCarregados += transactionTemplate.execute(status -> carregar(staging));
                processados++;
            } catch (Exception ex) {
                erros++;
                log.error("evento=carga_falhou stagingId={}", staging.getId(), ex);
                transactionTemplate.executeWithoutResult(status -> {
                    var deadLetter = rawPayloadRepository.findById(staging.getId()).orElseThrow();
                    deadLetter.marcarErro(ex.getMessage(), OffsetDateTime.now(clock));
                    rawPayloadRepository.save(deadLetter);
                });
            }
        }

        log.info("evento=carga_concluida stagingProcessados={} registrosCarregados={} erros={}",
                processados, registrosCarregados, erros);
        return new LoadResult(processados, registrosCarregados, erros);
    }

    private int carregar(RawPayload staging) {
        var registros = mapper.mapear(staging.getPayload());
        for (var registro : registros) {
            var chaves = dimensionSyncService.sincronizar(registro);
            upsertRepository.upsert(new TradeFactUpsertRepository.UpsertCommand(
                    Fluxo.EXPORT,
                    chaves.tempoId(), chaves.ncmId(), chaves.paisId(), chaves.ufId(), chaves.viaId(),
                    registro.kgLiquido(), registro.valorFobUsd()));
        }
        var gerenciado = rawPayloadRepository.findById(staging.getId()).orElseThrow();
        gerenciado.marcarProcessado(OffsetDateTime.now(clock));
        rawPayloadRepository.save(gerenciado);
        return registros.size();
    }
}
