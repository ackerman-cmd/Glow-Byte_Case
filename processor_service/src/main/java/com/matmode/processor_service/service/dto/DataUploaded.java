package com.matmode.processor_service.service.dto;


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
