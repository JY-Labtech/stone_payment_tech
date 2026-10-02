package br.com.jylabtech.stone_payment_tech.core;

/**
 * Garante que a chamada do MethodChannel seja respondida exatamente uma vez:
 * respostas repetidas são ignoradas (responder duas vezes derruba o app).
 */
public class ReplyOnce implements MethodReply {

    private final MethodReply delegate;
    private boolean replied = false;

    public ReplyOnce(MethodReply delegate) {
        this.delegate = delegate;
    }

    public synchronized boolean hasReplied() {
        return replied;
    }

    private synchronized boolean markReplied(String kind) {
        if (replied) {
            System.err.println("ReplyOnce: resposta duplicada ignorada (" + kind + ")");
            return false;
        }
        replied = true;
        return true;
    }

    @Override
    public void success(Object value) {
        if (markReplied("success")) {
            delegate.success(value);
        }
    }

    @Override
    public void error(String code, String message, Object details) {
        if (markReplied("error " + code)) {
            delegate.error(code, message, details);
        }
    }

    @Override
    public void notImplemented() {
        if (markReplied("notImplemented")) {
            delegate.notImplemented();
        }
    }
}
