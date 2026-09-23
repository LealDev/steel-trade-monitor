package com.steeltrade.status;

import java.util.List;

import com.steeltrade.status.dto.SourceFreshnessResponse;

public interface DataFreshnessService {

    /** Uma entrada por fonte já executada, com a última execução e o último sucesso. */
    List<SourceFreshnessResponse> consultarFontes();
}
