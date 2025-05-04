package com.matmode.processor_service.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStatusMessage {
    private String type;
    private String batchId;
    private String message;
    private Object data; // Для дополнительных данных, например, метрик
}