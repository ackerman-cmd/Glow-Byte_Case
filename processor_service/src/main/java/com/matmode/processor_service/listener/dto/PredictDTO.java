package com.matmode.processor_service.listener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class PredictDTO {
    private LocalDate date;
    private Integer warehouse;
    @JsonProperty("stack_number")
    private Integer stackNumber;
    @JsonProperty("coal_brand")
    private String coalBrand;
    @JsonProperty("fire_label")
    private Integer fireLabel;
    @JsonProperty("fire_probability")
    private Double fireProbability;
    @JsonProperty("batch_id")
    private String batchId;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Integer warehouse) {
        this.warehouse = warehouse;
    }

    public Integer getStackNumber() {
        return stackNumber;
    }

    public void setStackNumber(Integer stackNumber) {
        this.stackNumber = stackNumber;
    }

    public String getCoalBrand() {
        return coalBrand;
    }

    public void setCoalBrand(String coalBrand) {
        this.coalBrand = coalBrand;
    }

    public Integer getFireLabel() {
        return fireLabel;
    }

    public void setFireLabel(Integer fireLabel) {
        this.fireLabel = fireLabel;
    }

    public Double getFireProbability() {
        return fireProbability;
    }

    public void setFireProbability(Double fireProbability) {
        this.fireProbability = fireProbability;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}