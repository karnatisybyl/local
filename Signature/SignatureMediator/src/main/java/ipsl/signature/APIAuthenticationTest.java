package ipsl.signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class APIAuthenticationTest {
    
    private static final String SECRET_KEY = "your_secret_key_here"; // Replace with actual key from Family Bank
    
    public static class AuthRequest {
        private String login;
        private String hash;
        private String requestId;
        
        public AuthRequest(String login, String requestId, String hash) {
            this.login = login;
            this.requestId = requestId;
            this.hash = hash;
        }
        
        @Override
        public String toString() {
            return String.format("AuthRequest{login='%s', requestId='%s', hash='%s'}", 
                login, requestId, hash);
        }
    }
    
    public static AuthRequest createAuthRequest(String login, String requestId) {
        String hash = generateHash(login, requestId);
        return new AuthRequest(login, requestId, hash);
    }
    
    public static String generateHash(String login, String requestId) {
        try {
            // Step 1: Create base string
            String baseString = requestId + ":" + login;
            
            // Step 2: Base64 encode
            byte[] encodedData = Base64.getEncoder().encode(baseString.getBytes());
            
            // Step 3: Calculate HMAC-SHA256
            byte[] hashBytes = calculateHmacSha256(SECRET_KEY.getBytes(), encodedData);
            
            // Step 4: Convert to hex
            return byteArrayToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash", e);
        }
    }
    
    public static byte[] calculateHmacSha256(byte[] key, byte[] data) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
    
    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // Utility method to generate a simple request ID
    public static String generateRequestId() {
        return "REQ_" + System.currentTimeMillis();
    }
    
    public static void main(String[] args) {
        try {
            // Test Case 1: Basic Authentication
            String login = "testUser";
            String requestId = generateRequestId();
            AuthRequest request = createAuthRequest(login, requestId);
            System.out.println("Test Case 1 - Basic Authentication:");
            System.out.println(request);
            System.out.println("Hash Length: " + request.hash.length());
            System.out.println();
            
            // Test Case 2: Different User
            login = "anotherUser";
            requestId = generateRequestId();
            request = createAuthRequest(login, requestId);
            System.out.println("Test Case 2 - Different User:");
            System.out.println(request);
            System.out.println();
            
            // Test Case 3: Same User, Different Request ID
            login = "testUser";
            requestId = generateRequestId();
            request = createAuthRequest(login, requestId);
            System.out.println("Test Case 3 - Same User, Different Request ID:");
            System.out.println(request);
            System.out.println();
            
            // Test Case 4: Special Characters
            login = "test@User#123";
            requestId = generateRequestId();
            request = createAuthRequest(login, requestId);
            System.out.println("Test Case 4 - Special Characters:");
            System.out.println(request);
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

