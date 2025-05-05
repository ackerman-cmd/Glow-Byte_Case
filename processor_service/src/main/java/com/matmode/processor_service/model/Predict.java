package com.matmode.processor_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "predict")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Predict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private Integer id;

    // Forecast date
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Warehouse number
    @Column(name = "warehouse", nullable = false)
    private Integer warehouse;

    // Stack number
    @Column(name = "stack_number", nullable = false)
    @JsonProperty("stack_number")
    private Integer stackNumber;

    // Coal brand
    @Column(name = "coal_brand", nullable = false)
    @JsonProperty("coal_brand")
    private String coalBrand;

    // Fire label (0 = no fire, 1 = fire)
    @Column(name = "fire_label", nullable = false)
    @JsonProperty("fire_label")
    private Integer fireLabel;

    // Fire probability (0.0 to 1.0)
    @Column(name = "fire_probability", nullable = false)
    @JsonProperty("fire_probability")
    private Double fireProbability;

    @Column(name = "batch_id", nullable = false)
    @JsonProperty("batch_id")
    private String batchId;
}