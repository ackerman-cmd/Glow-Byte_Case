package com.matmode.processor_service.repository;

import com.matmode.processor_service.model.Fire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FireRepository extends JpaRepository<Fire, Long> {

    // Поиск по дате составления отчета
    List<Fire> findByReportDate(LocalDate reportDate);

    // Поиск по складу
    List<Fire> findByWarehouse(Integer warehouse);

    // Поиск по номеру штабеля
    List<Fire> findByStackNumber(Integer stackNumber);

    // Поиск по дате начала в указанном диапазоне
    List<Fire> findByStartDateTimeBetween(LocalDateTime start, LocalDateTime end);

    // Поиск по грузу (частичное совпадение)
    List<Fire> findByCargoContainingIgnoreCase(String cargo);
}