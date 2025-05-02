package myChain;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.*;
import java.util.Base64;
// Requirement 1: Secure Private Key Storage 
//Utility class for AES Encryption/Decryption
public class CryptoUtils {
    private static final String ALGO = "AES";

    // Generate AES key
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGO);
        keyGen.init(128);
        return keyGen.generateKey();
    }

    // Save AES key to file
    public static void saveKey(SecretKey key, String path) throws Exception {
        byte[] encoded = key.getEncoded();
        Files.write(Paths.get(path), encoded);
    }

    // Load AES key from file
    public static SecretKey loadKey(String path) throws Exception {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new SecretKeySpec(encoded, ALGO);
    }

    // Encrypt data using AES key
    public static String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encVal);
    }

    // Decrypt data using AES key
    public static String decrypt(String encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decValue = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decValue);
    }
}

