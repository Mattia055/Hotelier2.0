package lib.share.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class HashUtils {

    private static final SecureRandom random = new SecureRandom();
    public static final int HASH_LEN = 256;

    /**
     * Generates a random salt.
     * 
     * @return the salt as a Base64 encoded string
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Computes the SHA-256 hash of the given input string with a salt.
     * 
     * @param input the string to hash
     * @param salt the salt to add to the input
     * @return the SHA-256 hash as a hexadecimal string
     */
    public static String computeSHA256Hash(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] inputBytes = (input + salt).getBytes();
            byte[] hashBytes = digest.digest(inputBytes);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     * 
     * @param bytes the byte array
     * @return the hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
