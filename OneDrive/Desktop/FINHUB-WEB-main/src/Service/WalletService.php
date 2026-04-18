<?php

namespace App\Service;

use App\Entity\User;
use App\Entity\VirtualCard;
use App\Entity\Wallet;
use App\Entity\WalletTransaction;
use Doctrine\ORM\EntityManagerInterface;

class WalletService
{
    public function __construct(
        private EntityManagerInterface $em
    ) {}

    public function transferByEmail(int $senderWalletId, string $recipientEmail, float $amountRaw): array
    {
        $amount = number_format($amountRaw, 3, '.', ''); // Ensure 3 decimal precision
        
        if ($amountRaw <= 0) {
            throw new \Exception("Amount must be greater than 0.");
        }

        $recipientUser = $this->em->getRepository(User::class)->findOneBy(['email' => $recipientEmail]);
        if (!$recipientUser) {
            throw new \Exception("User with email {$recipientEmail} not found.");
        }

        $recipientWallet = $recipientUser->getWallet();
        if (!$recipientWallet) {
            throw new \Exception("Recipient does not have an active wallet.");
        }

        if ($recipientWallet->getStatus() === 'FROZEN') {
            throw new \Exception("Recipient wallet is frozen. Cannot receive funds.");
        }

        $senderWallet = $this->em->getRepository(Wallet::class)->find($senderWalletId);
        if (!$senderWallet) {
            throw new \Exception("Sender wallet not found.");
        }
        
        if ($senderWallet->getStatus() === 'FROZEN') {
            throw new \Exception("Your wallet is frozen. Cannot send funds.");
        }

        if ($senderWallet->getId() === $recipientWallet->getId()) {
            throw new \Exception("Cannot transfer to the same wallet.");
        }

        // Integrity Scan Before Transferring
        $this->checkStatus($senderWallet->getId());
        $this->checkStatus($recipientWallet->getId());

        // Use loose float comparison without bcmath
        if ((float) $senderWallet->getBalance() < (float) $amount) {
            throw new \Exception("Insufficient balance.");
        }

        $senderName = $senderWallet->getUser()->getFullName() ?? 'Unknown';
        $recipientName = $recipientUser->getFullName() ?? 'Unknown';

        // Same ref format as Java: "Transfer to [Name] (Wallet [ID])"
        $senderRef = "Transfer to " . $recipientName . " (Wallet " . $recipientWallet->getId() . ")";
        $receiverRef = "Transfer from " . $senderName . " (Wallet " . $senderWalletId . ")";

        $this->transferInternal($senderWallet, $recipientWallet, $amount, $senderRef, $receiverRef);
        
        return ['success' => true, 'message' => "Successfully transferred {$amountRaw} TND to {$recipientName}."];
    }

    public function cashInByVirtualCard(int $recipientWalletId, string $cardNumberInput, string $expiryInput, string $cvvInput, float $amountRaw): array
    {
        $context = $this->resolveVirtualCardCashInContext($recipientWalletId, $cardNumberInput, $expiryInput, $cvvInput, $amountRaw);
        $amount = $context['amount'];
        $recipientWallet = $context['recipientWallet'];
        $sourceWallet = $context['sourceWallet'];
        $senderRef = $context['senderRef'];
        $receiverRef = $context['receiverRef'];

        $this->em->getConnection()->beginTransaction();

        try {
            $sourceWallet->setBalance(number_format((float) $sourceWallet->getBalance() - (float) $amount, 3, '.', ''));
            $recipientWallet->setBalance(number_format((float) $recipientWallet->getBalance() + (float) $amount, 3, '.', ''));

            $this->em->persist($sourceWallet);
            $this->em->persist($recipientWallet);

            $this->recordTransaction($sourceWallet, 'DEBIT', $amount, $senderRef);
            $this->recordTransaction($recipientWallet, 'DEPOSIT', $amount, $receiverRef);

            $this->em->flush();
            $this->em->getConnection()->commit();
        } catch (\Exception $e) {
            $this->em->getConnection()->rollBack();
            throw new \Exception('Card cash in failed: ' . $e->getMessage());
        }

        return [
            'success' => true,
            'message' => 'Successfully cashed in ' . $amount . ' TND using virtual card ending ' . $context['lastFour'] . '.',
        ];
    }

