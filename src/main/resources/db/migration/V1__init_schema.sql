CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(255),
    role VARCHAR(255) NOT NULL DEFAULT 'CUSTOMER',
    password VARCHAR(255) NOT NULL DEFAULT 'password123'
);

CREATE TABLE IF NOT EXISTS policies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_number VARCHAR(255) NOT NULL UNIQUE,
    property_address VARCHAR(255) NOT NULL,
    coverage_limit DOUBLE NOT NULL,
    customer_id INTEGER NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS claims (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cause_of_fire VARCHAR(255) NOT NULL,
    estimated_property_damage DOUBLE NOT NULL,
    estimated_content_damage DOUBLE NOT NULL,
    is_livable BOOLEAN NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'SUBMITTED',
    submission_date DATE NOT NULL,
    reviewer_notes VARCHAR(255),
    policy_id INTEGER NOT NULL,
    FOREIGN KEY (policy_id) REFERENCES policies (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    claim_id INTEGER NOT NULL,
    FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE CASCADE
);
