package br.com.jylabtech.stone_payment_tech.payments;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import br.com.jylabtech.stone_payment_tech.core.ActionResult;
import br.com.jylabtech.stone_payment_tech.core.BasicResult;
import br.com.jylabtech.stone_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.stone_payment_tech.licence.LicenceResult;
import br.com.jylabtech.stone_payment_tech.printer.StonePrinter;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.stone.posandroid.providers.PosPrintReceiptProvider;
import br.com.stone.posandroid.providers.PosTransactionProvider;
import stone.application.StoneStart;
import stone.application.enums.Action;
import stone.application.enums.ReceiptType;
import stone.application.enums.TransactionStatusEnum;
import stone.application.interfaces.StoneActionCallback;
import stone.application.interfaces.StoneCallbackInterface;
import stone.application.xml.enums.ResponseCodeEnum;
import stone.database.transaction.TransactionDAO;
import stone.database.transaction.TransactionObject;
import stone.providers.ActiveApplicationProvider;
import stone.providers.CancellationProvider;
import stone.providers.ReversalProvider;
import stone.user.UserModel;
import stone.utils.Stone;
import io.flutter.plugin.common.MethodChannel;
import stone.utils.keys.StoneKeyType;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.content.ContextCompat;

public class PaymentsUseCase {

  private PaymentsFragment mFragment;
  private StoneHelper mStoneHelper;
  private StonePrinter mStonePrinter;
  private TransactionObject currentTransactionObject;
  private PosTransactionProvider posTransactionProvider;
  private ActiveApplicationProvider activeApplicationProvider;
  private List<UserModel> userModel;
  private List<String> optionList;
  private boolean isInitialized = false;
  private boolean isDebugLog = false;
  List<TransactionObject> transactionObjects;
  private boolean isAbortRunning = false;
  private boolean isActivationRunning = false;

  private Context context;
  private CheckLicenceTech licenceTech;
  Gson gson = null;
  
  private static final int ACTIVATION_TIMEOUT_MS = 60000; // 60 segundos
  private android.os.Handler timeoutHandler;
  private Runnable timeoutRunnable;



  public PaymentsUseCase(MethodChannel channel, boolean isDebugLog, Context context1) {
    this.mFragment = new PaymentsFragment(channel);
    this.mStonePrinter = new StonePrinter(channel);
    this.mStoneHelper = new StoneHelper();
    this.isDebugLog = isDebugLog;
    this.context = context1;
    this.licenceTech = new CheckLicenceTech(context1);
    this.timeoutHandler = new Handler(Looper.getMainLooper());
  }

  private boolean isValidLicence() throws Exception {
    boolean result = licenceTech.validateExistsLicence();
    if(result) {
       return true;
    }

    throw new Exception("Licença inválida ou não foi inicializada!");
  }

  private Gson getGson() {
    if (gson == null) {
      gson = new Gson();
    }
    return gson;
  }

  public void initTransaction(Context context,
                              String amount,
                              int typeTransaction,
                              String initiatorTransactionKey,
                              int parc,
                              boolean withInterest,
                              Map<StoneKeyType, String> stoneKeys,
                              boolean printCustomerSlip,
                              boolean printEstablishmentSlip
                              ) throws Exception {
    isValidLicence();
    isAbortRunning = false;
    if(stoneKeys == null) {
      checkUserModel(context);
      transaction(context, amount, typeTransaction, initiatorTransactionKey, parc, withInterest, printCustomerSlip, printEstablishmentSlip);
      return;
    }

    if(userModel == null) {
      userModel = StoneStart.init(context, stoneKeys);
    }
    
    transaction(context, amount, typeTransaction, initiatorTransactionKey, parc, withInterest, printCustomerSlip, printEstablishmentSlip);
  }

  private void checkUserModel(Context context) {

      isInitialized = StoneStart.INSTANCE.getSDKInitialized();

      if (isInitialized) {
        return;
      }

      if(userModel == null) {
        Map<StoneKeyType, String> stoneKeys = new HashMap<>();
        userModel = StoneStart.init(context, stoneKeys);
      }
  }

