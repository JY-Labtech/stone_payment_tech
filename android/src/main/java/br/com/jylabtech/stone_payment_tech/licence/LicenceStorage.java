package br.com.jylabtech.stone_payment_tech.licence;

/**
 * Abstração do cache local da licença (EncryptedSharedPreferences em produção).
 */
public interface LicenceStorage {
    CheckLicenceEntity load();

    boolean save(CheckLicenceEntity entity);

    void delete();
}
