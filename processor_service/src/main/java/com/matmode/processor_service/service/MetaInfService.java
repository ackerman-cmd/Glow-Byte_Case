package com.matmode.processor_service.service;


import com.matmode.processor_service.model.MetaInf;
import com.matmode.processor_service.repository.MetaInfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MetaInfService {

    private final MetaInfRepository metaInfRepository;

    @Autowired
    public MetaInfService(MetaInfRepository metaInfRepository) {
        this.metaInfRepository = metaInfRepository;
    }

    // Сохранение новой метаинформации
    public MetaInf saveMetaInf(MetaInf metaInf) {
        validateMetaInf(metaInf);
        return metaInfRepository.save(metaInf);
    }

    // Получение записи по ID
    public Optional<MetaInf> findMetaInfById(Integer id) {
        return metaInfRepository.findById(id);
    }

    // Получение всех записей
    public List<MetaInf> findAllMetaInf() {
        return metaInfRepository.findAll();
    }

    // Обновление записи
    public MetaInf updateMetaInf(Integer id, MetaInf updatedMetaInf) {
        Optional<MetaInf> existingMetaInf = metaInfRepository.findById(id);
        if (existingMetaInf.isEmpty()) {
            throw new IllegalArgumentException("MetaInf with ID " + id + " not found");
        }
        updatedMetaInf.setId(id);
        validateMetaInf(updatedMetaInf);
        return metaInfRepository.save(updatedMetaInf);
    }

    // Удаление записи
    public void deleteMetaInf(Integer id) {
        if (!metaInfRepository.existsById(id)) {
            throw new IllegalArgumentException("MetaInf with ID " + id + " not found");
        }
        metaInfRepository.deleteById(id);
    }

    // Поиск по batchId
    public Optional<MetaInf> findByBatchId(String batchId) {
        return metaInfRepository.findByBatchId(batchId);
    }

    // Проверка существования по batchId
    public boolean existsByBatchId(String batchId) {
        return metaInfRepository.existsByBatchId(batchId);
    }

    // Поиск по имени целевого файла
    public Optional<MetaInf> findByTargetFileName(String targetFileName) {
        return metaInfRepository.findByTargetFileName(targetFileName);
    }

    // Валидация данных
    private void validateMetaInf(MetaInf metaInf) {
        if (metaInf.getBatchId() == null || metaInf.getBatchId().isBlank()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty");
        }
        if (metaInf.getFileNames() == null || metaInf.getFileNames().length == 0) {
            throw new IllegalArgumentException("File names cannot be null or empty");
        }
        if (metaInf.getTargetFileName() == null || metaInf.getTargetFileName().isBlank()) {
            throw new IllegalArgumentException("Target file name cannot be null or empty");
        }
    }
}
