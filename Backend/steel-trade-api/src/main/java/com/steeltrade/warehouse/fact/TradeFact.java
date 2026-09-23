package com.steeltrade.warehouse.fact;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.steeltrade.warehouse.dimension.Country;
import com.steeltrade.warehouse.dimension.FederativeUnit;
import com.steeltrade.warehouse.dimension.Ncm;
import com.steeltrade.warehouse.dimension.TimeDimension;
import com.steeltrade.warehouse.dimension.TransportMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Fato de comércio exterior.
 * Grão: uma linha = um mês, um NCM, um país, uma UF, uma via, um fluxo.
 * A escrita é feita por upsert nativo ({@link TradeFactUpsertRepository});
 * esta entidade existe para leitura analítica.
 */
@Entity
@Table(name = "fato_comercio_exterior")
public class TradeFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fluxo fluxo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_tempo_id", nullable = false)
    private TimeDimension tempo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_ncm_id", nullable = false)
    private Ncm ncm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_pais_id", nullable = false)
    private Country pais;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_uf_id", nullable = false)
    private FederativeUnit uf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_via_id", nullable = false)
    private TransportMode via;

    @Column(name = "kg_liquido", nullable = false)
    private BigDecimal kgLiquido;

    @Column(name = "valor_fob_usd", nullable = false)
    private BigDecimal valorFobUsd;

    @Column(name = "quantidade_estatistica")
    private BigDecimal quantidadeEstatistica;

    @Column(name = "carregado_em", nullable = false)
    private OffsetDateTime carregadoEm;

    protected TradeFact() {
    }

    public Long getId() {
        return id;
    }

    public Fluxo getFluxo() {
        return fluxo;
    }

    public TimeDimension getTempo() {
        return tempo;
    }

    public Ncm getNcm() {
        return ncm;
    }

    public Country getPais() {
        return pais;
    }

    public FederativeUnit getUf() {
        return uf;
    }

    public TransportMode getVia() {
        return via;
    }

    public BigDecimal getKgLiquido() {
        return kgLiquido;
    }

    public BigDecimal getValorFobUsd() {
        return valorFobUsd;
    }

    public BigDecimal getQuantidadeEstatistica() {
        return quantidadeEstatistica;
    }

    public OffsetDateTime getCarregadoEm() {
        return carregadoEm;
    }
}
