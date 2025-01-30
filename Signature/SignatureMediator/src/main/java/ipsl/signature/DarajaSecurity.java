package ipsl.signature;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.crypto.Cipher;
import java.util.Base64;

public class DarajaSecurity {
    public static String encryptPassword(String password, String certPath) throws Exception {
       
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream(certPath);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(fis);
        
     
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey());
        
        
        byte[] encryptedBytes = cipher.doFinal(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static void main(String[] args) throws Exception {
        String certPath = "C:\\Program Files\\OpenSSL-Win64\\bin\\SandboxCertificate.cer";
        String password = "Tanganyika*600";
        String encryptedPassword = encryptPassword(password, certPath);
        System.out.println("Encrypted Password: " + encryptedPassword);
    }
}
