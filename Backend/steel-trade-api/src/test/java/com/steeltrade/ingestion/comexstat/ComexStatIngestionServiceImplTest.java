package com.steeltrade.ingestion.comexstat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.ingestion.staging.RawPayload;
import com.steeltrade.ingestion.staging.RawPayloadRepository;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.warehouse.fact.Fluxo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComexStatIngestionServiceImplTest {

    private static final Instant AGORA = Instant.parse("2026-09-21T12:00:00Z");
    private static final YearMonth JUNHO = YearMonth.of(2025, 6);

    @Mock
    private ComexStatGateway gateway;

    @Mock
    private RawPayloadRepository repository;

    private ComexStatIngestionServiceImpl service;

    @BeforeEach
    void configurar() {
        service = new ComexStatIngestionServiceImpl(gateway, repository,
                JsonMapper.builder().build(), Clock.fixed(AGORA, ZoneOffset.UTC));
        when(repository.save(any(RawPayload.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void respostaDeSucessoViraStagingPendente() {
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{\"flow\":\"export\"}", 200, "{\"data\":{\"list\":[]}}"));

        var resultado = service.coletar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        var captor = ArgumentCaptor.forClass(RawPayload.class);
        verify(repository).save(captor.capture());
        var staging = captor.getValue();

        assertThat(staging.getFonte()).isEqualTo("COMEXSTAT");
        assertThat(staging.getEndpoint()).isEqualTo("/general?language=pt");
        assertThat(staging.getStatusProcessamento()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(staging.getPayload()).isEqualTo("{\"data\":{\"list\":[]}}");
        assertThat(staging.getColetadoEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC));
        assertThat(resultado.status()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(resultado.mensagemErro()).isNull();
    }

    @Test
    void respostaHttpDeErroViraDeadLetterSemQuebrarOFluxo() {
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{}", 429, "{\"error\":{\"code\":429}}"));

        var resultado = service.coletar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        var captor = ArgumentCaptor.forClass(RawPayload.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getStatusProcessamento()).isEqualTo(StatusProcessamento.ERRO);
        assertThat(captor.getValue().getMensagemErro()).contains("429");
        assertThat(resultado.status()).isEqualTo(StatusProcessamento.ERRO);
        assertThat(resultado.statusHttp()).isEqualTo(429);
    }

    @Test
    void corpoNaoJsonEEnvelopadoParaCaberNaColunaJsonb() {
        String html = "<html>Attention Required! | Cloudflare</html>";
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{}", 403, html));

        service.coletar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        var captor = ArgumentCaptor.forClass(RawPayload.class);
        verify(repository).save(captor.capture());

        String payloadArmazenado = captor.getValue().getPayload();
        // precisa ser JSON válido (string JSON com o HTML dentro)
        var lido = JsonMapper.builder().build().readTree(payloadArmazenado);
        assertThat(lido.isString()).isTrue();
        assertThat(lido.asString()).contains("Cloudflare");
    }
}
