package br.com.jylabtech.stone_payment_tech.core;

import java.util.concurrent.Executor;

import br.com.jylabtech.stone_payment_tech.licence.LicenceResult;

/**
 * Trata uma chamada do MethodChannel sem depender do Flutter/Android
 * (testável na JVM):
 * <ol>
 *   <li>método desconhecido → {@code notImplemented} (sem consultar licença);</li>
 *   <li>gate de licença: cache local válido (sem rede, na thread atual) ou
 *       validação online no executor de background;</li>
 *   <li>licença inválida → {@code error("LICENCE_ERROR")} e o SDK NÃO é chamado;</li>
 *   <li>licença válida → {@link MethodDispatcher} na thread principal.</li>
 * </ol>
 * Toda chamada é respondida exatamente uma vez ({@link ReplyOnce}).
 */
public final class StoneMethodHandler {

    public static final String LICENCE_ERROR_CODE = "LICENCE_ERROR";

    /** Fonte da licença (em produção, CheckLicenceTech). */
    public interface LicenceSource {
        boolean hasValidCachedLicence(String licenceKey, String licenceInternalKey);

        /** Bloqueante (rede): roda no executor de background. */
        LicenceResult validateLicence(String licenceKey, String licenceInternalKey);
    }

    /** Eventos de licença enviados ao Dart e liberação da 2ª barreira do caso de uso. */
    public interface LicenceListener {
        void onLicenceChecking();

        void onLicenceResult(LicenceResult result);

        /**
         * Chamado com {@code true} imediatamente antes do despacho de um método
         * liberado pelo gate e com {@code false} logo depois.
         */
        void onLicenceGranted(boolean granted);
    }

    private final LicenceSource licence;
    private final LicenceListener listener;
    private final StoneActions actions;
    private final Executor background;
    private final Executor main;
    private final String licenceKey;
    private final String licenceInternalKey;

    public StoneMethodHandler(LicenceSource licence,
                              LicenceListener listener,
                              StoneActions actions,
                              Executor background,
                              Executor main,
                              String licenceKey,
                              String licenceInternalKey) {
        this.licence = licence;
        this.listener = listener;
        this.actions = actions;
        this.background = background;
        this.main = main;
        this.licenceKey = licenceKey;
        this.licenceInternalKey = licenceInternalKey;
    }

    public void handle(String method, final MethodArgs args, MethodReply channelReply) {
        final ReplyOnce reply = channelReply instanceof ReplyOnce ? (ReplyOnce) channelReply : new ReplyOnce(channelReply);
        try {
            final PaymentMethod paymentMethod = PaymentMethod.fromString(method);
            if (paymentMethod == null) {
                reply.notImplemented();
                return;
            }

            listener.onLicenceChecking();
            if (licence.hasValidCachedLicence(licenceKey, licenceInternalKey)) {
                listener.onLicenceResult(LicenceResult.success(null));
                dispatchGranted(paymentMethod, args, reply);
                return;
            }

            background.execute(new Runnable() {
                @Override
                public void run() {
                    LicenceResult validated;
                    try {
                        validated = licence.validateLicence(licenceKey, licenceInternalKey);
                    } catch (Exception e) {
                        validated = LicenceResult.error(e.getMessage());
                    }
                    final LicenceResult licenceResult = validated;
                    main.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.onLicenceResult(licenceResult);
                                if (licenceResult == null || !licenceResult.isSuccess()) {
                                    reply.error(LICENCE_ERROR_CODE, licenceErrorMessage(licenceResult), null);
                                    return;
                                }
                                dispatchGranted(paymentMethod, args, reply);
                            } catch (Exception e) {
                                reply.error(MethodDispatcher.ERROR_CODE, e.getMessage(), null);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            reply.error(MethodDispatcher.ERROR_CODE, e.getMessage(), null);
        }
    }

    private void dispatchGranted(PaymentMethod paymentMethod, MethodArgs args, ReplyOnce reply) {
        listener.onLicenceGranted(true);
        try {
            MethodDispatcher.dispatch(paymentMethod, args, actions, reply);
        } finally {
            listener.onLicenceGranted(false);
        }
    }

    public static String licenceErrorMessage(LicenceResult resultLicence) {
        String errorMessage = resultLicence != null ? resultLicence.getErrorMessage() : null;
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Licença não ativada";
        }
        return errorMessage;
    }
}
