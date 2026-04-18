import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestHash {
    public static void main(String[] args) {
        String prevHash = "96f8ef4c1d9d6fed9fdfe3073d44b44060eb856fd5aac59329fe4cba2c56c32c";
        int walletId = 113;
        String type = "HOLD";
        BigDecimal amount = new BigDecimal("10.000");
        String ref = "Escrow: salut";
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 4, 20, 38, 55);

        String expected = tn.finhub.util.HashUtils.generateTransactionHash(675, walletId, type, amount, ref, createdAt, prevHash);
        System.out.println("Expected Java Hash: " + expected);
        
        String amountStr = amount.setScale(3, java.math.RoundingMode.HALF_UP).toString();
        java.time.LocalDateTime truncatedTime = createdAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String data = prevHash + walletId + type + amountStr + ref + truncatedTime;
        System.out.println("Data String: " + data);
    }
}
