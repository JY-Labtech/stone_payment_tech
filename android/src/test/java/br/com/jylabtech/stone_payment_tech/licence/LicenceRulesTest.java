package br.com.jylabtech.stone_payment_tech.licence;

import static br.com.jylabtech.stone_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Date;

public class LicenceRulesTest {

    // ---- isLicenceUsable ----

    @Test
    public void usableWhenValid() {
        assertTrue(LicenceRules.isLicenceUsable(CheckLicenceEntity.fromJson(validJson()), NOW));
    }

    @Test
    public void notUsableWhenExpiredByDate() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreJson("2026-09-30T23:59:59", false, false, false));
        assertFalse(LicenceRules.isLicenceUsable(e, NOW));
    }

    @Test
    public void notUsableWhenFlagIsExpired() {
        assertFalse(LicenceRules.isLicenceUsable(CheckLicenceEntity.fromJson(coreJson("2027-01-01T00:00:00", true, false, false)), NOW));
    }

    @Test
    public void notUsableWhenInactive() {
        assertFalse(LicenceRules.isLicenceUsable(CheckLicenceEntity.fromJson(coreJson("2027-01-01T00:00:00", false, true, false)), NOW));
    }

    @Test
    public void notUsableWhenCanceled() {
        assertFalse(LicenceRules.isLicenceUsable(CheckLicenceEntity.fromJson(coreJson("2027-01-01T00:00:00", false, false, true)), NOW));
    }

    @Test
    public void notUsableWithoutExpiredAt() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreJson(null, false, false, false));
        assertNotNull(e);
        assertNull(e.getExpiredAt());
        assertFalse(LicenceRules.isLicenceUsable(e, NOW));
    }

    @Test
    public void notUsableWhenNull() {
        assertFalse(LicenceRules.isLicenceUsable(null, NOW));
    }

    @Test
    public void masterKey2099IsUsable() {
        assertTrue(LicenceRules.isLicenceUsable(CheckLicenceEntity.fromJson(coreJson("2099-12-31T23:59:59", false, false, false)), NOW));
    }

    // ---- parse da resposta do core ----

    @Test
    public void parsesRealCoreResponse() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(validJson());
        assertNotNull(e);
        assertEquals(KEY, e.getLicenceEncrypt());
        assertEquals("00000000-0000-0000-0000-000000000000", e.getLicenceKey());
        assertEquals(date("2027-01-01T23:59:59"), e.getExpiredAt());
        assertEquals(date("2026-01-01T10:00:00"), e.getCreatedAt());
        assertFalse(e.isExpired());
        assertFalse(e.isInactive());
        assertFalse(e.isCanceled());
        assertFalse(e.isTrial());
    }

    @Test
    public void roundTripKeepsCanceledAndDates() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreJson("2027-01-01T23:59:59", false, false, true));
        e.setSavedLocalDatabaseAt(NOW);
        CheckLicenceEntity back = CheckLicenceEntity.fromJson(e.toJson());
        assertTrue(back.isCanceled());
        assertEquals(NOW, back.getSavedLocalDatabaseAt());
        assertEquals(e.getExpiredAt(), back.getExpiredAt());
    }

    @Test
    public void invalidJsonReturnsNull() {
        assertNull(CheckLicenceEntity.fromJson("not json"));
        assertNull(CheckLicenceEntity.fromJson(""));
        assertNull(CheckLicenceEntity.fromJson(null));
    }

    // ---- evaluateHttpResponse ----

    @Test
    public void http200ValidIsSuccess() {
        LicenceResult r = LicenceRules.evaluateHttpResponse(200, validJson(), NOW);
        assertTrue(r.isSuccess());
        assertEquals(0, r.getResult());
        assertNotNull(r.getLicenceEntity());
    }

    @Test
    public void http200ExpiredIsError() {
        LicenceResult r = LicenceRules.evaluateHttpResponse(200, coreJson("2026-01-01T00:00:00", false, false, false), NOW);
        assertFalse(r.isSuccess());
        assertEquals(LicenceResult.RESULT_ERROR, r.getResult());
    }

    @Test
    public void http200FlagsAreError() {
        assertFalse(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", true, false, false), NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, true, false), NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true), NOW).isSuccess());
    }

    @Test
    public void http200InvalidJsonOrMissingExpiredAtIsError() {
        LicenceResult r = LicenceRules.evaluateHttpResponse(200, "<html>ok</html>", NOW);
        assertFalse(r.isSuccess());
        assertEquals(LicenceRules.MSG_INVALID_RESPONSE, r.getErrorMessage());
        assertFalse(LicenceRules.evaluateHttpResponse(200, coreJson(null, false, false, false), NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, "", NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, null, NOW).isSuccess());
    }

    @Test
    public void errorStatusesUseServerMessage() {
        int[] codes = {404, 412, 428, 403, 400, 429, 500};
        for (int code : codes) {
            LicenceResult r = LicenceRules.evaluateHttpResponse(code, "{\"message\":\"Mensagem " + code + "\"}", NOW);
            assertFalse("HTTP " + code, r.isSuccess());
            assertEquals(LicenceResult.RESULT_ERROR, r.getResult());
            assertEquals("Mensagem " + code, r.getErrorMessage());
        }
    }

    @Test
    public void errorWithoutJsonBodyShowsHttpCode() {
        assertEquals("Erro ao validar licença (HTTP 502)", LicenceRules.evaluateHttpResponse(502, null, NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.evaluateHttpResponse(404, "", NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 500)", LicenceRules.evaluateHttpResponse(500, "Internal Server Error", NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 428)", LicenceRules.evaluateHttpResponse(428, "{\"message\":null}", NOW).getErrorMessage());
    }

    @Test
    public void newResultStartsAsError() {
        assertEquals(LicenceResult.RESULT_ERROR, new LicenceResult().getResult());
        assertFalse(new LicenceResult().isSuccess());
    }

    // ---- cache ----

    @Test
    public void cacheValid() {
        assertTrue(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() - DAY)), KEY, CORE_BASE64, NOW));
        assertTrue(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() - 3 * DAY)), KEY, CORE_BASE64, NOW));
    }

    @Test
    public void cacheOlderThan72HoursIsInvalid() {
        assertFalse(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() - 3 * DAY - MINUTE)), KEY, CORE_BASE64, NOW));
    }

    @Test
    public void cacheOfFourOrSevenDaysIsNoLongerValid() {
        assertFalse(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() - 4 * DAY)), KEY, CORE_BASE64, NOW));
        assertFalse(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() - 7 * DAY)), KEY, CORE_BASE64, NOW));
    }

    @Test
    public void shouldRevalidateOnFirstCheckOfProcessAndAfterInterval() {
        CheckLicenceEntity cache = validCache(new Date(NOW.getTime() - 10 * MINUTE));
        assertTrue(LicenceRules.shouldRevalidate(cache, false, NOW));
        assertFalse(LicenceRules.shouldRevalidate(cache, true, NOW));
        assertTrue(LicenceRules.shouldRevalidate(null, true, NOW));
        cache.setSavedLocalDatabaseAt(new Date(NOW.getTime() - LicenceRules.REVALIDATE_INTERVAL_MS));
        assertTrue(LicenceRules.shouldRevalidate(cache, true, NOW));
        cache.setSavedLocalDatabaseAt(null);
        assertTrue(LicenceRules.shouldRevalidate(cache, true, NOW));
    }

    @Test
    public void definitiveRejectionCodes() {
        for (int code : new int[]{400, 401, 402, 403, 404, 409, 410, 412, 423, 428}) {
            assertTrue("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
        for (int code : new int[]{200, 301, 408, 429, 500, 502, 503, 504}) {
            assertFalse("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
    }

    @Test
    public void cacheSavedInTheFutureIsInvalid() {
        assertFalse(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() + 10 * MINUTE)), KEY, CORE_BASE64, NOW));
        assertTrue(LicenceRules.isCacheValid(validCache(new Date(NOW.getTime() + 2 * MINUTE)), KEY, CORE_BASE64, NOW));
    }

    @Test
    public void cacheWithoutSavedAtIsInvalid() {
        assertFalse(LicenceRules.isCacheValid(validCache(null), KEY, CORE_BASE64, NOW));
    }

    @Test
    public void cacheWithOtherKeyIsInvalid() {
        assertFalse(LicenceRules.isCacheValid(validCache(NOW), "other-key", CORE_BASE64, NOW));
        assertFalse(LicenceRules.isCacheValid(validCache(NOW), null, CORE_BASE64, NOW));
    }

    @Test
    public void cacheWithOtherUrlIsInvalid() {
        CheckLicenceEntity cache = validCache(NOW);
        assertFalse(LicenceRules.isCacheValid(cache, KEY, CORE_URL + "/invoice", NOW));
        cache.setLicenceInternalKey(OLD_CLOUD_RUN_BASE64);
        assertFalse(LicenceRules.isCacheValid(cache, KEY, OLD_CLOUD_RUN_BASE64, NOW));
        cache.setLicenceInternalKey("https://evil.com/pos-payments/licence/check");
        assertFalse(LicenceRules.isCacheValid(cache, KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheSavedWithCoreBase64StillMatches() {
        CheckLicenceEntity cache = validCache(NOW);
        cache.setLicenceInternalKey(CORE_BASE64);
        assertTrue(LicenceRules.isCacheValid(cache, KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheWithExpiredLicenceIsInvalid() {
        CheckLicenceEntity cache = CheckLicenceEntity.fromJson(coreJson("2026-09-30T00:00:00", false, false, false));
        cache.setSavedLocalDatabaseAt(NOW);
        cache.setLicenceInternalKey(CORE_URL);
        assertFalse(LicenceRules.isCacheValid(cache, KEY, CORE_URL, NOW));
    }
}
