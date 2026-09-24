package com.steeltrade.status;

import java.util.List;

import com.steeltrade.ingestion.core.IngestionLog;
import com.steeltrade.ingestion.core.IngestionLogRepository;
import com.steeltrade.ingestion.core.StatusExecucao;
import com.steeltrade.shared.pagination.PageResponse;
import com.steeltrade.status.dto.ExecutionResponse;
import com.steeltrade.status.dto.SourceFreshnessResponse;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExecutionResponse> listarExecucoes(int page, int size) {
        var pagina = ingestionLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "iniciadoEm")));
        return PageResponse.de(pagina, execucao -> new ExecutionResponse(
                execucao.getId(),
                execucao.getFonte(),
                execucao.getStatus(),
                execucao.getIniciadoEm(),
                execucao.getFinalizadoEm(),
                execucao.getPeriodoReferencia(),
                execucao.getRegistrosGravados(),
                execucao.getMensagem()));
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
