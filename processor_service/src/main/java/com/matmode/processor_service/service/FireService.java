package com.matmode.processor_service.service;


import com.matmode.processor_service.model.Fire;
import com.matmode.processor_service.repository.FireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FireService {

    private final FireRepository fireRepository;

    @Autowired
    public FireService(FireRepository fireRepository) {
        this.fireRepository = fireRepository;
    }

    // Сохранение новой записи о возгорании
    public Fire saveFire(Fire fire) {
        validateFire(fire);
        return fireRepository.save(fire);
    }

    // Получение записи по ID
    public Optional<Fire> findFireById(Long id) {
        return fireRepository.findById(id);
    }

    // Получение всех записей
    public List<Fire> findAllFires() {
        return fireRepository.findAll();
    }

    // Обновление записи
    public Fire updateFire(Long id, Fire updatedFire) {
        Optional<Fire> existingFire = fireRepository.findById(id);
        if (existingFire.isEmpty()) {
            throw new IllegalArgumentException("Fire with ID " + id + " not found");
        }
        updatedFire.setId(id);
        validateFire(updatedFire);
        return fireRepository.save(updatedFire);
    }

    // Удаление записи
    public void deleteFire(Long id) {
        if (!fireRepository.existsById(id)) {
            throw new IllegalArgumentException("Fire with ID " + id + " not found");
        }
        fireRepository.deleteById(id);
    }

    // Поиск по дате составления отчета
    public List<Fire> findFiresByReportDate(LocalDate reportDate) {
        return fireRepository.findByReportDate(reportDate);
    }

    // Поиск по складу
    public List<Fire> findFiresByWarehouse(Integer warehouse) {
        return fireRepository.findByWarehouse(warehouse);
    }

    // Поиск по номеру штабеля
    public List<Fire> findFiresByStackNumber(Integer stackNumber) {
        return fireRepository.findByStackNumber(stackNumber);
    }

    // Поиск по диапазону дат начала
    public List<Fire> findFiresByStartDateTimeBetween(LocalDateTime start, LocalDateTime end) {
        return fireRepository.findByStartDateTimeBetween(start, end);
    }

    // Валидация данных
    private void validateFire(Fire fire) {
        if (fire.getCargo() == null || fire.getCargo().isBlank()) {
            throw new IllegalArgumentException("Cargo cannot be null or empty");
        }
        if (fire.getWeight() != null && fire.getWeight() < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
    }
}
