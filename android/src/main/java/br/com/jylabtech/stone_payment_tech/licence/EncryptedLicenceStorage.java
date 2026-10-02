package br.com.jylabtech.stone_payment_tech.licence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Cache local da licença em EncryptedSharedPreferences
 * (mesmo nome de prefs e chave das versões anteriores).
 */
public class EncryptedLicenceStorage implements LicenceStorage {

    private static final String PREFS_NAME = "licence_prefs_encrypted";
    private static final String KEY_JSON = "licence_entity";

    private final Context context;
    /** Criado uma vez (MasterKey + EncryptedSharedPreferences são caros: Keystore + Tink). */
    private SharedPreferences cachedPrefs;

    public EncryptedLicenceStorage(Context context) {
        this.context = context;
    }

    private synchronized SharedPreferences prefs() throws Exception {
        if (cachedPrefs != null) {
            return cachedPrefs;
        }
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        // Só guarda após sucesso: se a criação falhar, a próxima chamada tenta de novo.
        cachedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        return cachedPrefs;
    }

    @Override
    public CheckLicenceEntity load() {
        try {
            String savedJson = prefs().getString(KEY_JSON, null);
            if (savedJson != null) {
                return CheckLicenceEntity.fromJson(savedJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean save(CheckLicenceEntity entity) {
        try {
            String json = entity.toJson();
            if (json == null || json.isEmpty()) {
                return false;
            }
            return prefs().edit().putString(KEY_JSON, json).commit();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void delete() {
        try {
            prefs().edit().remove(KEY_JSON).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
