package tn.finhub.test;

import tn.finhub.model.BlockchainManager;
import tn.finhub.model.BlockchainRecord;

public class BlockchainTest {

    public static void main(String[] args) {
        System.out.println("=== Blockchain Verification Test ===");

        BlockchainManager blockchainManager = new BlockchainManager();

        // 1. Verify Current Chain
        System.out.println("\n[1] Verifying current chain integrity...");
        boolean isValid = blockchainManager.verifyChain();
        if (isValid) {
            System.out.println("✅ Chain is VALID.");
        } else {
            System.err.println("❌ Chain is INVALID.");
        }

        // 2. Add Test Block
        System.out.println("\n[2] Adding a test block...");
        blockchainManager.addBlock("TEST_BLOCK", "This is a test entry", null, null);
        System.out.println("Block added.");

        // 3. Verify Again
        System.out.println("\n[3] Verifying chain after addition...");
        isValid = blockchainManager.verifyChain();
        if (isValid) {
            System.out.println("✅ Chain is VALID.");
        } else {
            System.err.println("❌ Chain is INVALID.");
        }
    }
}
