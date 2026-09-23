package com.steeltrade.ingestion.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IngestionLogRepository extends JpaRepository<IngestionLog, Long> {

    @Query("select distinct l.fonte from IngestionLog l order by l.fonte")
    List<String> listarFontes();

    Optional<IngestionLog> findFirstByFonteOrderByIniciadoEmDesc(String fonte);

    Optional<IngestionLog> findFirstByFonteAndStatusOrderByFinalizadoEmDesc(String fonte, StatusExecucao status);
}
