-- Add columns for Profile picture and Phone number
ALTER TABLE users_local ADD COLUMN phone_number VARCHAR(20) DEFAULT NULL;
ALTER TABLE users_local ADD COLUMN profile_photo_url VARCHAR(255) DEFAULT NULL;

-- Create KYC Requests table
CREATE TABLE IF NOT EXISTS kyc_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    document_type VARCHAR(20) NOT NULL, -- 'ID_CARD' or 'VIDEO'
    document_url VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users_local(user_id) ON DELETE CASCADE
);

-- Knowledge Base Table
DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
