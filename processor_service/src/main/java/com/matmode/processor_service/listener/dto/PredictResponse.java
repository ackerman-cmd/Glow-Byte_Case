package com.matmode.processor_service.listener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PredictResponse {
    @JsonProperty("predictions")
    private List<PredictDTO> predictions;

    @JsonProperty("metrics")
    private MetricsDTO metrics;

    public List<PredictDTO> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<PredictDTO> predictions) {
        this.predictions = predictions;
    }

    public MetricsDTO getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsDTO metrics) {
        this.metrics = metrics;
    }
}