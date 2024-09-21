package lib.share.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/*
 * Classe singleton per l'hashing delle passowrd
 */
public class HashUtils {

    //Generatore di numeri pseudo-casuale
    private static  final SecureRandom random = new SecureRandom();
    public static   final int HASH_LEN = 64;    //Lunghezza dell'hash (sha256) come stringa esadecimale

    
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /*
     * Esegue l'hashing di una stringa con SHA-256
     * 
     * @param input la stringa da hashare
     * @param salt il sale da aggiungere alla stringa
     */
    public static String computeSHA256Hash(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] inputBytes = (input + salt).getBytes();
            byte[] hashBytes = digest.digest(inputBytes);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {  //non dovrebbe mai accadere
            throw new RuntimeException("Algoritmo SHA-256 non trovato", e);
        }
    }

    /*
     * Converte un array di byte in una stringa esadecimale
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder Hex = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                Hex.append('0');
            }
            Hex.append(hex);
        }
        return Hex.toString();
    }

}
