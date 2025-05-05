package com.matmode.processor_service.listener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricsDTO {
    private Double precision;
    private Double recall;
    private Double f1;

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getRecall() {
        return recall;
    }

    public void setRecall(Double recall) {
        this.recall = recall;
    }

    public Double getF1() {
        return f1;
    }

    public void setF1(Double f1) {
        this.f1 = f1;
    }
}