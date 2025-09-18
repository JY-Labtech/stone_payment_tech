package br.com.jylabtech.stone_payment_tech.payments;

import android.content.Context;


import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import br.com.jylabtech.stone_payment_tech.core.ActionResult;
import br.com.jylabtech.stone_payment_tech.core.BasicResult;
import br.com.jylabtech.stone_payment_tech.licence.CheckLicenceTech;
import io.flutter.plugin.common.MethodChannel;
import io.reactivex.disposables.Disposable;
import stone.utils.keys.StoneKeyType;
import android.util.Log;

public class PaymentsPresenter {

    private PaymentsUseCase mUseCase;
    private PaymentsFragment mFragment;
    private Disposable mSubscribe;
    private Boolean hasAborted = false;
    private int countPassword = 0;
    private boolean isDebugLog = false;
    private CheckLicenceTech licenceTech;
    Gson gson = null;

    private Context context;

    //@Inject
    public PaymentsPresenter(MethodChannel channel, boolean isDebugLog, Context context1) {
        this.context = context1;
        this.mUseCase = new PaymentsUseCase(channel, isDebugLog, context1);
        this.mFragment = new PaymentsFragment(channel);
        this.licenceTech = new CheckLicenceTech(context1);
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
        Map<StoneKeyType, String> stoneKeys = new HashMap<StoneKeyType, String>()
        {
            {
                put(StoneKeyType.QRCODE_AUTHORIZATION, "Bearer " + qrCodeAuthorization);
                put(StoneKeyType.QRCODE_PROVIDERID, qrCodeProviderid);
            }
        };
        mUseCase.initTransaction(context, amount, typeTransaction, initiatorTransactionKey, parc, withInterest, stoneKeys, printCustomerSlip, printEstablishmentSlip);
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
        Map<StoneKeyType, String> stoneKeys = new HashMap<StoneKeyType, String>()
        {
            {
                put(StoneKeyType.QRCODE_AUTHORIZATION, "Bearer " + qrCodeAuthorization);
                put(StoneKeyType.QRCODE_PROVIDERID, qrCodeProviderid);
            }
        };
        mUseCase.initializeAndActivatePinPadWithCredentials(appName, stoneCode, stoneKeys, context);

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
        System.out.println("InitiatorTransactionKey: " + InitiatorTransactionKey);
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

    public boolean checkValidLicence(String licenceKey, String licenceInternalKey) throws Exception {
        return this.mUseCase.checkValidLicence(licenceKey, licenceInternalKey);
    }



    public void dispose() {
        if (mSubscribe != null) {
            mSubscribe.dispose();
        }
    }
}
