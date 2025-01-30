package ipsl.signature;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONArray;
import org.json.JSONObject;

public class CustomDBMediator extends AbstractMediator {
   public boolean mediate(MessageContext messageContext) {
      String host = (String)messageContext.getProperty("host");
      String port = (String)messageContext.getProperty("port");
      String dbname = (String)messageContext.getProperty("dbname");
      String userName = (String)messageContext.getProperty("userName");
      String password = (String)messageContext.getProperty("password");
      String driverClass = (String)messageContext.getProperty("driverClass");
     // String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbname + "?allowPublicKeyRetrieval=true&useSSL=false";
      String jdbcUrl = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + dbname;      
      JSONArray jsonArray = new JSONArray();
      JSONObject jsonResponse = new JSONObject();

      try {
         Class.forName(driverClass);
         Connection connection = DriverManager.getConnection(jdbcUrl, userName, password);
         System.out.println("*****************connection*******************" + connection);
         Statement statement = connection.createStatement();
         String query = (String)messageContext.getProperty("query");
         if (!query.trim().toUpperCase().startsWith("SELECT") && !query.trim().toUpperCase().startsWith("CALL")) {
            int rowsAffected = statement.executeUpdate(query);
            if (rowsAffected > 0) {
               jsonResponse.put("status", "success");
            } else {
               jsonResponse.put("status", "failure");
            }
         } else {
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData metaData = (ResultSetMetaData)resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while(true) {
               if (!resultSet.next()) {
                  if (jsonArray.length() > 0) {
                     jsonResponse.put("status", "success");
                     jsonResponse.put("data", jsonArray);
                  } else {
                     jsonResponse.put("status", "no records found");
                  }

                  resultSet.close();
                  break;
               }

               JSONObject jsonObject = new JSONObject();

               for(int i = 1; i <= columnCount; ++i) {
                  String columnName = metaData.getColumnName(i);
                  String columnValue = resultSet.getString(i);
                  jsonObject.put(columnName, columnValue);
               }

               jsonArray.put(jsonObject);
            }
         }

         statement.close();
         connection.close();
      } catch (Exception var21) {
         jsonResponse.put("status", "failure");
         var21.printStackTrace();
      }

      messageContext.setProperty("queryResult", jsonResponse.toString());
      return true;
   }
}