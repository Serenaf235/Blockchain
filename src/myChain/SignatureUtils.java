package myChain;
import java.security.*;
import java.util.Base64;
//Requirement 2: Bitcoin Style Signature
// Utility class for signing hashed data
public class SignatureUtils {

    // Hash input data using SHA-256
    public static byte[] applySHA256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes("UTF-8"));
    }

    // Sign hashed data using ECDSA
    public static byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature ecdsa = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsa.initSign(privateKey);
        ecdsa.update(data);
        return ecdsa.sign(); // DER-encoded
    }

    // Verify signature on hashed data
    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature ecdsa = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsa.initVerify(publicKey);
        ecdsa.update(data);
        return ecdsa.verify(signatureBytes);
    }

    // Helper to Base64-encode signature for easy display
    public static String encodeSignature(byte[] signature) {
        return Base64.getEncoder().encodeToString(signature);
    }
}
