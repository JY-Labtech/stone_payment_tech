package br.com.jylabtech.stone_payment_tech.licence;

import static br.com.jylabtech.stone_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.*;

import org.junit.Test;

/** Recusa definitiva x falha temporária na resposta HTTP do servidor de licença. */
public class LicenceHttpResultTest {

    @Test
    public void refusalsAreDefinitiveAndOutagesAreNot() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            assertTrue("HTTP " + code, LicenceRules.evaluateHttpResponse(code, "{\"message\":\"x\"}", NOW).isRejectedByServer());
        }
        for (int code : new int[]{408, 429, 500, 502, 503}) {
            assertFalse("HTTP " + code, LicenceRules.evaluateHttpResponse(code, "{\"message\":\"x\"}", NOW).isRejectedByServer());
        }
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true), NOW).isRejectedByServer());
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson("2026-01-01T00:00:00", false, false, false), NOW).isRejectedByServer());
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, true, false), NOW).isRejectedByServer());
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson(null, false, false, false), NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, validJson(), NOW).isRejectedByServer());
        // corpo ilegível (proxy/captive portal) não é recusa da licença
        assertFalse(LicenceRules.evaluateHttpResponse(200, "<html>oops</html>", NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, "", NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, null, NOW).isRejectedByServer());
    }

    @Test
    public void customerBlockedReasonHasClearMessage() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(428,
                "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}", NOW);
        assertTrue(result.isRejectedByServer());
        assertEquals("Cliente bloqueado. Entre em contato com a JY Labtech.", result.getErrorMessage());
    }

    @Test
    public void newResultIsNotRejected() {
        assertFalse(new LicenceResult().isRejectedByServer());
        assertFalse(LicenceResult.error("x").isRejectedByServer());
        assertTrue(LicenceResult.rejected("x").isRejectedByServer());
    }
}