    public function getVirtualCardCashInOtpRecipient(int $recipientWalletId, string $cardNumberInput, string $expiryInput, string $cvvInput, float $amountRaw): array
    {
        $context = $this->resolveVirtualCardCashInContext($recipientWalletId, $cardNumberInput, $expiryInput, $cvvInput, $amountRaw);
        $holder = $context['sourceWallet']->getUser();

        if (!$holder || !$holder->getEmail()) {
            throw new \Exception('Virtual card holder email is unavailable.');
        }

        return [
            'email' => $holder->getEmail(),
            'lastFour' => $context['lastFour'],
        ];
    }

    private function resolveVirtualCardCashInContext(int $recipientWalletId, string $cardNumberInput, string $expiryInput, string $cvvInput, float $amountRaw): array
    {
        $amount = number_format($amountRaw, 3, '.', '');

        if ($amountRaw <= 0) {
            throw new \Exception('Amount must be greater than 0.');
        }

        $recipientWallet = $this->em->getRepository(Wallet::class)->find($recipientWalletId);
        if (!$recipientWallet) {
            throw new \Exception('Recipient wallet not found.');
        }

        if ($recipientWallet->getStatus() === 'FROZEN') {
            throw new \Exception('Your wallet is frozen. Cannot receive funds.');
        }

        $normalizedCardNumber = preg_replace('/\D+/', '', $cardNumberInput) ?? '';
        $normalizedExpiry = strtoupper(trim($expiryInput));
        $normalizedCvv = trim($cvvInput);

        /** @var VirtualCard|null $card */
        $card = $this->em->getRepository(VirtualCard::class)->findOneBy(['cardNumber' => $normalizedCardNumber]);
        if (!$card) {
            throw new \Exception('Virtual card not found.');
        }

        $sourceWallet = $card->getWallet();
        if (!$sourceWallet) {
            throw new \Exception('Virtual card is not linked to a valid wallet.');
        }

        if ($sourceWallet->getId() === $recipientWallet->getId()) {
            throw new \Exception('You cannot cash in using your own virtual card.');
        }

        if ($card->getStatus() !== 'ACTIVE') {
            throw new \Exception('This virtual card is not active.');
        }

        if ($sourceWallet->getStatus() === 'FROZEN') {
            throw new \Exception('This card wallet is frozen and cannot be used.');
        }

        if ($card->getCvv() !== $normalizedCvv) {
            throw new \Exception('Invalid virtual card CVV.');
        }

        $cardExpiry = $card->getExpiryDate();
        if (!$cardExpiry) {
            throw new \Exception('Virtual card expiry date is invalid.');
        }

        if ($cardExpiry->format('m/y') !== $normalizedExpiry) {
            throw new \Exception('Card expiration date does not match.');
        }

        $expiryEnd = \DateTimeImmutable::createFromMutable((clone $cardExpiry)->modify('last day of this month')->setTime(23, 59, 59));
        if ($expiryEnd < new \DateTimeImmutable('now')) {
            throw new \Exception('This virtual card is expired.');
        }

        $this->checkStatus($sourceWallet->getId());
        $this->checkStatus($recipientWallet->getId());

        if ((float) $sourceWallet->getBalance() < (float) $amount) {
            throw new \Exception('This virtual card wallet does not have enough balance.');
        }

        $senderName = $sourceWallet->getUser()?->getFullName() ?: 'Unknown';
        $recipientName = $recipientWallet->getUser()?->getFullName() ?: 'Unknown';

        return [
            'amount' => $amount,
            'recipientWallet' => $recipientWallet,
            'sourceWallet' => $sourceWallet,
            'senderRef' => 'Card cash-in to ' . $recipientName . ' (Wallet ' . $recipientWallet->getId() . ')',
            'receiverRef' => 'Card cash-in from ' . $senderName . ' via card ending ' . substr($normalizedCardNumber, -4),
            'lastFour' => substr($normalizedCardNumber, -4),
        ];
    }

