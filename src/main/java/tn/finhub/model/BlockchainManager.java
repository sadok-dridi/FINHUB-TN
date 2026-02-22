package tn.finhub.model;

import tn.finhub.util.DBConnection;
import tn.finhub.util.HashUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BlockchainManager {

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    /**
     * Adds a new block to the chain.
     * 
     * @param type                TRANSACTION | ESCROW
     * @param data                String representation of data (e.g. JSON or
     *                            concatenated string)
     * @param walletTransactionId Optional ID
     * @param escrowId            Optional ID
     */
    public void addBlock(String type, String data, Integer walletTransactionId, Integer escrowId) {
        try {
            // 1. Get Previous Hash
            String previousHash = getLastHash();
            if (previousHash == null) {
                previousHash = "0000000000000000000000000000000000000000000000000000000000000000"; // Genesis
            }

            // 2. Hash the Data
            String dataHash = HashUtils.sha256(data);

            // 3. Find Nonce (Proof of Work - simplified for this project)
            // We just need a valid hash, maybe starting with "0" or just any valid hash.
            // For now, let's just use 0, or we can implement a simple loop if we want
            // "mining".
            // Let's keep it fast: nonce = 0.
            int nonce = 0;
            LocalDateTime now = LocalDateTime.now();

            // 4. Calculate Current Hash
            // Hash = sha256(prev + data + type + nonce + timestamp)
            // Note: Timestamp precision might vary between Java and DB, so rely on what we
            // insert.
            // Ideally, we generate timestamp here.
            String currentHash = calculateHash(previousHash, dataHash, type, nonce, now);

            // 5. Insert
            String sql = """
                        INSERT INTO blockchain_ledger
                        (previous_hash, data_hash, type, nonce, timestamp, current_hash, wallet_transaction_id, escrow_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, previousHash);
                ps.setString(2, dataHash);
                ps.setString(3, type);
                ps.setInt(4, nonce);
                ps.setTimestamp(5, Timestamp.valueOf(now));
                ps.setString(6, currentHash);
                if (walletTransactionId != null)
                    ps.setInt(7, walletTransactionId);
                else
                    ps.setNull(7, Types.INTEGER);
                if (escrowId != null)
                    ps.setInt(8, escrowId);
                else
                    ps.setNull(8, Types.INTEGER);

                ps.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Blockchain Error: " + e.getMessage());
        }
    }

    public boolean verifyChain() {
        List<BlockchainRecord> chain = getAllBlocks();
        if (chain.isEmpty())
            return true;

        for (int i = 0; i < chain.size(); i++) {
            BlockchainRecord current = chain.get(i);

            // 1. Verify Previous Hash Link
            if (i > 0) {
                BlockchainRecord previous = chain.get(i - 1);
                if (!current.getPreviousHash().equals(previous.getCurrentHash())) {
                    System.err.println("Chain Broken at Block " + current.getId() + ": Previous Hash Mismatch");
                    return false;
                }
            } else {
                // Genesis Block check
                if (!current.getPreviousHash()
                        .equals("0000000000000000000000000000000000000000000000000000000000000000")) {
                    System.err.println("Genesis Block Missing Correct Zero Hash");
                    return false;
                }
            }

            // 2. Verify Data Integrity (Re-calculate hash)
            String recalculatedHash = calculateHash(
                    current.getPreviousHash(),
                    current.getDataHash(),
                    current.getType(),
                    current.getNonce(),
                    current.getTimestamp());

            if (!recalculatedHash.equals(current.getCurrentHash())) {
                System.err.println("Data Tampered at Block " + current.getId() + ": Hash Calculation Mismatch");
                System.err.println("Stored: " + current.getCurrentHash());
                System.err.println("Calc  : " + recalculatedHash);
                return false;
            }
        }
        return true;
    }

    private String getLastHash() throws SQLException {
        String sql = "SELECT current_hash FROM blockchain_ledger ORDER BY id DESC LIMIT 1";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("current_hash");
            }
        }
        return null; // Implies Genesis
    }

    private List<BlockchainRecord> getAllBlocks() {
        List<BlockchainRecord> blocks = new ArrayList<>();
        String sql = "SELECT * FROM blockchain_ledger ORDER BY id ASC";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                BlockchainRecord b = new BlockchainRecord();
                b.setId(rs.getInt("id"));
                b.setPreviousHash(rs.getString("previous_hash"));
                b.setDataHash(rs.getString("data_hash"));
                b.setType(rs.getString("type"));
                b.setNonce(rs.getInt("nonce"));
                b.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                b.setCurrentHash(rs.getString("current_hash"));
                b.setWalletTransactionId(rs.getInt("wallet_transaction_id"));
                if (rs.wasNull())
                    b.setWalletTransactionId(null);
                b.setEscrowId(rs.getInt("escrow_id"));
                if (rs.wasNull())
                    b.setEscrowId(null);
                blocks.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return blocks;
    }

    private String calculateHash(String prevHash, String dataHash, String type, int nonce, LocalDateTime timestamp) {
        // Truncate timestamp to seconds to match DB precision usually, or use string
        // format
        // In this project, let's use the .toString() of LocalDateTime, but be careful
        // about nanos.
        // Better: Convert timestamp to epoch seconds or a standard format.
        // For simplicity and robustness with DB retrieval:
        // Let's use `Timestamp.valueOf(timestamp).toString()` which aligns with JDBC.
        String timeStr = Timestamp.valueOf(timestamp).toString();
        // Remove nanos if zero? Timestamp.toString() often formats like
        // "yyyy-mm-dd hh:mm:ss.fffffffff"
        // Let's stick to standard string concatenation.
        String input = prevHash + dataHash + type + nonce + timeStr;
        return HashUtils.sha256(input);
    }
}
