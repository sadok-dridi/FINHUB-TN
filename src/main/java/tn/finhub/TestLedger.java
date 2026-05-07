package tn.finhub;

import tn.finhub.model.WalletTransaction;
import tn.finhub.model.TransactionManager;
import tn.finhub.model.WalletModel;
import tn.finhub.model.Wallet;

import java.util.List;
import java.math.BigDecimal;

public class TestLedger {
    public static void main(String[] args) {
        WalletModel walletModel = new WalletModel();
        TransactionManager txDAO = new TransactionManager();

        try {
            java.sql.Connection conn = walletModel.getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM wallet LIMIT 5");
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int walletId = rs.getInt(1);
                Wallet w = walletModel.findById(walletId);
                System.out.println("--- WALLET " + walletId + " ---");
                System.out.println("Balance: " + w.getBalance() + ", Escrow: " + w.getEscrowBalance());

                List<WalletTransaction> txs = txDAO.findByWalletId(walletId);
                BigDecimal calcBalance = BigDecimal.ZERO;
                BigDecimal calcEscrow = BigDecimal.ZERO;

                for (WalletTransaction tx : txs) {
                    BigDecimal amt = tx.getAmount();
                    System.out.println("  " + tx.getType() + " : " + amt);
                    switch (tx.getType()) {
                        case "CREDIT":
                        case "DEPOSIT":
                        case "RELEASE":
                        case "TRANSFER_RECEIVED":
                        case "GENESIS":
                        case "ESCROW_RCVD":
                        case "ESCROW_FEE":
                        case "ESCROW_REFUND":
                            calcBalance = calcBalance.add(amt);
                            break;
                        case "DEBIT":
                        case "HOLD":
                        case "TRANSFER_SENT":
                            calcBalance = calcBalance.subtract(amt);
                            break;
                    }
                    if ("HOLD".equals(tx.getType()))
                        calcEscrow = calcEscrow.add(amt);
                    if ("RELEASE".equals(tx.getType()) || "ESCROW_SENT".equals(tx.getType())
                            || "ESCROW_REFUND".equals(tx.getType()))
                        calcEscrow = calcEscrow.subtract(amt);
                }

                System.out.println("CALC BAL: " + calcBalance + " == ACTUAL " + w.getBalance());
                System.out.println("CALC ESCROW: " + calcEscrow + " == ACTUAL " + w.getEscrowBalance());
                System.out.println("verifyBalance returned: " + walletModel.verifyBalance(walletId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
