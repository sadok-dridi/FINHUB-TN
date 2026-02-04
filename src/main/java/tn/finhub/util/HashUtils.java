package tn.finhub.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String generateTransactionHash(int txId, int walletId, String type, java.math.BigDecimal amount,
            String ref, java.time.LocalDateTime createdAt, String prevHash) {
        String amountStr = amount.setScale(3, java.math.RoundingMode.HALF_UP).toString();
        java.time.LocalDateTime truncatedTime = createdAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String data = prevHash + walletId + type + amountStr + ref + truncatedTime;
        return sha256(data);
    }
}
