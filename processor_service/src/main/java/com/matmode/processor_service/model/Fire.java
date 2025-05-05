package com.matmode.processor_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fires")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Fire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "cargo")
    private String cargo;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "warehouse")
    private Integer warehouse;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "stack_forming_start")
    private LocalDateTime stackFormingStart;

    @Column(name = "stack_number")
    private Integer stackNumber;

    @Column(name = "batch_id", nullable = false)
    private String batchId;
}