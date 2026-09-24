package com.steeltrade.status;

import java.util.List;

import com.steeltrade.shared.pagination.PageResponse;
import com.steeltrade.status.dto.ExecutionResponse;
import com.steeltrade.status.dto.SourceFreshnessResponse;

public interface DataFreshnessService {

    /** Uma entrada por fonte já executada, com a última execução e o último sucesso. */
    List<SourceFreshnessResponse> consultarFontes();

    /** Histórico de execuções, da mais recente para a mais antiga. */
    PageResponse<ExecutionResponse> listarExecucoes(int page, int size);
}
