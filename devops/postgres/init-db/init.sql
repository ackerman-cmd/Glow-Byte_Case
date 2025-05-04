-- Table for storing fire incident data
CREATE TABLE fires (
                       id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                       report_date DATE,                    -- Report date
                       cargo VARCHAR(255),                  -- Cargo type
                       weight DOUBLE PRECISION,             -- Weight in tons
                       warehouse INTEGER,                   -- Warehouse number
                       start_date_time TIMESTAMP,           -- Start date and time
                       end_date_time TIMESTAMP,             -- End date and time
                       stack_forming_start TIMESTAMP,       -- Stack forming start time
                       stack_number INTEGER                 -- Stack number
);

-- Table for storing metadata of uploaded files
CREATE TABLE meta (
                      id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                      batch_id VARCHAR(255) NOT NULL UNIQUE,      -- Batch ID
                      file_names TEXT[] NOT NULL,                 -- Array of file names
                      target_file_name VARCHAR(255) NOT NULL,     -- Target file name
                      upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Upload timestamp
);

-- Table for storing fire prediction data
CREATE TABLE predict (
                         id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         date DATE NOT NULL,                         -- Forecast date
                         warehouse INTEGER NOT NULL,                 -- Warehouse number
                         stack_number INTEGER NOT NULL,              -- Stack number
                         coal_brand VARCHAR(255) NOT NULL,           -- Coal brand
                         fire_label INTEGER NOT NULL,                -- Fire label (0 = no fire, 1 = fire)
                         fire_probability DOUBLE PRECISION NOT NULL  -- Fire probability (0.0 to 1.0)
);

-- Table for storing model performance metrics
CREATE TABLE metrics (
                         id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         batch_id VARCHAR(255) NOT NULL UNIQUE,      -- Batch ID
                         roc_auc DOUBLE PRECISION NOT NULL,           -- ROC AUC score
                         precision DOUBLE PRECISION NOT NULL,         -- Precision score
                         recall DOUBLE PRECISION NOT NULL,            -- Recall score
                         f1 DOUBLE PRECISION NOT NULL                -- F1 score
);

-- Indexes for query optimization
CREATE INDEX idx_fires_warehouse ON fires(warehouse);
CREATE INDEX idx_fires_stack_number ON fires(stack_number);
CREATE INDEX idx_fires_report_date ON fires(report_date);

CREATE INDEX idx_meta_batch_id ON meta(batch_id);

CREATE INDEX idx_predict_date ON predict(date);
CREATE INDEX idx_predict_warehouse ON predict(warehouse);
CREATE INDEX idx_predict_stack_number ON predict(stack_number);

CREATE INDEX idx_metrics_batch_id ON metrics(batch_id);

-- Table comments
COMMENT ON TABLE fires IS 'Table for storing fire incident data';
COMMENT ON TABLE meta IS 'Table for storing metadata of uploaded files';
COMMENT ON TABLE predict IS 'Table for storing fire prediction data';
COMMENT ON TABLE metrics IS 'Table for storing model performance metrics';