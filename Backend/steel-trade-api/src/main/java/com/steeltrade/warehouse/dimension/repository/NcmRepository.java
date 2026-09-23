package com.steeltrade.warehouse.dimension.repository;

import java.util.Optional;

import com.steeltrade.warehouse.dimension.Ncm;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NcmRepository extends JpaRepository<Ncm, Long> {

    Optional<Ncm> findByCodigoNcm(String codigoNcm);
}
