from fastapi import FastAPI, UploadFile, HTTPException, Form
from pydantic import BaseModel
from typing import List
import logging
from datetime import date
from joblib import load
from sklearn.metrics import roc_auc_score, precision_score, recall_score, f1_score
import pandas as pd
from io import StringIO
import os
from fastapi import File

# Настройка логирования
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title="Fire Prediction API", description="API для предсказания вероятности возгорания по CSV")

# Pydantic-модели
class Predict(BaseModel):
    date: date
    warehouse: int
    stack_number: int
    coal_brand: str
    fire_label: int
    fire_probability: float
    batch_id: str

    class Config:
        json_encoders = {
            date: lambda v: v.isoformat()
        }

class Metrics(BaseModel):
    # roc_auc: float
    precision: float
    recall: float
    f1: float

class PredictResponse(BaseModel):
    predictions: List[Predict]
    metrics: Metrics




def make_predict(df, batch_id):
    try:
        df = df.drop(['Возгорание', 'date'], axis=1)
        df = df.dropna()

        # Целевая переменная
        target = df['is_fire']
        df = df.drop('is_fire', axis=1)

        # Загрузка модели
        model = load('trained_model.joblib')

        # Преобразование столбца 'Дата' в datetime и создание новых столбцов
        df['Дата'] = pd.to_datetime(df['Дата'], errors='coerce')
        df['Год'] = df['Дата'].dt.year
        df['Месяц'] = df['Дата'].dt.month
        df['День'] = df['Дата'].dt.day

        # Убираем 'Дата' после создания новых столбцов
        data = df['Дата']
        df = df.drop('Дата', axis=1)

        # Предсказания и вероятности
        fire_label = model.predict(df)
        fire_probability = model.predict_proba(df)[:, 1]

        # Метрики
        # roc_auc = roc_auc_score(target, fire_probability)
        precision = precision_score(target, fire_label)
        recall = recall_score(target, fire_label)
        f1 = f1_score(target, fire_label)
        # Создаем итоговый JSON
        output_json = {
            'date': data.tolist(),
            'warehouse': df['Склад'].tolist(),
            'stack_number': df['Штабель'].tolist(),  # исправлено, чтобы использовать 'Штабель'
            'coal_brand': ['A1'] * len(df),
            'fire_label': fire_label.tolist(),
            'fire_probability': fire_probability.tolist(),
            # 'roc-auc': roc_auc,
            'precision': precision,
            'recall': recall,
            'f1': f1
        }

        return output_json

    except Exception as e:
        # Логирование ошибки
        print(f"Ошибка при обработке: {str(e)}")
        raise


# Роут для загрузки одного CSV файла
@app.post("/predict/", response_model=PredictResponse)
async def predict(file: UploadFile = File(...), batch_id: str = Form(...)):
    logger.info(f"Получен файл: {file.filename}, batch_id: {batch_id}")

    if not file.filename.endswith('.csv'):
        raise HTTPException(status_code=400, detail="Файл должен быть в формате CSV")

    try:
        content = await file.read()
        try:
            csv_content = content.decode('utf-8')
        except UnicodeDecodeError:
            raise HTTPException(status_code=400, detail="Файл не в формате UTF-8")

        try:
            df = pd.read_csv(StringIO(csv_content))
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Ошибка чтения CSV: {e}")

        logger.info(f"Обрабатываем файл: {file.filename}, строк: {len(df)}")
        result = make_predict(df, batch_id)

        predictions = [
            Predict(
                date=result['date'][i],
                warehouse=result['warehouse'][i],
                stack_number=result['stack_number'][i],
                coal_brand=result['coal_brand'][i],
                fire_label=int(result['fire_label'][i]),
                fire_probability=float(result['fire_probability'][i]),
                batch_id=batch_id
            )
            for i in range(len(result['date']))
        ]

        metrics = Metrics(
            # roc_auc=result['roc_auc'],
            precision=result['precision'],
            recall=result['recall'],
            f1=result['f1']
        )

        logger.info(f"Готово {len(predictions)} предсказаний для batch_id {batch_id}")
        return PredictResponse(predictions=predictions, metrics=metrics)

    except Exception as e:
        logger.error(f"Ошибка в обработке файла: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Ошибка обработки: {str(e)}")
