package com.nikworkspace.AnyShare.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodeGenerator {

    // Words for generating memorable room codes
    private static final String[] WORDS = {
            "SWIFT", "QUICK", "FLASH", "RAPID", "BLAZE",
            "SPARK", "BOLT", "DASH", "ZOOM", "PULSE",
            "WAVE", "STORM", "FIRE", "WIND", "FROST"
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a human-readable room code
     * Format: WORD-NNNN (e.g., SWIFT-7284)
     *
     * @return Generated room code
     */
    public String generateRoomCode() {
        String word = WORDS[RANDOM.nextInt(WORDS.length)];
        int number = RANDOM.nextInt(10000); // 0-9999
        return String.format("%s-%04d", word, number);
    }
}