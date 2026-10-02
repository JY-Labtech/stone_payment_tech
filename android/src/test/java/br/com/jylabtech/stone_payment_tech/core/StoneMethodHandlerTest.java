package br.com.jylabtech.stone_payment_tech.core;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import br.com.jylabtech.stone_payment_tech.licence.LicenceResult;

/** Gate de licença + despacho de cada método do canal. */
public class StoneMethodHandlerTest {

    static class FakeLicence implements StoneMethodHandler.LicenceSource {
        boolean cached;
        LicenceResult online = LicenceResult.error("Licença vencida");
        RuntimeException onlineThrows;
        int cachedCalls;
        int onlineCalls;
        String lastKey;
        String lastInternalKey;

        @Override public boolean hasValidCachedLicence(String key, String internal) {
            cachedCalls++;
            lastKey = key;
            lastInternalKey = internal;
            return cached;
        }

        @Override public LicenceResult validateLicence(String key, String internal) {
            onlineCalls++;
            if (onlineThrows != null) throw onlineThrows;
            return online;
        }
    }

    static class FakeListener implements StoneMethodHandler.LicenceListener {
        final List<String> events = new ArrayList<>();
        boolean grantedDuringDispatch;
        boolean granted;

        @Override public void onLicenceChecking() { events.add("checking"); }
        @Override public void onLicenceResult(LicenceResult r) { events.add(r != null && r.isSuccess() ? "ok" : "fail"); }
        @Override public void onLicenceGranted(boolean g) { granted = g; events.add("granted=" + g); }
    }

    /** Executor que guarda as tarefas para rodar quando o teste quiser. */
    static class QueueExecutor implements Executor {
        final List<Runnable> tasks = new ArrayList<>();
        @Override public void execute(Runnable r) { tasks.add(r); }
        void runAll() {
            while (!tasks.isEmpty()) tasks.remove(0).run();
        }
    }

    private FakeLicence licence;
    private FakeListener listener;
    private MethodDispatcherTest.FakeActions actions;
    private QueueExecutor background;
    private QueueExecutor main;
    private StoneMethodHandler handler;

    @Before
    public void setUp() {
        licence = new FakeLicence();
        listener = new FakeListener();
        actions = new MethodDispatcherTest.FakeActions() {
            @Override public void reversal() {
                super.reversal();
                listener.grantedDuringDispatch = listener.granted;
            }
        };
        background = new QueueExecutor();
        main = new QueueExecutor();
        handler = new StoneMethodHandler(licence, listener, actions, background, main, "LIC-KEY", "internal");
    }

    private MethodDispatcherTest.FakeReply handle(String method) {
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle(method, MethodDispatcherTest.fullArgsStatic(), reply);
        background.runAll();
        main.runAll();
        return reply;
    }

    @Test
    public void unknownMethodIsNotImplementedWithoutCheckingLicence() {
        MethodDispatcherTest.FakeReply reply = handle("paymentRefund");
        assertEquals(List.of("notImplemented"), reply.calls);
        assertEquals(0, licence.cachedCalls);
        assertEquals(0, licence.onlineCalls);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void validCacheDispatchesSynchronouslyWithoutNetwork() {
        licence.cached = true;
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle("paymentReversal", new MethodDispatcherTest.MapArgs(), reply);
        // Sem passar pelos executores: resposta imediata.
        assertEquals(List.of("success"), reply.calls);
        assertEquals(Boolean.TRUE, reply.value);
        assertEquals(List.of("reversal"), actions.calls);
        assertEquals(0, licence.onlineCalls);
        assertTrue(background.tasks.isEmpty());
        assertEquals("LIC-KEY", licence.lastKey);
        assertEquals("internal", licence.lastInternalKey);
        assertEquals(List.of("checking", "ok", "granted=true", "granted=false"), listener.events);
    }

    @Test
    public void secondBarrierIsGrantedOnlyDuringDispatch() {
        licence.cached = true;
        handle("paymentReversal");
        assertTrue(listener.grantedDuringDispatch);
        assertFalse(listener.granted);
    }

    @Test
    public void onlineValidationRunsInBackgroundAndRepliesOnMain() {
        licence.online = LicenceResult.success(null);
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        handler.handle("paymentReversal", new MethodDispatcherTest.MapArgs(), reply);
        assertTrue("sem resposta antes da validação", reply.calls.isEmpty());
        assertEquals(1, background.tasks.size());
        background.runAll();
        assertEquals(1, licence.onlineCalls);
        assertTrue("responde na main thread", reply.calls.isEmpty());
        main.runAll();
        assertEquals(List.of("success"), reply.calls);
        assertEquals(List.of("reversal"), actions.calls);
    }

    @Test
    public void invalidLicenceRepliesLicenceErrorForEveryMethodAndNeverCallsSdk() {
        for (PaymentMethod method : PaymentMethod.values()) {
            MethodDispatcherTest.FakeReply reply = handle(method.getMethod());
            assertEquals(method.getMethod(), List.of("error"), reply.calls);
            assertEquals(method.getMethod(), StoneMethodHandler.LICENCE_ERROR_CODE, reply.errorCode);
            assertEquals("Licença vencida", reply.errorMessage);
        }
        assertTrue("SDK não pode ser chamado", actions.calls.isEmpty());
        assertFalse(listener.events.contains("granted=true"));
    }

    @Test
    public void nullOrThrowingValidationIsLicenceError() {
        licence.online = null;
        MethodDispatcherTest.FakeReply reply = handle("paymentCredit");
        assertEquals(StoneMethodHandler.LICENCE_ERROR_CODE, reply.errorCode);
        assertEquals("Licença não ativada", reply.errorMessage);

        licence.onlineThrows = new IllegalStateException("DNS falhou");
        reply = handle("paymentDebit");
        assertEquals(List.of("error"), reply.calls);
        assertEquals(StoneMethodHandler.LICENCE_ERROR_CODE, reply.errorCode);
        assertEquals("DNS falhou", reply.errorMessage);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void checkLicenceRepliesTrueWhenValid() {
        licence.cached = true;
        MethodDispatcherTest.FakeReply reply = handle("checkLicence");
        assertEquals(Boolean.TRUE, reply.value);
        assertTrue(actions.calls.isEmpty());
    }

    @Test
    public void everyKnownMethodRepliesExactlyOnceWithValidLicence() {
        licence.online = LicenceResult.success(null);
        for (PaymentMethod method : PaymentMethod.values()) {
            MethodDispatcherTest.FakeReply reply = handle(method.getMethod());
            assertEquals(method.getMethod(), List.of("success"), reply.calls);
        }
    }

    @Test
    public void sdkFailureAfterGateRepliesErrorOnce() {
        licence.cached = true;
        actions.failWith = new IllegalStateException("SDK não ativado");
        MethodDispatcherTest.FakeReply reply = handle("paymentCredit");
        assertEquals(List.of("error"), reply.calls);
        assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        assertFalse("2ª barreira fechada mesmo com erro", listener.granted);
    }

    @Test
    public void listenerFailureStillRepliesOnce() {
        StoneMethodHandler broken = new StoneMethodHandler(licence, new FakeListener() {
            @Override public void onLicenceChecking() { throw new IllegalStateException("canal fechado"); }
        }, actions, background, main, "k", "i");
        MethodDispatcherTest.FakeReply reply = new MethodDispatcherTest.FakeReply();
        broken.handle("paymentReversal", new MethodDispatcherTest.MapArgs(), reply);
        assertEquals(List.of("error"), reply.calls);
        assertEquals(MethodDispatcher.ERROR_CODE, reply.errorCode);
        assertTrue(actions.calls.isEmpty());
    }
}
