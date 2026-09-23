package com.steeltrade.warehouse.fact;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

/**
 * INSERT ... ON CONFLICT DO UPDATE sobre a constraint única da chave natural.
 * Rodar duas vezes com o mesmo dado produz o mesmo estado final (idempotência).
 */
@Repository
public class TradeFactUpsertRepositoryImpl implements TradeFactUpsertRepository {

    private static final String SQL = """
            INSERT INTO fato_comercio_exterior
                (fluxo, dim_tempo_id, dim_ncm_id, dim_pais_id, dim_uf_id, dim_via_id,
                 kg_liquido, valor_fob_usd, carregado_em)
            VALUES (:fluxo, :tempoId, :ncmId, :paisId, :ufId, :viaId, :kg, :fob, now())
            ON CONFLICT ON CONSTRAINT uk_fato_chave_natural
            DO UPDATE SET kg_liquido    = EXCLUDED.kg_liquido,
                          valor_fob_usd = EXCLUDED.valor_fob_usd,
                          carregado_em  = now()
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void upsert(UpsertCommand comando) {
        entityManager.createNativeQuery(SQL)
                .setParameter("fluxo", comando.fluxo().name())
                .setParameter("tempoId", comando.tempoId())
                .setParameter("ncmId", comando.ncmId())
                .setParameter("paisId", comando.paisId())
                .setParameter("ufId", comando.ufId())
                .setParameter("viaId", comando.viaId())
                .setParameter("kg", comando.kgLiquido())
                .setParameter("fob", comando.valorFobUsd())
                .executeUpdate();
    }
}
