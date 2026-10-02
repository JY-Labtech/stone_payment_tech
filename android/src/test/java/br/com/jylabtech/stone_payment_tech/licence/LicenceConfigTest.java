package br.com.jylabtech.stone_payment_tech.licence;

import static br.com.jylabtech.stone_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class LicenceConfigTest {

    private static String b64(String s) {
        // codificador simples só para os testes
        return java.util.Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void constantPointsToCore() {
        assertEquals("https://portal-api.jylabtech.com.br/pos-payments/licence/check", LicenceConfig.LICENCE_CHECK_URL);
    }

    @Test
    public void acceptsCorePlainUrl() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(CORE_URL));
    }

    @Test
    public void acceptsCoreBase64() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(CORE_BASE64));
        assertTrue(LicenceConfig.isTrustedLicenceInternalKey(CORE_BASE64));
    }

    @Test
    public void acceptsInvoiceAndSubscriptionVariants() {
        String invoice = CORE_URL + "/invoice";
        String subscription = CORE_URL + "/subscription";
        assertEquals(invoice, LicenceConfig.resolveLicenceUrl(invoice));
        assertEquals(subscription, LicenceConfig.resolveLicenceUrl(subscription));
        assertEquals(invoice, LicenceConfig.resolveLicenceUrl(b64(invoice)));
        assertEquals(subscription, LicenceConfig.resolveLicenceUrl(b64(subscription)));
    }

    @Test
    public void oldCloudRunUrlFallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(OLD_CLOUD_RUN_URL));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(OLD_CLOUD_RUN_BASE64));
        assertFalse(LicenceConfig.isTrustedLicenceInternalKey(OLD_CLOUD_RUN_BASE64));
    }

    @Test
    public void otherHostFallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(b64("https://evil.com/pos-payments/licence/check")));
    }

    @Test
    public void maliciousLookalikeHostsFallBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br.evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://evil.com/portal-api.jylabtech.com.br/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("evil.com/portal-api.jylabtech.com.br"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://xportal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void userInfoFallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br@evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://user:pass@portal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void httpWithoutTlsFallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("http://portal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void wrongPathOrPortFallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br/outro/endpoint"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br/pos-payments/licence/checkout"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br:8443/pos-payments/licence/check"));
    }

    @Test
    public void emptyAndNullFallBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(null));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl(""));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("   "));
        assertFalse(LicenceConfig.isTrustedLicenceInternalKey(null));
    }

    @Test
    public void garbageBase64FallsBackToCore() {
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("!!!não-é-base64###"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("abcd"));
        assertEquals(CORE_URL, LicenceConfig.resolveLicenceUrl("a"));
    }
}
