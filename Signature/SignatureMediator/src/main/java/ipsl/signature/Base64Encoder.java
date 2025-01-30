package ipsl.signature;

import java.util.Base64;

public class Base64Encoder {

    public static void main(String[] args) {
        String data = "8ba95b8d8caad8a1ff986caf34e9c06ad01f158115e014a7de5ec873096bf1cf";
        String encodedData = Base64.getEncoder().encodeToString(data.getBytes());
        System.out.println("Encoded String: " + encodedData);
    }
}