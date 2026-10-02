package br.com.jylabtech.stone_payment_tech.licence;

import static br.com.jylabtech.stone_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

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

    private FakeStorage storage;
    private FakeHttp http;
    private CheckLicenceTech tech;

    @Before
    public void setUp() {
        storage = new FakeStorage();
        http = new FakeHttp();
        tech = new CheckLicenceTech(storage, http, new LicenceClock() {
            @Override public Date now() { return NOW; }
        });
    }

    @Test
    public void validCacheSkipsNetwork() {
        storage.json = validCache(new Date(NOW.getTime() - DAY)).toJson();
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertEquals(0, http.calls);
        assertTrue(tech.hasValidCachedLicence(KEY, CORE_BASE64));
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
        http.error = new UnknownHostException("licence-server.invalid");
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
        storage.json = validCache(new Date(NOW.getTime() - 8 * DAY)).toJson();
        http.response = new LicenceHttpResponse(200, validJson());
        LicenceResult r = tech.validateLicence(KEY, CORE_BASE64);
        assertTrue(r.isSuccess());
        assertEquals(1, storage.deletes);
        assertEquals(1, http.calls);
        assertEquals(NOW, storage.load().getSavedLocalDatabaseAt());
    }

    @Test
    public void staleCacheIsNotUsedWhenOffline() {
        storage.json = validCache(new Date(NOW.getTime() - 8 * DAY)).toJson();
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