  public void handleTransactionError(Context context, String message, ActionResult actionResult, BasicResult basicResult) {

    mFragment.onMessage(message);
    basicResult.setResult(999999);
    actionResult.setResult(999999);
    if(isDebugLog) {
    Log.d("print", "***** handleTransactionError: " + message);
    }


    TransactionStatusEnum status = TransactionStatusEnum.UNKNOWN;

    if(posTransactionProvider != null) {
      status = posTransactionProvider.getTransactionStatus();
    }

    if (!isAbortRunning && posTransactionProvider != null) {
      actionResult.setTransactionStatus(status.toString());
      actionResult.setMessageFromAuthorize(posTransactionProvider.getMessageFromAuthorize());
      actionResult.setAuthorizationCode(posTransactionProvider.getAuthorizationCode());
      actionResult.setErrorMessage(mStoneHelper.getErrorFromErrorList(posTransactionProvider.getListOfErrors()));
      basicResult.setErrorMessage(mStoneHelper.getErrorFromErrorList(posTransactionProvider.getListOfErrors()));
      mFragment.onMessage(posTransactionProvider.getMessageFromAuthorize());    
    }

    if( posTransactionProvider != null) {
      checkStatusWithErrorTransaction(status, context);
    }

    String jsonError = convertActionToJson(actionResult);

    mFragment.onError(convertBasicResultToJson(basicResult));
    mFragment.onFinishedResponse(jsonError);
    mFragment.onMessage("Transação concluída");
  }

  public void handleTransactionSuccess(Context context, ActionResult actionResult, boolean printCustomerSlip, boolean printEstablishmentSlip, TransactionObject transaction) {
    TransactionDAO transactionDAO = new TransactionDAO(context);
    if(isDebugLog) {
    Log.d("print", "****** handleTransactionSuccess: " + transaction.getTransactionStatus());
    }
    TransactionObject transactionObject = transactionDAO.findTransactionWithInitiatorTransactionKey(transaction.getInitiatorTransactionKey());
    if(!userModel.isEmpty()){
      String userModelString = getGson().toJson(userModel.get(0));
      actionResult.setUserModel(userModelString);
    }

    TransactionStatusEnum transactionStatusPos = TransactionStatusEnum.UNKNOWN;
    TransactionStatusEnum currentTransactionStatus = transaction.getTransactionStatus();


      if (!isAbortRunning && posTransactionProvider != null) {
      transactionStatusPos = posTransactionProvider.getTransactionStatus();
      actionResult.setTransactionStatus(transactionStatusPos.toString());
      actionResult.setMessageFromAuthorize(posTransactionProvider.getMessageFromAuthorize());
      actionResult.setAuthorizationCode(posTransactionProvider.getAuthorizationCode());
      printerReceiptTransaction(context, transaction, transactionStatusPos, printCustomerSlip, printEstablishmentSlip);
      boolean isPaymentApproved = transactionStatusPos == TransactionStatusEnum.APPROVED || currentTransactionStatus == TransactionStatusEnum.APPROVED;
       actionResult.buildResponseStoneTransaction(transactionObject, isPaymentApproved);
    }

    String jsonStoneResult = convertActionToJson(actionResult);
    finishTransaction(jsonStoneResult);

    mFragment.onMessage("Transação concluída");
    mFragment.onTransactionSuccess();
  }

  public void handleTransactionStatusChanged(Action action, TransactionObject transaction, BasicResult basicResult) {
    String actionMessage = mStoneHelper.getMessageFromTransactionAction(action);
    if(isDebugLog) {
    Log.d("print", "**** handleTransactionStatusChanged: " + action.toString());
    }
    mFragment.onMessage(actionMessage);
    if (action == Action.TRANSACTION_WAITING_QRCODE_SCAN) {
      basicResult.setMethod("QRCode");
      basicResult.setMessage(mStoneHelper.convertBitmapToString(transaction.getQRCode()));
      mFragment.onChanged(convertBasicResultToJson(basicResult));
      mFragment.onAuthProgress(convertBasicResultToJson(basicResult));
    }

    if(action == Action.TRANSACTION_TYPE_SELECTION) {
      List<String> options = posTransactionProvider.getTransactionTypeOptions();
      optionList = options;
      basicResult.setMethod("PaymentOptions");
      basicResult.setOptions(options);
      basicResult.setMessage(actionMessage);
      mFragment.onChanged(convertBasicResultToJson(basicResult));
      mFragment.onAuthProgress(convertBasicResultToJson(basicResult));
    }
  }

