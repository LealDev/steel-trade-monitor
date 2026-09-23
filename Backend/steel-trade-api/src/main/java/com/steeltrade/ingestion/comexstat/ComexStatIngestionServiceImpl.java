package com.steeltrade.ingestion.comexstat;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.IngestionRunResponse;
import com.steeltrade.ingestion.staging.RawPayload;
import com.steeltrade.ingestion.staging.RawPayloadRepository;
import com.steeltrade.ingestion.staging.StatusProcessamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ComexStatIngestionServiceImpl implements ComexStatIngestionService {

    public static final String FONTE = "COMEXSTAT";

    private static final Logger log = LoggerFactory.getLogger(ComexStatIngestionServiceImpl.class);

    private final ComexStatGateway gateway;
    private final RawPayloadRepository rawPayloadRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ComexStatIngestionServiceImpl(ComexStatGateway gateway,
                                         RawPayloadRepository rawPayloadRepository,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.gateway = gateway;
        this.rawPayloadRepository = rawPayloadRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IngestionRunResponse coletarExportacoes(YearMonth de, YearMonth ate, int capitulo) {
        var resposta = gateway.buscarExportacoes(de, ate, capitulo);

        var status = resposta.sucesso() ? StatusProcessamento.PENDENTE : StatusProcessamento.ERRO;
        String mensagemErro = resposta.sucesso() ? null : "Fonte respondeu HTTP " + resposta.statusHttp();

        var staging = new RawPayload(
                FONTE,
                resposta.endpoint(),
                resposta.parametrosJson(),
                garantirJson(resposta.corpo()),
                resposta.statusHttp(),
                status,
                mensagemErro,
                OffsetDateTime.now(clock));
        staging = rawPayloadRepository.save(staging);

        log.info("evento=coleta_concluida fonte={} stagingId={} statusHttp={} status={}",
                FONTE, staging.getId(), resposta.statusHttp(), status);

        return new IngestionRunResponse(staging.getId(), resposta.statusHttp(), status,
                mensagemErro, staging.getColetadoEm());
    }

    /**
     * A coluna de staging é JSONB: corpo que não for JSON válido (ex.: página
     * HTML do Cloudflare) é envelopado como string JSON para não perder o dado.
     */
    private String garantirJson(String corpo) {
        try {
            objectMapper.readTree(corpo);
            return corpo;
        } catch (JacksonException ex) {
            return objectMapper.writeValueAsString(corpo);
        }
    }
}
