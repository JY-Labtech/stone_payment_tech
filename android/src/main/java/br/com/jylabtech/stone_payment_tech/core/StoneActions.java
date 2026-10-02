package br.com.jylabtech.stone_payment_tech.core;

/**
 * Operações do SDK Stone disparadas pelo canal. Em produção é implementada
 * sobre o PaymentsPresenter; os resultados assíncronos (transações, ativação,
 * impressão) continuam chegando ao Dart pelos eventos do canal.
 */
public interface StoneActions {
    void activeDebugLog(boolean isDebugLog);

    String getPosSerialNumber();

    String getPosManufacture();

    void customPrinter(String printerParams) throws Exception;

    void printFromBase64(String printerParams) throws Exception;

    void printWrapPaper(int lines);

    void getAllTransactions();

    void reversal();

    void getTransactionByInitiatorTransactionKey(String initiatorTransactionKey);

    void printerCurrentTransaction(boolean printCustomerSlip);

    void printerFromTransactionKey(String transactionKey) throws Exception;

    void activate(String appName, String stoneCode) throws Exception;

    void activateWithCredentials(String appName, String stoneCode, String qrCodeAuthorization, String qrCodeProviderid) throws Exception;

    void abortPix();

    void abort();

    void abortActivation();

    void setPaymentOption(String option);

    void cancelTransaction(int idFromBase) throws Exception;

    void doTransaction(String amount, int typeTransaction, String initiatorTransactionKey, int installment,
                       boolean withInterest, String qrCodeAuthorization, String qrCodeProviderid,
                       boolean printCustomerSlip, boolean printEstablishmentSlip) throws Exception;
}
