package com.steeltrade.ingestion.core;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import com.steeltrade.ingestion.comexstat.ComexStatIngestionService;
import com.steeltrade.ingestion.comexstat.dto.IngestionRunResponse;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.shared.config.ComexStatProperties;
import com.steeltrade.shared.exception.ExternalSourceException;
import com.steeltrade.warehouse.fact.Fluxo;
import com.steeltrade.warehouse.loader.TradeFactLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionRunnerImplTest {

    private static final Instant AGORA = Instant.parse("2026-09-23T03:00:00Z");
    private static final YearMonth ABRIL = YearMonth.of(2025, 4);
    private static final YearMonth JUNHO = YearMonth.of(2025, 6);

    @Mock
    private ComexStatIngestionService ingestionService;

    @Mock
    private TradeFactLoader tradeFactLoader;

    @Mock
    private IngestionLogRepository ingestionLogRepository;

    private IngestionRunnerImpl runner;

    @BeforeEach
    void configurar() {
        // pausa 0 nos testes
        var properties = new ComexStatProperties("https://x", "ua", 6, 0);
        runner = new IngestionRunnerImpl(ingestionService, tradeFactLoader, ingestionLogRepository,
                properties, Clock.fixed(AGORA, ZoneOffset.UTC));
        when(ingestionLogRepository.save(any(IngestionLog.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private IngestionRunResponse coleta(StatusProcessamento status) {
        return new IngestionRunResponse(1L, status == StatusProcessamento.ERRO ? 429 : 200,
                status, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void coletaMesAMesUmaChamadaPorMesDaJanela() {
        when(ingestionService.coletar(any(), any(), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.PENDENTE));
        when(tradeFactLoader.processarPendentes()).thenReturn(new TradeFactLoader.LoadResult(3, 3000, 0));

        var resultado = runner.executarComexStat(Fluxo.EXPORT, ABRIL, JUNHO, 72);

        // abril, maio e junho — cada mês na sua própria chamada (a API trunca janelas longas)
        verify(ingestionService, times(3)).coletar(eq(Fluxo.EXPORT), any(), any(), eq(72));
        verify(ingestionService).coletar(Fluxo.EXPORT, ABRIL, ABRIL, 72);
        verify(ingestionService).coletar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.SUCESSO);
        assertThat(resultado.mesesColetados()).isEqualTo(3);
        assertThat(resultado.mesesComErro()).isZero();
        assertThat(resultado.carga().registrosCarregados()).isEqualTo(3000);

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        var execucao = captor.getValue();
        assertThat(execucao.getStatus()).isEqualTo(StatusExecucao.SUCESSO);
        assertThat(execucao.getPeriodoReferencia()).isEqualTo("EXPORT cap72 2025-04..2025-06");
        assertThat(execucao.getRegistrosGravados()).isEqualTo(3000);
    }

    @Test
    void mesComFalhaNaoImpedeOsDemaisEExecucaoFicaParcial() {
        when(ingestionService.coletar(eq(Fluxo.EXPORT), eq(ABRIL), eq(ABRIL), anyInt()))
                .thenThrow(new ExternalSourceException("timeout", null));
        when(ingestionService.coletar(eq(Fluxo.EXPORT), eq(YearMonth.of(2025, 5)), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.PENDENTE));
        when(ingestionService.coletar(eq(Fluxo.EXPORT), eq(JUNHO), eq(JUNHO), anyInt()))
                .thenReturn(coleta(StatusProcessamento.ERRO));
        when(tradeFactLoader.processarPendentes()).thenReturn(new TradeFactLoader.LoadResult(1, 1000, 0));

        var resultado = runner.executarComexStat(Fluxo.EXPORT, ABRIL, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.PARCIAL);
        assertThat(resultado.mesesColetados()).isEqualTo(1);
        assertThat(resultado.mesesComErro()).isEqualTo(2);

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).contains("2 mes(es)");
    }

    @Test
    void todosOsMesesFalhandoRegistraFalhaSemDispararACarga() {
        when(ingestionService.coletar(any(), any(), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.ERRO));

        var resultado = runner.executarComexStat(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.FALHA);
        verify(tradeFactLoader, never()).processarPendentes();
    }

    @Test
    void cargaComDeadLetterRegistraExecucaoParcial() {
        when(ingestionService.coletar(any(), any(), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.PENDENTE));
        when(tradeFactLoader.processarPendentes()).thenReturn(new TradeFactLoader.LoadResult(1, 500, 1));

        var resultado = runner.executarComexStat(Fluxo.IMPORT, JUNHO, JUNHO, 73);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.PARCIAL);

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getPeriodoReferencia()).isEqualTo("IMPORT cap73 2025-06..2025-06");
    }

    @Test
    void excecaoInesperadaRegistraFalhaEPropaga() {
        when(ingestionService.coletar(any(), any(), any(), anyInt()))
                .thenThrow(new IllegalStateException("erro inesperado"));

        try {
            runner.executarComexStat(Fluxo.EXPORT, JUNHO, JUNHO, 72);
        } catch (IllegalStateException esperada) {
            // propagada após registrar
        }

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusExecucao.FALHA);
        assertThat(captor.getValue().getFinalizadoEm()).isNotNull();
    }
}
