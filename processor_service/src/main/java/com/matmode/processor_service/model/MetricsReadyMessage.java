package com.matmode.processor_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsReadyMessage {
    private String status;
    private String batchId;
    private String message;
    private Object data;
}
