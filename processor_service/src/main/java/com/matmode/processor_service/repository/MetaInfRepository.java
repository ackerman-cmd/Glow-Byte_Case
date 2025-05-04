package com.matmode.processor_service.repository;

import com.matmode.processor_service.model.MetaInf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetaInfRepository extends JpaRepository<MetaInf, Integer> {

    // Поиск по batchId
    Optional<MetaInf> findByBatchId(String batchId);

    // Проверка существования по batchId
    boolean existsByBatchId(String batchId);

    // Поиск по имени целевого файла
    Optional<MetaInf> findByTargetFileName(String targetFileName);
}