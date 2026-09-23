package com.steeltrade.ingestion.comexstat;

import com.steeltrade.ingestion.comexstat.dto.IngestionRunRequest;
import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.ingestion.core.IngestionRunner;
import com.steeltrade.ingestion.core.StatusExecucao;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo manual do pipeline Comex Stat: coleta → staging → fato.
 * Em produção quem chama é o job agendado; esta rota existe para backfill
 * e desenvolvimento.
 */
@RestController
@RequestMapping("/v1/ingestion/comexstat")
public class ComexStatIngestionController {

    private final IngestionRunner ingestionRunner;

    public ComexStatIngestionController(IngestionRunner ingestionRunner) {
        this.ingestionRunner = ingestionRunner;
    }

    @PostMapping("/runs")
    public ResponseEntity<PipelineRunResponse> executar(@Valid @RequestBody IngestionRunRequest request) {
        var resultado = ingestionRunner.executarComexStat(request.from(), request.to(), request.chapter());
        var status = resultado.statusExecucao() == StatusExecucao.FALHA
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado);
    }
}
