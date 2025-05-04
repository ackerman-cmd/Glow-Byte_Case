package com.matmode.processor_service.model;

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
    private Integer id;

    // Forecast date
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Warehouse number
    @Column(name = "warehouse", nullable = false)
    private Integer warehouse;

    // Stack number
    @Column(name = "stack_number", nullable = false)
    private Integer stackNumber;

    // Coal brand
    @Column(name = "coal_brand", nullable = false)
    private String coalBrand;

    // Fire label (0 = no fire, 1 = fire)
    @Column(name = "fire_label", nullable = false)
    private Integer fireLabel;

    // Fire probability (0.0 to 1.0)
    @Column(name = "fire_probability", nullable = false)
    private Double fireProbability;
}