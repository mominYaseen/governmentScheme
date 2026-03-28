-- Widen legacy VARCHAR(50) ids before FK children (ignored on empty DB via continue-on-error)
ALTER TABLE schemes ALTER COLUMN id TYPE VARCHAR(128);
ALTER TABLE eligibility_rules ALTER COLUMN scheme_id TYPE VARCHAR(128);
ALTER TABLE scheme_documents ALTER COLUMN scheme_id TYPE VARCHAR(128);

-- Core tables (new installs)
CREATE TABLE IF NOT EXISTS schemes (
    id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    description TEXT,
    ministry VARCHAR(255),
    benefits TEXT,
    apply_process TEXT,
    apply_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    slug VARCHAR(128),
    gov_level VARCHAR(32),
    eligibility_raw TEXT,
    tags TEXT,
    source VARCHAR(32) DEFAULT 'LEGACY'
);

CREATE TABLE IF NOT EXISTS eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(128) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
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
    scheme_id VARCHAR(128) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL,
    is_mandatory BOOLEAN DEFAULT true,
    description TEXT
);

ALTER TABLE schemes ADD COLUMN IF NOT EXISTS slug VARCHAR(128);
ALTER TABLE schemes ADD COLUMN IF NOT EXISTS gov_level VARCHAR(32);
ALTER TABLE schemes ADD COLUMN IF NOT EXISTS eligibility_raw TEXT;
ALTER TABLE schemes ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE schemes ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'LEGACY';

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS scheme_categories (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(128) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    UNIQUE (scheme_id, category_id)
);

CREATE TABLE IF NOT EXISTS eligibility_criteria (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(128) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    min_income_annual BIGINT,
    max_income_annual BIGINT,
    state_codes VARCHAR(512),
    occupations VARCHAR(512),
    gender VARCHAR(16),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_eligibility_criteria_scheme ON eligibility_criteria (scheme_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_schemes_slug ON schemes (slug) WHERE slug IS NOT NULL;

-- OAuth users + saved schemes (inventory) + apply reminders
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(512) NOT NULL,
    display_name VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, provider_subject)
);

CREATE INDEX IF NOT EXISTS idx_app_users_email ON app_users (email);

CREATE TABLE IF NOT EXISTS user_saved_schemes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    scheme_id VARCHAR(128) NOT NULL REFERENCES schemes (id) ON DELETE CASCADE,
    remind_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_reminder_sent_at TIMESTAMP,
    next_reminder_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, scheme_id)
);

CREATE INDEX IF NOT EXISTS idx_user_saved_schemes_user ON user_saved_schemes (user_id);
CREATE INDEX IF NOT EXISTS idx_user_saved_schemes_reminder ON user_saved_schemes (next_reminder_at)
    WHERE remind_enabled = TRUE;