  public void transaction(
          Context context,
          String amount,
          int typeTransaction,
          String InitiatorTransactionKey,
          int parc,
          boolean withInterest,
          boolean printCustomerSlip,
          boolean printEstablishmentSlip
  ) throws Exception {
    isValidLicence();

    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("transaction");
    ActionResult actionResult = new ActionResult();
    actionResult.setMethod("transaction");

    try {
      mFragment.onMessage("Iniciando transação");
      currentTransactionObject = null;
      posTransactionProvider = null;
      isAbortRunning = false;

      String serialNumber = getSerialNumber();
      actionResult.setDeviceSerialNumber(serialNumber);

      final TransactionObject transaction = mStoneHelper.getTransactionObject(amount, typeTransaction, parc, withInterest);
      currentTransactionObject = transaction;

      // Essa validação é para verificar se a transação já foi realizada (recomendação da própria Stone) para evitar duplicidade
      if(InitiatorTransactionKey != null && !InitiatorTransactionKey.isEmpty()){
        TransactionDAO transactionDAO = new TransactionDAO(context);

        TransactionObject transactionObject = transactionDAO.findTransactionWithInitiatorTransactionKey(InitiatorTransactionKey);
        if(transactionObject != null) {
          if(transactionObject.getTransactionStatus() == TransactionStatusEnum.APPROVED) {
            actionResult.setTransactionStatus(transactionObject.getTransactionStatus().toString());
            actionResult.setMessageFromAuthorize(transactionObject.getAuthorizationCode());
            actionResult.setAuthorizationCode(transactionObject.getAuthorizationCode());

            if(!userModel.isEmpty()){
              String userModelString = getGson().toJson(userModel.get(0));
              actionResult.setUserModel(userModelString);
            }

            boolean isPaymentApproved = transactionObject.getTransactionStatus() == TransactionStatusEnum.APPROVED;

            actionResult.buildResponseStoneTransaction(transactionObject, isPaymentApproved);
            String jsonStoneResult = convertActionToJson(actionResult);
            finishTransaction(jsonStoneResult);

            mFragment.onMessage("Transação concluída");
            mFragment.onTransactionSuccess();
          } else {
            handleTransactionError(context, "Transação já realizada mas não aprovada", actionResult, basicResult);
          }
          return;
        }

        transaction.setInitiatorTransactionKey(InitiatorTransactionKey);
      }


      //Essa variavel posTransactionProvider está armazenando o provider para que possamos cancelar a transação
      posTransactionProvider = new PosTransactionProvider(context, transaction, userModel.get(0));

      mFragment.onMessage("Comunicando com o servidor Stone. Aguarde.");

      posTransactionProvider.setConnectionCallback(new StoneActionCallback() {
        @Override
        public void onSuccess() {
          handleTransactionSuccess(context, actionResult, printCustomerSlip, printEstablishmentSlip, transaction);
        }
        @Override
        public void onStatusChanged(Action action) {
          handleTransactionStatusChanged(action, transaction, basicResult);
        }
        @Override
        public void onError() {
          handleTransactionError(context, "Erro ao realizar transação", actionResult, basicResult);
        }

      });
      posTransactionProvider.execute();
    } catch (Exception e) {
      Log.d("print", "****ERRO_transaction: " + e.getMessage());

      basicResult.setResult(999999);
      basicResult.setErrorMessage(e.getMessage());

      mFragment.onError(convertBasicResultToJson(basicResult));
      abortCurrentPosTransaction();
    }
  }

  public void setPaymentOption(String value) {
    if(isDebugLog) {
    Log.d("print", "****setPaymentOption: " + value);
    }
    if(posTransactionProvider == null || optionList.isEmpty()) return;
    Integer index = optionList.indexOf(value);
    posTransactionProvider.setTransactionTypeSelected(index);
  }

  private void changePrinterRequest() {
    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("printerTransaction");
    basicResult.setMessage("Deseja imprimir sua via?");
    mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
  }

  public void printerCurrentTransaction(Context context, boolean printCustomerSlip) {
    if(currentTransactionObject == null) {
      if(isDebugLog) {
      Log.d("print", "****ERROR_currentTransactionObject: invalid null value");
      }
      return;
    }

    try {
      mStonePrinter.printerFromTransaction(context, currentTransactionObject);
    } catch (Exception error) {
      Log.d("print", "****ERROR_printerCurrentTransaction: " + error.getMessage());
    }
  }

  public void printerFromTransactionKey(Context context, String transactionKey) throws Exception {
    isValidLicence();
    TransactionDAO transactionDAO = new TransactionDAO(context);
    TransactionObject transactionObject = transactionDAO.findTransactionWithInitiatorTransactionKey(transactionKey);
    if(transactionObject == null) return;
    try {
      mStonePrinter.printerFromTransaction(context, transactionObject);
    } catch (Exception error) {
      Log.d("print", "****ERROR_printerCurrentTransaction: " + error.getMessage());
    }
  }

  public void printerReceiptTransaction(
          Context context,
          TransactionObject transactionObject,
          TransactionStatusEnum status,
          boolean printCustomerSlip,
          boolean printEstablishmentSlip
  ) {
    try {
      boolean isValidTransaction = status == TransactionStatusEnum.APPROVED || transactionObject.getTransactionStatus() == TransactionStatusEnum.APPROVED;
      
      if(!isValidTransaction) return;

      if(printEstablishmentSlip) {
        printReceipt(context, transactionObject, ReceiptType.MERCHANT);
      }

      if (!printCustomerSlip) return;
      printReceipt(context, transactionObject, ReceiptType.CLIENT);

    } catch(Exception e) {
      Log.d("print", "****ERROR_printerReceiptTransaction: " + e.getMessage());
    }
  }

