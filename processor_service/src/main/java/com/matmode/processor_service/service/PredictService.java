package com.matmode.processor_service.service;

import com.matmode.processor_service.model.Predict;
import com.matmode.processor_service.repository.PredictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PredictService {

    private final PredictRepository predictRepository;

    @Autowired
    public PredictService(PredictRepository predictRepository) {
        this.predictRepository = predictRepository;
    }

    // Сохранение нового прогноза
    public Predict savePredict(Predict predict) {
        validatePredict(predict);
        return predictRepository.save(predict);
    }

    // Получение записи по ID
    public Optional<Predict> findPredictById(Integer id) {
        return predictRepository.findById(id);
    }

    // Получение всех записей
    public List<Predict> findAllPredicts() {
        return predictRepository.findAll();
    }

    // Обновление записи
    public Predict updatePredict(Integer id, Predict updatedPredict) {
        Optional<Predict> existingPredict = predictRepository.findById(id);
        if (existingPredict.isEmpty()) {
            throw new IllegalArgumentException("Predict with ID " + id + " not found");
        }
        updatedPredict.setId(id);
        validatePredict(updatedPredict);
        return predictRepository.save(updatedPredict);
    }

    // Удаление записи
    public void deletePredict(Integer id) {
        if (!predictRepository.existsById(id)) {
            throw new IllegalArgumentException("Predict with ID " + id + " not found");
        }
        predictRepository.deleteById(id);
    }

    // Поиск по дате прогноза
    public List<Predict> findByDate(LocalDate date) {
        return predictRepository.findByDate(date);
    }

    // Поиск по складу
    public List<Predict> findByWarehouse(Integer warehouse) {
        return predictRepository.findByWarehouse(warehouse);
    }

    // Поиск по номеру штабеля
    public List<Predict> findByStackNumber(Integer stackNumber) {
        return predictRepository.findByStackNumber(stackNumber);
    }

    // Поиск по марке угля
    public List<Predict> findByCoalBrand(String coalBrand) {
        return predictRepository.findByCoalBrand(coalBrand);
    }

    // Поиск по метке возгорания
    public List<Predict> findByFireLabel(Integer fireLabel) {
        return predictRepository.findByFireLabel(fireLabel);
    }

    // Поиск записей с высокой вероятностью возгорания
    public List<Predict> findByFireProbabilityGreaterThanEqual(Double probability) {
        return predictRepository.findByFireProbabilityGreaterThanEqual(probability);
    }

    // Валидация данных
    private void validatePredict(Predict predict) {
        if (predict.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (predict.getCoalBrand() == null || predict.getCoalBrand().isBlank()) {
            throw new IllegalArgumentException("Coal brand cannot be null or empty");
        }
        if (predict.getFireProbability() < 0.0 || predict.getFireProbability() > 1.0) {
            throw new IllegalArgumentException("Fire probability must be between 0.0 and 1.0");
        }
        if (predict.getFireLabel() != 0 && predict.getFireLabel() != 1) {
            throw new IllegalArgumentException("Fire label must be 0 or 1");
        }
    }
}