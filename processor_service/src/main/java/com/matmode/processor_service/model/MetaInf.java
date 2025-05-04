package com.matmode.processor_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meta")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MetaInf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private String batchId;

    @Column(name = "file_names", columnDefinition = "text[]", nullable = false)
    private String[] fileNames;

    @Column(name = "target_file_name", nullable = false)
    private String targetFileName;

    @Column(name = "upload_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime uploadTime;
}