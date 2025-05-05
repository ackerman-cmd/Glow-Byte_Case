package com.matmode.processor_service.web;


import com.matmode.processor_service.model.Metrics;
import com.matmode.processor_service.repository.MetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    private final MetricsRepository metricsRepository;

    @Autowired
    public MetricController(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    // Get all metrics
    @GetMapping
    public ResponseEntity<List<Metrics>> getAllMetrics() {
        List<Metrics> metrics = metricsRepository.findAll();
        return ResponseEntity.ok(metrics);
    }

    // Get metrics by batchId
    @GetMapping("/{batchId}")
    public ResponseEntity<Metrics> getMetricsByBatchId(@PathVariable String batchId) {
        Optional<Metrics> metrics = metricsRepository.findByBatchId(batchId);
        return metrics.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
