package tn.finhub.test;

import tn.finhub.dao.VirtualCardDAO;
import tn.finhub.dao.WalletDAO;
import tn.finhub.model.VirtualCard;
import tn.finhub.model.Wallet;
import tn.finhub.service.VirtualCardService;

import java.math.BigDecimal;

public class VirtualCardTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Virtual Card Verification ---");

        VirtualCardService service = new VirtualCardService();
        WalletDAO walletDAO = new WalletDAO();
        VirtualCardDAO cardDAO = new VirtualCardDAO(); // Triggers table creation

        // 1. Setup Wallet (Assuming User ID 1 exists, create wallet if not)
        int userId = 1;
        walletDAO.createWallet(userId); // Ignores if exists usually, or we should check
        Wallet wallet = walletDAO.findByUserId(userId);

        if (wallet == null) {
            System.err.println("CRITICAL: Could not find or create wallet for User ID " + userId);
            return;
        }
        System.out.println("Wallet ID: " + wallet.getId());

        // 2. Clear existing cards for clean test (Optional)
        // ...

        // 3. Generate Card
        System.out.println("\n[Test] Generating Card...");
        VirtualCard card = service.createCardForWallet(wallet.getId());
        System.out.println("Generated Card: " + card.getCardNumber());
        System.out.println("CVV: " + card.getCvv());
        System.out.println("Expiry: " + card.getExpiryDate());

        // 4. Verify Luhn
        boolean isLuhnValid = VirtualCardService.checkLuhn(card.getCardNumber());
        System.out.println("Luhn Check: " + (isLuhnValid ? "PASS" : "FAIL"));

        // 5. Verify Persistence
        VirtualCard fetchedCard = cardDAO.findByCardNumber(card.getCardNumber());
        System.out.println("DB Fetch: " + (fetchedCard != null ? "PASS" : "FAIL"));

        // 6. Test Shared Balance Simulation
        System.out.println("\n[Test] Transaction Simulation...");

        // Reset Balance to 100
        try {
            // This is a hack for testing, normally updateBalance adds/subs
            // We'll just define a known starting point if possible or just read it.
            System.out.println("Current Wallet Balance: " + wallet.getBalance());
            // Ensure we have some funds
            walletDAO.updateBalance(wallet.getId(), new BigDecimal("100.00"));
            wallet = walletDAO.findById(wallet.getId());
            System.out.println("Updated Wallet Balance: " + wallet.getBalance());

            // Try spending user balance + 10 (Should Fail)
            double tooMuch = wallet.getBalance().doubleValue() + 10.0;
            boolean resultFail = service.simulateTransaction(card.getCardNumber(), card.getCvv(), tooMuch);
            System.out.println("Tx Amount: " + tooMuch + " -> Result: "
                    + (resultFail ? "APPROVED (Unexpected)" : "DECLINED (Expected)"));

            // Try spending 5.0 (Should Pass)
            double okAmount = 5.0;
            boolean resultPass = service.simulateTransaction(card.getCardNumber(), card.getCvv(), okAmount);
            System.out.println("Tx Amount: " + okAmount + " -> Result: "
                    + (resultPass ? "APPROVED (Expected)" : "DECLINED (Unexpected)"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n--- Verification Complete ---");
    }
}
