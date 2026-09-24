package com.steeltrade.status;

import java.util.List;

import com.steeltrade.shared.pagination.PageResponse;
import com.steeltrade.status.dto.ExecutionResponse;
import com.steeltrade.status.dto.SourceFreshnessResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/v1/status")
public class DataFreshnessController {

    private final DataFreshnessService dataFreshnessService;

    public DataFreshnessController(DataFreshnessService dataFreshnessService) {
        this.dataFreshnessService = dataFreshnessService;
    }

    @GetMapping("/freshness")
    public List<SourceFreshnessResponse> freshness() {
        return dataFreshnessService.consultarFontes();
    }

    @GetMapping("/executions")
    public PageResponse<ExecutionResponse> execucoes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return dataFreshnessService.listarExecucoes(page, size);
    }
}
