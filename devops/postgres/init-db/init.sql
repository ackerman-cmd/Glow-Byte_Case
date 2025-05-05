

-- Table for storing metadata of uploaded files
CREATE TABLE meta (
                      id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                      batch_id VARCHAR(255) NOT NULL UNIQUE,      -- Batch ID
                      file_names TEXT[] NOT NULL,                 -- Array of file names
                      target_file_name VARCHAR(255) NOT NULL,     -- Target file name
                      upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Upload timestamp
);

CREATE TABLE predict (
                         id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         date DATE NOT NULL,
                         warehouse INTEGER NOT NULL,
                         stack_number INTEGER NOT NULL,
                         coal_brand VARCHAR(255) NOT NULL,
                         fire_label INTEGER NOT NULL,
                         fire_probability DOUBLE PRECISION NOT NULL,
                         batch_id VARCHAR(255) NOT NULL
);

CREATE TABLE fires (
                       id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                       report_date DATE NOT NULL,
                       cargo VARCHAR(255) NOT NULL,
                       weight DOUBLE PRECISION NOT NULL,
                       warehouse INTEGER NOT NULL,
                       start_date_time TIMESTAMP NOT NULL,
                       end_date_time TIMESTAMP NOT NULL,
                       stack_forming_start TIMESTAMP NOT NULL,
                       stack_number INTEGER NOT NULL,
                       batch_id VARCHAR(255) NOT NULL
);

CREATE TABLE metrics (
                         id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         batch_id VARCHAR(255) NOT NULL UNIQUE,
                         roc_auc DOUBLE PRECISION NOT NULL,
                         precision DOUBLE PRECISION NOT NULL,
                         recall DOUBLE PRECISION NOT NULL
);


-- Table comments
COMMENT ON TABLE fires IS 'Table for storing fire incident data';
COMMENT ON TABLE meta IS 'Table for storing metadata of uploaded files';
COMMENT ON TABLE predict IS 'Table for storing fire prediction data';
COMMENT ON TABLE metrics IS 'Table for storing model performance metrics';