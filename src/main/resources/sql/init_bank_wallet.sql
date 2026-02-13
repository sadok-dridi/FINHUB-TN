-- ==========================================
-- INIT BANK WALLET & USER
-- ==========================================

-- 1. Create Bank User (Admin)
INSERT INTO users_local (user_id, email, role, full_name)
VALUES (999999, 'bank@finhub.tn', 'ADMIN', 'FinHub Central Bank')
ON DUPLICATE KEY UPDATE role='ADMIN';

-- 2. Create Bank Wallet (High Balance)
INSERT INTO wallets (user_id, currency, balance, escrow_balance, status)
VALUES (999999, 'TND', 100000000.000, 0.000, 'ACTIVE')
ON DUPLICATE KEY UPDATE balance = 100000000.000;

-- 3. Create Virtual Card for Bank (Valid Source)
-- We need the wallet ID first. Assuming we just inserted it, we can look it up.
-- But inside a script without variables it's tricky.
-- We can use a subquery for the wallet_id.

INSERT INTO virtual_cards (wallet_id, card_number, cvv, expiry_date, status)
SELECT id, '4000123456789010', '999', '2030-12-31', 'ACTIVE'
FROM wallets
WHERE user_id = 999999
LIMIT 1;
