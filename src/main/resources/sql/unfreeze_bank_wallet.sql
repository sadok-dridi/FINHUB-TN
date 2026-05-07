-- Unfreeze the Bank Wallet
UPDATE wallet SET status = 'ACTIVE' WHERE id = (SELECT id FROM wallet WHERE user_id = 999999 LIMIT 1);
-- Also clear any flags if you want to be clean (optional but recommended)
DELETE FROM ledger_flags WHERE wallet_id = (SELECT id FROM wallet WHERE user_id = 999999 LIMIT 1);
