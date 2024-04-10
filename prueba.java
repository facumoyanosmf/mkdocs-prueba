package com.smf.pucara.poscustomizations;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import com.smf.pucara.poscustomizations.data.SMFPOSCPaymentConfig;
public class DebitProcess {
  private static final String CONTENT_TYPE = "application/json";
  private static final SMFPOSCPaymentConfig configPayment;
  private static String apiKey;
  private static String email;
  private static String password;
  private static String urlBase;
  static {
    try {
      configPayment = getPaymentConfig("Duc");
      generateCredentials();
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    }
  }
  private DebitProcess() {
    throw new AssertionError();
  }
  private static void generateCredentials() {
    apiKey = configPayment.getAPIKey();
    email = configPayment.getUserEmail();
    password = configPayment.getUserPassword();
    urlBase = configPayment.getUrl();
    if (Boolean.TRUE.equals(configPayment.isTestMode())) {
      urlBase = configPayment.getTestUrl();
      apiKey = configPayment.getAPIKeyTest();
    }
  }
  public static String[] sendDebitRequest(String accessToken, String payload) {
    String urlEndpoint = "/api/private/cards/debit";
    String url = urlBase+urlEndpoint;
    int responseCode = 0;
    String responseMessage = null;
    String responseBody = null;
    String errorMessage = null;
    try {
      URL obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("x-api-key", apiKey);
      con.setRequestProperty("Authorization", "Bearer " + accessToken);
      con.setRequestProperty("Content-Type", CONTENT_TYPE);
      con.setDoOutput(true);
      OutputStream os = con.getOutputStream();
      os.write(payload.getBytes(StandardCharsets.UTF_8));
      os.flush();
      os.close();
      responseCode = con.getResponseCode();
      responseMessage = con.getResponseMessage();
      BufferedReader in;
      if (responseCode >= 400) {
        in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
      } else {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      }
      StringBuilder response = new StringBuilder();
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      responseBody = response.toString();
      if (responseCode >= 400) {
        JSONObject errorJson = new JSONObject(responseBody);
        errorMessage = errorJson.getString("message");
      }
      String[] responseArray = new String[3];
      responseArray[0] = String.valueOf(responseCode);
      responseArray[1] = responseMessage;
      responseArray[2] = errorMessage;
      return responseArray;
    } catch (IOException | JSONException e) {
      String[] responseArray = new String[3];
      responseArray[0] = String.valueOf(responseCode);
      responseArray[1] = responseMessage + e.getMessage();
      responseArray[2] = responseBody;
      return responseArray;
    }
  }
  public static JSONObject getTokenAccess() {
    try {
      JSONObject response;
      String urlEndpoint = "/api/auth/login";
      String url = urlBase+urlEndpoint;
      URL urlLogin = new URL(url);
      HttpURLConnection http = (HttpURLConnection) urlLogin.openConnection();
      http.setRequestMethod("POST");
      http.setRequestProperty("x-api-key", apiKey);
      http.setRequestProperty("Content-Type", CONTENT_TYPE);
      http.setDoOutput(true);
      JSONObject bodyParams = new JSONObject();
      bodyParams.put("email", email);
      bodyParams.put("password", password);
      OutputStream os = http.getOutputStream();
      os.write(bodyParams.toString().getBytes());
      os.flush();
      os.close();
      BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
      StringBuilder responseBuilder = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        responseBuilder.append(line);
      }
      in.close();
      response = new JSONObject(responseBuilder.toString());
      return response;
    } catch (Exception e) {
      throw new OBException("Caught an unexpected exception: " + e.getMessage());
    }
  }
  public static SMFPOSCPaymentConfig getPaymentConfig(String providerName) throws OBException {
    try {
      OBCriteria<SMFPOSCPaymentConfig> paymentCriteria = OBDal.getInstance().createCriteria(SMFPOSCPaymentConfig.class);
      paymentCriteria.add(Restrictions.eq("providerName", providerName));
      paymentCriteria.setMaxResults(1);
      return (SMFPOSCPaymentConfig) paymentCriteria.uniqueResult();
    } catch (Exception e) {
      throw new OBException("Error al obtener la configuración de pago: " + e.getMessage());
    }
  }
}