package br.com.jylabtech.stone_payment_tech.core;

import br.com.jylabtech.stone_payment_tech.BuildConfig;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.jylabtech.stone_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.stone_payment_tech.licence.LicenceResult;
import br.com.jylabtech.stone_payment_tech.payments.PaymentsPresenter;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;




public class StoneTech {
    final MethodChannel mChannel;
    PaymentsPresenter payment;
    final Context currentContext;
    private String TAG = "StoneTech";
    private boolean isDebugLogActive = false;

    public static final String LICENCE_ERROR_CODE = StoneMethodHandler.LICENCE_ERROR_CODE;
    private final ExecutorService licenceExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** Uma única instância (e um único EncryptedSharedPreferences) por engine. */
    private final CheckLicenceTech licenceTech;
    private final StoneMethodHandler handler;

    public StoneTech(Context context, MethodChannel channel) {
        this.currentContext = context;
        this.mChannel = channel;
        this.licenceTech = new CheckLicenceTech(context);
        this.handler = new StoneMethodHandler(
                new StoneMethodHandler.LicenceSource() {
                    @Override
                    public boolean hasValidCachedLicence(String licenceKey, String licenceInternalKey) {
                        return licenceTech.hasValidCachedLicence(licenceKey, licenceInternalKey);
                    }

                    @Override
                    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
                        return licenceTech.validateLicence(licenceKey, licenceInternalKey);
                    }
                },
                new StoneMethodHandler.LicenceListener() {
                    @Override
                    public void onLicenceChecking() {
                        presenter().notifyLicenceChecking();
                    }

                    @Override
                    public void onLicenceResult(LicenceResult result) {
                        if (result == null || !result.isSuccess()) {
                            Log.i(TAG, "Licença inválida: método bloqueado");
                        }
                        presenter().notifyLicenceResult(result);
                    }

                    @Override
                    public void onLicenceGranted(boolean granted) {
                        presenter().setLicenceGranted(granted);
                    }
                },
                actions,
                licenceExecutor,
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        mainHandler.post(command);
                    }
                },
                BuildConfig.LICENCE_KEY,
                BuildConfig.LICENCE_INTERNAL_KEY);
    }

    private PaymentsPresenter presenter() {
        if (this.payment == null) {
            this.payment = new PaymentsPresenter(this.mChannel, isDebugLogActive, this.currentContext, licenceTech);
        }
        return this.payment;
    }

    public void initPayment(final MethodCall call, final MethodChannel.Result channelResult) {
        Log.i(TAG, "Call Method: " + call.method);
        // Toda chamada é respondida exatamente uma vez (sucesso, erro ou notImplemented).
        handler.handle(call.method, new MethodArgs() {
            @Override
            public <T> T argument(String key) {
                return call.argument(key);
            }
        }, new FlutterReply(channelResult));
    }

    /** Operações do canal sobre o PaymentsPresenter atual. */
    private final StoneActions actions = new StoneActions() {
        @Override
        public void activeDebugLog(boolean isDebugLog) {
            // Antes recriava o PaymentsPresenter, perdendo o estado da transação
            // em andamento (abort deixava de funcionar) sem liberar o anterior.
            isDebugLogActive = isDebugLog;
            presenter().setDebugLog(isDebugLog);
        }

        @Override
        public String getPosSerialNumber() {
            return presenter().getPosSerialNumber();
        }

        @Override
        public String getPosManufacture() {
            return presenter().getPosManufacture();
        }

        @Override
        public void customPrinter(String printerParams) throws Exception {
            presenter().customPrinter(printerParams, currentContext);
        }

        @Override
        public void printFromBase64(String printerParams) throws Exception {
            presenter().printFromBase64(printerParams, currentContext);
        }

        @Override
        public void printWrapPaper(int lines) {
            presenter().printWrapPaper(lines, currentContext);
        }

        @Override
        public void getAllTransactions() {
            presenter().getAllTransactions(currentContext);
        }

        @Override
        public void reversal() {
            presenter().onReversal(currentContext);
        }

        @Override
        public void getTransactionByInitiatorTransactionKey(String initiatorTransactionKey) {
            presenter().getTransactionByInitiatorTransactionKey(currentContext, initiatorTransactionKey);
        }

        @Override
        public void printerCurrentTransaction(boolean printCustomerSlip) {
            presenter().printerCurrentTransaction(currentContext, printCustomerSlip);
        }

        @Override
        public void printerFromTransactionKey(String transactionKey) throws Exception {
            presenter().printerFromTransactionKey(currentContext, transactionKey);
        }

        @Override
        public void activate(String appName, String stoneCode) throws Exception {
            presenter().activate(appName, stoneCode, currentContext);
        }

        @Override
        public void activateWithCredentials(String appName, String stoneCode, String qrCodeAuthorization, String qrCodeProviderid) throws Exception {
            presenter().activateWithCredentials(appName, stoneCode, qrCodeAuthorization, qrCodeProviderid, currentContext);
        }

        @Override
        public void abortPix() {
            presenter().abortPIXtransaction(currentContext);
        }

        @Override
        public void abort() {
            presenter().abortCurrentPosTransaction();
        }

        @Override
        public void abortActivation() {
            presenter().abortActivation();
        }

        @Override
        public void setPaymentOption(String option) {
            presenter().setPaymentOption(option);
        }

        @Override
        public void cancelTransaction(int idFromBase) throws Exception {
            presenter().cancelTransaction(currentContext, idFromBase);
        }

        @Override
        public void doTransaction(String amount, int typeTransaction, String initiatorTransactionKey, int installment,
                                  boolean withInterest, String qrCodeAuthorization, String qrCodeProviderid,
                                  boolean printCustomerSlip, boolean printEstablishmentSlip) throws Exception {
            presenter().doTransaction(currentContext, amount, typeTransaction, initiatorTransactionKey, installment,
                    withInterest, qrCodeAuthorization, qrCodeProviderid, printCustomerSlip, printEstablishmentSlip);
        }
    };

    /** Adapta o MethodChannel.Result do Flutter para MethodReply. */
    private static final class FlutterReply implements MethodReply {
        private final MethodChannel.Result result;

        FlutterReply(MethodChannel.Result result) {
            this.result = result;
        }

        @Override
        public void success(Object value) {
            result.success(value);
        }

        @Override
        public void error(String code, String message, Object details) {
            result.error(code, message, details);
        }

        @Override
        public void notImplemented() {
            result.notImplemented();
        }
    }

    public void dispose() {
        licenceExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        if (this.payment != null) {
            this.payment.dispose();
            this.payment = null;
        }
    }
}