    private function transferInternal(Wallet $fromWallet, Wallet $toWallet, string $amount, string $senderRef, string $receiverRef)
    {
        // Execute atomic balances and ledger update
        $this->em->getConnection()->beginTransaction();
        
        try {
            // 1. Debit Sender
            $newSenderBalance = number_format((float) $fromWallet->getBalance() - (float) $amount, 3, '.', '');
            $fromWallet->setBalance($newSenderBalance);
            $this->em->persist($fromWallet);
            
            $this->recordTransaction($fromWallet, 'TRANSFER_SENT', $amount, $senderRef);

            // 2. Credit Receiver
            $newReceiverBalance = number_format((float) $toWallet->getBalance() + (float) $amount, 3, '.', '');
            $toWallet->setBalance($newReceiverBalance);
            $this->em->persist($toWallet);
            
            $this->recordTransaction($toWallet, 'TRANSFER_RECEIVED', $amount, $receiverRef);

            // Commit to Database
            $this->em->flush();
            $this->em->getConnection()->commit();
            
        } catch (\Exception $e) {
            $this->em->getConnection()->rollBack();
            throw new \Exception("Ledger transaction failed: " . $e->getMessage());
        }
    }

    public function recordTransaction(Wallet $wallet, string $type, string $amountStr, string $ref)
    {
        // Reverted: Use System Time (Local Time) exactly like Java java.time.LocalDateTime.now()
        // The user's server default might be UTC, so explicitly set Africa/Tunis to guarantee +1 UTC!
        $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
        
        // Match Java's .truncatedTo(ChronoUnit.SECONDS): Drop microseconds completely.
        $now = new \DateTimeImmutable($now->format('Y-m-d H:i:s'), new \DateTimeZone('Africa/Tunis'));

        $prevHash = $this->getLastHash($wallet->getId());

        $data = $this->formatForHash($prevHash, $wallet->getId(), $type, $amountStr, $ref, $now);
        $txHash = hash('sha256', $data);

        $tx = new WalletTransaction();
        $tx->setWallet($wallet);
        $tx->setType($type);
        $tx->setAmount($amountStr);
        $tx->setReference($ref);
        $tx->setPrevHash($prevHash);
        $tx->setTxHash($txHash);
        $tx->setCreatedAt($now);

        $this->em->persist($tx);
    }

    private function getLastHash(int $walletId): string
    {
        $lastTx = $this->em->getRepository(WalletTransaction::class)
            ->findOneBy(['wallet' => $walletId], ['created_at' => 'DESC', 'id' => 'DESC']);

        if (!$lastTx) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
        
        return $lastTx->getTxHash();
    }

    // ============================================
    // JAVA-PORTED SECURITY & VERIFICATION LOGIC
    // ============================================

    public function checkStatus(int $walletId)
    {
        $wallet = $this->em->getRepository(Wallet::class)->find($walletId);
        if (!$wallet) return;

        $conn = $this->em->getConnection();
        
        // Exact Java Logic: ledgerDAO.hasActiveFlags(walletId)
        $flagCount = $conn->fetchOne(
            "SELECT COUNT(*) FROM ledger_flags WHERE wallet_id = ?",
            [$walletId]
        );

        if ($flagCount > 0) {
            throw new \Exception("Wallet is frozen due to integrity violation. Contact support.");
        }

        if ($wallet->getStatus() === 'FROZEN') {
            // Attempt Self-Healing
            if ($this->verifyBalance($walletId) && $this->verifyLedger($walletId)) {
                $wallet->setStatus('ACTIVE');
                // Java unfreezeWallet() deletes the flags
                $conn->executeStatement("DELETE FROM ledger_flags WHERE wallet_id = ?", [$walletId]);
                $this->em->flush();
                return; // Healed
            }
            throw new \Exception("Wallet is FROZEN. Contact support.");
        }

        if (!$this->verifyLedger($walletId)) {
            $reason = "Ledger integrity violation – wallet frozen";
            $this->insertLedgerFlag($walletId, $reason);
            $wallet->setStatus('FROZEN');
            $this->em->flush();
            throw new \Exception("Security Alert: " . $reason);
        }

        if (!$this->verifyBalance($walletId)) {
            $reason = "Balance mismatch detected – wallet frozen";
            $this->insertLedgerFlag($walletId, $reason);
            $wallet->setStatus('FROZEN');
            $this->em->flush();
            throw new \Exception("Security Alert: " . $reason);
        }
    }

    private function insertLedgerFlag(int $walletId, string $reason)
    {
        try {
            $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
            $this->em->getConnection()->executeStatement(
                "INSERT INTO ledger_flags (wallet_id, reason, flagged_at) VALUES (?, ?, ?)",
                [$walletId, $reason, $now->format('Y-m-d H:i:s')]
            );
        } catch (\Exception $e) {
            // Ignore if table doesn't exist yet, but try to log.
        }
    }

