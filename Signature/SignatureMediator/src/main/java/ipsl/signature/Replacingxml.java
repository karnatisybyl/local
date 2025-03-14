package ipsl.signature;

public class Replacingxml {

    public static void main(String[] args) {
        String escapedXml = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n"
                + "	<soapenv:Envelope>\r\n"
                + "		<soapenv:Body>\r\n"
                + "			<res:ResultMsg xmlns:res=\"http://api-v1.gen.mm.vodafone.com/mminterface/result\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?>&lt;Result xmlns=\"http://api-v1.gen.mm.vodafone.com/mminterface/result\">&lt;ResultType>0&lt;/ResultType>&lt;ResultCode>2040&lt;/ResultCode>&lt;ResultDesc>Credit Party customer type (Unregistered or Registered Customer) can&amp;apos;t be supported by the service .&lt;/ResultDesc>&lt;OriginatorConversationID>70365803919&lt;/OriginatorConversationID>&lt;ConversationID>AG_20241030_201070ec8583cfe29946&lt;/ConversationID>&lt;TransactionID>SJU0000000&lt;/TransactionID>&lt;ReferenceData>&lt;ReferenceItem>&lt;Key>QueueTimeoutURL&lt;/Key>&lt;Value>https://surveys.familybank.co.ke/api/daraja/callback/1.0/transactionstatus/response&lt;/Value>&lt;/ReferenceItem>&lt;/ReferenceData>&lt;/Result></res:ResultMsg>\r\n"
                + "		</soapenv:Body>\r\n"
                + "	</soapenv:Envelope>\r\n"
                + "</soapenv:Body>"; // Your escaped XML string
        
        // Replace all escaped characters
        String unescapedXml = escapedXml
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;apos;", "'")
                .replaceAll("&amp;", "&");
        
        // Remove the XML declaration
        String backendResponse = unescapedXml.replaceAll("(<\\?xml.*?\\?>)", "");

        System.out.println("Unescaped XML: \n" + backendResponse);
    }
}
