package com.steeltrade.status;

import java.util.List;

import com.steeltrade.ingestion.core.IngestionLog;
import com.steeltrade.ingestion.core.IngestionLogRepository;
import com.steeltrade.ingestion.core.StatusExecucao;
import com.steeltrade.status.dto.SourceFreshnessResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataFreshnessServiceImpl implements DataFreshnessService {

    private final IngestionLogRepository ingestionLogRepository;

    public DataFreshnessServiceImpl(IngestionLogRepository ingestionLogRepository) {
        this.ingestionLogRepository = ingestionLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceFreshnessResponse> consultarFontes() {
        return ingestionLogRepository.listarFontes().stream()
                .map(this::freshnessDaFonte)
                .toList();
    }

    private SourceFreshnessResponse freshnessDaFonte(String fonte) {
        IngestionLog ultima = ingestionLogRepository.findFirstByFonteOrderByIniciadoEmDesc(fonte).orElseThrow();
        var ultimoSucesso = ingestionLogRepository
                .findFirstByFonteAndStatusOrderByFinalizadoEmDesc(fonte, StatusExecucao.SUCESSO)
                .map(IngestionLog::getFinalizadoEm)
                .orElse(null);
        return new SourceFreshnessResponse(
                fonte,
                ultima.getStatus(),
                ultima.getIniciadoEm(),
                ultima.getFinalizadoEm(),
                ultima.getPeriodoReferencia(),
                ultima.getRegistrosGravados(),
                ultima.getMensagem(),
                ultimoSucesso);
    }
}
