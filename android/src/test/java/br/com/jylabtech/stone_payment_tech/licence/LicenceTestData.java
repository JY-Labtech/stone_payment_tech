package br.com.jylabtech.stone_payment_tech.licence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class LicenceTestData {
    static final String KEY = "test-licence-key";
    static final String CORE_URL = LicenceConfig.LICENCE_CHECK_URL;
    static final String CORE_BASE64 = "aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=";
    static final String OLD_CLOUD_RUN_URL = "https://pos-payments-api-577464235408.southamerica-east1.run.app/pos-payments/licence/check";
    static final String OLD_CLOUD_RUN_BASE64 = "aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjaw==";

    static final long DAY = 24L * 60 * 60 * 1000;
    static final long MINUTE = 60L * 1000;

    private LicenceTestData() {}

    static Date date(String value) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return sdf.parse(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static final Date NOW = date("2026-10-01T12:00:00");

    /** JSON no formato do LegacyLicence do jylabtech_core (valores fictícios). */
    static String coreJson(String expiredAt, boolean isExpired, boolean isInactive, boolean isCanceled) {
        return "{"
                + "\"id\":\"lic1\",\"uid\":\"lic1\",\"deviceUid\":\"\",\"deviceSerialNumber\":\"\","
                + "\"userUid\":\"cust1\",\"productId\":\"prod1\",\"productName\":\"Licença plugin de pagamento\","
                + "\"productDescription\":\"\",\"integrationLibId\":\"\",\"integrationLibName\":\"pos_plugin\","
                + "\"checkoutSessionId\":\"\",\"licenceKey\":\"00000000-0000-0000-0000-000000000000\","
                + "\"licenceEncrypt\":\"" + KEY + "\","
                + (expiredAt == null ? "" : "\"expiredAt\":\"" + expiredAt + "\",")
                + "\"isExpired\":" + isExpired + ",\"isInactive\":" + isInactive + ","
                + "\"isTrial\":false,\"isDebug\":true,\"isCanceled\":" + isCanceled + ",\"canceledAt\":null,"
                + "\"licencesQuantity\":-1,\"subscriptionId\":\"\",\"subscription\":null,"
                + "\"checkoutType\":\"one_time\",\"createdAt\":\"2026-01-01T10:00:00\","
                + "\"updatedAt\":\"2026-09-30T10:00:00\",\"coreToken\":\"fake.core.token\""
                + "}";
    }

    static String validJson() {
        return coreJson("2027-01-01T23:59:59", false, false, false);
    }

    static CheckLicenceEntity validCache(Date savedAt) {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(validJson());
        e.setSavedLocalDatabaseAt(savedAt);
        e.setLicenceInternalKey(CORE_URL);
        return e;
    }
}
