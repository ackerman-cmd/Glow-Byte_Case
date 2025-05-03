CREATE TABLE meta_information
(
    id UUID DEFAULT generateUUIDv4(),
    data_file_names Array(String),
    target_file_name String,
    upload_time DateTime DEFAULT now()
)
    ENGINE = MergeTree()
ORDER BY upload_time;


CREATE TABLE model_metrics
(
    meta_id UUID,
    precision Float32,
    recall Float32,
    roc_auc Float32,
    evaluated_at DateTime DEFAULT now()
)
    ENGINE = MergeTree()
ORDER BY evaluated_at;


CREATE TABLE report_table
(
    id UUID DEFAULT generateUUIDv4(),
    meta_id UUID,
    prediction_date Date,
    warehouse_id String,
    pile_id String,
    prediction UInt8,         -- 0 или 1
    probability Float32,
    target Nullable(UInt8),   -- заполняется после загрузки таргета
    precision Nullable(Float32),
    recall Nullable(Float32),
    f1 Nullable(Float32),
    created_at DateTime DEFAULT now()
)
    ENGINE = MergeTree()
PARTITION BY toYYYYMM(prediction_date)
ORDER BY (prediction_date, warehouse_id, pile_id);
