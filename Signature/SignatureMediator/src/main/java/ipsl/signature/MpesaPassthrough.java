package ipsl.signature;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MpesaPassthrough extends AbstractMediator {

    public boolean mediate(MessageContext context) {
        String clientRequest = (String) context.getProperty("request");
        String spId = (String) context.getProperty("spId");
        String spPassword = (String) context.getProperty("spPassword");
        String serviceId = (String) context.getProperty("serviceId");
        String timeStamp = (String) context.getProperty("timeStamp");
        String backendUrl = (String) context.getProperty("backendUrl");

        try {
            String responseXml = prepareSOAPRequest(clientRequest, spId, spPassword, serviceId, timeStamp);

            System.out.println("Formatted Request XML: \n" + responseXml);

            String backendResponse = sendToBackend(backendUrl, responseXml);

            System.out.println("Backend Response: \n" + backendResponse);

            context.setProperty("response", backendResponse);

        } catch (Exception e) {
            e.printStackTrace();
            context.setProperty("response", "Error occurred: " + e.getMessage());
            return false;
        }

        return true;
    }

    private String prepareSOAPRequest(String clientRequest, String spId, String spPassword, String serviceId, String timeStamp) 
            throws ParserConfigurationException, SAXException, IOException {

        InputStream inputStream = new ByteArrayInputStream(clientRequest.getBytes("UTF-8"));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        document.getDocumentElement().normalize();

        NodeList requestNodes = document.getElementsByTagNameNS("http://api-v1.gen.mm.vodafone.com/mminterface/request", "request");
        String parsedRequest = nodeToString(requestNodes.item(0));

        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:req=\"http://api-v1.gen.mm.vodafone.com/mminterface/request\">" +
                "<soapenv:Header xmlns:tns=\"http://www.huawei.com/schema/osg/common/v2_1\">" +
                "<tns:RequestSOAPHeader>" +
                "<tns:spId>" + spId + "</tns:spId>" +
                "<tns:spPassword>" + spPassword + "</tns:spPassword>" +
                "<tns:serviceId>" + serviceId + "</tns:serviceId>" +
                "<tns:timeStamp>" + timeStamp + "</tns:timeStamp>" +
                "</tns:RequestSOAPHeader>" +
                "</soapenv:Header>" +
                "<soapenv:Body>" +
                "<req:RequestMsg><![CDATA[" + parsedRequest + "]]></req:RequestMsg>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
    }

    private String sendToBackend(String backendUrl, String soapRequest) throws IOException {
        String backendResponse;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(backendUrl);
            StringEntity entity = new StringEntity(soapRequest);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "text/xml");

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
                StringBuilder responseString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseString.append(line);
                }
                backendResponse = responseString.toString();
            }
        }
        return backendResponse;
    }

    private static String nodeToString(Node node) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            DOMSource source = new DOMSource(node);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.getBuffer().toString();
        } catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
