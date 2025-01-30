package ipsl.signature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SPAuthentication {

    // Method to encode a string using Base64
    public static String encodeBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    // Method to generate SP Password
    public static String generateSPPassword(String spId, String password, String timeStamp) throws NoSuchAlgorithmException {
        // Concatenate spId, password, and timeStamp
        String dataToEncrypt = spId + password + timeStamp;
        System.out.println("Data to encrypt: [" + dataToEncrypt + "]");  // For debugging

        // Create SHA-256 digest
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(dataToEncrypt.getBytes(StandardCharsets.UTF_8));

        // Print the raw hash in hexadecimal format
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        System.out.println("Hash (hex): [" + hexString.toString() + "]");  // Print raw hash
        String data = hexString.toString();
        String encodedData = encodeBase64(data);
        System.out.println("Encoded String: " + encodedData);
        // Encode the hash using Base64
      //  String spPassword = encodeBase64(new String(hash, StandardCharsets.UTF_8));
       // System.out.println("spPassword (Base64): [" + spPassword + "]");  // For debugging

        return encodedData;
    }

    public static void main(String[] args) {
        try {
            // Example usage
            String spId = "107031";  // Example spId
            String password = "Safaricom123!";  // Example password
            String timeStamp = "20241025071745";  // Example timestamp in format yyyyMMddHHmmss

            System.out.println("Generated TimeStamp: [" + timeStamp + "]");

            // Generate the SP Password
            String spPassword = generateSPPassword(spId, password, timeStamp);
          //  System.out.println("Generated SP Password: [" + spPassword + "]");

            // Example usage of Base64 encoding
           // String data = hexString.toString();
           // String encodedData = encodeBase64(data);
           // System.out.println("Encoded String: " + encodedData);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
