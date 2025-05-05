package com.matmode.processor_service.repository;

import com.matmode.processor_service.model.Metrics;
import com.matmode.processor_service.model.Predict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricsRepository extends JpaRepository<Metrics, Integer> {

    Optional<Metrics> findByBatchId(String batchId);
}