  public void printReceipt(Context context, TransactionObject transactionObject, ReceiptType type) {
    try {
      PosPrintReceiptProvider printer = new PosPrintReceiptProvider(context, transactionObject, type);
      printer.execute();
    } catch (Exception e) {
      Log.d("print", "****ERROR_printMerchantVia: " + e.getMessage());
    }
  }

  public void abortCurrentPosTransaction() {
    if(isDebugLog) {
    Log.d("print", "****** abortCurrentPosTransaction");
    }
    if(posTransactionProvider == null || isAbortRunning){
      return;
    }
    try {
      posTransactionProvider.abortPayment();
      isAbortRunning = true;
    } catch (Exception error){
      Log.d("print", "****ERROR_abortCurrentPosTransaction: " + error.getMessage());
      BasicResult basicResult = new BasicResult();
      basicResult.setResult(999999);
      basicResult.setErrorMessage(error.getMessage());
      mFragment.onError(convertBasicResultToJson(basicResult));
    }
  }

  public void abortActivation() {
    if (isActivationRunning) {
      if (isDebugLog) {
        Log.d("print", "****Abortando ativação do terminal (método público)...");
      }
      
      isActivationRunning = false;
      cancelActivationTimeout();
      
      BasicResult abortResult = new BasicResult();
      abortResult.setMethod("active");
      abortResult.setResult(999999);
      abortResult.setErrorMessage("Ativação cancelada pelo usuário");
      
      mFragment.onMessage("Ativação cancelada");
      mFragment.onError(convertBasicResultToJson(abortResult));
      mFragment.onFinishedResponse(convertBasicResultToJson(abortResult));
      
      activeApplicationProvider = null;
    }
  }

  public void abortPIXTransaction(Context context) {
    if(isDebugLog) {
    Log.d("print", "***** abortPIXTransaction");
    }
    try {
      cancelTransactionPIX(context);
    }catch (Exception err) {
      Log.d("print", "****ERRO_cancelTransactionPIX: " + err.getMessage());
    }

    try {
      posTransactionProvider.abortPayment();
      isAbortRunning = true;
    } catch (Exception error){
      Log.d("print", "****ERRO_abortCurrentPosTransaction: " + error.getMessage());
    }


    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("abortPix");
    mFragment.onMessage("Transação cancelada");
    basicResult.setResult(999999);
    basicResult.setMessage("Transação cancelada");
    basicResult.setErrorMessage("Transação cancelada");
    String jsonError = convertBasicResultToJson(basicResult);
    mFragment.onFinishedResponse(jsonError);
  }

  private String convertActionToJson(ActionResult actionResult) {
    return getGson().toJson(actionResult);
  }

  private String convertBasicResultToJson(BasicResult basicResult) {
    return getGson().toJson(basicResult);
  }

  private void finishTransaction(String result) {
    mFragment.onTransactionSuccess();
    mFragment.onFinishedResponse(result);
  }


  public void initializeAndActivatePinpad(
          String appName,
          String stoneCode,
          Context context
  ) throws Exception {
    isValidLicence();
    try {
      BasicResult basicResult = new BasicResult();
      String serialNumber = getSerialNumber();
      basicResult.setDeviceSerialNumber(serialNumber);

      if(isDebugLog) {
        Log.d("print", "****initializeAndActivatePinpad");
      }
      mFragment.onMessage("Iniciando ativação");
      Stone.setAppName(appName);
      Map<StoneKeyType, String> stoneKeys = new HashMap<>();
      List<UserModel> userList = StoneStart.init(context, stoneKeys);
      userModel = userList;

      if (userList == null) {
        if (isActivationRunning) {
          if (isDebugLog) {
            Log.d("print", "****Ativação já em andamento, cancelando anterior...");
          }
          abortActivationInternal();
        }
        
        activeApplicationProvider = getActiveApplicationProvider(context);
        isActivationRunning = true;
        
        // Configurar timeout
        setupActivationTimeout(context);
        
        try {
          activeApplicationProvider.activate(stoneCode);
        } catch (Exception activateError) {
          isActivationRunning = false;
          cancelActivationTimeout();
          if (isDebugLog) {
            Log.d("print", "****ERRO ao chamar activate: " + activateError.getMessage());
          }
          BasicResult errorResult = new BasicResult();
          errorResult.setMethod("active");
          errorResult.setResult(999999);
          errorResult.setErrorMessage("Erro ao iniciar ativação: " + activateError.getMessage());
          mFragment.onError(convertBasicResultToJson(errorResult));
          mFragment.onFinishedResponse(convertBasicResultToJson(errorResult));
          throw activateError;
        }

      } else {
        mFragment.onMessage("Terminal ativado");
        basicResult.setMethod("active");
        basicResult.setResult(0);
        basicResult.setMessage("Terminal ativado");
        if(!userModel.isEmpty()) {
          String userModelString = getGson().toJson(userModel.get(0));
          basicResult.setUserModel(userModelString);
        }
        String resultJson = convertBasicResultToJson(basicResult);
        mFragment.onFinishedResponse(resultJson);
        mFragment.onAuthProgress(resultJson);
      }
    } catch (Exception error) {
      Log.d("print", "****ERRO_initializeAndActivatePinpad: " + error.getMessage());
      isActivationRunning = false;
      cancelActivationTimeout();
      BasicResult errorResult = new BasicResult();
      errorResult.setMethod("active");
      errorResult.setResult(999999);
      errorResult.setErrorMessage("Erro ao ativar terminal: " + error.getMessage());
      mFragment.onError(convertBasicResultToJson(errorResult));
      mFragment.onFinishedResponse(convertBasicResultToJson(errorResult));
    }
  }

