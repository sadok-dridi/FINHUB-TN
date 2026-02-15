-- ==========================================
-- MODULE 6: TRUST, ESCROW & BLOCKCHAIN LEDGER
-- Database Schema Script (Revised)
-- ==========================================

-- 1. Create Blockchain Ledger Table
CREATE TABLE IF NOT EXISTS blockchain_ledger (
    id INT AUTO_INCREMENT PRIMARY KEY,
    previous_hash VARCHAR(64) NOT NULL,
    data_hash VARCHAR(64) NOT NULL,
    type VARCHAR(50) NOT NULL COMMENT 'TRANSACTION, ESCROW_CREATE, ESCROW_RELEASE, etc.',
    nonce INT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    current_hash VARCHAR(64) NOT NULL UNIQUE,
    wallet_transaction_id INT,
    escrow_id INT,
    CONSTRAINT fk_ledger_transaction FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transaction(id) ON DELETE SET NULL
);

-- 2. Create Escrow Table
CREATE TABLE IF NOT EXISTS escrow (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_wallet_id INT NOT NULL,
    receiver_wallet_id INT NOT NULL,
    amount DECIMAL(15, 3) NOT NULL,
    condition_text TEXT,
    escrow_type VARCHAR(20) DEFAULT 'QR_CODE' COMMENT 'QR_CODE or ADMIN_APPROVAL',
    secret_code VARCHAR(255) COMMENT 'Hashed token for QR release',
    qr_code_image VARCHAR(255) COMMENT 'Path to QR image file',
    admin_approver_id INT COMMENT 'User ID of admin who approved (if applicable)',
    expiry_date TIMESTAMP NULL COMMENT 'Auto-release date',
    is_disputed BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'LOCKED' COMMENT 'LOCKED, RELEASED, REFUNDED, DISPUTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_escrow_sender FOREIGN KEY (sender_wallet_id) REFERENCES wallet(id),
    CONSTRAINT fk_escrow_receiver FOREIGN KEY (receiver_wallet_id) REFERENCES wallet(id),
    CONSTRAINT fk_escrow_admin FOREIGN KEY (admin_approver_id) REFERENCES users_local(user_id)
);

-- 3. Update Users Table (Trust Score)
ALTER TABLE users_local 
ADD COLUMN trust_score INT DEFAULT 100;

-- ==========================================
-- END OF SCRIPT
-- ==========================================
