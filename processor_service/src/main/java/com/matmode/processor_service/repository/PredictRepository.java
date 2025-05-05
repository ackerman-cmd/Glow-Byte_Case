package com.matmode.processor_service.repository;

import com.matmode.processor_service.model.Predict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PredictRepository extends JpaRepository<Predict, Integer> {

    // Поиск по дате прогноза
    List<Predict> findByDate(LocalDate date);

    // Поиск по складу
    List<Predict> findByWarehouse(Integer warehouse);

    // Поиск по номеру штабеля
    List<Predict> findByStackNumber(Integer stackNumber);

    // Поиск по марке угля
    List<Predict> findByCoalBrand(String coalBrand);

    // Поиск по метке возгорания
    List<Predict> findByFireLabel(Integer fireLabel);

    // Поиск записей с вероятностью возгорания выше заданного значения
    List<Predict> findByFireProbabilityGreaterThanEqual(Double probability);

    List<Predict> findByBatchId(String batchId);
}