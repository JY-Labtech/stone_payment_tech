package br.com.jylabtech.stone_payment_tech.licence;

import static br.com.jylabtech.stone_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

public class CheckLicenceTechTest {

    static class FakeStorage implements LicenceStorage {
        String json;
        int saves;
        int deletes;
        boolean failSave;
        boolean throwOnSave;

        @Override public CheckLicenceEntity load() { return json == null ? null : CheckLicenceEntity.fromJson(json); }
        @Override public boolean save(CheckLicenceEntity entity) {
            if (throwOnSave) throw new RuntimeException("Keystore indisponível");
            if (failSave) return false;
            saves++;
            json = entity.toJson();
            return true;
        }
        @Override public void delete() { deletes++; json = null; }
    }

    static class FakeHttp implements LicenceHttpClient {
        LicenceHttpResponse response;
        Exception error;
        int calls;
        String lastUrl;
        String lastKey;

        @Override public LicenceHttpResponse post(String url, String licenceKey) throws Exception {
            calls++;
            lastUrl = url;
            lastKey = licenceKey;
            if (error != null) throw error;
            return response;
        }
    }

    private static final long HOUR = 60 * MINUTE;

    private FakeStorage storage;
    private FakeHttp http;
    private CheckLicenceTech tech;
    private Date now;

    @Before
    public void setUp() {
        now = NOW;
        storage = new FakeStorage();
        http = new FakeHttp();
        tech = new CheckLicenceTech(storage, http, new LicenceClock() {
            @Override public Date now() { return now; }
        });
    }

    private void advance(long millis) {
        now = new Date(now.getTime() + millis);
    }

    private LicenceResult validate() {
        return tech.validateLicence(KEY, CORE_BASE64);
    }

    @Test
    public void firstCheckOfProcessGoesOnlineEvenWithValidCache() {
        storage.json = validCache(new Date(NOW.getTime() - HOUR)).toJson();
        http.response = new LicenceHttpResponse(200, validJson());
        assertTrue(validate().isSuccess());
        assertEquals(1, http.calls);
        assertEquals(NOW, storage.load().getSavedLocalDatabaseAt());
    }

    @Test
    public void validCacheSkipsNetworkAfterOnlineCheckUntilRevalidationInterval() {
        http.response = new LicenceHttpResponse(200, validJson());
        validate();
        assertEquals(1, http.calls);

        advance(HOUR);
        assertTrue(validate().isSuccess());
        assertEquals(1, http.calls);
        assertTrue(tech.hasValidCachedLicence(KEY, CORE_BASE64));

        advance(LicenceRules.REVALIDATE_INTERVAL_MS);
        assertTrue(validate().isSuccess());
        assertEquals(2, http.calls);
    }

    @Test
    public void cacheOnlyShortcutRequiresOnlineCheckInThisProcess() {
        storage.json = validCache(NOW).toJson();
        // app recém-aberto: o atalho sem rede não libera, força a consulta ao servidor
        assertFalse(tech.hasValidCachedLicence(KEY, CORE_BASE64));
        assertNotNull(storage.json);

        http.response = new LicenceHttpResponse(200, validJson());
        validate();
        assertTrue(tech.hasValidCachedLicence(KEY, CORE_BASE64));

        advance(LicenceRules.REVALIDATE_INTERVAL_MS);
        assertFalse(tech.hasValidCachedLicence(KEY, CORE_BASE64));

        // servidor fora do ar: o atalho libera pelo cache durante a espera entre tentativas
        http.error = new IOException("sem internet");
        assertTrue(validate().isSuccess());
        assertTrue(tech.hasValidCachedLicence(KEY, CORE_BASE64));
        advance(LicenceRules.RETRY_AFTER_FAILURE_MS);
        assertFalse(tech.hasValidCachedLicence(KEY, CORE_BASE64));
    }