  public void initializeAndActivatePinPadWithCredentials(
          String appName,
          String stoneCode,
          Map<StoneKeyType, String> stoneKeys,
          Context context
  ) throws Exception {
    isValidLicence();
    try {
      BasicResult basicResult = new BasicResult();
      if(isDebugLog) {
        Log.d("print", "****initializeAndActivatePinPadWithCredentials");
      }
      mFragment.onMessage("Iniciando ativação");
      Stone.setAppName(appName);
      List<UserModel> userList = StoneStart.init(context, stoneKeys);
      userModel = userList;

      if (userList == null) {
        if (isActivationRunning) {
          if (isDebugLog) {
            Log.d("print", "****Ativação já em andamento, cancelando anterior...");
          }
          abortActivationInternal();
        }
        
        activeApplicationProvider = getActiveApplicationProvider(context);
        isActivationRunning = true;
        
        // Configurar timeout
        setupActivationTimeout(context);
        
        try {
          activeApplicationProvider.activate(stoneCode);
        } catch (Exception activateError) {
          isActivationRunning = false;
          cancelActivationTimeout();
          if (isDebugLog) {
            Log.d("print", "****ERRO ao chamar activate: " + activateError.getMessage());
          }
          BasicResult errorResult = new BasicResult();
          errorResult.setMethod("active");
          errorResult.setResult(999999);
          errorResult.setErrorMessage("Erro ao iniciar ativação: " + activateError.getMessage());
          mFragment.onError(convertBasicResultToJson(errorResult));
          mFragment.onFinishedResponse(convertBasicResultToJson(errorResult));
          throw activateError;
        }

      } else {
        mFragment.onMessage("Terminal ativado");
        basicResult.setMethod("active");
        basicResult.setResult(0);
        basicResult.setMessage("Terminal ativado");
        if(!userModel.isEmpty()){
          String userModelString = getGson().toJson(userModel.get(0));
          basicResult.setUserModel(userModelString);
        }
        String resultJson = convertBasicResultToJson(basicResult);
        mFragment.onFinishedResponse(resultJson);
        mFragment.onAuthProgress(resultJson);
      }
    }catch (Exception error){
      Log.d("print", "****ERRO_initializeAndActivatePinPadWithCredentials: " + error.getMessage());
      isActivationRunning = false;
      cancelActivationTimeout();
      BasicResult errorResult = new BasicResult();
      errorResult.setMethod("active");
      errorResult.setResult(999999);
      errorResult.setErrorMessage("Erro ao ativar terminal: " + error.getMessage());
      mFragment.onError(convertBasicResultToJson(errorResult));
      mFragment.onFinishedResponse(convertBasicResultToJson(errorResult));
    }
  }

  public void cancelTransaction(Context context, int idFromBase) throws Exception {
    isValidLicence();
    if(isDebugLog) {
      Log.d("print", "*** cancelTransaction");
    }
    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("cancel");
    try {
      TransactionDAO transactionDAO = new TransactionDAO(context);
      TransactionObject transaction = transactionDAO.findTransactionWithId(idFromBase);
      if(transaction == null) {
        mFragment.onError("Transação não encontrada");
        basicResult.setResult(999999);
        basicResult.setErrorMessage("Transação não encontrada");
        String jsonError = convertBasicResultToJson(basicResult);
        mFragment.onFinishedResponse(jsonError);
        return;
      }
      final CancellationProvider cancellationProvider = getCancellationProvider(context, transaction);
      cancellationProvider.execute();
    } catch (Exception error) {
      Log.d("print", "****ERRO_cancelTransaction: " + error.getMessage());
      mFragment.onMessage("Erro ao cancelar transação");
      basicResult.setResult(999999);
      basicResult.setErrorMessage(error.getMessage());
      mFragment.onError(convertBasicResultToJson(basicResult));
    }
  }

