package ipsl.signature;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SecuredRestCall {
    public static void main(String[] args) throws Exception {
        // Paths to your .key and .pem files
        String keyFilePath = "path/to/your/private.key";
        String certFilePath = "path/to/your/certificate.pem";

        // Load the private key
        PrivateKey privateKey = loadPrivateKey(keyFilePath);

        // Load the certificate
        X509Certificate certificate = loadCertificate(certFilePath);

        // Create a KeyStore and load the private key and certificate
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("alias", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{certificate});

        // Set up the KeyManager and TrustManager with the KeyStore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Create an SSLContext using the KeyManagers and TrustManagers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        // Set the default SSLContext
        SSLContext.setDefault(sslContext);

        // Now make the HTTPS POST call
        URL url = new URL("https://secured-endpoint.com");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());

        // Set request method to POST
        connection.setRequestMethod("POST");

        // Set request headers to send XML
        connection.setRequestProperty("Content-Type", "application/xml");
        connection.setDoOutput(true); // Enable writing to the connection output stream

        // XML payload to send
        String xmlPayload = "<request><field1>value1</field1><field2>value2</field2></request>";

        // Write XML payload to the request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = xmlPayload.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }

        // Get the response code
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        // Optionally, read the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response: " + response.toString());
        }
    }

    // Method to load the private key from a .key file
    private static PrivateKey loadPrivateKey(String keyFilePath) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(keyFilePath)))
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    // Method to load the X509 certificate from a .pem file
    private static X509Certificate loadCertificate(String certFilePath) throws Exception {
        try (InputStream certInputStream = new FileInputStream(certFilePath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(certInputStream);
        }
    }
}