    private function insertAuditLog(int $walletId, bool $status, string $message)
    {
        try {
            $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
            $this->em->getConnection()->executeStatement(
                "INSERT INTO ledger_audit_log (wallet_id, verified, checked_at, message) VALUES (?, ?, ?, ?)",
                [$walletId, $status ? 1 : 0, $now->format('Y-m-d H:i:s'), $message]
            );
        } catch (\Exception $e) {
            // Safe fallback
        }
    }

    public function verifyLedger(int $walletId): bool
    {
        $txs = $this->em->getRepository(WalletTransaction::class)
            ->findBy(['wallet' => $walletId], ['created_at' => 'ASC', 'id' => 'ASC']); // ASC to mimic reverse()

        $previousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        foreach ($txs as $tx) {
            $data = $this->formatForHash(
                $previousHash, 
                $walletId, 
                $tx->getType(), 
                $tx->getAmount(), 
                $tx->getReference(), 
                $tx->getCreatedAt()
            );
            $expectedHash = hash('sha256', $data);
            
            if ($expectedHash !== $tx->getTxHash()) {
                $this->insertAuditLog($walletId, false, "Hash mismatch tx " . $tx->getId());
                return false;
            }
            $previousHash = $tx->getTxHash();
        }
        $this->insertAuditLog($walletId, true, "Ledger integrity verified");
        return true;
    }

    public function verifyBalance(int $walletId): bool
    {
        $txs = $this->em->getRepository(WalletTransaction::class)->findBy(['wallet' => $walletId]);
        
        $calcBalance = 0.0;
        $calcEscrow = 0.0;

        foreach ($txs as $tx) {
            $amt = (float) $tx->getAmount();
            switch ($tx->getType()) {
                case "CREDIT":
                case "DEPOSIT":
                case "RELEASE":
                case "TRANSFER_RECEIVED":
                case "GENESIS":
                case "ESCROW_RCVD":
                case "ESCROW_FEE":
                case "ESCROW_REFUND":
                    $calcBalance += $amt;
                    break;
                case "DEBIT":
                case "HOLD":
                case "TRANSFER_SENT":
                    $calcBalance -= $amt;
                    break;
            }
            
            if ($tx->getType() === "HOLD") {
                $calcEscrow += $amt;
            }
            if (in_array($tx->getType(), ["RELEASE", "ESCROW_SENT", "ESCROW_REFUND"])) {
                $calcEscrow -= $amt;
            }
        }

        $calcBalanceStr = number_format($calcBalance, 3, '.', '');
        $calcEscrowStr = number_format($calcEscrow, 3, '.', '');

        $wallet = $this->em->getRepository(Wallet::class)->find($walletId);
        if (!$wallet) return false;

        $actualBalanceStr = number_format((float) $wallet->getBalance(), 3, '.', '');
        // Note: Java model has getEscrowBalance. Assuming Web App Wallet also has escrow column logic 
        // if not, fallback to 0.000 for strict comparison.
        $actualEscrowStr = "0.000"; 
        if (method_exists($wallet, 'getEscrowBalance')) {
            $actualEscrowStr = number_format((float) $wallet->getEscrowBalance(), 3, '.', '');
        }

        return $actualBalanceStr === $calcBalanceStr && $actualEscrowStr === $calcEscrowStr;
    }

    public function isTransactionTampered(WalletTransaction $tx): bool
    {
        $prevHash = $tx->getPrevHash() ?? "0000000000000000000000000000000000000000000000000000000000000000";
        $data = $this->formatForHash(
            $prevHash,
            $tx->getWallet()->getId(),
            $tx->getType(),
            $tx->getAmount(),
            $tx->getReference() ?? '',
            $tx->getCreatedAt()
        );
        $expectedHash = hash('sha256', $data);
        return $expectedHash !== $tx->getTxHash();
    }

    private function formatForHash(string $prevHash, int $walletId, string $type, string $amountStr, string $ref, \DateTimeImmutable $createdAt): string
    {
        // Emulate Java's LocalDateTime.toString()
        // Format: '2024-04-03T12:05:01' 
        $dateStr = $createdAt->format('Y-m-d\TH:i:s');
        
        // Java quirk: If seconds are '00', it drops them explicitly
        if (substr($dateStr, -3) === ':00') {
            $dateStr = substr($dateStr, 0, -3); 
        }

        // prevHash + walletId + type + amountStr + ref + truncatedTime
        return $prevHash . $walletId . $type . $amountStr . $ref . $dateStr;
    }
}
