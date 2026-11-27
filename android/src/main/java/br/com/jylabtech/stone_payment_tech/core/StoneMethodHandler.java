package br.com.jylabtech.stone_payment_tech.core;

/**
 * Trata uma chamada do MethodChannel sem depender do Flutter/Android
 * (testável na JVM):
 * <ol>
 *   <li>método desconhecido → {@code notImplemented};</li>
 *   <li>método conhecido → {@link MethodDispatcher}, que chama o SDK Stone
 *       na thread atual (a thread principal, de onde o canal chama).</li>
 * </ol>
 * Toda chamada é respondida exatamente uma vez ({@link ReplyOnce}).
 */
public final class StoneMethodHandler {

    private final StoneActions actions;

    public StoneMethodHandler(StoneActions actions) {
        this.actions = actions;
    }

    public void handle(String method, final MethodArgs args, MethodReply channelReply) {
        final ReplyOnce reply = channelReply instanceof ReplyOnce ? (ReplyOnce) channelReply : new ReplyOnce(channelReply);
        try {
            final PaymentMethod paymentMethod = PaymentMethod.fromString(method);
            if (paymentMethod == null) {
                reply.notImplemented();
                return;
            }
            MethodDispatcher.dispatch(paymentMethod, args, actions, reply);
        } catch (Exception e) {
            reply.error(MethodDispatcher.ERROR_CODE, e.getMessage(), null);
        }
    }
}
