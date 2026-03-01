package tn.finhub.test;

import tn.finhub.model.VirtualCard;
import tn.finhub.model.Wallet;
import tn.finhub.model.VirtualCardModel;
import tn.finhub.model.WalletModel;
import java.math.BigDecimal;

public class VirtualCardTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Virtual Card Verification ---");

        VirtualCardModel cardModel = new VirtualCardModel();
        WalletModel walletModel = new WalletModel();

        // 1. Setup Wallet (Assuming User ID 1 exists, create wallet if not)
        int userId = 1;
        walletModel.createWalletIfNotExists(userId);
        Wallet wallet = walletModel.findByUserId(userId);

        if (wallet == null) {
            System.err.println("CRITICAL: Could not find or create wallet for User ID " + userId);
            return;
        }
        System.out.println("Wallet ID: " + wallet.getId());

        // 3. Generate Card
        System.out.println("\n[Test] Generating Card...");
        VirtualCard card = cardModel.createCardForWallet(wallet.getId());
        System.out.println("Generated Card: " + card.getCardNumber());
        System.out.println("CVV: " + card.getCvv());
        System.out.println("Expiry: " + card.getExpiryDate());

        // 4. Verify Luhn
        boolean isLuhnValid = tn.finhub.util.CardUtils.checkLuhn(card.getCardNumber());
        System.out.println("Luhn Check: " + (isLuhnValid ? "PASS" : "FAIL"));

        // 5. Verify Persistence
        VirtualCard fetchedCard = cardModel.findByCardNumber(card.getCardNumber());
        System.out.println("DB Fetch: " + (fetchedCard != null ? "PASS" : "FAIL"));

        // 6. Test Shared Balance Simulation
        System.out.println("\n[Test] Transaction Simulation (Logic Test)");

        // Reset Balance to 100
        try {
            System.out.println("Current Wallet Balance: " + wallet.getBalance());
            // Ensure we have some funds
            walletModel.credit(wallet.getId(), new BigDecimal("100.00"), "Test Funding");
            wallet = walletModel.findById(wallet.getId());
            System.out.println("Updated Wallet Balance: " + wallet.getBalance());

            // Try spending user balance + 10 (Should Fail)
            double tooMuch = wallet.getBalance().doubleValue() + 10.0;
            boolean resultFail = cardModel.simulateTransaction(card.getCardNumber(), card.getCvv(), tooMuch);
            System.out.println("Tx Amount: " + tooMuch + " -> Result: "
                    + (resultFail ? "APPROVED (Unexpected)" : "DECLINED (Expected)"));

            // Try spending 5.0 (Should Pass)
            double okAmount = 5.0;
            boolean resultPass = cardModel.simulateTransaction(card.getCardNumber(), card.getCvv(), okAmount);
            System.out.println("Tx Amount: " + okAmount + " -> Result: "
                    + (resultPass ? "APPROVED (Expected)" : "DECLINED (Unexpected)"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n--- Verification Complete ---");
    }
}
