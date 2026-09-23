package com.steeltrade.ingestion.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawPayloadRepository extends JpaRepository<RawPayload, Long> {

    List<RawPayload> findByStatusProcessamentoAndFonteOrderByIdAsc(StatusProcessamento status, String fonte);
}
