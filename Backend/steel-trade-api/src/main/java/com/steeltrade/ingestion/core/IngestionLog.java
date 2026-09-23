package com.steeltrade.ingestion.core;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma linha por rodada de ingestão (tabela ctl_execucao_ingestao).
 * É o histórico operacional: alimenta a tela de freshness e responde
 * "a última carga funcionou?" sem precisar abrir o banco.
 */
@Entity
@Table(name = "ctl_execucao_ingestao")
public class IngestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fonte;

    @Column(name = "iniciado_em", nullable = false)
    private OffsetDateTime iniciadoEm;

    @Column(name = "finalizado_em")
    private OffsetDateTime finalizadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusExecucao status;

    @Column(name = "registros_lidos")
    private Integer registrosLidos;

    @Column(name = "registros_gravados")
    private Integer registrosGravados;

    @Column(name = "periodo_referencia")
    private String periodoReferencia;

    private String mensagem;

    protected IngestionLog() {
    }

    public static IngestionLog iniciar(String fonte, String periodoReferencia, OffsetDateTime quando) {
        var execucao = new IngestionLog();
        execucao.fonte = fonte;
        execucao.periodoReferencia = periodoReferencia;
        execucao.iniciadoEm = quando;
        execucao.status = StatusExecucao.EM_ANDAMENTO;
        return execucao;
    }

    public void finalizar(StatusExecucao statusFinal, Integer registrosLidos, Integer registrosGravados,
                          String mensagem, OffsetDateTime quando) {
        this.status = statusFinal;
        this.registrosLidos = registrosLidos;
        this.registrosGravados = registrosGravados;
        this.mensagem = mensagem;
        this.finalizadoEm = quando;
    }

    public Long getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
    }

    public OffsetDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public OffsetDateTime getFinalizadoEm() {
        return finalizadoEm;
    }

    public StatusExecucao getStatus() {
        return status;
    }

    public Integer getRegistrosLidos() {
        return registrosLidos;
    }

    public Integer getRegistrosGravados() {
        return registrosGravados;
    }

    public String getPeriodoReferencia() {
        return periodoReferencia;
    }

    public String getMensagem() {
        return mensagem;
    }
}
