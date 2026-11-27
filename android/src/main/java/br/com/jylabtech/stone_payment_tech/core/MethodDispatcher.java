package br.com.jylabtech.stone_payment_tech.core;

/**
 * Despacha um método do canal para o SDK
 * Stone e responde a chamada exatamente uma vez:
 * - getters respondem o valor (String);
 * - as demais operações respondem {@code true} assim que são disparadas no SDK
 *   (o resultado da transação/ativação/impressão continua chegando pelos
 *   eventos do canal, como antes);
 * - exceção ao disparar responde {@code error("ERROR", ...)}.
 */
public class MethodDispatcher {

    public static final String ERROR_CODE = "ERROR";

    static final int TYPE_CREDIT = 1;
    static final int TYPE_DEBIT = 2;
    static final int TYPE_PIX = 3;
    static final int TYPE_VOUCHER = 4;

    private MethodDispatcher() {}

    public static void dispatch(PaymentMethod paymentMethod, MethodArgs args, StoneActions actions, MethodReply result) {
        final MethodReply reply = result instanceof ReplyOnce ? result : new ReplyOnce(result);
        try {
            Object value = run(paymentMethod, args, actions);
            if (value == NOT_IMPLEMENTED) {
                reply.notImplemented();
            } else {
                reply.success(value);
            }
        } catch (Exception e) {
            reply.error(ERROR_CODE, e.getMessage(), null);
        }
    }

    private static final Object NOT_IMPLEMENTED = new Object();

    private static Object run(PaymentMethod paymentMethod, MethodArgs args, StoneActions actions) throws Exception {
        if (paymentMethod == null) {
            return NOT_IMPLEMENTED;
        }
        switch (paymentMethod) {
            case PAYMENT_ACTIVE_DEBUG_LOG:
                boolean isDebugLog = args.argument("isDebugLog");
                actions.activeDebugLog(isDebugLog);
                return true;
            case PAYMENT_GET_SERIAL_NUMBER:
                return actions.getPosSerialNumber();
            case PAYMENT_GET_MANUFACTURE:
                return actions.getPosManufacture();
            case PAYMENT_CUSTOM_PRINTER:
                actions.customPrinter(args.<String>argument("printerParams"));
                return true;
            case PAYMENT_PRINT_BASE64:
                actions.printFromBase64(args.<String>argument("printerParams"));
                return true;
            case PAYMENT_PRINT_WRAP_PAPER:
                int lines = args.argument("lines");
                actions.printWrapPaper(lines);
                return true;
            case PAYMENT_GET_ALL_TRANSACTIONS:
                actions.getAllTransactions();
                return true;
            case PAYMENT_REVERSAL:
                actions.reversal();
                return true;
            case PAYMENT_GET_TRANSACTION_BY_INITIATOR_TRANSACTION_KEY:
                actions.getTransactionByInitiatorTransactionKey(args.<String>argument("InitiatorTransactionKey"));
                return true;
            case PAYMENT_PRINTER_TRANSACTION:
                boolean printCustomerSlip = args.argument("printCustomerSlip");
                actions.printerCurrentTransaction(printCustomerSlip);
                return true;
            case PAYMENT_PRINTER_TRANSACTION_KEY:
                // O Dart envia "transactionKey"; "paymentPrinterTransactionKey" é o nome antigo.
                String transactionKey = args.argument("paymentPrinterTransactionKey");
                if (transactionKey == null) {
                    transactionKey = args.argument("transactionKey");
                }
                actions.printerFromTransactionKey(transactionKey);
                return true;
            case ACTIVE_PINPAD:
                actions.activate(args.<String>argument("appName"), args.<String>argument("stoneCode"));
                return true;
            case ACTIVE_PINPAD_CREDENTIALS:
                actions.activateWithCredentials(
                        args.<String>argument("appName"),
                        args.<String>argument("stoneCode"),
                        args.<String>argument("qrCodeAuthorization"),
                        args.<String>argument("qrCodeProviderid"));
                return true;
            case PAYMENT_ABORT_PIX:
                actions.abortPix();
                return true;
            case PAYMENT_ABORT:
                actions.abort();
                return true;
            case PAYMENT_ABORT_ACTIVATION:
                actions.abortActivation();
                return true;
            case PAYMENT_OPTIONS:
                actions.setPaymentOption(args.<String>argument("option"));
                return true;
            case PAYMENT_CANCEL_TRANSACTION:
                int idFromBase = args.argument("idFromBase");
                actions.cancelTransaction(idFromBase);
                return true;
            case PAYMENT_DEBIT:
                return doTransaction(TYPE_DEBIT, args, actions, false);
            case PAYMENT_PIX:
                return doTransaction(TYPE_PIX, args, actions, true);
            case PAYMENT_CREDIT:
            case PAYMENT_CREDIT_PARC:
                return doTransaction(TYPE_CREDIT, args, actions, false);
            case PAYMENT_VOUCHER:
                return doTransaction(TYPE_VOUCHER, args, actions, false);
            default:
                return NOT_IMPLEMENTED;
        }
    }

    private static Object doTransaction(int type, MethodArgs args, StoneActions actions, boolean withQrCode) throws Exception {
        String amount = args.argument("amount");
        int installment = args.argument("installment");
        boolean withInterest = args.argument("withInterest");
        boolean withCustomerSlip = args.argument("printCustomerSlip");
        boolean withEstablishmentSlip = args.argument("printEstablishmentSlip");
        String initiatorKey = args.argument("initiatorTransactionKey");
        String qrCodeAuthorization = withQrCode ? args.<String>argument("qrCodeAuthorization") : null;
        String qrCodeProviderid = withQrCode ? args.<String>argument("qrCodeProviderid") : null;
        actions.doTransaction(amount, type, initiatorKey, installment, withInterest,
                qrCodeAuthorization, qrCodeProviderid, withCustomerSlip, withEstablishmentSlip);
        return true;
    }
}
