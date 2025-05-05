package com.work.matmode.upload_service.service.kafka.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TargetUploaded {

    private String fileName;

    private String batchId;
}
