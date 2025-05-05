package com.matmode.processor_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metrics")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Batch ID of uploaded files
    @Column(name = "batch_id", nullable = false, unique = true)
    private String batchId;

    // F1 score
    @Column(name = "f1", nullable = false)
    private Double f1;

    // Precision score
    @Column(name = "precision", nullable = false)
    private Double precision;

    // Recall score
    @Column(name = "recall", nullable = false)
    private Double recall;
}