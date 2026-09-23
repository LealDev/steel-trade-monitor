package com.steeltrade.warehouse.dimension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * O /general do Comex Stat só traz o nome do país — por isso a chave de
 * lookup é nome_pt; código e ISO ficam para enriquecimento futuro.
 */
@Entity
@Table(name = "dim_pais")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    @Column(name = "nome_pt", nullable = false)
    private String nomePt;

    @Column(name = "nome_en")
    private String nomeEn;

    @Column(name = "bloco_economico")
    private String blocoEconomico;

    @Column(name = "iso_alpha3")
    private String isoAlpha3;

    protected Country() {
    }

    public Country(String nomePt) {
        this.nomePt = nomePt;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNomePt() {
        return nomePt;
    }

    public String getNomeEn() {
        return nomeEn;
    }

    public String getBlocoEconomico() {
        return blocoEconomico;
    }

    public String getIsoAlpha3() {
        return isoAlpha3;
    }
}
