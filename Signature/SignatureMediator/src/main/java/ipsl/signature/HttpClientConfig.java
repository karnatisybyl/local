package ipsl.signature;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class HttpClientConfig extends AbstractMediator{
	protected static String typAlgorithm = "http://www.w3.org/2000/09/xmldsig#enveloped-signature";
    protected static String canAlgorithm = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    protected static String digAlgorithm = "http://www.w3.org/2001/04/xmlenc#sha256";
    protected static String sigAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    protected static boolean omitDec = true;
    protected static boolean indent = false;
    public String getXmlStringRaw() {
		return xmlStringRaw;
	}

	public void setXmlStringRaw(String xmlStringRaw) {
		this.xmlStringRaw = xmlStringRaw;
	}

	public String getKeystoreFile() {
		return keystoreFile;
	}

	public void setKeystoreFile(String keystoreFile) {
		this.keystoreFile = keystoreFile;
	}

	public String getKeystorePass() {
		return keystorePass;
	}

	public void setKeystorePass(String keystorePass) {
		this.keystorePass = keystorePass;
	}

	public String getKeystoreAlias() {
		return keystoreAlias;
	}

	public void setKeystoreAlias(String keystoreAlias) {
		this.keystoreAlias = keystoreAlias;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}
	private String xmlStringRaw;
    private String keystoreFile;
    private String keystorePass; 
    private String keystoreAlias; 
    private String endpointUrl; 

    public static String generateSignature(String rawXml, String keystorePath, String keystorePws) {
        return "";
    }

    public static KeyStore loadKeyStore(String keystoreFile, String keystorePass) throws Exception {
        KeyStore keyStore = null;

        if (keystoreFile == null || keystoreFile.length() == 0) {
            return keyStore;
        }

        InputStream is = openResource(keystoreFile);
        if (is == null) {
            return keyStore;
        }

        if (keystorePass == null)
            keystorePass = "";

        if (keystoreFile.endsWith(".p12")) {
            keyStore = KeyStore.getInstance("PKCS12", "BC");
        } else {
            keyStore = KeyStore.getInstance("JKS");
        }
        keyStore.load(is, keystorePass.toCharArray());

        return keyStore;
    }

    public static InputStream openResource(String fileName) throws Exception {
        if (fileName == null || fileName.length() == 0) {
            return null;
        }
        if (fileName.startsWith("/") || fileName.indexOf(':') > 0) {
            return new FileInputStream(fileName);
        }
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }

    public static PrivateKey loadPrivateKey(String keystoreAlias, KeyStore keyStore, String keystorePass)
            throws Exception {

        Key aliasKey = keyStore.getKey(keystoreAlias, keystorePass.toCharArray());
        return aliasKey instanceof PrivateKey ? (PrivateKey) aliasKey : null;
    }

    public static Certificate loadCertificate(String keystoreAlias, KeyStore keyStore, String keystorePass)
            throws Exception {
        Certificate aliasCertificate = keyStore.getCertificate(keystoreAlias);
        return aliasCertificate instanceof X509Certificate ? (X509Certificate) aliasCertificate : null;
    }

    public static String sign(String content, String keystoreFile, String keystorePass, String keystoreAlias)
            throws Exception {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(content.getBytes()));

        KeyStore ks = loadKeyStore(keystoreFile, keystorePass);
        XMLSignature xmlSignature = createXMLSignature(document, ks, keystorePass, keystoreAlias);
        Element rootNode = document.getDocumentElement();

        DOMSignContext domSignContext = new DOMSignContext(loadPrivateKey(keystoreAlias, ks, keystorePass), rootNode);
        domSignContext.setDefaultNamespacePrefix("ds");
        xmlSignature.sign(domSignContext);

        return nodeToString(rootNode);
    }

    public static String nodeToString(Node node) throws Exception {
        StringWriter stringWriter = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDec ? "yes" : "no");
        transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    public static XMLSignature createXMLSignature(Document document, KeyStore ks, String keystorePass,
            String keystoreAlias) throws Exception {

        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM",
                new org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI());
        CanonicalizationMethod c14nMethod = xmlSignatureFactory.newCanonicalizationMethod(canAlgorithm,
                (C14NMethodParameterSpec) null);
        DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(digAlgorithm, null);
        SignatureMethod signMethod = xmlSignatureFactory.newSignatureMethod(sigAlgorithm, null);
        Transform sigTransform = xmlSignatureFactory.newTransform(typAlgorithm, (TransformParameterSpec) null);
        Transform canTransform = xmlSignatureFactory.newTransform(canAlgorithm, (TransformParameterSpec) null);

        List<Transform> transforms = new ArrayList();
        transforms.add(sigTransform);
        transforms.add(canTransform);

        String UNIQUE_GUID = UUID.randomUUID().toString();
        String sQualifyingPropertiesId = "_" + UNIQUE_GUID;

        Reference referenceDoc = xmlSignatureFactory.newReference("", digestMethod, transforms, null, null);
        Reference referenceQuP = xmlSignatureFactory.newReference("#" + sQualifyingPropertiesId,
                xmlSignatureFactory.newDigestMethod(digAlgorithm, null));

        List<Reference> references = new ArrayList();
        references.add(referenceDoc);
        references.add(referenceQuP);

        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(c14nMethod, signMethod, references);

        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(loadCertificate(keystoreAlias, ks, keystorePass)));
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data), sQualifyingPropertiesId);

        return xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, null, "_" + UUID.randomUUID(), null);
    }

    private String sendSignedXml(String signedXml, String endpointUrl, String keystoreFile, String keystorePass) throws Exception {
    	
    	 KeyStore keyStore = loadKeyStore(keystoreFile, keystorePass);
         TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(keyStore);

         SSLContext sslContext = SSLContext.getInstance("TLS");
         sslContext.init(null, tmf.getTrustManagers(), null);
         
         // Set default SSL context
         SSLContext.setDefault(sslContext);

         // Create HTTP connection
         URL url = new URL(endpointUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("POST");
         connection.setDoOutput(true);
         connection.setRequestProperty("Content-Type", "application/xml");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(signedXml.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Get the response
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }

        return response.toString();
    }

    @Override
    public boolean mediate(MessageContext context) {
        try {
            String signedXml = sign(xmlStringRaw, keystoreFile, keystorePass, keystoreAlias);
            System.out.println("Signed XML: " + signedXml);

            // Send the signed XML and capture the response
            String endpointResponse = sendSignedXml(signedXml, endpointUrl, keystoreFile, keystorePass);
            System.out.println("Endpoint Response: " + endpointResponse);
            context.setProperty("output", endpointResponse);
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error during mediation: " + e.getMessage());
            //e.printStackTrace();
            context.setProperty("output", e.getMessage());

        }
        return true;
    }


    
}
