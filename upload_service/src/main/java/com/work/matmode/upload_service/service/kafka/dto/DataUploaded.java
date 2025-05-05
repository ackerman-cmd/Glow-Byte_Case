package com.work.matmode.upload_service.service.kafka.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
@Builder
public class DataUploaded {
    private List<String> fileNames;

    private String batchId;
}
