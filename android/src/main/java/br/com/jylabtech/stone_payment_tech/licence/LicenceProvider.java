package br.com.jylabtech.stone_payment_tech.licence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Exceções personalizadas
class LicenceInvalidException extends Exception { LicenceInvalidException(String msg) { super(msg); } }
class LicenceUnAuthorizedException extends Exception { LicenceUnAuthorizedException(String msg) { super(msg); } }
class LicenceNotFoundException extends Exception { LicenceNotFoundException(String msg) { super(msg); } }
class LicenceConditionRequiredException extends Exception { LicenceConditionRequiredException(String msg) { super(msg); } }
class LicenceExpiredException extends Exception { LicenceExpiredException(String msg) { super(msg); } }
class LicenceInternalErrorException extends Exception { LicenceInternalErrorException(String msg) { super(msg); } }
class LicenceErrorException extends Exception { LicenceErrorException(String msg) { super(msg); } }

public class LicenceProvider {

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LicenceProvider(Context context) {
        this.context = context;
    }

    /**
     * Faz a requisição POST de forma assíncrona usando ExecutorService.
     * Bloqueia a chamada atual até completar.
     */
    public CheckLicenceEntity checkLicence(final String urlString, final String licenceKey) throws Exception {
        Callable<CheckLicenceEntity> task = new Callable<CheckLicenceEntity>() {
            @Override
            public CheckLicenceEntity call() throws Exception {
                HttpURLConnection connection = null;
                try {
                    String deviceId = getAndroidId();
                    String deviceUid = getSerialNumber();
                    String deviceLocalId = getDeviceUUID();
                    String deviceName = Build.MODEL;

                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");

                    if (!licenceKey.isEmpty()) {
                        connection.setRequestProperty("Authorization", "Bearer licence:" + licenceKey);
                    }

                    connection.setDoOutput(true);

                    JSONObject body = new JSONObject();
//                    body.put("deviceId", deviceId);
//                    body.put("deviceUid", deviceUid);
//                    body.put("deviceLocalId", deviceLocalId);
//                    body.put("deviceName", deviceName);
                    body.put("serialNumber", deviceUid);

                    OutputStream os = connection.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = connection.getResponseCode();
                    BufferedReader reader;

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder responseStr = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseStr.append(line);
                        }
                        reader.close();

                        return CheckLicenceEntity.fromJson(responseStr.toString());
                    } else {
                        handleErrors(connection);
                        return null; // nunca chega aqui
                    }

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        };

        Future<CheckLicenceEntity> future = executor.submit(task);
        try {
            return future.get(); // aguarda a execução completa
        } catch (ExecutionException e) {
            throw e.getCause() != null ? (Exception) e.getCause() : e;
        }
    }

    private void handleErrors(HttpURLConnection connection) throws Exception {
        int statusCode = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        StringBuilder responseStr = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseStr.append(line);
        }
        reader.close();

        String message = null;
        try {
            JSONObject json = new JSONObject(responseStr.toString());
            message = json.optString("message", null);
        } catch (JSONException e) { }

        switch (statusCode) {
            case 400: throw new LicenceInvalidException(message != null ? message : "Bad Request");
            case 401: throw new LicenceUnAuthorizedException(message != null ? message : "Unauthorized");
            case 404: throw new LicenceNotFoundException(message != null ? message : "Not Found");
            case 412: throw new LicenceConditionRequiredException(message != null ? message : "Precondition Failed");
            case 428: throw new LicenceExpiredException(message != null ? message : "Licence Expired");
            case 500: throw new LicenceInternalErrorException(message != null ? message : "Internal Server Error");
            default: throw new LicenceErrorException(message != null ? message : "Unknown Error");
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
