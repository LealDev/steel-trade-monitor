package com.steeltrade.warehouse.dimension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dim_uf")
public class FederativeUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sigla;

    @Column(nullable = false)
    private String nome;

    private String regiao;

    protected FederativeUnit() {
    }

    public FederativeUnit(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }
}
