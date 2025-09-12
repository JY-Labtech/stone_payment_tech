import 'package:stone_payment_tech/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/app/model/stone_payment_params.dart';
import 'package:stone_payment_tech/app/model/stone_status.dart';
import 'package:stone_payment_tech/interface/model/stone_printer.dart';
import 'package:stone_payment_tech/stone_smart_flutter.dart';

class StoneStreamController {
  final StoneStreamHandler? handler;

  StoneTech get stone => StoneTech.I;
  late StoneCredentialsModel credentials;
  late StonePaymentParams paymentParams;

  StonePaymentType? currentMethod;
  bool isFinishPayment = false;

  Stream<StoneStatus> get stream => handler?.statusStream ?? const Stream.empty();

  StoneStreamController({
    required this.handler,
  });

  Future<void> initialize({
    required StoneCredentialsModel stoneCredentials,
    required StonePaymentParams stonePaymentParams,
  }) async {
    if (handler == null) {
      return;
    }
    credentials = stoneCredentials;
    paymentParams = stonePaymentParams;

    await stone.initPayment(
      handler: handler!,
      licenceKey: credentials.licenceKey,
    );
    isFinishPayment = false;

    if (credentials.qrCodeAuthorization != null && credentials.qrCodeProviderid != null) {
      await stone.payment.activePinpadWithCredentials(
        appName: credentials.appName,
        stoneCode: credentials.stoneCode,
        qrCodeAuthroization: credentials.qrCodeAuthorization ?? '',
        qrCodeProviderid: credentials.qrCodeProviderid ?? '',
      );
      return;
    }
    await stone.payment.activePinpad(
      appName: credentials.appName,
      stoneCode: credentials.stoneCode,
    );
  }

  int _getSaleValueStone(double value) {
    int saleValue = 0;
    final valueFixed = (value * 100).toStringAsFixed(0);
    final valueInt = int.parse(valueFixed);
    saleValue = valueInt;
    return saleValue;
  }

  Future<void> startTransaction({
    required StonePaymentParams params,
    required StoneCredentialsModel credentials,
  }) async {
    isFinishPayment = false;
    handler?.clear();
    if (handler == null) {
      return;
    }
    if (!stone.payment.isPinpadActivated) {
      handler!.setStreamStatus(const StoneStatusActivateRunning(message: 'Ativando Terminal...'));
      await initialize(stoneCredentials: credentials, stonePaymentParams: params);
      return;
    }
    final amountInt = _getSaleValueStone(params.amount);
    currentMethod = params.type;
    switch (currentMethod) {
      case StonePaymentType.credit:
        {
          if (params.installments != null) {
            stone.payment.creditPaymentParc(
              value: amountInt,
              installment: params.installments!,
              printEstablishmentSlip: true,
              printCustomerSlip: false,
            );
          } else {
            stone.payment.creditPayment(
              amountInt,
              printEstablishmentSlip: true,
              printCustomerSlip: false,
            );
          }
        }
        break;
      case StonePaymentType.debit:
        {
          stone.payment.debitPayment(
            amountInt,
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      case StonePaymentType.pix:
        {
          stone.payment.pixPayment(
            amount: amountInt,
            qrCodeAuthroization: credentials.qrCodeAuthorization ?? '',
            qrCodeProviderid: credentials.qrCodeProviderid ?? '',
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      case StonePaymentType.voucher:
        {
          stone.payment.voucherPayment(
            amountInt,
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      default:
        throw Exception('Method not supported');
    }
  }

  void abortTransaction() {
    if (currentMethod == StonePaymentType.pix) {
      stone.payment.abortPIXTransaction();
    } else {
      stone.payment.abortTransaction();
    }
  }

  void printer(StonePrinterParams params) {
    stone.payment.customPrinter(params);
  }

  void printerFromBase64(String base64) {
    stone.payment.printFromBase64(base64);
  }

  void cancelTransaction({required int idFromBase}) {
    handler?.clear();
    stone.payment.cancelTransaction(idFromBase: idFromBase);
  }

  void printCurrentTransaction() {
    stone.payment.printerCurrentTransaction(printCustomerSlip: true);
  }

  void dispose() {
    if (handler == null) {
      return;
    }
    handler!.dispose();
  }

  void onPrinterCustomer(bool value) async {
    stone.payment.printerCurrentTransaction(printCustomerSlip: value);
  }
}
