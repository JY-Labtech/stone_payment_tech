package br.com.jylabtech.stone_payment_tech.core;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Despacho de cada método do canal direto para o SDK (sem gate). */
public class StoneMethodHandlerTest {

    private MethodDispatcherTest.FakeActions actions;
    private StoneMethodHandler handler;

    @Before
    public void setUp() {
        actions = new MethodDispatcherTest.FakeActions();
        handler = new StoneMethodHandler(actions);
    }

    private MethodDispatcherTest.FakeReply handle(String method) {
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle(method, MethodDispatcherTest.fullArgsStatic(), reply);
        return reply;
    }

    @Test
    public void unknownMethodIsNotImplementedAndNeverCallsSdk() {
        MethodDispatcherTest.FakeReply reply = handle("paymentRefund");
        assertEquals(List.of("notImplemented"), reply.calls);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void removedCheckLicenceMethodIsNotImplemented() {
        MethodDispatcherTest.FakeReply reply = handle("checkLicence");
        assertEquals(List.of("notImplemented"), reply.calls);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void knownMethodCallsSdkSynchronously() {
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle("paymentReversal", new MethodDispatcherTest.MapArgs(), reply);
        // Resposta imediata, na mesma thread (sem executores).
        assertEquals(List.of("success"), reply.calls);
        assertEquals(Boolean.TRUE, reply.value);
        assertEquals(List.of("reversal"), actions.calls);
    }

    @Test
    public void everyKnownMethodRepliesExactlyOnceAndCallsSdk() {
        for (PaymentMethod method : PaymentMethod.values()) {
            int before = actions.calls.size();
            MethodDispatcherTest.FakeReply reply = handle(method.getMethod());
            assertEquals(method.getMethod(), List.of("success"), reply.calls);
            assertTrue(method.getMethod() + " chama o SDK", actions.calls.size() > before);
        }
    }

    @Test
    public void paymentMethodsCallSdkWithTransactionType() {
        handle("paymentCredit");
        assertEquals(MethodDispatcher.TYPE_CREDIT, actions.lastType);
        handle("paymentCreditParc");
        assertEquals(MethodDispatcher.TYPE_CREDIT, actions.lastType);
        handle("paymentDebit");
        assertEquals(MethodDispatcher.TYPE_DEBIT, actions.lastType);
        handle("paymentPix");
        assertEquals(MethodDispatcher.TYPE_PIX, actions.lastType);
        handle("paymentVoucher");
        assertEquals(MethodDispatcher.TYPE_VOUCHER, actions.lastType);
    }

    @Test
    public void sdkFailureRepliesErrorOnce() {
        actions.failWith = new IllegalStateException("SDK não ativado");
        MethodDispatcherTest.FakeReply reply = handle("paymentCredit");
        assertEquals(List.of("error"), reply.calls);
        assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        assertEquals("SDK não ativado", reply.errorMessage);
    }

    @Test
    public void missingArgumentRepliesErrorOnce() {
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle("paymentPrintWrapPaper", new MethodDispatcherTest.MapArgs(), reply);
        assertEquals(List.of("error"), reply.calls);
        assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        assertTrue(actions.calls.isEmpty());
    }
}
