package br.com.jylabtech.stone_payment_tech.licence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

// Exceções personalizadas
class LicenceInvalidException extends Exception { LicenceInvalidException(String msg) { super(msg); } }
class LicenceUnAuthorizedException extends Exception { LicenceUnAuthorizedException(String msg) { super(msg); } }
class LicenceNotFoundException extends Exception { LicenceNotFoundException(String msg) { super(msg); } }
class LicenceConditionRequiredException extends Exception { LicenceConditionRequiredException(String msg) { super(msg); } }
class LicenceExpiredException extends Exception { LicenceExpiredException(String msg) { super(msg); } }
class LicenceInternalErrorException extends Exception { LicenceInternalErrorException(String msg) { super(msg); } }
class LicenceErrorException extends Exception { LicenceErrorException(String msg) { super(msg); } }

public class LicenceProvider implements LicenceHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;

    private final Context context;

    public LicenceProvider(Context context) {
        this.context = context;
    }

    /**
     * Faz o POST de validação de forma bloqueante. Deve ser chamado fora da
     * main thread. Devolve status e corpo; a decisão fica em LicenceRules.
     */
    @Override
    public LicenceHttpResponse post(final String urlString, final String licenceKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            String deviceId = getAndroidId();
            String deviceUid = getSerialNumber();
            String deviceLocalId = getDeviceUUID();
            String deviceName = Build.MODEL;

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Content-Type", "application/json");

            if (licenceKey != null && !licenceKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer licence:" + licenceKey);
            }

            connection.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("deviceId", deviceId);
            body.put("deviceUid", deviceUid);
            body.put("deviceLocalId", deviceLocalId);
            body.put("deviceName", deviceName);

            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode == HttpURLConnection.HTTP_OK
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            return new LicenceHttpResponse(responseCode, readAll(stream));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            return responseStr.toString();
        } finally {
            reader.close();
        }
    }

    @SuppressLint("HardwareIds")
    private String getAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getSerialNumber() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial();
            } else {
                return Build.SERIAL != null ? Build.SERIAL : "UNKNOWN_SERIAL";
            }
        } catch (SecurityException e) {
            return "UNKNOWN_SERIAL";
        } catch (Exception e) {
            return "UNKNOWN_SERIAL";
        }
    }

    private String getDeviceUUID() {
        return UUID.randomUUID().toString();
    }
}