  public void cancelTransactionPIX(Context context) throws Exception {
    isValidLicence();
    if(isDebugLog) {
    Log.d("print", "****** cancelTransactionPIX");
    }
    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("abortPix");
    if (posTransactionProvider != null) {
        posTransactionProvider.abortPayment();
        isAbortRunning = true;
    }
    try {
      final CancellationProvider cancellationProvider = new CancellationProvider(context, currentTransactionObject);
      cancellationProvider.execute();
    } catch (Exception error) {
      Log.d("print", "****ERRO_cancelTransactionPIX: " + error.getMessage());
      mFragment.onMessage("Transação cancelada");
      basicResult.setResult(999999);
    }
  }

  @NonNull
  private CancellationProvider getCancellationProvider(Context context, TransactionObject transaction) {
    if(isDebugLog) {
    Log.d("print", "***** getCancellationProvider");
    }
    mFragment.onMessage("Obtendo dados da transação");
    final CancellationProvider provider = new CancellationProvider(context, transaction);
    mFragment.onMessage("Processando cancelamento");
    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("cancel");

    provider.setConnectionCallback(new StoneCallbackInterface() {
      @Override
      public void onSuccess() {
       mFragment.onMessage("Transação cancelada");
       mFragment.onTransactionSuccess();
       basicResult.setResult(0);
       ResponseCodeEnum responseCode = provider.getResponseCodeEnum();
       basicResult.setMessage(mStoneHelper.getMessageFromResponseCodeEnum(responseCode));
       mFragment.onAuthProgress("Transação cancelada");
       mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
      }
      @Override
      public void onError() {
        String error = currentTransactionObject.getActionCode();
        basicResult.setResult(999999);
        basicResult.setErrorMessage(mStoneHelper.getErrorFromErrorList(provider.getListOfErrors()));
        mFragment.onMessage("Erro ao cancelar transação");
        mFragment.onAuthProgress(error);
        mFragment.onError(convertBasicResultToJson(basicResult));
        mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
      }
    });

    return provider;
  }

  @NonNull
  private ActiveApplicationProvider getActiveApplicationProvider(Context context) {
    ActiveApplicationProvider activeApplicationProvider = new ActiveApplicationProvider(context);

    activeApplicationProvider.setDialogMessage("Ativando o Stone Code");
    activeApplicationProvider.setDialogTitle("Aguarde");
    activeApplicationProvider.useDefaultUI(true);

    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("active");

    mFragment.onMessage("Conectando com terminal");
    activeApplicationProvider.setConnectionCallback(new StoneCallbackInterface() {
      @Override
      public void onSuccess() {
        isActivationRunning = false;
        cancelActivationTimeout();
        mFragment.onMessage("Terminal ativado");
        mFragment.onTransactionSuccess();
        basicResult.setResult(0);
        basicResult.setMessage("Terminal ativado");

        mFragment.onAuthProgress(convertBasicResultToJson(basicResult));
        mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
      }

      @Override
      public void onError() {
        isActivationRunning = false;
        cancelActivationTimeout();
        mFragment.onMessage("Erro ao ativar terminal");
        basicResult.setResult(999999);
        basicResult.setErrorMessage(mStoneHelper.getErrorFromErrorList(activeApplicationProvider.getListOfErrors()));
        mFragment.onError(convertBasicResultToJson(basicResult));
        mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
      }
    });
    return activeApplicationProvider;
  }

  private void setupActivationTimeout(Context context) {
    cancelActivationTimeout();
    
    timeoutRunnable = new Runnable() {
      @Override
      public void run() {
        if (isActivationRunning && activeApplicationProvider != null) {
          if (isDebugLog) {
            Log.d("print", "****TIMEOUT: Ativação demorou mais de " + (ACTIVATION_TIMEOUT_MS / 1000) + " segundos");
          }
          
          isActivationRunning = false;
          BasicResult timeoutResult = new BasicResult();
          timeoutResult.setMethod("active");
          timeoutResult.setResult(999999);
          timeoutResult.setErrorMessage("Timeout: Não foi possível conectar ao terminal. Verifique se o terminal está ligado e próximo.");
          
          mFragment.onMessage("Timeout: Terminal não respondeu");
          mFragment.onError(convertBasicResultToJson(timeoutResult));
          mFragment.onFinishedResponse(convertBasicResultToJson(timeoutResult));
          
          // Tentar abortar a ativação
          abortActivationInternal();
        }
      }
    };
    
    timeoutHandler.postDelayed(timeoutRunnable, ACTIVATION_TIMEOUT_MS);
  }

