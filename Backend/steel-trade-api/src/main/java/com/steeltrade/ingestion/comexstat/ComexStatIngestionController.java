package com.steeltrade.ingestion.comexstat;

import com.steeltrade.ingestion.comexstat.dto.IngestionRunRequest;
import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.ingestion.core.IngestionRunner;
import com.steeltrade.ingestion.core.StatusExecucao;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo manual do pipeline Comex Stat: coleta → staging → fato.
 * Em produção quem chama é o job agendado; esta rota existe para backfill
 * e desenvolvimento — por isso, quando app.ingestion.token está definido,
 * ela exige o header X-Ingestion-Token (senão qualquer um queimaria a
 * cota da fonte num servidor público).
 */
@RestController
@RequestMapping("/v1/ingestion/comexstat")
public class ComexStatIngestionController {

    static final String HEADER_TOKEN = "X-Ingestion-Token";

    private final IngestionRunner ingestionRunner;
    private final String tokenConfigurado;

    public ComexStatIngestionController(IngestionRunner ingestionRunner,
                                        @Value("${app.ingestion.token:}") String tokenConfigurado) {
        this.ingestionRunner = ingestionRunner;
        this.tokenConfigurado = tokenConfigurado;
    }

    @PostMapping("/runs")
    public ResponseEntity<PipelineRunResponse> executar(
            @Valid @RequestBody IngestionRunRequest request,
            @RequestHeader(name = HEADER_TOKEN, required = false) String token) {
        if (StringUtils.hasText(tokenConfigurado) && !tokenConfigurado.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var resultado = ingestionRunner.executarComexStat(request.fluxoOuPadrao(),
                request.from(), request.to(), request.chapter());
        var status = resultado.statusExecucao() == StatusExecucao.FALHA
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado);
    }
}
