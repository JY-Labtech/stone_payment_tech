package br.com.jylabtech.stone_payment_tech.licence;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import android.util.Log;


public class CheckLicenceTech {

    private static final String PREFS_NAME = "licence_prefs_encrypted";
    private static final String KEY_JSON = "licence_entity";

    private final Context context;


    private final LicenceProvider licenceProvider;

    public CheckLicenceTech(Context context) {
        this.context = context;
        this.licenceProvider = new LicenceProvider(context);
    }

    /**
     * Valida a licença chamando a API e salva localmente se sucesso.
     */
    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        LicenceResult licenceResult = new LicenceResult();
        licenceResult.setMethod("licence");
        try {
            boolean licenceDatabase = validateLicenceDatabase(licenceKey, licenceInternalKey);
            if(licenceDatabase) {
                licenceResult.setResult(0);
                return licenceResult;
            }

            String apiUrl = sanitizeUrl(licenceInternalKey);
            // Chama o provider que já pega deviceId, serialNumber e uuid internamente
            LicenceResult licence = licenceProvider.checkLicence(apiUrl, licenceKey);
            if (licence != null && licence.getResult() == 0) {
                boolean isValidLicence = validateLicenceServer(licence.getLicenceEntity());
                if(isValidLicence) {
                    CheckLicenceEntity entity = licence.getLicenceEntity();
                    entity.setLicenceInternalKey(licenceInternalKey);
                    saveLicenceEncrypted(entity);
                    licenceResult.setResult(0);
                    licenceResult.setLicenceEntity(entity);
                }
            } else {
                return licence;
            }

            return licenceResult;
        } catch (Exception e) {
            licenceResult.setResult(999999);
            licenceResult.setErrorMessage(e.getMessage());
            e.printStackTrace();
        }
        return licenceResult;
    }

    public boolean validateExistsLicence() {
        boolean aux = false;
        Date now = new Date();

        try {
            CheckLicenceEntity licenceDatabase = getLicenceEncrypted();
            if(licenceDatabase!= null) {
                if(licenceDatabase.getExpiredAt().after(now)) {
                    aux = true;
                }
                if(licenceDatabase.isExpired() || licenceDatabase.isInactive()) {
                    aux = false;
                }
                if(licenceDatabase.getSavedLocalDatabaseAt() != null) {
                    long diffInMillis = now.getTime() - licenceDatabase.getSavedLocalDatabaseAt().getTime();
                    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                    if (diffInDays > 7) {
                        deleteLicenceDatabase();
                        aux = false;
                    }
                }
            }
            return aux;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateLicenceServer(CheckLicenceEntity entity) {
        boolean aux = false;
        Date now = new Date();
        if(entity.getExpiredAt().after(now)) {
            aux = true;
        }

        if(entity.isExpired() || entity.isInactive()) {
            aux = false;
        }

        return aux;
    }

    private boolean validateLicenceDatabase(String licenceKey, String licenceInternalKey) {
        boolean aux = false;
        Date now = new Date();

        try {
            CheckLicenceEntity licenceDatabase = getLicenceEncrypted();
            if(licenceDatabase!= null) {
                if(licenceDatabase.getExpiredAt().after(now)) {
                    aux = true;
                }
                if(licenceDatabase.isExpired() || licenceDatabase.isInactive()) {
                    aux = false;
                }
                if(licenceDatabase.getSavedLocalDatabaseAt() != null) {
                    long diffInMillis = now.getTime() - licenceDatabase.getSavedLocalDatabaseAt().getTime();
                    long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                    if (diffInDays > 7) {
                        deleteLicenceDatabase();
                        aux = false;
                    }
                }

                if(!Objects.equals(licenceDatabase.getLicenceEncrypt(), licenceKey)) {
                    aux = false;
                }

                if(!Objects.equals(licenceDatabase.getLicenceInternalKey(), licenceInternalKey)) {
                    deleteLicenceDatabase();
                    aux = false;
                }
            }
            return aux;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva a entidade de licença localmente de forma criptografada.
     */
    private boolean saveLicenceEncrypted(CheckLicenceEntity entity) {
        try {
            entity.setSavedLocalDatabaseAt(new Date());

            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            prefs.edit().putString(KEY_JSON, entity.toJson()).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recupera a licença salva localmente de forma criptografada.
     */
    private CheckLicenceEntity getLicenceEncrypted() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String savedJson = prefs.getString(KEY_JSON, null);
            if (savedJson != null) {
                return CheckLicenceEntity.fromJson(savedJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteLicenceDatabase() {
        try {
            // Cria ou recupera a chave mestre de forma segura
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Acessa o SharedPreferences criptografado
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,                  // chave mestre
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Remove a licença salva
            prefs.edit().remove(KEY_JSON).apply();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sanitizeUrl(String data) {
        if (data.contains("http")) {
            return data;
        } else {
            byte[] decodedBytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        }
    }
}