  private void cancelActivationTimeout() {
    if (timeoutHandler != null && timeoutRunnable != null) {
      timeoutHandler.removeCallbacks(timeoutRunnable);
      timeoutRunnable = null;
    }
  }

  private void abortActivationInternal() {
    if (activeApplicationProvider != null && isActivationRunning) {
      try {
        if (isDebugLog) {
          Log.d("print", "****Abortando ativação do terminal (interno)...");
        }
        // Nota: ActiveApplicationProvider pode não ter método de abort explícito
        // Mas podemos limpar o estado
        activeApplicationProvider = null;
        isActivationRunning = false;
        cancelActivationTimeout();
      } catch (Exception e) {
        if (isDebugLog) {
          Log.d("print", "****ERRO ao abortar ativação: " + e.getMessage());
        }
      }
    }
  }

  public void dispose() {
    abortActivationInternal();
    cancelActivationTimeout();
    if (timeoutHandler != null) {
      timeoutHandler.removeCallbacksAndMessages(null);
    }
  }

  public void checkStatusWithErrorTransaction(TransactionStatusEnum status, Context context) {
    if(isDebugLog) {
    Log.d("print", "**** checkStatusWithErrorTransaction: " + status);
    }
     if(status == null || (status != TransactionStatusEnum.WITH_ERROR && status != TransactionStatusEnum.APPROVED) ) {
       if(posTransactionProvider != null &&  !isAbortRunning) {
         mFragment.onMessage(posTransactionProvider.getMessageFromAuthorize());
         posTransactionProvider.abortPayment();
         isAbortRunning = true;
       }
       return;
     }
     if(status == TransactionStatusEnum.WITH_ERROR) {
       mFragment.onMessage("Revertendo a transação");
       onReversalTransaction(context);
     }
  }

  public void onReversalTransaction(Context context) {
    if(isDebugLog) {
    Log.d("print", "**** onReversalTransaction");
    }
    if (posTransactionProvider != null) {
      TransactionStatusEnum status = posTransactionProvider.getTransactionStatus();
      if(status == TransactionStatusEnum.APPROVED) {
        return;
      }
    }
    BasicResult basicResult = new BasicResult();
    basicResult.setMethod("reversal");
    try {
      ReversalProvider reversalProvider = new ReversalProvider(context);
      reversalProvider.setConnectionCallback(new StoneCallbackInterface() {
        @Override
        public void onSuccess() {
          basicResult.setResult(0);
          basicResult.setMessage("OK");
          mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
          // printReceipt(context, currentTransactionObject, ReceiptType.MERCHANT);
        }

        @Override
        public void onError() {
          basicResult.setResult(999999);
          basicResult.setMessage("ERROR");
          mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
        }
      });
      reversalProvider.execute();
    } catch (Exception error) {
      Log.d("print", "****ERRO_onReversalTransaction: " + error.getMessage());
      basicResult.setResult(999999);
      basicResult.setErrorMessage(error.getMessage());
      basicResult.setMessage("ERROR");
          mFragment.onFinishedResponse(convertBasicResultToJson(basicResult));
    }
  }

    public void getTransactionByInitiatorTransactionKey(Context context, String InitiatorTransactionKey) {
      if(isDebugLog) {
      Log.d("print", "**** getTransactionByInitiatorTransactionKey");
      }
      BasicResult basicResult = new BasicResult();
      basicResult.setMethod("paymentGetTransactionByInitiatorTransactionKey");
      ActionResult actionResult = new ActionResult();
      boolean isPaymentApproved = false;

      try {
        TransactionDAO transactionDAO = new TransactionDAO(context);
        TransactionObject transactionObject = transactionDAO.findTransactionWithInitiatorTransactionKey(InitiatorTransactionKey);

        if (transactionObject == null) {
          handleTransactionError(context, "Transação não encontrada", actionResult, basicResult);
          return;
        }

        basicResult.setResult(0);
        basicResult.setMessage("Transação encontrada");

        if(transactionObject.getTransactionStatus() == TransactionStatusEnum.APPROVED){
          isPaymentApproved = true;
        }

        actionResult.setTransactionStatus(transactionObject.getTransactionStatus().toString());
        actionResult.setMessageFromAuthorize(transactionObject.getAuthorizationCode());
        actionResult.setAuthorizationCode(transactionObject.getAuthorizationCode());

        if(!userModel.isEmpty()){
          String userModelString = getGson().toJson(userModel.get(0));
          actionResult.setUserModel(userModelString);
        }
        actionResult.buildResponseStoneTransaction(transactionObject, isPaymentApproved);
        actionResult.buildResponseStoneTransaction(transactionObject, isPaymentApproved);

        String jsonStoneResult = convertActionToJson(actionResult);

        finishTransaction(jsonStoneResult);

      } catch (Exception error) {
        Log.d("print", "****ERRO_getTransactionByInitiatorTransactionKey: " + error.getMessage());
        handleTransactionError(context, "Erro ao buscar transação", actionResult, basicResult);
      }
    }

