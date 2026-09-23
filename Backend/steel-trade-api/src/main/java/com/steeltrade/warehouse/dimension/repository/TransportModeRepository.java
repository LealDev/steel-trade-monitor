package com.steeltrade.warehouse.dimension.repository;

import java.util.Optional;

import com.steeltrade.warehouse.dimension.TransportMode;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportModeRepository extends JpaRepository<TransportMode, Long> {

    Optional<TransportMode> findByDescricao(String descricao);
}
