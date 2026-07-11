CREATE DATABASE IF NOT EXISTS macna_banking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE macna_banking;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL,
    phone VARCHAR(20) NULL,
    isActive BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(128) NOT NULL,
    address VARCHAR(256) NOT NULL,
    phoneNumber VARCHAR(20) NOT NULL,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    accountNumber VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    customer_id BIGINT NOT NULL,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    isActive BOOLEAN NOT NULL DEFAULT TRUE,
    lastInterestAppliedDate DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_number (accountNumber),
    KEY idx_accounts_customer (customer_id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    isArchived BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    timestamp DATETIME(6) NOT NULL,
    description VARCHAR(256) NULL,
    PRIMARY KEY (id),
    KEY idx_transactions_account_time (account_id, timestamp),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS scheduled_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    scheduledTime DATETIME(6) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_scheduled_due (processed, scheduledTime),
    KEY idx_scheduled_from (from_account_id),
    KEY idx_scheduled_to (to_account_id),
    CONSTRAINT fk_scheduled_from FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_scheduled_to FOREIGN KEY (to_account_id) REFERENCES accounts (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO roles (name) VALUES ('CUSTOMER'), ('EMPLOYEE'), ('ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (username, password, email, phone, isActive)
VALUES (
    'mac',
    '$2a$12$ysK.DvVF.z2yQNvod6WeQ.4AA6w8sLXv8E96eP9YSawsYPHeM4X7W',
    'mac@nawwa.local',
    NULL,
    TRUE
)
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'mac';