    public void getAllTransactions(Context context) {
      if(isDebugLog) {
      Log.d("print", "**** getAllTransactions");
      }
      ActionResult actionResult = new ActionResult();
      try {
        mFragment.onMessage("Obtendo todas as transações");
        TransactionDAO transactionDAO = new TransactionDAO(context);
        transactionObjects = transactionDAO.getAllTransactionsOrderByIdDesc();


        actionResult.setMethod("AllTransactions");

        if(transactionObjects == null || transactionObjects.isEmpty()) {
          transactionObjects = new ArrayList<TransactionObject>();
        }

        actionResult.buildAllTransactions(transactionObjects);
        actionResult.setResult(0);
        String jsonStoneResult = convertActionToJson(actionResult);
        mFragment.onFinishedResponse(jsonStoneResult);
      } catch (Exception e) {
        Log.d("print", "****ERRO_getAllTransactions: " + e.getMessage());
        mFragment.onMessage("Erro ao buscar transações");
        actionResult.buildAllTransactions(transactionObjects);
        actionResult.setResult(999999);
        actionResult.setErrorMessage(e.getMessage());
        String jsonStoneResult = convertActionToJson(actionResult);
        mFragment.onFinishedResponse(jsonStoneResult);
        mFragment.onError(e.getMessage());
      }
    }


    public void customPrinter(String params, Context context) throws Exception {
      isValidLicence();
      if(isDebugLog) {
      Log.d("print", "**** customPrinter");
      }
      mStonePrinter.customPrinter(params, context);
    }

    public void printFromBase64(String params, Context context) throws Exception {
      isValidLicence();
      if(isDebugLog) {
      Log.d("print", "**** printFromBase64");
      }
      mStonePrinter.printFromBase64(params, context);
    }

    public void printWrapPaper(int lines, Context context) {
      if(isDebugLog) {
      Log.d("print", "**** printWrapPaper");
      }
      mStonePrinter.printWrapPaper(lines, context);
    }

    public String getSerialNumber() {
    try {
      String result =  Stone.getPosAndroidDevice().getPosAndroidSerialNumber();
      if ("Mobile".equals(result)) {
        return getSerialNumberAndroid();
      }
      return result;
    }catch (Exception error) {
        return null;
    }
    }

  public String getSerialNumberAndroid() {
    String serial = "unknown";

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serial = Build.getSerial(); // Pode lançar SecurityException se a permissão não estiver concedida
        } else {
            serial = Build.SERIAL; // Deprecated, mas funciona em APIs < 26
        }
    } catch (SecurityException se) {
        serial = "permission_not_granted";
    } catch (Exception e) {
        serial = "error_" + e.getMessage();
    }

    return serial;
}


    public String getPosManufacture() {
    try {
      if(!isInitialized) {
        return null;
      }
      return Stone.getPosAndroidDevice().getPosAndroidManufacturer();
    } catch (Exception error) {
      return null;
    }
    }


  public boolean checkValidLicence(String licenceKey, String licenceInternalKey) throws Exception {
    boolean aux = false;
    BasicResult result = new BasicResult();
    result.setMethod("licence");
    this.mFragment.onMessage("Verificando licença");
    LicenceResult resultLicence =  licenceTech.validateLicence(licenceKey, licenceInternalKey);


    if(resultLicence.getResult() == 0) {
      aux = true;
      this.mFragment.onMessage("Licença ativada");
      result.setResult(0);
      result.setMessage("Licença ativada com sucesso");
      this.mFragment.onFinishedResponse(convertBasicResultToJson(result));
    }

    if(!aux) {
      String errorMessage = resultLicence.getErrorMessage();
      if(errorMessage == null || errorMessage.isEmpty()) {
        errorMessage = "Licença não ativada";
      }
      this.mFragment.onMessage(errorMessage);
      result.setResult(999999);
      result.setErrorMessage(errorMessage);
      this.mFragment.onFinishedResponse(convertBasicResultToJson(result));
      throw new Exception("Licença não ativada, consulte o suporte do plugin.");
    }

    return aux;
  }
}
