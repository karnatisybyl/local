package ipsl.signature;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
public class SignXml  extends AbstractMediator{

	

    private static final Log log = LogFactory.getLog(SignXml.class);

	protected static String typAlgorithm = "http://www.w3.org/2000/09/xmldsig#enveloped-signature";
	protected static String canAlgorithm = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
	protected static String digAlgorithm = "http://www.w3.org/2001/04/xmlenc#sha256";
	protected static String sigAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

	protected static boolean omitDec = true;
	protected static boolean indent = false;

	 private String xmlStringRaw;
	    private String keystoreFile;
	    private String keystorePass;
	    private String keystoreAlias;

	    // Getter and Setter for xmlStringRaw
	    public String getXmlStringRaw() {
	        return xmlStringRaw;
	    }

	    public void setXmlStringRaw(String xmlStringRaw) {
	        this.xmlStringRaw = xmlStringRaw;
	    }

	    // Getter and Setter for keystoreFile
	    public String getKeystoreFile() {
	        return keystoreFile;
	    }

	    public void setKeystoreFile(String keystoreFile) {
	        this.keystoreFile = keystoreFile;
	    }

	    // Getter and Setter for keystorePass
	    public String getKeystorePass() {
	        return keystorePass;
	    }

	    public void setKeystorePass(String keystorePass) {
	        this.keystorePass = keystorePass;
	    }

	    // Getter and Setter for keystoreAlias
	    public String getKeystoreAlias() {
	        return keystoreAlias;
	    }

	    public void setKeystoreAlias(String keystoreAlias) {
	        this.keystoreAlias = keystoreAlias;
	    }
	
	@Override
	public boolean mediate(MessageContext context) {
		
		

			try {
				//System.out.println(sign(xmlStringRaw, keystoreFile, keystorePass, keystoreAlias));
				log.info(sign(xmlStringRaw, keystoreFile, keystorePass, keystoreAlias));
				context.setProperty("output", sign(xmlStringRaw, keystoreFile, keystorePass, keystoreAlias));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		
		

		
		return true;
	}


	public static String generateSignature(String rawXml, String keystorePath, String keystorePws) {

		return "";
	}

	public static KeyStore loadKeyStore(String keystoreFile, String keystorePass) throws Exception {
		KeyStore keyStore = null;

		if (keystoreFile == null || keystoreFile.length() == 0) {
			return keyStore;
		}

		System.out.println(keystoreFile);
		InputStream is = openResource(keystoreFile);
		if (is == null) {
			System.out.println("null ks");
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
		URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
		if (url == null)
			return null;
		return url.openStream();
	}

	public static PrivateKey loadPrivateKey(String keystoreAlias, KeyStore keyStore, String keystorePass)
			throws Exception {

		PrivateKey privateKey = null;

		Key aliasKey = keyStore.getKey(keystoreAlias, keystorePass.toCharArray());
		if (aliasKey instanceof PrivateKey) {
			privateKey = (PrivateKey) aliasKey;
		}

		return privateKey;

	}

	public static Certificate loadCertificate(String keystoreAlias, KeyStore keyStore, String keystorePass)
			throws Exception {
		Certificate certificate = null;

		Certificate aliasCertificate = keyStore.getCertificate(keystoreAlias);

		if (aliasCertificate instanceof X509Certificate) {
			certificate = (X509Certificate) aliasCertificate;
		}

		return certificate;
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
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDec ? "yes" : "no");
			transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
			transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
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

		List<Transform> transforms = new ArrayList<Transform>();
		transforms.add(sigTransform);
		transforms.add(canTransform);

		String UNIQUE_GUID = UUID.randomUUID().toString();
		String sQualifyingPropertiesId = "_" + UNIQUE_GUID;

		Reference referenceDoc = xmlSignatureFactory.newReference("", digestMethod, transforms, null, null);
		Reference referenceQuP = xmlSignatureFactory.newReference("#" + sQualifyingPropertiesId,
				xmlSignatureFactory.newDigestMethod(digAlgorithm, null));

		List<Reference> references = new ArrayList<Reference>();
		references.add(referenceDoc);
		references.add(referenceQuP);

		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(c14nMethod, signMethod, references);

		KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
		X509Data x509Data = keyInfoFactory
				.newX509Data(Collections.singletonList(loadCertificate(keystoreAlias, ks, keystorePass)));
		KeyInfo keyInfo = null;
		keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data), sQualifyingPropertiesId);

		return xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, null, "_" + UUID.randomUUID(), null);
	}

}