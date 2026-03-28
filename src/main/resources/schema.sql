CREATE TABLE IF NOT EXISTS schemes (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    ministry VARCHAR(255),
    benefits TEXT,
    apply_process TEXT,
    apply_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(50) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    value_string VARCHAR(255),
    value_number DECIMAL(15,2),
    value_boolean BOOLEAN,
    is_mandatory BOOLEAN DEFAULT true,
    failure_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scheme_documents (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(50) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL,
    is_mandatory BOOLEAN DEFAULT true,
    description TEXT
);
