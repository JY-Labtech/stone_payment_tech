package br.com.jylabtech.stone_payment_tech.core;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodDispatcherTest {

    static class FakeReply implements MethodReply {
        final List<String> calls = new ArrayList<>();
        Object value;
        String errorCode;
        String errorMessage;

        @Override public void success(Object value) { calls.add("success"); this.value = value; }
        @Override public void error(String code, String message, Object details) { calls.add("error"); errorCode = code; errorMessage = message; }
        @Override public void notImplemented() { calls.add("notImplemented"); }
    }

    static class MapArgs implements MethodArgs {
        final Map<String, Object> map = new HashMap<>();

        MapArgs put(String key, Object value) { map.put(key, value); return this; }

        @SuppressWarnings("unchecked")
        @Override public <T> T argument(String key) { return (T) map.get(key); }
    }

    static class FakeActions implements StoneActions {
        final List<String> calls = new ArrayList<>();
        /** Argumentos recebidos pela última ação, na ordem da assinatura. */
        List<Object> lastArgs = new ArrayList<>();
        RuntimeException failWith;
        int lastType;
        String lastTransactionKey;

        private void hit(String name, Object... args) {
            calls.add(name);
            lastArgs = java.util.Arrays.asList(args);
            if (failWith != null) throw failWith;
        }

        @Override public void activeDebugLog(boolean isDebugLog) { hit("activeDebugLog", isDebugLog); }
        @Override public String getPosSerialNumber() { hit("serial"); return "SN123"; }
        @Override public String getPosManufacture() { hit("manufacture"); return "SUNMI"; }
        @Override public void customPrinter(String printerParams) { hit("customPrinter", printerParams); }
        @Override public void printFromBase64(String printerParams) { hit("printFromBase64", printerParams); }
        @Override public void printWrapPaper(int lines) { hit("printWrapPaper", lines); }
        @Override public void getAllTransactions() { hit("getAllTransactions"); }
        @Override public void reversal() { hit("reversal"); }
        @Override public void getTransactionByInitiatorTransactionKey(String key) { hit("getByInitiator", key); }
        @Override public void printerCurrentTransaction(boolean printCustomerSlip) { hit("printerCurrent", printCustomerSlip); }
        @Override public void printerFromTransactionKey(String transactionKey) { hit("printerKey", transactionKey); lastTransactionKey = transactionKey; }
        @Override public void activate(String appName, String stoneCode) { hit("activate", appName, stoneCode); }
        @Override public void activateWithCredentials(String a, String s, String q, String p) { hit("activateCredentials", a, s, q, p); }
        @Override public void abortPix() { hit("abortPix"); }
        @Override public void abort() { hit("abort"); }
        @Override public void abortActivation() { hit("abortActivation"); }
        @Override public void setPaymentOption(String option) { hit("option", option); }
        @Override public void cancelTransaction(int idFromBase) { hit("cancel", idFromBase); }
        @Override public void doTransaction(String amount, int type, String key, int installment, boolean withInterest,
                                            String qrAuth, String qrProvider, boolean customerSlip, boolean establishmentSlip) {
            hit("doTransaction", amount, type, key, installment, withInterest, qrAuth, qrProvider, customerSlip, establishmentSlip);
            lastType = type;
        }
    }

    private FakeActions actions;

    @Before
    public void setUp() {
        actions = new FakeActions();
    }

    private MapArgs fullArgs() {
        return fullArgsStatic();
    }

    static MapArgs fullArgsStatic() {
        return new MapArgs()
                .put("isDebugLog", true)
                .put("printerParams", "{}")
                .put("lines", 2)
                .put("InitiatorTransactionKey", "init")
                .put("printCustomerSlip", true)
                .put("transactionKey", "tk")
                .put("appName", "app")
                .put("stoneCode", "123")
                .put("qrCodeAuthorization", "a")
                .put("qrCodeProviderid", "b")
                .put("option", "opt")
                .put("idFromBase", 1)
                .put("amount", "100")
                .put("installment", 1)
                .put("withInterest", false)
                .put("printEstablishmentSlip", false)
                .put("initiatorTransactionKey", "");
    }

    private FakeReply dispatch(PaymentMethod method, MethodArgs args) {
        FakeReply reply = new FakeReply();
        MethodDispatcher.dispatch(method, args, actions, reply);
        return reply;
    }

    @Test
    public void everyMethodRepliesExactlyOnceWithSuccess() {
        for (PaymentMethod method : PaymentMethod.values()) {
            FakeReply reply = dispatch(method, fullArgs());
            assertEquals("resposta de " + method.getMethod(), 1, reply.calls.size());
            assertEquals("resposta de " + method.getMethod(), "success", reply.calls.get(0));
            assertNotNull("valor de " + method.getMethod(), reply.value);
        }
    }

    @Test
    public void operationsReplyTrueSoDartFutureBoolCompletes() {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method == PaymentMethod.PAYMENT_GET_SERIAL_NUMBER || method == PaymentMethod.PAYMENT_GET_MANUFACTURE) {
                continue;
            }
            assertEquals(method.getMethod(), Boolean.TRUE, dispatch(method, fullArgs()).value);
        }
    }

    @Test
    public void gettersReplyTheirValue() {
        assertEquals("SN123", dispatch(PaymentMethod.PAYMENT_GET_SERIAL_NUMBER, new MapArgs()).value);
        assertEquals("SUNMI", dispatch(PaymentMethod.PAYMENT_GET_MANUFACTURE, new MapArgs()).value);
    }

    @Test
    public void reversalActivateAndPaymentsCallTheSdk() {
        dispatch(PaymentMethod.PAYMENT_REVERSAL, new MapArgs());
        dispatch(PaymentMethod.ACTIVE_PINPAD, fullArgs());
        dispatch(PaymentMethod.ACTIVE_PINPAD_CREDENTIALS, fullArgs());
        dispatch(PaymentMethod.PAYMENT_ABORT, new MapArgs());
        assertEquals(List.of("reversal", "activate", "activateCredentials", "abort"), actions.calls);

        dispatch(PaymentMethod.PAYMENT_CREDIT, fullArgs());
        assertEquals(MethodDispatcher.TYPE_CREDIT, actions.lastType);
        dispatch(PaymentMethod.PAYMENT_DEBIT, fullArgs());
        assertEquals(MethodDispatcher.TYPE_DEBIT, actions.lastType);
        dispatch(PaymentMethod.PAYMENT_PIX, fullArgs());
        assertEquals(MethodDispatcher.TYPE_PIX, actions.lastType);
        dispatch(PaymentMethod.PAYMENT_VOUCHER, fullArgs());
        assertEquals(MethodDispatcher.TYPE_VOUCHER, actions.lastType);
    }

    @Test
    public void sdkExceptionRepliesErrorOnce() {
        actions.failWith = new IllegalStateException("SDK não ativado");
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method == PaymentMethod.CHECK_LICENCE) {
                continue;
            }
            FakeReply reply = dispatch(method, fullArgs());
            assertEquals(method.getMethod(), List.of("error"), reply.calls);
            assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        }
    }

    @Test
    public void missingRequiredArgumentRepliesErrorOnce() {
        FakeReply reply = dispatch(PaymentMethod.PAYMENT_PRINT_WRAP_PAPER, new MapArgs());
        assertEquals(List.of("error"), reply.calls);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void unknownMethodIsNotImplemented() {
        assertEquals(List.of("notImplemented"), dispatch(null, new MapArgs()).calls);
    }

    @Test
    public void printerTransactionKeyReadsKeySentByDart() {
        dispatch(PaymentMethod.PAYMENT_PRINTER_TRANSACTION_KEY, new MapArgs().put("transactionKey", "tk"));
        assertEquals("tk", actions.lastTransactionKey);
        dispatch(PaymentMethod.PAYMENT_PRINTER_TRANSACTION_KEY, new MapArgs().put("paymentPrinterTransactionKey", "old"));
        assertEquals("old", actions.lastTransactionKey);
    }

    @Test
    public void replyOnceIgnoresSecondReply() {
        FakeReply inner = new FakeReply();
        ReplyOnce once = new ReplyOnce(inner);
        once.success(true);
        once.error("ERROR", "late", null);
        once.success(false);
        once.notImplemented();
        assertEquals(List.of("success"), inner.calls);
        assertEquals(Boolean.TRUE, inner.value);
        assertTrue(once.hasReplied());
    }

    // ---- Argumentos exatos de cada método do canal -> ação do SDK ----

    private void assertAction(PaymentMethod method, MapArgs args, String action, Object... expectedArgs) {
        actions.calls.clear();
        FakeReply reply = dispatch(method, args);
        assertEquals(method.getMethod(), List.of("success"), reply.calls);
        assertEquals(method.getMethod(), List.of(action), actions.calls);
        assertEquals(method.getMethod(), java.util.Arrays.asList(expectedArgs), actions.lastArgs);
    }

    @Test
    public void simpleOperationsForwardTheirArguments() {
        assertAction(PaymentMethod.PAYMENT_ACTIVE_DEBUG_LOG, new MapArgs().put("isDebugLog", false), "activeDebugLog", false);
        assertAction(PaymentMethod.PAYMENT_CUSTOM_PRINTER, new MapArgs().put("printerParams", "{\"printers\":[]}"), "customPrinter", "{\"printers\":[]}");
        assertAction(PaymentMethod.PAYMENT_PRINT_BASE64, new MapArgs().put("printerParams", "aGVsbG8="), "printFromBase64", "aGVsbG8=");
        assertAction(PaymentMethod.PAYMENT_PRINT_WRAP_PAPER, new MapArgs().put("lines", 3), "printWrapPaper", 3);
        assertAction(PaymentMethod.PAYMENT_GET_ALL_TRANSACTIONS, new MapArgs(), "getAllTransactions");
        assertAction(PaymentMethod.PAYMENT_REVERSAL, new MapArgs(), "reversal");
        assertAction(PaymentMethod.PAYMENT_GET_TRANSACTION_BY_INITIATOR_TRANSACTION_KEY,
                new MapArgs().put("InitiatorTransactionKey", "ITK-1"), "getByInitiator", "ITK-1");
        assertAction(PaymentMethod.PAYMENT_PRINTER_TRANSACTION, new MapArgs().put("printCustomerSlip", false), "printerCurrent", false);
        assertAction(PaymentMethod.PAYMENT_PRINTER_TRANSACTION_KEY, new MapArgs().put("transactionKey", "TK"), "printerKey", "TK");
        assertAction(PaymentMethod.ACTIVE_PINPAD, new MapArgs().put("appName", "Loja").put("stoneCode", "206192723"),
                "activate", "Loja", "206192723");
        assertAction(PaymentMethod.ACTIVE_PINPAD_CREDENTIALS,
                new MapArgs().put("appName", "Loja").put("stoneCode", "206192723").put("qrCodeAuthorization", "tok").put("qrCodeProviderid", "prov"),
                "activateCredentials", "Loja", "206192723", "tok", "prov");
        assertAction(PaymentMethod.PAYMENT_ABORT_PIX, new MapArgs(), "abortPix");
        assertAction(PaymentMethod.PAYMENT_ABORT, new MapArgs(), "abort");
        assertAction(PaymentMethod.PAYMENT_ABORT_ACTIVATION, new MapArgs(), "abortActivation");
        assertAction(PaymentMethod.PAYMENT_OPTIONS, new MapArgs().put("option", "Refeição"), "option", "Refeição");
        assertAction(PaymentMethod.PAYMENT_CANCEL_TRANSACTION, new MapArgs().put("idFromBase", 42), "cancel", 42);
    }

    private MapArgs paymentArgs() {
        return new MapArgs()
                .put("amount", "1050")
                .put("installment", 3)
                .put("withInterest", true)
                .put("printCustomerSlip", false)
                .put("printEstablishmentSlip", true)
                .put("initiatorTransactionKey", "ITK")
                .put("qrCodeAuthorization", "tok")
                .put("qrCodeProviderid", "prov");
    }

    @Test
    public void paymentsForwardAmountInCentsInstallmentsSlipsAndType() {
        assertAction(PaymentMethod.PAYMENT_CREDIT, paymentArgs(), "doTransaction",
                "1050", MethodDispatcher.TYPE_CREDIT, "ITK", 3, true, null, null, false, true);
        assertAction(PaymentMethod.PAYMENT_CREDIT_PARC, paymentArgs(), "doTransaction",
                "1050", MethodDispatcher.TYPE_CREDIT, "ITK", 3, true, null, null, false, true);
        assertAction(PaymentMethod.PAYMENT_DEBIT, paymentArgs(), "doTransaction",
                "1050", MethodDispatcher.TYPE_DEBIT, "ITK", 3, true, null, null, false, true);
        assertAction(PaymentMethod.PAYMENT_VOUCHER, paymentArgs(), "doTransaction",
                "1050", MethodDispatcher.TYPE_VOUCHER, "ITK", 3, true, null, null, false, true);
        // Só o PIX repassa as credenciais do QRCode.
        assertAction(PaymentMethod.PAYMENT_PIX, paymentArgs(), "doTransaction",
                "1050", MethodDispatcher.TYPE_PIX, "ITK", 3, true, "tok", "prov", false, true);
    }

    @Test
    public void paymentWithoutRequiredArgumentRepliesErrorAndDoesNotCallSdk() {
        FakeReply reply = dispatch(PaymentMethod.PAYMENT_CREDIT, new MapArgs().put("amount", "100"));
        assertEquals(List.of("error"), reply.calls);
        assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        assertTrue(actions.calls.isEmpty());

        reply = dispatch(PaymentMethod.PAYMENT_CANCEL_TRANSACTION, new MapArgs());
        assertEquals(List.of("error"), reply.calls);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void errorMessageFromSdkIsForwarded() {
        actions.failWith = new IllegalStateException("Usuário não encontrado");
        FakeReply reply = dispatch(PaymentMethod.PAYMENT_DEBIT, paymentArgs());
        assertEquals("Usuário não encontrado", reply.errorMessage);
    }

    @Test
    public void everyChannelNameMapsToItsEnum() {
        for (PaymentMethod method : PaymentMethod.values()) {
            assertSame(method, PaymentMethod.fromString(method.getMethod()));
        }
        assertNull(PaymentMethod.fromString("paymentRefund"));
        assertNull(PaymentMethod.fromString(null));
        assertEquals(24, PaymentMethod.values().length);
    }
}
