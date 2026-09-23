package com.steeltrade.status;

import java.util.List;

import com.steeltrade.status.dto.SourceFreshnessResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}
