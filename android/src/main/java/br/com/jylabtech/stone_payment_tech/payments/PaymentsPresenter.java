package br.com.jylabtech.stone_payment_tech.payments;

import android.content.Context;



import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import io.reactivex.disposables.Disposable;
import stone.utils.keys.StoneKeyType;

public class PaymentsPresenter {

    private PaymentsUseCase mUseCase;
    private PaymentsFragment mFragment;
    private Disposable mSubscribe;

    private Context context;

    public PaymentsPresenter(MethodChannel channel, boolean isDebugLog, Context context1) {
        this.context = context1;
        this.mUseCase = new PaymentsUseCase(channel, isDebugLog, context1);
        this.mFragment = new PaymentsFragment(channel);
    }

    public void setDebugLog(boolean isDebugLog) {
        mUseCase.setDebugLog(isDebugLog);
    }

    public void doTransaction(Context context,
                              String amount,
                              int typeTransaction,
                              String initiatorTransactionKey,
                              int parc,
                              boolean withInterest,
                              String qrCodeAuthorization,
                              String qrCodeProviderid,
                              boolean printCustomerSlip,
                              boolean printEstablishmentSlip) throws Exception {
        if(qrCodeProviderid == null && qrCodeAuthorization == null){
            mUseCase.initTransaction(context, amount, typeTransaction, initiatorTransactionKey, parc, withInterest, null, printCustomerSlip, printEstablishmentSlip);
            return;
        }
        mUseCase.initTransaction(context, amount, typeTransaction, initiatorTransactionKey, parc, withInterest, qrCodeKeys(qrCodeAuthorization, qrCodeProviderid), printCustomerSlip, printEstablishmentSlip);
    }

    /** Chaves do PIX/QRCode enviadas ao StoneStart.init (o token vai com o prefixo "Bearer "). */
    static Map<StoneKeyType, String> qrCodeKeys(String qrCodeAuthorization, String qrCodeProviderid) {
        Map<StoneKeyType, String> stoneKeys = new HashMap<>();
        stoneKeys.put(StoneKeyType.QRCODE_AUTHORIZATION, "Bearer " + qrCodeAuthorization);
        stoneKeys.put(StoneKeyType.QRCODE_PROVIDERID, qrCodeProviderid);
        return stoneKeys;
    }

    public void printerCurrentTransaction(Context context, boolean printCustomerSlip) {
        mUseCase.printerCurrentTransaction(context, printCustomerSlip);
    }

    public void printerFromTransactionKey(Context context, String transactionKey) throws Exception {
        mUseCase.printerFromTransactionKey(context, transactionKey);
    }

    public void setPaymentOption(String value) {
        mUseCase.setPaymentOption(value);
    }

    public void activate(String appName,
                         String stoneCode,
                         Context context) throws Exception {
        mFragment.onMessage("Ativando terminal");
        mUseCase.initializeAndActivatePinpad(appName, stoneCode, context);
    }

    public void activateWithCredentials(String appName,
                                        String stoneCode,
                                        String qrCodeAuthorization,
                                        String qrCodeProviderid,
                                        Context context) throws Exception {
        mUseCase.initializeAndActivatePinPadWithCredentials(appName, stoneCode, qrCodeKeys(qrCodeAuthorization, qrCodeProviderid), context);

    }

    public void abortActivation() {
        mUseCase.abortActivation();
    }
    public void cancelTransaction(Context context, int idFromBase) throws Exception {
        mUseCase.cancelTransaction(context, idFromBase);
    }

    public void onReversal(Context context) {
        mUseCase.onReversalTransaction(context);
    }

    public void abortCurrentPosTransaction() {
        mUseCase.abortCurrentPosTransaction();
    }

    public void abortPIXtransaction(Context context){
        mUseCase.abortPIXTransaction(context);
    }

    public void getTransactionByInitiatorTransactionKey(Context context, String InitiatorTransactionKey) {
        mUseCase.getTransactionByInitiatorTransactionKey(context, InitiatorTransactionKey);
    }

    public void getAllTransactions(Context context) {
        mUseCase.getAllTransactions(context);
    }

    public void customPrinter(String params, Context context) throws Exception {
        mUseCase.customPrinter(params, context);
    }

    public void printFromBase64(String params, Context context) throws Exception {
        mUseCase.printFromBase64(params, context);
    }

    public void printWrapPaper(int lines, Context context) {
        mUseCase.printWrapPaper(lines, context);
    }

    public String getPosSerialNumber() {
        return mUseCase.getSerialNumber();
    }

    public String getPosManufacture() {
        return mUseCase.getPosManufacture();
    }




    public void dispose() {
        if (mSubscribe != null) {
            mSubscribe.dispose();
        }
        if (mUseCase != null) {
            mUseCase.dispose();
        }
    }
}