    @Test
    public void customerBlockedClearsCacheAndBlocksImmediately() {
        http.response = new LicenceHttpResponse(200, validJson());
        validate();
        advance(LicenceRules.REVALIDATE_INTERVAL_MS);
        http.response = new LicenceHttpResponse(428,
                "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}");

        LicenceResult r = validate();
        assertFalse(r.isSuccess());
        assertTrue(r.isRejectedByServer());
        assertEquals(LicenceRules.MSG_CUSTOMER_BLOCKED, r.getErrorMessage());
        assertNull(storage.json);
        assertFalse(tech.hasValidCachedLicence(KEY, CORE_BASE64));
        // sem cache: a próxima operação também consulta o servidor e continua bloqueada
        assertFalse(validate().isSuccess());
        assertEquals(3, http.calls);
    }

    @Test
    public void everyDefinitiveRefusalClearsValidCache() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            setUp();
            storage.json = validCache(NOW).toJson();
            http.response = new LicenceHttpResponse(code, "{\"message\":\"recusada\"}");
            LicenceResult r = validate();
            assertFalse("HTTP " + code, r.isSuccess());
            assertEquals("recusada", r.getErrorMessage());
            assertNull("HTTP " + code, storage.json);
        }
    }

    @Test
    public void online200CanceledClearsValidCache() {
        storage.json = validCache(NOW).toJson();
        http.response = new LicenceHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true));
        assertFalse(validate().isSuccess());
        assertNull(storage.json);
    }

    @Test
    public void outageUsesCacheUpTo72Hours() {
        http.response = new LicenceHttpResponse(200, validJson());
        validate();
        http.error = new UnknownHostException("portal-api.jylabtech.com.br");

        advance(71 * HOUR);
        assertTrue(validate().isSuccess());
        assertNotNull(storage.json);

        advance(HOUR + 1000);
        LicenceResult r = validate();
        assertFalse(r.isSuccess());
        assertEquals(LicenceRules.formatConnectionError(http.error), r.getErrorMessage());
        assertNull(storage.json);
    }

    @Test
    public void serverErrorsUseCache() {
        for (int code : new int[]{500, 502, 503, 408, 429}) {
            setUp();
            storage.json = validCache(NOW).toJson();
            http.response = new LicenceHttpResponse(code, "{\"message\":\"fora do ar\"}");
            assertTrue("HTTP " + code, validate().isSuccess());
            assertNotNull("HTTP " + code, storage.json);
        }
    }

    @Test
    public void unreadable200UsesCache() {
        storage.json = validCache(NOW).toJson();
        http.response = new LicenceHttpResponse(200, "<html>captive portal</html>");
        assertTrue(validate().isSuccess());
        assertNotNull(storage.json);
    }

    @Test
    public void afterOutageWaitsBeforeRetryingServer() {
        storage.json = validCache(NOW).toJson();
        http.error = new IOException("sem internet");
        validate();
        assertEquals(1, http.calls);

        advance(MINUTE);
        assertTrue(validate().isSuccess());
        assertEquals(1, http.calls);

        advance(LicenceRules.RETRY_AFTER_FAILURE_MS);
        http.error = null;
        http.response = new LicenceHttpResponse(428, "{\"message\":\"Licença vencida\"}");
        assertFalse(validate().isSuccess());
        assertEquals(2, http.calls);
        assertNull(storage.json);
    }

    @Test
    public void outageWithoutCacheBlocksAndKeepsTrying() {
        http.error = new IOException("sem internet");
        assertFalse(validate().isSuccess());
        assertFalse(validate().isSuccess());
        assertEquals(2, http.calls);
    }

    @Test
    public void onlineSuccessSavesCacheWithResolvedUrlAndNow() {
        http.response = new LicenceHttpResponse(200, validJson());
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertEquals(1, http.calls);
        assertEquals(CORE_URL, http.lastUrl);
        assertEquals(KEY, http.lastKey);
        assertEquals(1, storage.saves);
        CheckLicenceEntity saved = storage.load();
        assertEquals(NOW, saved.getSavedLocalDatabaseAt());
        assertEquals(CORE_URL, saved.getLicenceInternalKey());
        assertTrue(tech.hasValidCachedLicence(KEY, CORE_BASE64));
    }

    @Test
    public void maliciousInternalKeyStillCallsCore() {
        http.response = new LicenceHttpResponse(200, validJson());
        tech.validateLicence(KEY, "https://evil.com/pos-payments/licence/check");
        assertEquals(CORE_URL, http.lastUrl);
        tech = new CheckLicenceTech(new FakeStorage(), http, LicenceClock.SYSTEM);
        tech.validateLicence(KEY, OLD_CLOUD_RUN_BASE64);
        assertEquals(CORE_URL, http.lastUrl);
    }

    @Test
    public void http200ExpiredIsErrorAndNotSaved() {
        http.response = new LicenceHttpResponse(200, coreJson("2026-01-01T00:00:00", false, false, false));
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertFalse(r.isSuccess());
        assertEquals(0, storage.saves);
        assertNull(storage.json);
    }

    @Test
    public void http200CanceledIsErrorAndNotSaved() {
        http.response = new LicenceHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true));
        assertFalse(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
        assertEquals(0, storage.saves);
    }

    @Test
    public void http428IsErrorWithMessage() {
        http.response = new LicenceHttpResponse(428, "{\"message\":\"Licença vencida\"}");
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertFalse(r.isSuccess());
        assertEquals("Licença vencida", r.getErrorMessage());
    }

    @Test
    public void networkErrorsAreErrors() {
        http.error = new UnknownHostException("portal-api.jylabtech.com.br");
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertFalse(r.isSuccess());
        assertNotNull(r.getErrorMessage());

        http.error = new SocketTimeoutException();
        r = tech.validateLicence(KEY, CORE_BASE64);
        assertFalse(r.isSuccess());
        assertNotNull(r.getErrorMessage());
    }

    @Test
    public void nullResponseIsError() {
        http.response = null;
        assertFalse(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
    }

    @Test
    public void staleCacheIsDeletedAndRevalidatedOnline() {
        storage.json = validCache(new Date(NOW.getTime() - 4 * DAY)).toJson();
        http.response = new LicenceHttpResponse(200, validJson());
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertEquals(1, storage.deletes);
        assertEquals(1, http.calls);
        assertEquals(NOW, storage.load().getSavedLocalDatabaseAt());
    }

    @Test
    public void staleCacheIsNotUsedWhenOffline() {
        storage.json = validCache(new Date(NOW.getTime() - 4 * DAY)).toJson();
        http.error = new UnknownHostException("offline");
        assertFalse(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
        assertNull(storage.json);
    }

    @Test
    public void cacheOfOtherKeyIsDeleted() {
        storage.json = validCache(NOW).toJson();
        assertFalse(tech.hasValidCachedLicence("other-key", CORE_BASE64));
        assertNull(storage.json);
    }

    @Test
    public void saveReturningFalseStillAllowsWhenServerConfirmed() {
        storage.failSave = true;
        http.response = new LicenceHttpResponse(200, validJson());
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertNull(storage.json);
        // sem cache: a próxima operação revalida online
        assertFalse(tech.hasValidCachedLicence(KEY, CORE_BASE64));
        assertTrue(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
        assertEquals(2, http.calls);
    }

    @Test
    public void saveThrowingStillAllowsWhenServerConfirmed() {
        storage.throwOnSave = true;
        http.response = new LicenceHttpResponse(200, validJson());
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertNotNull(r.getLicenceEntity());
        assertNull(storage.json);
    }

    @Test
    public void saveFailureDoesNotTurnInvalidLicenceIntoSuccess() {
        storage.throwOnSave = true;
        http.response = new LicenceHttpResponse(200, coreJson("2026-01-01T00:00:00", false, false, false));
        assertFalse(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
        http.response = new LicenceHttpResponse(428, "{\"message\":\"Licença vencida\"}");
        assertFalse(tech.validateLicence(KEY, CORE_BASE64).isSuccess());
    }

    @Test
    public void emptyKeyIsErrorWithoutNetwork() {
        assertFalse(tech.validateLicence("", CORE_BASE64).isSuccess());
        assertFalse(tech.validateLicence(null, CORE_BASE64).isSuccess());
        assertEquals(0, http.calls);
    }
}
